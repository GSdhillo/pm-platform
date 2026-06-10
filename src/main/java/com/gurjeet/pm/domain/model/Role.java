package com.gurjeet.pm.domain.model;

public enum Role {
    VIEWER(0), MEMBER(1), PROJECT_LEAD(2), ADMIN(3);
    private final int rank;
    Role(int rank) { this.rank = rank; }
    public int rank() { return rank; }
}
