package com.campusloop.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户实体（ADT 设计：不可变对象，状态变化通过服务层重建对象落库）。
 */
public final class User {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final String contact;
    private final Role role;
    private final UserStatus status;
    private final int creditScore;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLoginAt;

    public User(Long id, String username, String passwordHash, String nickname, String contact,
                Role role, UserStatus status, int creditScore,
                LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.contact = contact;
        this.role = Objects.requireNonNull(role, "role");
        this.status = Objects.requireNonNull(status, "status");
        this.creditScore = creditScore;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lastLoginAt = lastLoginAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getContact() {
        return contact;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /** 带指定字段复制出新对象，保持不可变性。 */
    public User withProfile(String nickname, String contact) {
        return new User(id, username, passwordHash, nickname, contact, role, status,
                creditScore, createdAt, lastLoginAt);
    }

    public User withPasswordHash(String newHash) {
        return new User(id, username, newHash, nickname, contact, role, status,
                creditScore, createdAt, lastLoginAt);
    }

    public User withStatus(UserStatus newStatus) {
        return new User(id, username, passwordHash, nickname, contact, role, newStatus,
                creditScore, createdAt, lastLoginAt);
    }

    public User withCreditScore(int newScore) {
        return new User(id, username, passwordHash, nickname, contact, role, status,
                newScore, createdAt, lastLoginAt);
    }

    public User withLastLoginAt(LocalDateTime time) {
        return new User(id, username, passwordHash, nickname, contact, role, status,
                creditScore, createdAt, time);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', nickname='" + nickname
                + "', role=" + role + ", status=" + status + ", creditScore=" + creditScore + '}';
    }
}
