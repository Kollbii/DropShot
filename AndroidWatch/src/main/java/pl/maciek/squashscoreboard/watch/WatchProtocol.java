package pl.maciek.squashscoreboard.watch;

import java.util.UUID;

/**
 * Shared command and live-state protocol between AndroidWatch and the phone app (pl.maciek.squashscoreboard).
 * Uses the same 4 command names as ZeppOS plus live JSON state and match-end messages:
 * - POINT_1: Add 1 point to Player 1
 * - POINT_2: Add 1 point to Player 2
 * - UNDO: Undo the last point
 * - RESET: Start, advance to next game, or stop/reset the match
 */
public final class WatchProtocol {
    public static final String PHONE_PACKAGE_NAME = "pl.maciek.squashscoreboard";
    public static final String SERVICE_NAME = "DropShotWatchBridge";
    public static final UUID SERVICE_UUID = UUID.fromString("d8095407-4b6d-4765-9b3f-282820000001");

    public static final String CMD_POINT_1 = "POINT_1";
    public static final String CMD_POINT_2 = "POINT_2";
    public static final String CMD_UNDO = "UNDO";
    public static final String CMD_RESET = "RESET";
    public static final String CMD_TOGGLE_PAUSE = "TOGGLE_PAUSE";
    public static final String CMD_TOGGLE_SIDE = "TOGGLE_SIDE";

    public static final String MSG_STATE = "STATE";
    public static final String MSG_ACK = "ACK";
    public static final String MSG_MATCH_ENDED = "MATCH_ENDED";

    public static final int DEFAULT_P1_COLOR = 0xFF1479D1;
    public static final int DEFAULT_P2_COLOR = 0xFFD8424F;
    public static final int[] RAINBOW_STOPS = new int[]{
        0xFFFF5059, 0xFFFFA838, 0xFFF4DE4A, 0xFF57CE87, 0xFF48A9EF, 0xFF9B6AE8, 0xFFFF5059
    };

    public static final class WatchState {
        public final String playerOneInitials;
        public final String playerTwoInitials;
        public final int playerOnePoints;
        public final int playerTwoPoints;
        public final int playerOneSets;
        public final int playerTwoSets;
        public final int targetSets;
        public final int activeServer;
        public final String serviceSide;
        public final int playerOneColor;
        public final boolean playerOneRainbow;
        public final int playerTwoColor;
        public final boolean playerTwoRainbow;
        public final String actionLabel;
        public final boolean gameOrMatchBall;
        public final boolean matchFinished;
        public final boolean paused;

        public WatchState(
            String playerOneInitials,
            String playerTwoInitials,
            int playerOnePoints,
            int playerTwoPoints,
            int playerOneSets,
            int playerTwoSets,
            int targetSets,
            int activeServer,
            String serviceSide,
            int playerOneColor,
            boolean playerOneRainbow,
            int playerTwoColor,
            boolean playerTwoRainbow,
            String actionLabel,
            boolean gameOrMatchBall,
            boolean matchFinished,
            boolean paused
        ) {
            this.playerOneInitials = playerOneInitials;
            this.playerTwoInitials = playerTwoInitials;
            this.playerOnePoints = playerOnePoints;
            this.playerTwoPoints = playerTwoPoints;
            this.playerOneSets = playerOneSets;
            this.playerTwoSets = playerTwoSets;
            this.targetSets = targetSets;
            this.activeServer = activeServer;
            this.serviceSide = serviceSide;
            this.playerOneColor = playerOneColor;
            this.playerOneRainbow = playerOneRainbow;
            this.playerTwoColor = playerTwoColor;
            this.playerTwoRainbow = playerTwoRainbow;
            this.actionLabel = actionLabel;
            this.gameOrMatchBall = gameOrMatchBall;
            this.matchFinished = matchFinished;
            this.paused = paused;
        }

        public static WatchState defaultState() {
            return new WatchState(
                "P1",
                "P2",
                0,
                0,
                0,
                0,
                2,
                1,
                "R",
                DEFAULT_P1_COLOR,
                false,
                DEFAULT_P2_COLOR,
                false,
                "STOP",
                false,
                false,
                false
            );
        }
    }

    public static final class MatchEndedEvent {
        public final String winnerName;
        public final String finalScore;

        public MatchEndedEvent(String winnerName, String finalScore) {
            this.winnerName = winnerName;
            this.finalScore = finalScore;
        }
    }

    private WatchProtocol() {}

    public static boolean isAllowedCommand(String command) {
        return CMD_POINT_1.equals(command)
            || CMD_POINT_2.equals(command)
            || CMD_UNDO.equals(command)
            || CMD_RESET.equals(command)
            || CMD_TOGGLE_PAUSE.equals(command)
            || CMD_TOGGLE_SIDE.equals(command);
    }

    public static String encodeCommandLine(String command) {
        if (!isAllowedCommand(command)) {
            throw new IllegalArgumentException("Unsupported watch command: " + command);
        }
        return "{\"command\":\"" + command + "\"}\n";
    }

    public static boolean parseAcceptedResponse(String responseLine) {
        if (!isValidJsonEnvelope(responseLine)) return false;
        return extractBooleanField(responseLine, "accepted", false);
    }

    public static boolean parseAckBall(String responseLine) {
        if (!isValidJsonEnvelope(responseLine)) return false;
        return extractBooleanField(responseLine, "ball", false);
    }

    public static String parseMessageType(String line) {
        if (!isValidJsonEnvelope(line)) return null;
        String type = extractStringField(line, "type", null);
        if (type != null) return type;
        if (line.contains("\"accepted\":")) return MSG_ACK;
        return null;
    }

    public static WatchState parseWatchState(String line) {
        if (!isValidJsonEnvelope(line)) return null;
        String type = parseMessageType(line);
        if (!MSG_STATE.equals(type)) return null;

        String n1 = extractStringField(line, "n1", "P1");
        String n2 = extractStringField(line, "n2", "P2");
        int p1 = Math.max(0, extractIntField(line, "p1", 0));
        int p2 = Math.max(0, extractIntField(line, "p2", 0));
        int s1 = Math.max(0, extractIntField(line, "s1", 0));
        int s2 = Math.max(0, extractIntField(line, "s2", 0));
        int targetSets = Math.max(1, extractIntField(line, "targetSets", 2));
        int srv = extractIntField(line, "srv", 1) == 2 ? 2 : 1;
        String sideRaw = extractStringField(line, "side", "R");
        String side = "L".equals(sideRaw) ? "L" : "R";
        int c1 = extractIntField(line, "c1", DEFAULT_P1_COLOR);
        boolean r1 = extractBooleanField(line, "r1", false);
        int c2 = extractIntField(line, "c2", DEFAULT_P2_COLOR);
        boolean r2 = extractBooleanField(line, "r2", false);
        String actRaw = extractStringField(line, "act", null);
        if (actRaw == null) actRaw = extractStringField(line, "action", "STOP");
        String act = ("START".equals(actRaw) || "NEXT".equals(actRaw)) ? actRaw : "STOP";
        boolean ball = extractBooleanField(line, "ball", false);
        boolean fin = extractBooleanField(line, "fin", false);
        boolean paused = extractBooleanField(line, "paused", false);

        return new WatchState(
            n1.isEmpty() ? "P1" : n1,
            n2.isEmpty() ? "P2" : n2,
            p1,
            p2,
            s1,
            s2,
            targetSets,
            srv,
            side,
            c1,
            r1,
            c2,
            r2,
            act,
            ball,
            fin,
            paused
        );
    }

    public static MatchEndedEvent parseMatchEnded(String line) {
        if (!isValidJsonEnvelope(line)) return null;
        if (!MSG_MATCH_ENDED.equals(parseMessageType(line))) return null;
        String winner = extractStringField(line, "winner", "PLAYER");
        String score = extractStringField(line, "score", "0 - 0");
        return new MatchEndedEvent(
            winner.isEmpty() ? "PLAYER" : winner,
            score.isEmpty() ? "0 - 0" : score
        );
    }

    /**
     * Formats games (sets) won as dots, for example "●●○".
     */
    public static String formatSetsIndicator(int wonSets, int targetSets) {
        int clampedWon = Math.max(0, wonSets);
        int dotsCount = Math.min(5, Math.max(1, Math.max(targetSets, clampedWon)));
        StringBuilder sb = new StringBuilder(dotsCount);
        for (int i = 0; i < dotsCount; i++) {
            sb.append(i < clampedWon ? '●' : '○');
        }
        return sb.toString();
    }

    private static boolean isValidJsonEnvelope(String line) {
        if (line == null) return false;
        String trimmed = line.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 1024
            && trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    private static String extractStringField(String json, String key, String fallback) {
        String token = "\"" + key + "\":\"";
        int idx = json.indexOf(token);
        if (idx < 0) return fallback;
        int start = idx + token.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return fallback;
    }

    private static int extractIntField(String json, String key, int fallback) {
        String token = "\"" + key + "\":";
        int idx = json.indexOf(token);
        if (idx < 0) return fallback;
        int start = idx + token.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-') {
                end++;
            } else {
                break;
            }
        }
        if (end <= start) return fallback;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean extractBooleanField(String json, String key, boolean fallback) {
        String token = "\"" + key + "\":";
        int idx = json.indexOf(token);
        if (idx < 0) return fallback;
        int start = idx + token.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (json.startsWith("true", start)) return true;
        if (json.startsWith("false", start)) return false;
        return fallback;
    }
}
