-- ============================================================
-- FinDART PoC Revised MariaDB DDL
-- Architecture: MongoDB + MariaDB
--
-- MongoDB owns:
--   1. contents
--      - raw/normalized document body
--      - document-level summaries and LLM analysis payload
--      - model/prompt/token metadata
--
--   2. summaries
--      - intraday/day/week/month/quarter/half-year/year summary body
--      - variable structured payload
--      - generation metadata and detailed provenance
--
-- MariaDB owns:
--   - companies, sectors, categories, keywords
--   - MongoDB content/summary references
--   - content-company/sector/category/keyword relations
--   - tracked issue definitions and matching relations
--   - economic indicators, stock prices, financial facts
--   - scheduled events and retry jobs
--
-- Important:
--   - MongoDB _id is an application-generated UUID string.
--   - The same UUID is stored in MariaDB CHAR(36) columns.
--   - All DATETIME(6) values are stored in UTC.
--   - This script contains no JSON columns.
--   - Run against an empty schema or migrate existing tables separately.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- V3 is the cut-over point from the MariaDB-only PoC. The old tables
-- are intentionally removed because their document payloads now belong
-- to MongoDB. This migration assumes the reset/cut-over was approved.
DROP TABLE IF EXISTS processed_content_originals;
DROP TABLE IF EXISTS processed_contents;
DROP TABLE IF EXISTS original_contents;
DROP TABLE IF EXISTS content_documents;


-- ============================================================
-- 1. MASTER DATA
-- ============================================================

-- ------------------------------------------------------------
-- 1.1 Listed company master
--
-- PoC assumption:
--   One primary KOSPI/KOSDAQ ticker per company.
--   Preferred shares or multiple listed securities can later be
--   separated into a securities table.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS companies (
    id                          CHAR(36)      NOT NULL,
    name                        VARCHAR(255)  NOT NULL,
    ticker                      VARCHAR(20)   NOT NULL,
    market_type                 VARCHAR(20)   NOT NULL, -- KOSPI / KOSDAQ

    business_registration_no    VARCHAR(20)   NULL,
    corporate_registration_no   VARCHAR(20)   NULL,
    dart_corp_code              VARCHAR(20)   NULL,

    listed_at                   DATE          NULL,
    delisted_at                 DATE          NULL,
    is_active                   TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_companies_market_ticker (
        market_type,
        ticker
    ),
    UNIQUE KEY uk_companies_business_registration_no (
        business_registration_no
    ),
    UNIQUE KEY uk_companies_corporate_registration_no (
        corporate_registration_no
    ),
    UNIQUE KEY uk_companies_dart_corp_code (
        dart_corp_code
    ),

    KEY idx_companies_active_market (
        is_active,
        market_type
    ),
    KEY idx_companies_name (
        name
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 1.2 Hierarchical sector master
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sectors (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    parent_id                   BIGINT        NULL,

    classification_standard     VARCHAR(40)   NOT NULL DEFAULT 'CUSTOM',
    code                        VARCHAR(80)   NOT NULL,
    name                        VARCHAR(255)  NOT NULL,
    is_active                   TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sectors_standard_code (
        classification_standard,
        code
    ),

    KEY idx_sectors_parent (
        parent_id
    ),
    KEY idx_sectors_active_name (
        is_active,
        name
    ),

    CONSTRAINT fk_sectors_parent
        FOREIGN KEY (parent_id)
        REFERENCES sectors (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 1.3 Company-sector relation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS company_sectors (
    company_id                  CHAR(36)      NOT NULL,
    sector_id                   BIGINT        NOT NULL,
    is_primary                  TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        company_id,
        sector_id
    ),

    KEY idx_company_sectors_sector (
        sector_id,
        company_id
    ),

    CONSTRAINT fk_company_sectors_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_company_sectors_sector
        FOREIGN KEY (sector_id)
        REFERENCES sectors (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 1.4 Hierarchical content category master
--
-- Examples:
--   POLITICS, ECONOMY, SOCIETY, CULTURE, INTERNATIONAL,
--   INDUSTRY, TECHNOLOGY
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    parent_id                   BIGINT        NULL,

    code                        VARCHAR(80)   NOT NULL,
    name                        VARCHAR(200)  NOT NULL,
    is_active                   TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_categories_code (
        code
    ),

    KEY idx_categories_parent (
        parent_id
    ),
    KEY idx_categories_active_name (
        is_active,
        name
    ),

    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id)
        REFERENCES categories (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 1.5 Keyword dictionary
--
-- normalized_term must be normalized by the application.
-- Example:
--   display_term     = '원·달러 환율'
--   normalized_term  = '원달러환율'
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS keywords (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    normalized_term             VARCHAR(255)  NOT NULL,
    display_term                VARCHAR(255)  NOT NULL,
    language                    VARCHAR(16)   NOT NULL DEFAULT 'ko',
    is_active                   TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_keywords_language_normalized (
        language,
        normalized_term
    ),

    KEY idx_keywords_display_term (
        display_term
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 2. MONGODB CONTENT REFERENCES AND RELATIONS
-- ============================================================

-- ------------------------------------------------------------
-- 2.1 MongoDB contents reference
--
-- content_id = MongoDB findart.contents._id
--
-- Document body, title, publisher, URL, LLM summaries and detailed
-- metadata are stored only in MongoDB.
--
-- MariaDB keeps a minimal operational projection required for
-- relational filtering and cross-database reconciliation.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_references (
    content_id                  CHAR(36)      NOT NULL,
    mongo_database              VARCHAR(80)   NOT NULL DEFAULT 'findart',
    mongo_collection            VARCHAR(80)   NOT NULL DEFAULT 'contents',

    content_type                VARCHAR(40)   NOT NULL,
    source                      VARCHAR(100)  NOT NULL,
    external_id                 VARCHAR(255)  NOT NULL,
    revision                    INT           NOT NULL DEFAULT 1,

    published_at                DATETIME(6)   NOT NULL,
    collected_at                DATETIME(6)   NOT NULL,

    processing_status           VARCHAR(30)   NOT NULL DEFAULT 'COLLECTED',
    analysis_version            VARCHAR(80)   NULL,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                  DATETIME(6)   NULL,

    PRIMARY KEY (content_id),

    UNIQUE KEY uk_content_references_source_revision (
        content_type,
        source,
        external_id,
        revision
    ),

    KEY idx_content_references_type_published (
        content_type,
        published_at
    ),
    KEY idx_content_references_source_published (
        source,
        published_at
    ),
    KEY idx_content_references_processing_status (
        processing_status,
        collected_at
    ),
    KEY idx_content_references_deleted (
        deleted_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 2.2 Content-company relation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_company_links (
    content_id                  CHAR(36)      NOT NULL,
    company_id                  CHAR(36)      NOT NULL,

    relevance_score             DECIMAL(8,5)  NULL,
    sentiment_code              VARCHAR(30)   NULL,
    mention_count               INT           NULL,
    is_primary                  TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        content_id,
        company_id
    ),

    KEY idx_content_company_links_company (
        company_id,
        content_id
    ),

    CONSTRAINT fk_content_company_links_content
        FOREIGN KEY (content_id)
        REFERENCES content_references (content_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_content_company_links_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 2.3 Content-sector relation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_sector_links (
    content_id                  CHAR(36)      NOT NULL,
    sector_id                   BIGINT        NOT NULL,

    relevance_score             DECIMAL(8,5)  NULL,
    sentiment_code              VARCHAR(30)   NULL,
    is_primary                  TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        content_id,
        sector_id
    ),

    KEY idx_content_sector_links_sector (
        sector_id,
        content_id
    ),

    CONSTRAINT fk_content_sector_links_content
        FOREIGN KEY (content_id)
        REFERENCES content_references (content_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_content_sector_links_sector
        FOREIGN KEY (sector_id)
        REFERENCES sectors (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 2.4 Content-category relation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_category_links (
    content_id                  CHAR(36)      NOT NULL,
    category_id                 BIGINT        NOT NULL,

    confidence_score            DECIMAL(8,5)  NULL,
    is_primary                  TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        content_id,
        category_id
    ),

    KEY idx_content_category_links_category (
        category_id,
        content_id
    ),

    CONSTRAINT fk_content_category_links_content
        FOREIGN KEY (content_id)
        REFERENCES content_references (content_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_content_category_links_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 2.5 Content-keyword relation
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_keyword_links (
    content_id                  CHAR(36)      NOT NULL,
    keyword_id                  BIGINT        NOT NULL,

    relevance_score             DECIMAL(8,5)  NULL,
    occurrence_count            INT           NULL,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        content_id,
        keyword_id
    ),

    KEY idx_content_keyword_links_keyword (
        keyword_id,
        content_id
    ),

    CONSTRAINT fk_content_keyword_links_content
        FOREIGN KEY (content_id)
        REFERENCES content_references (content_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_content_keyword_links_keyword
        FOREIGN KEY (keyword_id)
        REFERENCES keywords (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 3. TRACKED ISSUES
-- ============================================================

-- ------------------------------------------------------------
-- 3.1 User-defined tracked issue
--
-- No user/account relation in the current PoC.
--
-- status examples:
--   ACTIVE / PAUSED / ARCHIVED
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tracked_issues (
    id                          CHAR(36)      NOT NULL,
    name                        VARCHAR(255)  NOT NULL,
    description                 TEXT          NULL,

    query_text                  TEXT          NULL,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    priority                    INT           NOT NULL DEFAULT 0,

    tracking_start_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    tracking_end_at             DATETIME(6)   NULL,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),
    archived_at                 DATETIME(6)   NULL,

    PRIMARY KEY (id),

    KEY idx_tracked_issues_status_priority (
        status,
        priority
    ),
    KEY idx_tracked_issues_tracking_period (
        tracking_start_at,
        tracking_end_at
    ),
    KEY idx_tracked_issues_archived (
        archived_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 3.2 Tracked issue-company condition
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tracked_issue_companies (
    issue_id                    CHAR(36)      NOT NULL,
    company_id                  CHAR(36)      NOT NULL,
    is_excluded                 TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        issue_id,
        company_id
    ),

    KEY idx_tracked_issue_companies_company (
        company_id,
        issue_id
    ),

    CONSTRAINT fk_tracked_issue_companies_issue
        FOREIGN KEY (issue_id)
        REFERENCES tracked_issues (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_tracked_issue_companies_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 3.3 Tracked issue-sector condition
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tracked_issue_sectors (
    issue_id                    CHAR(36)      NOT NULL,
    sector_id                   BIGINT        NOT NULL,
    is_excluded                 TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        issue_id,
        sector_id
    ),

    KEY idx_tracked_issue_sectors_sector (
        sector_id,
        issue_id
    ),

    CONSTRAINT fk_tracked_issue_sectors_issue
        FOREIGN KEY (issue_id)
        REFERENCES tracked_issues (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_tracked_issue_sectors_sector
        FOREIGN KEY (sector_id)
        REFERENCES sectors (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 3.4 Tracked issue-category condition
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tracked_issue_categories (
    issue_id                    CHAR(36)      NOT NULL,
    category_id                 BIGINT        NOT NULL,
    is_excluded                 TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        issue_id,
        category_id
    ),

    KEY idx_tracked_issue_categories_category (
        category_id,
        issue_id
    ),

    CONSTRAINT fk_tracked_issue_categories_issue
        FOREIGN KEY (issue_id)
        REFERENCES tracked_issues (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_tracked_issue_categories_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 3.5 Tracked issue-keyword condition
--
-- match_type examples:
--   EXACT / CONTAINS / SEMANTIC
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tracked_issue_keywords (
    issue_id                    CHAR(36)      NOT NULL,
    keyword_id                  BIGINT        NOT NULL,

    match_type                  VARCHAR(30)   NOT NULL DEFAULT 'CONTAINS',
    weight                      DECIMAL(8,5)  NOT NULL DEFAULT 1.00000,
    is_excluded                 TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        issue_id,
        keyword_id
    ),

    KEY idx_tracked_issue_keywords_keyword (
        keyword_id,
        issue_id
    ),

    CONSTRAINT fk_tracked_issue_keywords_issue
        FOREIGN KEY (issue_id)
        REFERENCES tracked_issues (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_tracked_issue_keywords_keyword
        FOREIGN KEY (keyword_id)
        REFERENCES keywords (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 3.6 Tracked issue-content match result
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS issue_content_matches (
    issue_id                    CHAR(36)      NOT NULL,
    content_id                  CHAR(36)      NOT NULL,

    match_score                 DECIMAL(8,5)  NOT NULL DEFAULT 0.00000,
    match_method                VARCHAR(40)   NOT NULL,
    evidence_text               TEXT          NULL,

    matched_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        issue_id,
        content_id
    ),

    KEY idx_issue_content_matches_content (
        content_id
    ),
    KEY idx_issue_content_matches_issue_time (
        issue_id,
        matched_at
    ),

    CONSTRAINT fk_issue_content_matches_issue
        FOREIGN KEY (issue_id)
        REFERENCES tracked_issues (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_issue_content_matches_content
        FOREIGN KEY (content_id)
        REFERENCES content_references (content_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 4. MONGODB SUMMARY REFERENCES
-- ============================================================

-- ------------------------------------------------------------
-- 4.1 MongoDB summaries reference
--
-- summary_id = MongoDB findart.summaries._id
--
-- summary_type examples:
--   TODAY_SUMMARY
--   WEEK_RECAP
--   NEXT_WEEK_OUTLOOK
--   ECONOMY_OVERVIEW
--   POLICY_BRIEFING
--   FEATURED_SECTOR
--   ISSUE_UPDATE
--
-- scope_type examples:
--   GLOBAL / COMPANY / SECTOR / CATEGORY / ISSUE / ECONOMIC_DOMAIN
--
-- time_grain examples:
--   INTRADAY / DAY / WEEK / MONTH / QUARTER / HALF_YEAR / YEAR
--
-- window_code examples:
--   MORNING_0700 / MIDDAY_1200 / MARKET_CLOSE_1500 / FULL_PERIOD
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS summary_references (
    summary_id                  CHAR(36)      NOT NULL,
    mongo_database              VARCHAR(80)   NOT NULL DEFAULT 'findart',
    mongo_collection            VARCHAR(80)   NOT NULL DEFAULT 'summaries',

    summary_type                VARCHAR(60)   NOT NULL,
    scope_type                  VARCHAR(30)   NOT NULL,
    scope_key                   VARCHAR(180)  NOT NULL,

    company_id                  CHAR(36)      NULL,
    sector_id                   BIGINT        NULL,
    category_id                 BIGINT        NULL,
    issue_id                    CHAR(36)      NULL,
    economic_domain             VARCHAR(80)   NULL,

    time_grain                  VARCHAR(20)   NOT NULL,
    window_code                 VARCHAR(40)   NOT NULL DEFAULT 'FULL_PERIOD',
    period_start                DATETIME(6)   NOT NULL,
    period_end                  DATETIME(6)   NOT NULL,
    source_cutoff_at            DATETIME(6)   NOT NULL,

    revision                    INT           NOT NULL DEFAULT 1,
    is_current                  TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                  DATETIME(6)   NULL,

    PRIMARY KEY (summary_id),

    UNIQUE KEY uk_summary_references_scope_period_revision (
        summary_type,
        scope_key,
        time_grain,
        window_code,
        period_start,
        period_end,
        revision
    ),

    KEY idx_summary_references_current_lookup (
        summary_type,
        scope_key,
        time_grain,
        period_end,
        is_current
    ),
    KEY idx_summary_references_company (
        company_id,
        period_end
    ),
    KEY idx_summary_references_sector (
        sector_id,
        period_end
    ),
    KEY idx_summary_references_category (
        category_id,
        period_end
    ),
    KEY idx_summary_references_issue (
        issue_id,
        period_end
    ),
    KEY idx_summary_references_deleted (
        deleted_at
    ),

    CONSTRAINT fk_summary_references_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT fk_summary_references_sector
        FOREIGN KEY (sector_id)
        REFERENCES sectors (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT fk_summary_references_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT fk_summary_references_issue
        FOREIGN KEY (issue_id)
        REFERENCES tracked_issues (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 4.2 Summary-content provenance
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS summary_content_links (
    summary_id                  CHAR(36)      NOT NULL,
    content_id                  CHAR(36)      NOT NULL,

    relevance_weight            DECIMAL(8,5)  NULL,
    is_key_source               TINYINT(1)    NOT NULL DEFAULT 0,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        summary_id,
        content_id
    ),

    KEY idx_summary_content_links_content (
        content_id,
        summary_id
    ),

    CONSTRAINT fk_summary_content_links_summary
        FOREIGN KEY (summary_id)
        REFERENCES summary_references (summary_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_summary_content_links_content
        FOREIGN KEY (content_id)
        REFERENCES content_references (content_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 4.3 Hierarchical summary provenance
--
-- Example:
--   parent weekly summary -> child daily summaries
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS summary_parent_links (
    parent_summary_id           CHAR(36)      NOT NULL,
    child_summary_id            CHAR(36)      NOT NULL,

    relevance_weight            DECIMAL(8,5)  NULL,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        parent_summary_id,
        child_summary_id
    ),

    KEY idx_summary_parent_links_child (
        child_summary_id,
        parent_summary_id
    ),

    CONSTRAINT fk_summary_parent_links_parent
        FOREIGN KEY (parent_summary_id)
        REFERENCES summary_references (summary_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_summary_parent_links_child
        FOREIGN KEY (child_summary_id)
        REFERENCES summary_references (summary_id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 5. ECONOMIC INDICATORS
-- ============================================================

-- ------------------------------------------------------------
-- 5.1 Economic indicator series
--
-- domain examples:
--   INTEREST_RATE / INFLATION / EXCHANGE_RATE / GROWTH / MARKET
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS economic_series (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,

    code                        VARCHAR(120)  NOT NULL,
    name                        VARCHAR(255)  NOT NULL,
    domain                      VARCHAR(40)   NOT NULL,

    frequency                   VARCHAR(20)   NOT NULL,
    unit                        VARCHAR(40)   NOT NULL,
    base_currency               VARCHAR(10)   NULL,
    quote_currency              VARCHAR(10)   NULL,

    source                      VARCHAR(100)  NOT NULL,
    is_active                   TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_economic_series_code (
        code
    ),

    KEY idx_economic_series_domain (
        domain,
        is_active
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 5.2 Economic indicator observation
--
-- Latest revision overwrites the row having the same
-- series_id + period_start + period_end.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS economic_observations (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    series_id                   BIGINT          NOT NULL,

    period_start                DATE            NOT NULL,
    period_end                  DATE            NOT NULL,
    released_at                 DATETIME(6)     NULL,

    value                       DECIMAL(30,10)  NOT NULL,
    source_content_id           CHAR(36)        NULL,

    created_at                  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                               ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_economic_observations_period (
        series_id,
        period_start,
        period_end
    ),

    KEY idx_economic_observations_period (
        period_end,
        series_id
    ),
    KEY idx_economic_observations_source_content (
        source_content_id
    ),

    CONSTRAINT fk_economic_observations_series
        FOREIGN KEY (series_id)
        REFERENCES economic_series (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_economic_observations_source_content
        FOREIGN KEY (source_content_id)
        REFERENCES content_references (content_id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 6. COMPANY MARKET AND FINANCIAL DATA
-- ============================================================

-- ------------------------------------------------------------
-- 6.1 Company daily stock price
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_prices_daily (
    company_id                  CHAR(36)       NOT NULL,
    trading_date                DATE           NOT NULL,

    open_price                  DECIMAL(24,6)  NOT NULL,
    high_price                  DECIMAL(24,6)  NOT NULL,
    low_price                   DECIMAL(24,6)  NOT NULL,
    close_price                 DECIMAL(24,6)  NOT NULL,
    adjusted_close_price        DECIMAL(24,6)  NULL,

    volume                      BIGINT         NULL,
    trading_value               DECIMAL(30,4)  NULL,
    market_cap                  DECIMAL(30,4)  NULL,

    created_at                  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                               ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (
        company_id,
        trading_date
    ),

    KEY idx_stock_prices_daily_date (
        trading_date,
        company_id
    ),

    CONSTRAINT fk_stock_prices_daily_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 6.2 Standard financial account
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS financial_accounts (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,

    code                        VARCHAR(120)  NOT NULL,
    name                        VARCHAR(255)  NOT NULL,
    statement_type              VARCHAR(40)   NOT NULL,
    value_type                  VARCHAR(30)   NOT NULL DEFAULT 'AMOUNT',

    is_active                   TINYINT(1)    NOT NULL DEFAULT 1,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_financial_accounts_code (
        code
    ),

    KEY idx_financial_accounts_statement (
        statement_type,
        is_active
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ------------------------------------------------------------
-- 6.3 Company financial fact
--
-- fiscal_period_code examples:
--   Q1 / Q2 / Q3 / FY
--
-- statement_scope examples:
--   CONSOLIDATED / SEPARATE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS company_financial_facts (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,

    company_id                  CHAR(36)        NOT NULL,
    account_id                  BIGINT          NOT NULL,

    fiscal_year                 SMALLINT        NOT NULL,
    fiscal_period_code          VARCHAR(16)     NOT NULL,
    period_start                DATE            NOT NULL,
    period_end                  DATE            NOT NULL,

    statement_scope             VARCHAR(30)     NOT NULL,
    amount                      DECIMAL(30,4)   NOT NULL,
    currency                    VARCHAR(10)     NOT NULL DEFAULT 'KRW',

    source_content_id           CHAR(36)        NULL,

    created_at                  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                               ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_company_financial_facts_period (
        company_id,
        account_id,
        fiscal_year,
        fiscal_period_code,
        statement_scope
    ),

    KEY idx_company_financial_facts_company_period (
        company_id,
        period_end
    ),
    KEY idx_company_financial_facts_account_period (
        account_id,
        period_end
    ),
    KEY idx_company_financial_facts_source_content (
        source_content_id
    ),

    CONSTRAINT fk_company_financial_facts_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,

    CONSTRAINT fk_company_financial_facts_account
        FOREIGN KEY (account_id)
        REFERENCES financial_accounts (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,

    CONSTRAINT fk_company_financial_facts_source_content
        FOREIGN KEY (source_content_id)
        REFERENCES content_references (content_id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 7. EVENTS
-- ============================================================

-- ------------------------------------------------------------
-- 7.1 Scheduled or extracted event
--
-- event_type examples:
--   RATE_DECISION / CPI_RELEASE / GDP_RELEASE / IPO
--   QUADRUPLE_WITCHING / EARNINGS / POLICY / DISCLOSURE_EVENT
--
-- PoC simplification:
--   One primary company, sector and category may be attached directly.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id                          CHAR(36)      NOT NULL,

    event_type                  VARCHAR(80)   NOT NULL,
    title                       VARCHAR(1000) NOT NULL,
    description                 TEXT          NULL,

    scheduled_at                DATETIME(6)   NOT NULL,
    ended_at                    DATETIME(6)   NULL,

    status                      VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    importance_level            VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',

    company_id                  CHAR(36)      NULL,
    sector_id                   BIGINT        NULL,
    category_id                 BIGINT        NULL,
    source_content_id           CHAR(36)      NULL,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    KEY idx_events_schedule (
        scheduled_at,
        importance_level
    ),
    KEY idx_events_type_schedule (
        event_type,
        scheduled_at
    ),
    KEY idx_events_company (
        company_id,
        scheduled_at
    ),
    KEY idx_events_sector (
        sector_id,
        scheduled_at
    ),
    KEY idx_events_category (
        category_id,
        scheduled_at
    ),

    CONSTRAINT fk_events_company
        FOREIGN KEY (company_id)
        REFERENCES companies (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT fk_events_sector
        FOREIGN KEY (sector_id)
        REFERENCES sectors (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT fk_events_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT,

    CONSTRAINT fk_events_source_content
        FOREIGN KEY (source_content_id)
        REFERENCES content_references (content_id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 8. CROSS-DATABASE PROCESSING JOBS
-- ============================================================

-- ------------------------------------------------------------
-- 8.1 Retry/idempotency job
--
-- target_type examples:
--   CONTENT / SUMMARY / ISSUE / ECONOMIC_SERIES / COMPANY
--
-- job_type examples:
--   SYNC_CONTENT_REFERENCE
--   ANALYZE_CONTENT
--   SYNC_CONTENT_RELATIONS
--   MATCH_TRACKED_ISSUES
--   GENERATE_SUMMARY
--   SYNC_SUMMARY_REFERENCE
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS processing_jobs (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,

    target_type                 VARCHAR(30)   NOT NULL,
    target_id                   VARCHAR(160)  NOT NULL,
    job_type                    VARCHAR(50)   NOT NULL,

    status                      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count                 INT           NOT NULL DEFAULT 0,
    max_retry_count             INT           NOT NULL DEFAULT 5,

    next_retry_at               DATETIME(6)   NULL,
    started_at                  DATETIME(6)   NULL,
    completed_at                DATETIME(6)   NULL,

    error_message               TEXT          NULL,

    created_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_processing_jobs_target_job (
        target_type,
        target_id,
        job_type
    ),

    KEY idx_processing_jobs_poll (
        status,
        next_retry_at,
        created_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 9. INITIAL MASTER DATA
-- ============================================================

-- ------------------------------------------------------------
-- 9.1 Basic categories
-- ------------------------------------------------------------
INSERT IGNORE INTO categories (
    code,
    name
) VALUES
    ('POLITICS',      '정치'),
    ('ECONOMY',       '경제'),
    ('SOCIETY',       '사회'),
    ('CULTURE',       '문화'),
    ('INTERNATIONAL', '국제'),
    ('INDUSTRY',      '산업'),
    ('TECHNOLOGY',    '기술');


-- ------------------------------------------------------------
-- 9.2 Basic economic indicator series
-- ------------------------------------------------------------
INSERT IGNORE INTO economic_series (
    code,
    name,
    domain,
    frequency,
    unit,
    base_currency,
    quote_currency,
    source
) VALUES
    (
        'KR_BASE_RATE',
        '한국 기준금리',
        'INTEREST_RATE',
        'DAILY',
        'PERCENT',
        NULL,
        NULL,
        'BOK'
    ),
    (
        'KR_CPI',
        '대한민국 소비자물가지수',
        'INFLATION',
        'MONTHLY',
        'INDEX',
        NULL,
        NULL,
        'KOSIS'
    ),
    (
        'USD_KRW',
        '원-달러 환율',
        'EXCHANGE_RATE',
        'DAILY',
        'KRW',
        'USD',
        'KRW',
        'BOK'
    ),
    (
        'JPY_KRW_100',
        '원-엔 환율(100엔)',
        'EXCHANGE_RATE',
        'DAILY',
        'KRW',
        'JPY',
        'KRW',
        'BOK'
    ),
    (
        'EUR_KRW',
        '원-유로 환율',
        'EXCHANGE_RATE',
        'DAILY',
        'KRW',
        'EUR',
        'KRW',
        'BOK'
    ),
    (
        'CNY_KRW',
        '원-위안 환율',
        'EXCHANGE_RATE',
        'DAILY',
        'KRW',
        'CNY',
        'KRW',
        'BOK'
    ),
    (
        'KR_GDP_GROWTH',
        '대한민국 실질 GDP 성장률',
        'GROWTH',
        'QUARTERLY',
        'PERCENT',
        NULL,
        NULL,
        'BOK'
    ),
    (
        'KOSPI_INDEX',
        'KOSPI 지수',
        'MARKET',
        'DAILY',
        'INDEX',
        NULL,
        NULL,
        'KRX'
    ),
    (
        'KOSDAQ_INDEX',
        'KOSDAQ 지수',
        'MARKET',
        'DAILY',
        'INDEX',
        NULL,
        NULL,
        'KRX'
    );


-- ------------------------------------------------------------
-- 9.3 Basic financial accounts
-- ------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    code,
    name,
    statement_type,
    value_type
) VALUES
    ('REVENUE',            '매출액',       'INCOME_STATEMENT', 'AMOUNT'),
    ('OPERATING_PROFIT',   '영업이익',     'INCOME_STATEMENT', 'AMOUNT'),
    ('NET_INCOME',         '당기순이익',   'INCOME_STATEMENT', 'AMOUNT'),
    ('TOTAL_ASSETS',       '자산총계',     'BALANCE_SHEET',    'AMOUNT'),
    ('TOTAL_LIABILITIES',  '부채총계',     'BALANCE_SHEET',    'AMOUNT'),
    ('TOTAL_EQUITY',       '자본총계',     'BALANCE_SHEET',    'AMOUNT'),
    ('OPERATING_CASH_FLOW','영업활동현금흐름','CASH_FLOW',      'AMOUNT');


-- ============================================================
-- 10. VIEWS
-- ============================================================

-- ------------------------------------------------------------
-- 10.1 Active tracked issues
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW v_active_tracked_issues AS
SELECT
    ti.*
FROM tracked_issues ti
WHERE ti.status = 'ACTIVE'
  AND ti.archived_at IS NULL
  AND ti.tracking_start_at <= CURRENT_TIMESTAMP(6)
  AND (
      ti.tracking_end_at IS NULL
      OR ti.tracking_end_at >= CURRENT_TIMESTAMP(6)
  );


-- ------------------------------------------------------------
-- 10.2 Current MongoDB summary references
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW v_current_summary_references AS
SELECT
    sr.*
FROM summary_references sr
WHERE sr.is_current = 1
  AND sr.deleted_at IS NULL;


-- ------------------------------------------------------------
-- 10.3 Pending jobs available for polling
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW v_pending_processing_jobs AS
SELECT
    pj.*
FROM processing_jobs pj
WHERE pj.status IN ('PENDING', 'RETRY')
  AND pj.retry_count < pj.max_retry_count
  AND (
      pj.next_retry_at IS NULL
      OR pj.next_retry_at <= CURRENT_TIMESTAMP(6)
  );


-- ------------------------------------------------------------
-- 10.4 Current content references
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW v_current_content_references AS
SELECT
    cr.*
FROM content_references cr
WHERE cr.deleted_at IS NULL;


SET FOREIGN_KEY_CHECKS = 1;
