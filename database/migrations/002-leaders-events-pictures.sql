-- =====================================================================
-- Spartan Club Central: upgrade an EXISTING database for Phase 8
-- (club officers, club events, club pictures). Keeps all your data.
--
-- Only needed if you created club_central BEFORE Phase 8. A fresh install
-- from database/schema.sql already has all of this. Run once, as root:
--   mysql -u root -p club_central < database/migrations/002-leaders-events-pictures.sql
-- (Workbench: File > Open SQL Script, then the lightning bolt.)
-- Running it twice gives "already exists" errors; that's harmless.
-- =====================================================================

USE club_central;

-- ---------- 8A: club officers (FR-13) ----------
CREATE TABLE club_officer (
  officer_id     BIGINT        AUTO_INCREMENT PRIMARY KEY,
  club_id        BIGINT        NOT NULL,
  name           VARCHAR(100)  NOT NULL,
  role           VARCHAR(50)   NOT NULL,
  display_order  INT           NOT NULL,
  CONSTRAINT fk_officer_club FOREIGN KEY (club_id) REFERENCES club(club_id) ON DELETE CASCADE,
  INDEX idx_officer_club_order (club_id, display_order)
);
