ALTER TABLE tb_users
    ADD COLUMN cnh_document_path        VARCHAR(255) NULL AFTER photo_path,
    ADD COLUMN cnh_document_uploaded_at DATETIME(6)  NULL AFTER cnh_document_path;
