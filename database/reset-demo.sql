-- =====================================================================
-- Spartan Club Central: reset the database before a demo
--
-- Removes every club, officer, event, leader account, join request, reset token and audit
-- entry so each demo starts the same. Categories and the administrator
-- account are kept.
--
-- Run as the MySQL root user (the app user is not allowed to ALTER tables),
-- then reload the sample clubs:
--   mysql -u root -p club_central < database/reset-demo.sql
--   mysql -u root -p club_central < database/sample-data.sql
-- =====================================================================

USE club_central;

DELETE FROM password_reset_token;
DELETE FROM join_request;
DELETE FROM club_officer;
DELETE FROM club_event;
DELETE FROM audit_log;
DELETE FROM club_leader;
DELETE FROM club;

ALTER TABLE password_reset_token AUTO_INCREMENT = 1;
ALTER TABLE join_request         AUTO_INCREMENT = 1;
ALTER TABLE club_officer         AUTO_INCREMENT = 1;
ALTER TABLE club_event           AUTO_INCREMENT = 1;
ALTER TABLE audit_log            AUTO_INCREMENT = 1;
ALTER TABLE club_leader          AUTO_INCREMENT = 1;
ALTER TABLE club                 AUTO_INCREMENT = 1;
