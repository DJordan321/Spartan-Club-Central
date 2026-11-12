-- =====================================================================
-- Spartan Club Central: upgrade an EXISTING database for Phase 8
-- (club officers, club events, club pictures). Keeps all your data.
--
-- Only needed if you created club_central BEFORE Phase 8. A fresh install
-- from database/schema.sql already has all of this. Run once, as root:
--   mysql -u root -p club_central < database/migrations/002-leaders-events-pictures.sql
-- (Workbench: File > Open SQL Script, then the lightning bolt.)
-- Safe to run more than once: anything already there is skipped.
-- =====================================================================

USE club_central;

-- ---------- 8A: club officers (FR-13) ----------
CREATE TABLE IF NOT EXISTS club_officer (
  officer_id     BIGINT        AUTO_INCREMENT PRIMARY KEY,
  club_id        BIGINT        NOT NULL,
  name           VARCHAR(100)  NOT NULL,
  role           VARCHAR(50)   NOT NULL,
  display_order  INT           NOT NULL,
  CONSTRAINT fk_officer_club FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE CASCADE,
  INDEX idx_officer_club_order (club_id, display_order)
);

-- ---------- 8B: club events (FR-14) ----------
CREATE TABLE IF NOT EXISTS club_event (
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
