package pl.maciek.squashscoreboard.watch;

import java.util.UUID;

/**
 * Shared command protocol between AndroidWatch and the phone app (pl.maciek.squashscoreboard).
 * Uses the same 4 command names and JSON payload format as ZeppOS:
 * - POINT_1: Add 1 point to Player 1
 * - POINT_2: Add 1 point to Player 2
 * - UNDO: Undo the last point
 * - RESET: Finish current match and start a new match
 */
public final class WatchProtocol {
    public static final String PHONE_PACKAGE_NAME = "pl.maciek.squashscoreboard";
    public static final String SERVICE_NAME = "DropShotWatchBridge";
    public static final UUID SERVICE_UUID = UUID.fromString("d8095407-4b6d-4765-9b3f-282820000001");

    public static final String CMD_POINT_1 = "POINT_1";
    public static final String CMD_POINT_2 = "POINT_2";
    public static final String CMD_UNDO = "UNDO";
    public static final String CMD_RESET = "RESET";

    private WatchProtocol() {}

    public static boolean isAllowedCommand(String command) {
        return CMD_POINT_1.equals(command)
            || CMD_POINT_2.equals(command)
            || CMD_UNDO.equals(command)
            || CMD_RESET.equals(command);
    }

    public static String encodeCommandLine(String command) {
        if (!isAllowedCommand(command)) {
            throw new IllegalArgumentException("Unsupported watch command: " + command);
        }
        return "{\"command\":\"" + command + "\"}\n";
    }

    public static boolean parseAcceptedResponse(String responseLine) {
        if (responseLine == null) return false;
        String trimmed = responseLine.trim();
        if (trimmed.length() < 2 || trimmed.length() > 256) return false;
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return false;
        String compact = trimmed.replace(" ", "").replace("\t", "");
        return compact.contains("\"accepted\":true");
    }
}

