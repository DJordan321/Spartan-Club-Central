-- =====================================================================
-- Spartan Club Central: database schema (MySQL 8.0.16 or newer)
--
-- Run ONCE as the MySQL root user:
--   mysql -u root -p < database/schema.sql
--
-- Hibernate runs with ddl-auto=validate, so it never creates or changes
-- tables. This file is the only place the structure is defined.
-- Column lengths match FieldLimits.java (the validators use the same numbers).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS club_central
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;   -- _ai_ci = case-insensitive text compare
USE club_central;

-- Club categories used by the filter (FR-02).
CREATE TABLE category (
  category_id    INT           AUTO_INCREMENT PRIMARY KEY,
  category_name  VARCHAR(50)   NOT NULL,
  CONSTRAINT uq_category_name UNIQUE (category_name)
);

-- One row per club listing (FR-05, FR-06, FR-10).
CREATE TABLE club (
  club_id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
  club_name         VARCHAR(100)  NOT NULL,
  description       VARCHAR(2000) NOT NULL,
  category_id       INT           NOT NULL,
  meeting_time      VARCHAR(100)  NULL,
  contact_email     VARCHAR(254)  NOT NULL,           -- where join requests are emailed (FR-07)
  contact_info      VARCHAR(255)  NULL,               -- optional display text
  instagram_url     VARCHAR(255)  NULL,
  website_url       VARCHAR(255)  NULL,
  other_social_url  VARCHAR(255)  NULL,
  image_filename    VARCHAR(64)   NULL,                -- uploaded picture (FR-15): server-made name, file in UPLOAD_DIR
  image_alt         VARCHAR(150)  NULL,                -- its alt text for screen readers (NFR-06)
  is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
  last_updated      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_club_name     UNIQUE (club_name),
  CONSTRAINT fk_club_category FOREIGN KEY (category_id) REFERENCES category(category_id),
  INDEX idx_club_active_category (is_active, category_id)   -- NFR-04: fast filter
);

-- Club leader accounts. club_id stays NULL until the leader creates a club (FR-08).
CREATE TABLE club_leader (
  leader_id      BIGINT        AUTO_INCREMENT PRIMARY KEY,
  club_id        BIGINT        NULL,
  name           VARCHAR(100)  NOT NULL,
  email          VARCHAR(254)  NOT NULL,
  password_hash  VARCHAR(60)   NOT NULL,                -- BCrypt hashes are exactly 60 chars (NFR-01)
  position       VARCHAR(50)   NULL,
  is_enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_leader_email UNIQUE (email),
  CONSTRAINT uq_leader_club  UNIQUE (club_id),          -- one leader per club, one club per leader
  CONSTRAINT fk_leader_club  FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE SET NULL
);

-- Administrator accounts (seeded at startup from environment variables).
CREATE TABLE administrator (
  admin_id       BIGINT        AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100)  NOT NULL,
  email          VARCHAR(254)  NOT NULL,
  password_hash  VARCHAR(60)   NOT NULL,
  CONSTRAINT uq_admin_email UNIQUE (email)
);

-- Club officers shown on the public club page (FR-13). Names and roles only:
-- no emails or photos of people (NFR-02). Up to 10 per club.
CREATE TABLE club_officer (
  officer_id     BIGINT        AUTO_INCREMENT PRIMARY KEY,
  club_id        BIGINT        NOT NULL,
  name           VARCHAR(100)  NOT NULL,
  role           VARCHAR(50)   NOT NULL,
  display_order  INT           NOT NULL,
  CONSTRAINT fk_officer_club FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE CASCADE,
  INDEX idx_officer_club_order (club_id, display_order)
);

-- Club events shown on the club page (FR-14). Leaders manage them from the dashboard.
CREATE TABLE club_event (
  event_id     BIGINT         AUTO_INCREMENT PRIMARY KEY,
  club_id      BIGINT         NOT NULL,
  title        VARCHAR(100)   NOT NULL,
  description  VARCHAR(1000)  NULL,
  starts_at    DATETIME       NOT NULL,              -- campus local time (DATETIME: never shifted by time zone)
  ends_at      DATETIME       NULL,
  location     VARCHAR(150)   NULL,
  created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_event_club FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE CASCADE,
  INDEX idx_event_club_start (club_id, starts_at),
  INDEX idx_event_start (starts_at)
);

-- Join/contact requests from students (FR-03). Named join_request, not
-- "request", so it is never confused with an HTTP request.
CREATE TABLE join_request (
  request_id      BIGINT        AUTO_INCREMENT PRIMARY KEY,
  club_id         BIGINT        NOT NULL,
  student_name    VARCHAR(100)  NOT NULL,
  student_email   VARCHAR(254)  NOT NULL,
  message         VARCHAR(1000) NOT NULL,
  status          VARCHAR(10)   NOT NULL DEFAULT 'SUBMITTED',
  date_submitted  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_request_status CHECK (status IN ('SUBMITTED','DELIVERED','FAILED')),
  CONSTRAINT fk_request_club FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE CASCADE,
  INDEX idx_request_club_date (club_id, date_submitted)   -- leader inbox (SPR-04)
);

-- Password reset links (FR-11). Only a SHA-256 hash of the emailed token is stored.
CREATE TABLE password_reset_token (
  token_id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
  leader_id   BIGINT       NOT NULL,
  token_hash  VARCHAR(64)  NOT NULL,
  expires_at  TIMESTAMP    NOT NULL,
  used_at     TIMESTAMP    NULL,
  CONSTRAINT uq_token_hash   UNIQUE (token_hash),
  CONSTRAINT fk_token_leader FOREIGN KEY (leader_id) REFERENCES club_leader(leader_id) ON DELETE CASCADE
);

-- Security event log (SPR-08). Account ids and actions only:
-- never passwords, emails, or student data.
CREATE TABLE audit_log (
  log_id       BIGINT       AUTO_INCREMENT PRIMARY KEY,
  event_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actor_type   VARCHAR(10)  NOT NULL,       -- LEADER, ADMIN, ANONYMOUS
  actor_id     BIGINT       NULL,
  action       VARCHAR(40)  NOT NULL,       -- LOGIN_SUCCESS, LOGIN_FAILED, CLUB_EDITED, ...
  target_type  VARCHAR(20)  NULL,           -- CLUB, LEADER
  target_id    BIGINT       NULL,
  success      BOOLEAN      NOT NULL,
  INDEX idx_audit_time (event_time)
);

-- Fixed category ids so sample-data.sql can refer to them.
INSERT INTO category (category_id, category_name) VALUES
  (1, 'Academic'), (2, 'Cultural'), (3, 'Sports'), (4, 'Service');
