CREATE TABLE car_model (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    color       VARCHAR(255) NULL,
    model       VARCHAR(255) NULL,
    plate       VARCHAR(255) NULL,
    rental_date DATE         NULL,
    return_date DATE         NULL,
    year        INT          NOT NULL,
    user_id     BIGINT       NULL,
    status      TINYINT      NULL,
    photo_path  VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tb_rentals (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    car_id               BIGINT       NOT NULL,
    car_model            VARCHAR(255) NULL,
    car_plate            VARCHAR(255) NULL,
    expected_return_date DATE         NULL,
    rental_date          DATE         NOT NULL,
    return_date          DATE         NULL,
    status               ENUM('ACTIVE', 'FINISHED') NOT NULL,
    user_cpf             VARCHAR(255) NULL,
    user_email           VARCHAR(255) NULL,
    user_id              BIGINT       NOT NULL,
    user_name            VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_rental_car_status (car_id, status),
    KEY idx_rental_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
