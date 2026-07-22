CREATE TABLE content_documents (
    id CHAR(36) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    source VARCHAR(100) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    revision INT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    collected_at TIMESTAMP(6) NOT NULL,
    effective_date DATE NULL,
    published_at TIMESTAMP(6) NULL,
    payload LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_content_documents_revision UNIQUE (content_type, source, external_id, revision)
);

CREATE INDEX idx_content_documents_type_effective_date
    ON content_documents (content_type, effective_date);

CREATE INDEX idx_content_documents_type_published_at
    ON content_documents (content_type, published_at);
