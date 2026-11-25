CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_user_active_slot
    ON appointments (user_id, appointment_date)
    WHERE status <> 'CANCELLED';
