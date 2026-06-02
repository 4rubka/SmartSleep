package com.smartsleep.model;

import java.util.UUID;

public final class PlayerStats {
    private final UUID uuid;
    private String name;
    private int voteStarts;
    private int successfulStarts;
    private int yesVotes;
    private int noVotes;
    private int participation;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public int voteStarts() {
        return voteStarts;
    }

    public int successfulStarts() {
        return successfulStarts;
    }

    public int yesVotes() {
        return yesVotes;
    }

    public int noVotes() {
        return noVotes;
    }

    public int participation() {
        return participation;
    }

    public void voteStarts(int value) {
        voteStarts = value;
    }

    public void successfulStarts(int value) {
        successfulStarts = value;
    }

    public void yesVotes(int value) {
        yesVotes = value;
    }

    public void noVotes(int value) {
        noVotes = value;
    }

    public void participation(int value) {
        participation = value;
    }

    public void addVoteStart() {
        voteStarts++;
    }

    public void addSuccessfulStart() {
        successfulStarts++;
    }

    public void addYesVote() {
        yesVotes++;
    }

    public void addNoVote() {
        noVotes++;
    }

    public void addParticipation() {
        participation++;
    }
}
