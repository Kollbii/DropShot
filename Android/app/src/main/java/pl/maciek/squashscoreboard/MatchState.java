package pl.maciek.squashscoreboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Match rules and reversible score state. */
public final class MatchState {
    private int pointsToWinSet = 11;
    private int setsToWinMatch = 3;
    private int playerOnePoints;
    private int playerTwoPoints;
    private int playerOneSets;
    private int playerTwoSets;
    private boolean matchFinished;
    private boolean serviceSideInverted;
    private final Deque<Snapshot> history = new ArrayDeque<>();
    private final ArrayList<Integer> pointWinners = new ArrayList<>();

    public void addPoint(int player) {
        if (matchFinished) return;
        history.push(snapshot());
        int prevServer = getActiveServer();
        if (player == 1) playerOnePoints++; else playerTwoPoints++;
        pointWinners.add(player);
        if (player != prevServer) {
            serviceSideInverted = false;
        }
        finishSetIfNeeded();
    }

    private void finishSetIfNeeded() {
        int difference = Math.abs(playerOnePoints - playerTwoPoints);
        if (Math.max(playerOnePoints, playerTwoPoints) < pointsToWinSet || difference < 2) return;
        if (playerOnePoints > playerTwoPoints) playerOneSets++; else playerTwoSets++;
        if (playerOneSets == setsToWinMatch || playerTwoSets == setsToWinMatch) {
            matchFinished = true;
        } else {
            playerOnePoints = 0;
            playerTwoPoints = 0;
            serviceSideInverted = false;
        }
    }

    public void configure(int pointsPerSet, int setsPerMatch) {
        pointsToWinSet = pointsPerSet;
        setsToWinMatch = setsPerMatch;
        reset();
    }

    public void undo() { if (!history.isEmpty()) restore(history.pop()); }
    public void reset() {
        playerOnePoints = 0; playerTwoPoints = 0;
        playerOneSets = 0; playerTwoSets = 0;
        matchFinished = false; history.clear(); pointWinners.clear();
        serviceSideInverted = false;
    }

    public int getPointsToWinSet() { return pointsToWinSet; }
    public int getSetsToWinMatch() { return setsToWinMatch; }
    public int getSetsInMatch() { return setsToWinMatch * 2 - 1; }
    public int getPlayerOnePoints() { return playerOnePoints; }
    public int getPlayerTwoPoints() { return playerTwoPoints; }
    public int getPlayerOneSets() { return playerOneSets; }
    public int getPlayerTwoSets() { return playerTwoSets; }
    public boolean isMatchFinished() { return matchFinished; }
    public int getWinner() { return playerOneSets > playerTwoSets ? 1 : 2; }
    public boolean hasPoints() { return !pointWinners.isEmpty(); }
    public List<Integer> getPointWinners() { return new ArrayList<>(pointWinners); }

    /** Returns the active server (1 or 2). Winner of the previous point serves next; Player 1 starts. */
    public int getActiveServer() {
        if (pointWinners.isEmpty()) return 1;
        return pointWinners.get(pointWinners.size() - 1);
    }

    /**
     * Toggles the current service box side ("R" <-> "L") chosen by the active server.
     */
    public void toggleServiceSide() {
        if (!matchFinished) {
            serviceSideInverted = !serviceSideInverted;
        }
    }

    /**
     * Returns the current squash service box side ("R" or "L").
     * A new server starts on "R" (or chosen box) and alternates "R" <-> "L" on each consecutive point won in the same game.
     */
    public String getServiceSide() {
        int a = 0;
        int b = 0;
        int currentServer = 0;
        boolean rightBox = true;
        for (int winner : pointWinners) {
            if (winner == 1) a++; else b++;
            if (Math.max(a, b) >= pointsToWinSet && Math.abs(a - b) >= 2) {
                // Game finished; next game starts fresh on the right service box
                a = 0;
                b = 0;
                currentServer = winner;
                rightBox = true;
            } else if (winner == currentServer) {
                rightBox = !rightBox;
            } else {
                currentServer = winner;
                rightBox = true;
            }
        }
        boolean effectiveSide = serviceSideInverted ? !rightBox : rightBox;
        return effectiveSide ? "R" : "L";
    }

    /** Returns true when either player is currently on game ball or match ball. */
    public boolean isGameOrMatchBall() {
        if (matchFinished) return false;
        boolean p1Ball = playerOnePoints >= pointsToWinSet - 1 && playerOnePoints > playerTwoPoints;
        boolean p2Ball = playerTwoPoints >= pointsToWinSet - 1 && playerTwoPoints > playerOnePoints;
        return p1Ball || p2Ball;
    }

    public void resume(int pointsPerSet, int setsPerMatch, int p1Points, int p2Points, int p1Sets, int p2Sets, boolean finished, List<Integer> winners) {
        pointsToWinSet = pointsPerSet; setsToWinMatch = setsPerMatch;
        playerOnePoints = p1Points; playerTwoPoints = p2Points;
        playerOneSets = p1Sets; playerTwoSets = p2Sets; matchFinished = finished;
        history.clear(); pointWinners.clear(); pointWinners.addAll(winners);
        serviceSideInverted = false;
    }
    public void continueWithThreeSets() { setsToWinMatch += 3; playerOnePoints = 0; playerTwoPoints = 0; matchFinished = false; serviceSideInverted = false; }
    public void conclude() {
        if (matchFinished) return;
        setsToWinMatch = Math.max(1, (playerOneSets + playerTwoSets + 2) / 2);
        matchFinished = true;
        serviceSideInverted = false;
    }

    private Snapshot snapshot() { return new Snapshot(playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets, matchFinished, pointWinners.size(), serviceSideInverted); }
    private void restore(Snapshot v) {
        playerOnePoints = v.playerOnePoints; playerTwoPoints = v.playerTwoPoints;
        playerOneSets = v.playerOneSets; playerTwoSets = v.playerTwoSets; matchFinished = v.matchFinished;
        serviceSideInverted = v.serviceSideInverted;
        while (pointWinners.size() > v.pointCount) pointWinners.remove(pointWinners.size() - 1);
    }

    private static final class Snapshot {
        final int playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets;
        final boolean matchFinished;
        final int pointCount;
        final boolean serviceSideInverted;
        Snapshot(int p1, int p2, int s1, int s2, boolean finished, int count, boolean inverted) {
            playerOnePoints = p1; playerTwoPoints = p2; playerOneSets = s1; playerTwoSets = s2; matchFinished = finished; pointCount = count;
            serviceSideInverted = inverted;
        }
    }
}
