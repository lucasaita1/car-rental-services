CREATE TABLE tb_users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    cnh        VARCHAR(255) NULL,
    cpf        VARCHAR(255) NULL,
    email      VARCHAR(255) NULL,
    name       VARCHAR(255) NULL,
    password   VARCHAR(255) NULL,
    photo_path VARCHAR(255) NULL,
    role       ENUM('ADMIN', 'USER') NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE tb_seeds (
    name        VARCHAR(255) NOT NULL,
    executed_at DATETIME(6)  NULL,
    PRIMARY KEY (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
