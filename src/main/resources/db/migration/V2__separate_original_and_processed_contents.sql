CREATE TABLE original_contents (
    id CHAR(36) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    source VARCHAR(100) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    revision INT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    title VARCHAR(1000) NOT NULL,
    publisher VARCHAR(255) NULL,
    language VARCHAR(16) NOT NULL,
    raw_body LONGTEXT NULL,
    attributes_json LONGTEXT NULL,
    published_at TIMESTAMP(6) NOT NULL,
    collected_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_original_contents_revision UNIQUE (content_type, source, external_id, revision)
);

CREATE INDEX idx_original_contents_type_published_at
    ON original_contents (content_type, published_at);

CREATE TABLE processed_contents (
    id CHAR(36) NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    source VARCHAR(100) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    revision INT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    effective_date DATE NULL,
    published_at TIMESTAMP(6) NOT NULL,
    collected_at TIMESTAMP(6) NOT NULL,
    payload LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_processed_contents_revision UNIQUE (content_type, source, external_id, revision)
);

CREATE INDEX idx_processed_contents_type_effective_date
    ON processed_contents (content_type, effective_date);

CREATE TABLE processed_content_originals (
    processed_content_id CHAR(36) NOT NULL,
    original_content_id CHAR(36) NOT NULL,
    PRIMARY KEY (processed_content_id, original_content_id),
    CONSTRAINT fk_processed_content_originals_processed FOREIGN KEY (processed_content_id) REFERENCES processed_contents (id),
    CONSTRAINT fk_processed_content_originals_original FOREIGN KEY (original_content_id) REFERENCES original_contents (id)
);
