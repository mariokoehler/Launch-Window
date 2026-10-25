package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.NetworkClient;
import de.mkoehler.launchwindow.net.NetworkConstants;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.CreateAccountRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.FrontPagesRequest;
import de.mkoehler.launchwindow.net.messages.FrontPagesResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.net.messages.SubmitStatementRequest;
import de.mkoehler.launchwindow.net.messages.SubmitStatementResponse;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.SetFundsAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * <p>Minimal interactive console client for exercising a running
 * {@code GameServer} over real KryoNet sockets, without needing the libGDX
 * client (see {@code design.md} &sect;5's "CLI/console dev client" M1
 * slice) - just enough to log in, submit an action, and read state back.
 * Never shipped; a development tool only.</p>
 *
 * @author Mario Koehler
 */
public final class DevClient {

    private static final String PROMPT = "> ";

    /**
     * Not instantiable; this class only holds the process entry point and
     * its static helpers.
     */
    private DevClient() {
    }

    /**
     * Connects to a server, logs in from credentials typed at the console,
     * then runs an interactive command loop until the user quits or the
     * connection drops.
     *
     * @param args optionally the server host as the first argument
     *             (default {@code localhost}) and the TCP port as the
     *             second (default {@link NetworkConstants#TCP_PORT})
     * @throws IOException if the console cannot be read
     */
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : NetworkConstants.TCP_PORT;
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        System.out.println("Launch Window dev client - connecting to " + host + ":" + port + "...");
        try (NetworkClient client = new NetworkClient()) {
            client.connect(host, port);
            if (!logIn(client, console)) {
                return;
            }
            runCommandLoop(client, console);
        }
        System.out.println("Disconnected.");
    }

    /**
     * Prompts for login credentials, sends the
     * handshake, and prints the server's response.
     *
     * @param client  the connected client to send the handshake on
     * @param console the console to read credentials from
     * @return {@code true} if the handshake was accepted
     * @throws IOException if the console cannot be read or the handshake
     *                      fails
     */
    private static boolean logIn(NetworkClient client, BufferedReader console) throws IOException {
        System.out.print("Login: ");
        String login = console.readLine();
        System.out.print("Password: ");
        String password = console.readLine();

        // A create logs in when the account exists and the password is its own, so the dev client
        // keeps making accounts on first use, as the handshake once did (review.md 2.3).
        HandshakeResponse response = client.sendAndReceive(
            new CreateAccountRequest(AppVersion.getVersion(), login, password), HandshakeResponse.class);
        System.out.println(response.message());
        return response.accepted();
    }

    /**
     * Reads and handles commands from the console until {@code quit}/
     * {@code exit} is entered or the console reaches end of input.
     *
     * @param client  the authenticated client to send commands over
     * @param console the console to read commands from
     * @throws IOException if the console cannot be read
     */
    private static void runCommandLoop(NetworkClient client, BufferedReader console) throws IOException {
        printHelp();
        String line;
        while (true) {
            System.out.print(PROMPT);
            line = console.readLine();
            if (line == null) {
                return;
            }
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("quit") || line.equals("exit")) {
                return;
            } else if (line.equals("help")) {
                printHelp();
            } else if (line.equals("state")) {
                handleState(client);
            } else if (line.startsWith("claim ")) {
                handleClaim(client, line);
            } else if (line.equals("pages")) {
                handlePages(client);
            } else if (line.equals("react") || line.startsWith("react ")) {
                handleReact(client, line);
            } else if (DevViews.isView(line)) {
                FetchStateResponse response = client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
                System.out.println(response.success() ? DevViews.render(line, response.state())
                    : "Failed: " + response.message());
            } else {
                handleAction(client, line);
            }
        }
    }

    /**
     * Sends a {@link FetchStateRequest} and prints the result.
     *
     * @param client the client to send the request on
     * @throws IOException if the request fails
     */
    private static void handleState(NetworkClient client) throws IOException {
        FetchStateResponse response = client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
        if (response.success()) {
            System.out.println(formatSnapshot(response.state()));
        } else if (ClaimSlotRequest.SLOT_CLAIM_REQUIRED.equals(response.message())) {
            System.out.println("No slot in this game yet - claim one: claim <NASA|SOVIET> <agencyName> <directorName>");
        } else {
            System.out.println("Failed: " + response.message());
        }
    }

    /**
     * Parses a funds amount and sends a {@link SetFundsAction} declaring
     * it, printing the result.
     *
     * @param client        the client to send the request on
     * @param fundsArgument the text after the {@code funds } command word
     * @throws IOException if the request fails
     */
    private static void handleClaim(NetworkClient client, String line) throws IOException {
        String[] words = line.strip().split("\\s+");
        if (words.length != 4) {
            System.out.println("Usage: claim <NASA|SOVIET> <agencyName> <directorName>");
            return;
        }
        Archetype archetype;
        try {
            archetype = Archetype.valueOf(words[1].toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.out.println("Archetype must be NASA or SOVIET.");
            return;
        }
        ClaimSlotResponse response = client.sendAndReceive(new ClaimSlotRequest(archetype, words[2], words[3]),
            ClaimSlotResponse.class);
        System.out.println(response.success() ? response.message() : "Failed: " + response.message());
    }

    /**
     * Lists the front pages printed in the game, newest first, with the player's statements for the open
     * turn ({@code ui-spec.md} &sect;4.18).
     *
     * @param client the client to send the request on
     * @throws IOException if the request fails
     */
    private static void handlePages(NetworkClient client) throws IOException {
        FrontPagesResponse response = client.sendAndReceive(new FrontPagesRequest(), FrontPagesResponse.class);
        if (!response.success()) {
            System.out.println("Failed: " + response.message());
            return;
        }
        response.pages().stream().sorted(java.util.Comparator.comparingInt(FrontPage::gameTurn).reversed())
            .forEach(page -> System.out.println("  " + page.id() + "  (" + page.kind() + ", turn " + page.gameTurn()
                + ", " + page.achievers().stream().map(FrontPage.Achiever::agencyName).toList() + ")"));
        for (FrontPage.Statement statement : response.yours()) {
            System.out.println("  yours: " + statement.pageId() + " " + statement.stance()
                + (statement.quote().isEmpty() ? "" : " \"" + statement.quote() + "\""));
        }
    }

    /**
     * Gives, changes or withdraws a statement on a rival's First for the open turn
     * ({@code ui-spec.md} &sect;4.18.1).
     *
     * @param client the client to send the request on
     * @param line   the command line
     * @throws IOException if a request fails
     */
    private static void handleReact(NetworkClient client, String line) throws IOException {
        GameInfoResponse info = client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class);
        if (!info.success()) {
            System.out.println("Failed: " + info.message());
            return;
        }
        SubmitStatementRequest request;
        try {
            request = parseReact(Math.toIntExact(info.gameTurn()), line);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        SubmitStatementResponse response = client.sendAndReceive(request, SubmitStatementResponse.class);
        System.out.println(response.success() ? "OK: " + response.yours() : "Refused: " + response.message());
    }

    /**
     * Parses {@code react <pageId> <stance|withdraw> [quote...]} into a request for the open turn. The stance
     * is any {@link FrontPage.Stance} name, in any case; the quote is the rest of the line, sent as typed so
     * the server's checks can be tried.
     *
     * @param openTurn the open turn
     * @param line     the command line
     * @return the request
     * @throws IllegalArgumentException with the usage, if the line doesn't parse
     */
    static SubmitStatementRequest parseReact(int openTurn, String line) {
        String[] words = line.strip().split("\\s+", 4);
        if (words.length < 3) {
            throw new IllegalArgumentException(REACT_USAGE);
        }
        if (words[2].equalsIgnoreCase("withdraw")) {
            return new SubmitStatementRequest(openTurn, words[1], null, "");
        }
        FrontPage.Stance stance;
        try {
            stance = FrontPage.Stance.valueOf(words[2].toUpperCase(java.util.Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(REACT_USAGE);
        }
        return new SubmitStatementRequest(openTurn, words[1], stance, words.length > 3 ? words[3] : "");
    }

    private static final String REACT_USAGE = "Usage: react <pageId> <enthusiastic|polite|next|downplay|no_comment"
        + "|withdraw> [quote...] - 'pages' lists the page ids.";

    private static void handleAction(NetworkClient client, String line) throws IOException {
        PlayerAction action;
        try {
            action = DevCommands.parse(line);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage() + " - type 'help' for the list.");
            return;
        }
        SubmitActionResponse response =
            client.sendAndReceive(new SubmitActionRequest(action), SubmitActionResponse.class);
        System.out.println(response.success() ? "OK: " + response.message() : "Refused: " + response.message());
    }

    /**
     * Prints the list of available console commands.
     */

    private static void printHelp() {
        System.out.println("Commands: claim <NASA|SOVIET> <agencyName> <directorName> | state | help | quit");
        System.out.println("Front pages: pages | react <pageId> <stance|withdraw> [quote...]");
        System.out.println(DevViews.HELP);
        System.out.println(DevCommands.HELP);
    }

    /**
     * Parses the numeric argument to the {@code funds} command.
     *
     * @param fundsArgument the text after the {@code funds } command word
     * @return the parsed funds amount
     * @throws NumberFormatException if it is not a whole number
     */
    static long parseFundsAmount(String fundsArgument) {
        return Long.parseLong(fundsArgument.strip());
    }

    /**
     * Formats a player snapshot for display at the console.
     *
     * @param snapshot the snapshot to format
     * @return a single-line, human-readable summary
     */
    static String formatSnapshot(PlayerSnapshot snapshot) {
        return String.format(
            "%s (%s) - funds: %d, prestige: %d, funding/season: %d, season: %d, astronauts: %d, SET: %d, mission control: %d",
            snapshot.getAgencyName(), snapshot.getArchetype(),
            snapshot.getFunds(), snapshot.getPrestige(), snapshot.getFundingRatePerSeason(), snapshot.getSeason(),
            snapshot.getAstronauts().size(), snapshot.getSetSpecialists().size(),
            snapshot.getMissionControlOfficers().size());
    }
}
