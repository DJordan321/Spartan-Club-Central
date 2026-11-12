-- =====================================================================
-- Spartan Club Central: upgrade an EXISTING database for the
-- "Mark as seen" button on the leader's join requests. Keeps all your data.
--
-- Only needed if you created club_central before this change. A fresh
-- install from database/schema.sql already has the column. Run once, as root:
--   mysql -u root -p club_central < database/migrations/003-request-seen.sql
-- (Workbench: File > Open SQL Script, then the lightning bolt.)
-- Safe to run more than once: the column is only added when it's missing.
-- =====================================================================

USE club_central;

SET @add_seen = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE join_request ADD COLUMN seen BOOLEAN NOT NULL DEFAULT FALSE AFTER status', 'DO 0')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'join_request' AND column_name = 'seen');
PREPARE stmt FROM @add_seen; EXECUTE stmt; DEALLOCATE PREPARE stmt;
