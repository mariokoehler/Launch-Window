package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Dossier;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.Tidbit;

import java.util.Objects;

/**
 * What one agency's intelligence service gets out of a resolution, one line each
 * ({@code design.md} &sect;2.9): settled once for every agency before anyone's season pass
 * ({@link IntelligenceBoard}), then applied by each agency's own pass ({@link Intelligence}).
 *
 * @param kind          what happened
 * @param operationId   the operation it settles, or {@code null} for a tidbit or a catch of theirs
 * @param operationKind the operation's kind, or {@code null}
 * @param rivalId       the rival's player id
 * @param rivalAgency   the rival's agency name
 * @param area          an assessment's area, or {@code null}
 * @param dossier       the new dossier, for {@link Kind#DOSSIER}
 * @param file          the new personnel file, for {@link Kind#PERSONNEL_FILE}
 * @param tidbit        the tidbit, for {@link Kind#TIDBIT}
 * @param level         the rival's level learned: their counter-intelligence for {@link Kind#CAUGHT},
 *                      their Intelligence level for {@link Kind#CAUGHT_THEIRS} and a plans dossier; else -1
 * @author Mario Koehler
 */
public record IntelFinding(Kind kind, String operationId, IntelOperation.Kind operationKind, String rivalId,
                           String rivalAgency, IntelArea area, Dossier dossier, PersonnelFile file, Tidbit tidbit,
                           int level) {

    /**
     * What happened.
     */
    public enum Kind {
        /** An assessment succeeded: a new dossier. */
        DOSSIER,
        /** A personnel file succeeded. */
        PERSONNEL_FILE,
        /** A passive find. */
        TIDBIT,
        /** An operation failed and learned nothing, and wasn't caught. */
        FAILED,
        /** An operation failed and our agent was caught: prestige lost, their level learned. */
        CAUGHT,
        /** We caught a rival's agent: their Intelligence level learned. */
        CAUGHT_THEIRS,
        /** The target is gone or isn't a rival, or has nobody in the group: nothing happened. */
        NO_TARGET,
        /** Queued too late to be settled with the turn: nothing happened. */
        NOT_SETTLED
    }

    /**
     * Checks the kind.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public IntelFinding {
        Objects.requireNonNull(kind, "kind must not be null");
    }

    /**
     * A finding about one of this agency's own operations.
     *
     * @param kind        what happened
     * @param operation   the operation
     * @param rivalAgency the rival's agency name, or {@code null}
     * @param level       the level learned, or -1
     * @return the finding
     */
    static IntelFinding about(Kind kind, IntelOperation operation, String rivalAgency, int level) {
        return new IntelFinding(kind, operation.id(), operation.kind(), operation.targetId(), rivalAgency,
            operation.area(), null, null, null, level);
    }
}
