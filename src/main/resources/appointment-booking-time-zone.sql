UPDATE appointments a
SET booking_time_zone = w.time_zone,
    version = a.version + 1
FROM weekly_availability w
WHERE w.user_id = a.user_id
  AND a.guest_email IS NOT NULL
  AND a.booking_time_zone IS NULL
^^^
