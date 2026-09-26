ALTER TABLE car_model
    ADD COLUMN status_text ENUM('AVAILABLE', 'MAINTENANCE', 'RENTED') NULL AFTER status;

UPDATE car_model
SET status_text = CASE status
                      WHEN 0 THEN 'AVAILABLE'
                      WHEN 1 THEN 'RENTED'
                      WHEN 2 THEN 'MAINTENANCE'
                  END;

ALTER TABLE car_model DROP COLUMN status;

ALTER TABLE car_model RENAME COLUMN status_text TO status;
