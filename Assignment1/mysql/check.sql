USE cinema_booking;
SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank;
SHOW CREATE TABLE booking_detail;
SELECT booking_status,COUNT(*) AS bookings FROM booking GROUP BY booking_status;
