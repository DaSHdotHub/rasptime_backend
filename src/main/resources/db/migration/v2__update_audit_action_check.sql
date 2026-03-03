ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS audit_log_action_check;

ALTER TABLE audit_log
    ADD CONSTRAINT audit_log_action_check
        CHECK (action IN (
            'CLOCK_IN',
            'CLOCK_OUT',
            'USER_CREATED',
            'USER_DELETED',
            'LOGIN_FAILED',
            'USER_UPDATED',
            'CLOCK_OUT_AUTO',
            'UNKNOWN_RFID'
        ));
