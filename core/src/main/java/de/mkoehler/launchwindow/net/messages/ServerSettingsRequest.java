package de.mkoehler.launchwindow.net.messages;

/**
 * Asks for the server's settings ({@code ui-spec.md} &sect;4.22, G21). Every admin request carries the admin
 * password, so nothing about the unlock is kept per connection (the client reconnects on its own).
 *
 * @param password the admin password
 * @author Mario Koehler
 */
public record ServerSettingsRequest(String password) {

    @Override
    public String toString() {
        return "ServerSettingsRequest[password=***]";
    }
}
