ALTER TABLE car_model
    ADD COLUMN daily_rate DECIMAL(10, 2) NULL AFTER year,
    ADD COLUMN details    JSON           NULL AFTER daily_rate;

ALTER TABLE tb_rentals
    ADD COLUMN daily_rate      DECIMAL(10, 2) NULL AFTER return_date,
    ADD COLUMN estimated_total DECIMAL(12, 2) NULL AFTER daily_rate,
    ADD COLUMN total_amount    DECIMAL(12, 2) NULL AFTER estimated_total;
