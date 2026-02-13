package io.github.dashdothub.rasptime_backend.entity;

public enum AuditAction {
    CLOCK_IN,
    CLOCK_OUT,
    USER_CREATED,
    USER_DELETED,
    LOGIN_FAILED,
    USER_UPDATED, CLOCK_OUT_AUTO, UNKNOWN_RFID
}
