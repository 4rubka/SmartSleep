package com.smartsleep.model;

public final class GlobalStats {
    private int nightsSkipped;
    private int rainSkipped;
    private int successfulVotes;
    private int failedVotes;

    public int nightsSkipped() {
        return nightsSkipped;
    }

    public int rainSkipped() {
        return rainSkipped;
    }

    public int successfulVotes() {
        return successfulVotes;
    }

    public int failedVotes() {
        return failedVotes;
    }

    public void nightsSkipped(int value) {
        nightsSkipped = value;
    }

    public void rainSkipped(int value) {
        rainSkipped = value;
    }

    public void successfulVotes(int value) {
        successfulVotes = value;
    }

    public void failedVotes(int value) {
        failedVotes = value;
    }

    public void addNightSkipped() {
        nightsSkipped++;
    }

    public void addRainSkipped() {
        rainSkipped++;
    }

    public void addSuccessfulVote() {
        successfulVotes++;
    }

    public void addFailedVote() {
        failedVotes++;
    }
}
