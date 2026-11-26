DO $$
BEGIN
    LOCK TABLE appointments IN ACCESS EXCLUSIVE MODE;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'appointments'::regclass
          AND conname = 'ex_appointments_user_active_interval'
    ) THEN
        CREATE EXTENSION IF NOT EXISTS btree_gist;
        ALTER TABLE appointments
            ADD CONSTRAINT chk_appointments_duration_minutes
                CHECK (duration_minutes BETWEEN 1 AND 1440),
            ADD CONSTRAINT ex_appointments_user_active_interval
                EXCLUDE USING gist (
                    user_id WITH =,
                    tsrange(
                        appointment_date,
                        appointment_date + COALESCE(duration_minutes, 30) * INTERVAL '1 minute',
                        '[)'
                    ) WITH &&
                ) WHERE (status <> 'CANCELLED');
        DROP INDEX IF EXISTS ux_appointments_user_active_slot;
    END IF;
END
$$
^^^
