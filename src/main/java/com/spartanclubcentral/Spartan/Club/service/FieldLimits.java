package com.spartanclubcentral.Spartan.Club.service;

/**
 * Single source of truth for maximum text lengths (SPR-05).
 *
 * The validators, the JPA @Column lengths and database/schema.sql all use
 * these numbers, so a value that passes validation always fits in MySQL.
 * If you change a number here, change schema.sql too.
 */
public final class FieldLimits {

    public static final int CATEGORY_NAME = 50;
    public static final int CLUB_NAME = 100;
    public static final int DESCRIPTION = 2000;
    public static final int MEETING_TIME = 100;
    /** 254 is the longest valid email address (RFC 5321). */
    public static final int EMAIL = 254;
    public static final int CONTACT_INFO = 255;
    public static final int URL = 255;
    public static final int PERSON_NAME = 100;
    public static final int POSITION = 50;
    public static final int MESSAGE = 1000;
    /** BCrypt always produces a 60-character hash. */
    public static final int PASSWORD_HASH = 60;
    /** SHA-256 written as hex is 64 characters. */
    public static final int TOKEN_HASH = 64;
    public static final int PASSWORD_MIN = 8;
    /** BCrypt ignores everything after 72 bytes, so longer passwords are rejected. */
    public static final int PASSWORD_MAX = 72;
    public static final int SEARCH = 100;
    /** Officer role, e.g. "President" (FR-13). */
    public static final int OFFICER_ROLE = 50;
    /** Most officers one club can list. */
    public static final int MAX_OFFICERS = 10;
    /** Club events (FR-14). */
    public static final int EVENT_TITLE = 100;
    public static final int EVENT_DESCRIPTION = 1000;
    public static final int EVENT_LOCATION = 150;
    /** Events can be scheduled at most this many days ahead. */
    public static final int EVENT_MAX_DAYS_AHEAD = 366;
    /** Club pictures (FR-15). */
    public static final int IMAGE_FILENAME = 64;
    public static final int IMAGE_ALT = 150;
    public static final long IMAGE_MAX_BYTES = 2L * 1024 * 1024;
    /** Uploaded pictures are resized to at most this width. */
    public static final int IMAGE_MAX_WIDTH = 1200;
    /** Refuse images bigger than this before decoding them (protects memory). */
    public static final int IMAGE_MAX_SOURCE_PIXELS = 40_000_000;

    private FieldLimits() {
    }
}
