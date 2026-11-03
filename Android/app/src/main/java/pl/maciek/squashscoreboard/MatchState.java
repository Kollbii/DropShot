package pl.maciek.squashscoreboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Match rules and reversible score state. */
public final class MatchState {
    private int pointsToWinSet = 11;
    private int setsToWinMatch = 3;
    private int initialServer = 1;
    private int playerOnePoints;
    private int playerTwoPoints;
    private int playerOneSets;
    private int playerTwoSets;
    private boolean matchFinished;
    private boolean currentRightBox = true;
    private boolean canChooseSide = true;
    private final Deque<Snapshot> history = new ArrayDeque<>();
    private final ArrayList<Integer> pointWinners = new ArrayList<>();
    private final ArrayList<Character> pointSides = new ArrayList<>();

    public void addPoint(int player) {
        if (matchFinished) return;
        if (player != 1 && player != 2) return;
        history.push(snapshot());
        int prevServer = getActiveServer();
        char servedBox = currentRightBox ? 'R' : 'L';
        if (player == 1) playerOnePoints++; else playerTwoPoints++;
        pointWinners.add(player);
        pointSides.add(servedBox);

        int difference = Math.abs(playerOnePoints - playerTwoPoints);
        boolean setCompleted = Math.max(playerOnePoints, playerTwoPoints) >= pointsToWinSet && difference >= 2;
        if (setCompleted) {
            if (playerOnePoints > playerTwoPoints) playerOneSets++; else playerTwoSets++;
            if (playerOneSets == setsToWinMatch || playerTwoSets == setsToWinMatch) {
                matchFinished = true;
                canChooseSide = false;
            } else {
                playerOnePoints = 0;
                playerTwoPoints = 0;
                currentRightBox = true;
                canChooseSide = true;
            }
        } else if (player != prevServer) {
            // Handout: the new server starts on R and can freely switch R <-> L before their first point
            currentRightBox = true;
            canChooseSide = true;
        } else {
            // Streak continues: service box alternates automatically and manual switching is locked
            currentRightBox = !currentRightBox;
            canChooseSide = false;
        }
    }

    public void configure(int pointsPerSet, int setsPerMatch) {
        pointsToWinSet = pointsPerSet;
        setsToWinMatch = setsPerMatch;
        reset();
    }

    public void undo() {
        if (!history.isEmpty()) {
            restore(history.pop());
        }
    }

    public void reset() {
        initialServer = 1;
        playerOnePoints = 0;
        playerTwoPoints = 0;
        playerOneSets = 0;
        playerTwoSets = 0;
        matchFinished = false;
        currentRightBox = true;
        canChooseSide = true;
        history.clear();
        pointWinners.clear();
        pointSides.clear();
    }

    public int getPointsToWinSet() { return pointsToWinSet; }
    public int getSetsToWinMatch() { return setsToWinMatch; }
    public int getSetsInMatch() { return setsToWinMatch * 2 - 1; }
    public int getInitialServer() { return initialServer; }
    public int getPlayerOnePoints() { return playerOnePoints; }
    public int getPlayerTwoPoints() { return playerTwoPoints; }
    public int getPlayerOneSets() { return playerOneSets; }
    public int getPlayerTwoSets() { return playerTwoSets; }
    public boolean isMatchFinished() { return matchFinished; }
    public int getWinner() { return playerOneSets > playerTwoSets ? 1 : 2; }
    public boolean hasPoints() { return !pointWinners.isEmpty(); }
    public List<Integer> getPointWinners() { return new ArrayList<>(pointWinners); }

    public String getEncodedPointSides() {
        StringBuilder sb = new StringBuilder(pointSides.size());
        for (int i = 0; i < pointSides.size(); i++) {
            sb.append(pointSides.get(i).charValue());
        }
        return sb.toString();
    }

    /** Returns true only at 0-0 in Game 1 before the first point of the match is scored. */
    public boolean canSelectInitialServer() {
        return !matchFinished && pointWinners.isEmpty();
    }

    /** Selects the starting server (1 or 2) before the first point of the match is scored. */
    public boolean selectInitialServer(int player) {
        if (!canSelectInitialServer() || (player != 1 && player != 2)) {
            return false;
        }
        if (initialServer != player) {
            initialServer = player;
            currentRightBox = true;
            canChooseSide = true;
        }
        return true;
    }

    /** Switches the starting server between Player 1 and Player 2 at 0-0 before the first point. */
    public boolean switchInitialServer() {
        return selectInitialServer(initialServer == 1 ? 2 : 1);
    }

    /** Returns the active server (1 or 2). Winner of the previous point serves next; initialServer starts. */
    public int getActiveServer() {
        if (pointWinners.isEmpty()) return initialServer;
        return pointWinners.get(pointWinners.size() - 1);
    }

    /**
     * Returns true when the active server is allowed to freely switch between [R] and [L]:
     * - At 0-0 at the start of a game, or
     * - Immediately after winning a handout (first point of their serve turn).
     * Returns false after the server scores a point on their own serve (streak locked).
     */
    public boolean canChooseServiceSide() {
        return !matchFinished && canChooseSide;
    }

    /**
     * Toggles the current service box side ("R" <-> "L") if choosing is unlocked.
     */
    public boolean toggleServiceSide() {
        if (!canChooseServiceSide()) {
            return false;
        }
        currentRightBox = !currentRightBox;
        return true;
    }

    /**
     * Returns the current squash service box side ("R" or "L").
     */
    public String getServiceSide() {
        return currentRightBox ? "R" : "L";
    }

    /** Returns true when either player is currently on game ball or match ball. */
    public boolean isGameOrMatchBall() {
        if (matchFinished) return false;
        boolean p1Ball = playerOnePoints >= pointsToWinSet - 1 && playerOnePoints > playerTwoPoints;
        boolean p2Ball = playerTwoPoints >= pointsToWinSet - 1 && playerTwoPoints > playerOnePoints;
        return p1Ball || p2Ball;
    }

    public void resume(
        int pointsPerSet,
        int setsPerMatch,
        int p1Points,
        int p2Points,
        int p1Sets,
        int p2Sets,
        boolean finished,
        List<Integer> winners
    ) {
        resume(pointsPerSet, setsPerMatch, p1Points, p2Points, p1Sets, p2Sets, finished, winners, 1, null);
    }

    public void resume(
        int pointsPerSet,
        int setsPerMatch,
        int p1Points,
        int p2Points,
        int p1Sets,
        int p2Sets,
        boolean finished,
        List<Integer> winners,
        int startServer,
        String encodedSides
    ) {
        pointsToWinSet = pointsPerSet;
        setsToWinMatch = setsPerMatch;
        initialServer = (startServer == 2) ? 2 : 1;
        playerOnePoints = 0;
        playerTwoPoints = 0;
        playerOneSets = 0;
        playerTwoSets = 0;
        matchFinished = false;
        currentRightBox = true;
        canChooseSide = true;
        history.clear();
        pointWinners.clear();
        pointSides.clear();

        if (winners != null) {
            for (int i = 0; i < winners.size(); i++) {
                int w = winners.get(i);
                if (w != 1 && w != 2) continue;
                if (encodedSides != null && i < encodedSides.length()) {
                    char sideChar = encodedSides.charAt(i);
                    if (sideChar == 'L' || sideChar == 'R') {
                        currentRightBox = (sideChar == 'R');
                    }
                }
                addPoint(w);
            }
        }
        history.clear();
        playerOnePoints = p1Points;
        playerTwoPoints = p2Points;
        playerOneSets = p1Sets;
        playerTwoSets = p2Sets;
        matchFinished = finished;
        if (finished) {
            canChooseSide = false;
        }
    }

    public void continueWithThreeSets() {
        setsToWinMatch += 3;
        playerOnePoints = 0;
        playerTwoPoints = 0;
        matchFinished = false;
        currentRightBox = true;
        canChooseSide = true;
    }

    public void conclude() {
        if (matchFinished) return;
        setsToWinMatch = Math.max(1, (playerOneSets + playerTwoSets + 2) / 2);
        matchFinished = true;
        canChooseSide = false;
    }

    private Snapshot snapshot() {
        return new Snapshot(
            initialServer,
            playerOnePoints,
            playerTwoPoints,
            playerOneSets,
            playerTwoSets,
            matchFinished,
            pointWinners.size(),
            currentRightBox,
            canChooseSide
        );
    }

    private void restore(Snapshot v) {
        initialServer = v.initialServer;
        playerOnePoints = v.playerOnePoints;
        playerTwoPoints = v.playerTwoPoints;
        playerOneSets = v.playerOneSets;
        playerTwoSets = v.playerTwoSets;
        matchFinished = v.matchFinished;
        currentRightBox = v.currentRightBox;
        canChooseSide = v.canChooseSide;
        while (pointWinners.size() > v.pointCount) {
            pointWinners.remove(pointWinners.size() - 1);
        }
        while (pointSides.size() > v.pointCount) {
            pointSides.remove(pointSides.size() - 1);
        }
    }

    private static final class Snapshot {
        final int initialServer, playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets;
        final boolean matchFinished;
        final int pointCount;
        final boolean currentRightBox;
        final boolean canChooseSide;

        Snapshot(
            int initSrv,
            int p1,
            int p2,
            int s1,
            int s2,
            boolean finished,
            int count,
            boolean rightBox,
            boolean chooseSide
        ) {
            initialServer = initSrv;
            playerOnePoints = p1;
            playerTwoPoints = p2;
            playerOneSets = s1;
            playerTwoSets = s2;
            matchFinished = finished;
            pointCount = count;
            currentRightBox = rightBox;
            canChooseSide = chooseSide;
        }
    }
}
