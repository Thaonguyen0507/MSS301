ALTER TABLE booking_detail ADD active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE booking_detail ADD active_seat VARCHAR(5) GENERATED ALWAYS AS (CASE WHEN active THEN seat_code ELSE NULL END) STORED;
CREATE UNIQUE INDEX uk_confirmed_seat ON booking_detail(showtime_id, active_seat);
