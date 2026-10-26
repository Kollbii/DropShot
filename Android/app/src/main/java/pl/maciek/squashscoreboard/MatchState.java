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
    private final Deque<Snapshot> history = new ArrayDeque<>();
    private final ArrayList<Integer> pointWinners = new ArrayList<>();

    public void addPoint(int player) {
        if (matchFinished) return;
        history.push(snapshot());
        if (player == 1) playerOnePoints++; else playerTwoPoints++;
        pointWinners.add(player);
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

    public void resume(int pointsPerSet, int setsPerMatch, int p1Points, int p2Points, int p1Sets, int p2Sets, boolean finished, List<Integer> winners) {
        pointsToWinSet = pointsPerSet; setsToWinMatch = setsPerMatch;
        playerOnePoints = p1Points; playerTwoPoints = p2Points;
        playerOneSets = p1Sets; playerTwoSets = p2Sets; matchFinished = finished;
        history.clear(); pointWinners.clear(); pointWinners.addAll(winners);
    }
    public void continueWithThreeSets() { setsToWinMatch += 3; playerOnePoints = 0; playerTwoPoints = 0; matchFinished = false; }
    public void conclude() {
        if (matchFinished) return;
        setsToWinMatch = Math.max(1, (playerOneSets + playerTwoSets + 2) / 2);
        matchFinished = true;
    }

    private Snapshot snapshot() { return new Snapshot(playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets, matchFinished, pointWinners.size()); }
    private void restore(Snapshot v) {
        playerOnePoints = v.playerOnePoints; playerTwoPoints = v.playerTwoPoints;
        playerOneSets = v.playerOneSets; playerTwoSets = v.playerTwoSets; matchFinished = v.matchFinished;
        while (pointWinners.size() > v.pointCount) pointWinners.remove(pointWinners.size() - 1);
    }

    private static final class Snapshot {
        final int playerOnePoints, playerTwoPoints, playerOneSets, playerTwoSets;
        final boolean matchFinished;
        final int pointCount;
        Snapshot(int p1, int p2, int s1, int s2, boolean finished, int count) {
            playerOnePoints = p1; playerTwoPoints = p2; playerOneSets = s1; playerTwoSets = s2; matchFinished = finished; pointCount = count;
        }
    }
}
