package com.smartsleep.vote;

public enum VoteType {
    NIGHT("Night"),
    RAIN("Rain");

    private final String display;

    VoteType(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
