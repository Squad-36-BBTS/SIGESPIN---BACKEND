-- Metadados anteriores continuam disponíveis para consulta e vínculo documental.
ALTER TABLE evidencias
    ADD COLUMN tamanho_bytes BIGINT,
    ADD COLUMN sha256 VARCHAR(64),
    ADD COLUMN enviado_por_id BIGINT REFERENCES usuarios(id),
    ADD COLUMN armazenamento VARCHAR(16),
    ADD CONSTRAINT validar_metadados_upload CHECK (
        armazenamento IS NULL OR (
            armazenamento IN ('LOCAL', 'S3')
            AND tamanho_bytes IS NOT NULL AND tamanho_bytes BETWEEN 1 AND 10485760
            AND sha256 IS NOT NULL AND sha256 ~ '^[a-f0-9]{64}$'
            AND enviado_por_id IS NOT NULL
        )
    );
