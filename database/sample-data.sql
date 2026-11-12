-- =====================================================================
-- Spartan Club Central: sample clubs for testing and demos
--
-- All clubs, emails and links are FICTIONAL (example.com addresses only).
-- No leader accounts are inserted here: leaders register through the
-- website so their passwords are hashed by the app (NFR-01).
--
-- Run after schema.sql (or after reset-demo.sql):
--   mysql -u root -p club_central < database/sample-data.sql
--
-- Built-in test data:
--   * "robotics" appears ONLY in the description of "Engineering Builders
--     Society" (no club name contains it), for TC-02.
--   * Many names contain "Club" across all four categories, for TC-05.
--   * "Retro Film Society" is inactive and must never appear in results.
-- Category ids: 1 Academic, 2 Cultural, 3 Sports, 4 Service
-- =====================================================================

USE club_central;

INSERT INTO club (club_name, description, category_id, meeting_time, contact_email, contact_info, instagram_url, website_url, other_social_url, is_active) VALUES
-- Academic (1)
('Chess Club', 'Casual and competitive chess for every level. We run weekly practice games, puzzle nights, and travel to two regional tournaments each semester.', 1, 'Tuesdays 6:00 PM, Library Room 210', 'chess@example.com', 'Stop by any Tuesday, boards provided.', 'https://example.com/instagram/chessclub', NULL, NULL, TRUE),
('Engineering Builders Society', 'Hands-on engineering projects for all majors. Teams design, build and program robots for intercollegiate robotics competitions and community STEM days.', 1, 'Thursdays 5:30 PM, Engineering Lab B', 'builders@example.com', 'Email us or visit the lab.', 'https://example.com/instagram/builders', 'https://example.com/builders', NULL, TRUE),
('Pre-Med Club', 'Support for students preparing for medical school: MCAT study groups, shadowing tips, and talks from health professionals.', 1, 'Every other Monday 7:00 PM, Science Hall 101', 'premed@example.com', NULL, 'https://example.com/instagram/premed', NULL, NULL, TRUE),
('Computer Science Club', 'Coding nights, hackathon teams, interview practice and tech talks. Beginners welcome, no experience needed.', 1, 'Wednesdays 6:30 PM, Tech Center 305', 'csclub@example.com', 'Join our Discord from the website.', 'https://example.com/instagram/csclub', 'https://example.com/csclub', 'https://example.com/discord/csclub', TRUE),
('Debate and Forensics Team', 'Competitive debate, public speaking and mock trial. Improve your argument skills and compete at regional tournaments.', 1, 'Mondays and Thursdays 4:00 PM, Humanities 120', 'debate@example.com', NULL, NULL, 'https://example.com/debate', NULL, TRUE),
('Math Circle', 'Problem-solving sessions, puzzle competitions and peer tutoring for anyone who enjoys mathematics.', 1, 'Fridays 3:00 PM, Science Hall 214', 'math@example.com', NULL, 'https://example.com/instagram/mathcircle', NULL, NULL, TRUE),
('Business Leaders Club', 'Networking events, resume workshops, case competitions and visits from local entrepreneurs.', 1, 'Tuesdays 7:00 PM, Business Building 150', 'business@example.com', 'Business casual for networking nights.', 'https://example.com/instagram/business', 'https://example.com/business', NULL, TRUE),
('Psychology Club', 'Discussions on current research, graduate school preparation and volunteer opportunities in mental health awareness.', 1, 'Wednesdays 5:00 PM, Social Sciences 230', 'psych@example.com', NULL, 'https://example.com/instagram/psych', NULL, NULL, TRUE),
-- Cultural (2)
('African Student Union', 'Celebrating African cultures through food, music, fashion shows and conversation. Everyone is welcome.', 2, 'Fridays 6:00 PM, Student Center Ballroom', 'asu@example.com', NULL, 'https://example.com/instagram/asu', NULL, NULL, TRUE),
('Caribbean Students Club', 'Bringing Caribbean culture to campus with heritage week, carnival celebrations and cooking nights.', 2, 'Thursdays 7:00 PM, Student Center 204', 'caribbean@example.com', NULL, 'https://example.com/instagram/caribbean', NULL, 'https://example.com/tiktok/caribbean', TRUE),
('Latin American Student Association', 'Cultural events, dance nights and community service highlighting Latin American heritage.', 2, 'Tuesdays 6:00 PM, Student Center 210', 'lasa@example.com', NULL, 'https://example.com/instagram/lasa', NULL, NULL, TRUE),
('Gospel Choir', 'Student-led gospel choir performing at campus events and churches across the region. No audition needed for rehearsals.', 2, 'Mondays and Wednesdays 7:30 PM, Fine Arts Hall', 'gospel@example.com', 'Rehearsals are open, just show up.', 'https://example.com/instagram/gospel', 'https://example.com/gospel', NULL, TRUE),
('Anime and Gaming Club', 'Anime screenings, tabletop games and casual video game tournaments.', 2, 'Fridays 7:00 PM, Student Center Game Room', 'anime@example.com', NULL, 'https://example.com/instagram/anime', NULL, 'https://example.com/discord/anime', TRUE),
('Poetry and Spoken Word Club', 'Open mics, writing workshops and slam poetry competitions. Share your voice or just come listen.', 2, 'Thursdays 8:00 PM, Campus Cafe', 'poetry@example.com', NULL, 'https://example.com/instagram/poetry', NULL, NULL, TRUE),
('International Students Club', 'A home away from home for international students: cultural nights, trips and help adjusting to campus life.', 2, 'Wednesdays 5:00 PM, International Office', 'international@example.com', NULL, 'https://example.com/instagram/international', 'https://example.com/international', NULL, TRUE),
('Dance Collective', 'Hip hop, contemporary and step. Weekly practice plus two showcases each semester.', 2, 'Tuesdays and Thursdays 8:00 PM, Gym Studio', 'dance@example.com', NULL, 'https://example.com/instagram/dance', NULL, 'https://example.com/tiktok/dance', TRUE),
('Retro Film Society', 'Classic movie screenings and discussion. Currently inactive.', 2, 'Not meeting this semester', 'film@example.com', NULL, NULL, NULL, NULL, FALSE),
-- Sports (3)
('Soccer Club', 'Co-ed recreational soccer with pickup games and intramural league teams.', 3, 'Saturdays 10:00 AM, South Field', 'soccer@example.com', NULL, 'https://example.com/instagram/soccer', NULL, NULL, TRUE),
('Basketball Club', 'Pickup games, skills clinics and intramural teams for all skill levels.', 3, 'Mondays and Wednesdays 8:00 PM, Main Gym', 'basketball@example.com', NULL, 'https://example.com/instagram/basketball', NULL, NULL, TRUE),
('Running Club', 'Group runs for every pace, from casual 5K to half marathon training.', 3, 'Tuesdays and Thursdays 6:30 AM, Track Entrance', 'running@example.com', 'All paces welcome.', 'https://example.com/instagram/running', NULL, 'https://example.com/strava/running', TRUE),
('Volleyball Club', 'Indoor and sand volleyball practices plus weekend tournaments.', 3, 'Sundays 2:00 PM, Rec Center Court 2', 'volleyball@example.com', NULL, 'https://example.com/instagram/volleyball', NULL, NULL, TRUE),
('Martial Arts Club', 'Self-defense, karate and Brazilian jiu-jitsu taught by certified student instructors.', 3, 'Mondays and Thursdays 6:00 PM, Rec Center Mat Room', 'martialarts@example.com', NULL, 'https://example.com/instagram/martialarts', NULL, NULL, TRUE),
('Esports Club', 'Competitive and casual gaming teams competing in collegiate esports leagues.', 3, 'Wednesdays 8:00 PM, Tech Center Lab 110', 'esports@example.com', NULL, 'https://example.com/instagram/esports', NULL, 'https://example.com/twitch/esports', TRUE),
('Swim Club', 'Lap swimming, stroke clinics and friendly meets. Must be able to swim one pool length.', 3, 'Tuesdays 7:00 PM, Aquatic Center', 'swim@example.com', NULL, 'https://example.com/instagram/swim', NULL, NULL, TRUE),
('Tennis Club', 'Rallies, drills and doubles ladders. Rackets available to borrow.', 3, 'Saturdays 9:00 AM, Campus Courts', 'tennis@example.com', NULL, 'https://example.com/instagram/tennis', NULL, NULL, TRUE),
-- Service (4)
('Habitat Builders', 'Volunteer construction days building affordable housing with local partners.', 4, 'Saturdays 8:00 AM, meet at Student Center', 'habitat@example.com', NULL, 'https://example.com/instagram/habitat', 'https://example.com/habitat', NULL, TRUE),
('Campus Food Pantry Volunteers', 'Stock shelves, run food drives and help fellow students access free groceries.', 4, 'Flexible shifts, Student Center 101', 'pantry@example.com', 'Sign up for shifts by email.', 'https://example.com/instagram/pantry', NULL, NULL, TRUE),
('Tutoring Corps', 'Volunteer tutors helping local middle and high school students with math, reading and science.', 4, 'Weekdays 3:30 PM, local schools', 'tutoring@example.com', NULL, NULL, 'https://example.com/tutoring', NULL, TRUE),
('Green Campus Club', 'Recycling drives, campus garden work days and sustainability awareness events.', 4, 'Thursdays 4:30 PM, Campus Garden', 'green@example.com', NULL, 'https://example.com/instagram/green', NULL, NULL, TRUE),
('Blood Drive Committee', 'Organizes campus blood drives with regional blood banks every semester.', 4, 'First Monday of each month 6:00 PM, Health Center', 'blood@example.com', NULL, 'https://example.com/instagram/blood', NULL, NULL, TRUE),
('Mentoring Club', 'Upper-class students mentor first-year and transfer students through their first semester.', 4, 'Wednesdays 6:00 PM, Student Center 220', 'mentoring@example.com', NULL, 'https://example.com/instagram/mentoring', NULL, NULL, TRUE),
('Animal Shelter Volunteers', 'Weekend volunteering at the local animal shelter: walking dogs, socializing cats and adoption events.', 4, 'Sundays 11:00 AM, carpool from Lot C', 'animals@example.com', NULL, 'https://example.com/instagram/animals', NULL, NULL, TRUE);

-- Officers (FR-13): fictional names and roles for some clubs. Chess Club's
-- officers are added by demo mode (the demo leader is its President).
INSERT INTO club_officer (club_id, name, role, display_order)
SELECT c.club_id, o.name, o.role, o.ord FROM club c JOIN (
  SELECT 'Engineering Builders Society' AS club, 'Aisha Johnson' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Engineering Builders Society', 'Kevin Tran', 'Build Lead', 2 UNION ALL
  SELECT 'Computer Science Club', 'Maya Robinson', 'President', 1 UNION ALL
  SELECT 'Computer Science Club', 'Daniel Brooks', 'Vice President', 2 UNION ALL
  SELECT 'Computer Science Club', 'Sofia Alvarez', 'Hackathon Coordinator', 3 UNION ALL
  SELECT 'Debate and Forensics Team', 'Jamal Carter', 'Captain', 1 UNION ALL
  SELECT 'African Student Union', 'Amara Okafor', 'President', 1 UNION ALL
  SELECT 'African Student Union', 'Kwame Mensah', 'Events Chair', 2 UNION ALL
  SELECT 'Gospel Choir', 'Naomi Wright', 'Director', 1 UNION ALL
  SELECT 'Soccer Club', 'Luis Moreno', 'Captain', 1 UNION ALL
  SELECT 'Soccer Club', 'Grace Kim', 'Treasurer', 2 UNION ALL
  SELECT 'Running Club', 'Tyler Evans', 'President', 1 UNION ALL
  SELECT 'Habitat Builders', 'Hannah Lee', 'President', 1 UNION ALL
  SELECT 'Habitat Builders', 'Marcus Bell', 'Volunteer Coordinator', 2 UNION ALL
  SELECT 'Tutoring Corps', 'Olivia Scott', 'Coordinator', 1
) o ON o.club = c.club_name;

-- Events (FR-14): dated relative to the day you load this file, so they're
-- always upcoming during a demo. Chess Club's events come from demo mode.
INSERT INTO club_event (club_id, title, description, starts_at, ends_at, location)
SELECT c.club_id, e.title, e.description,
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL e.days DAY), e.start_time),
       TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL e.days DAY), e.end_time),
       e.location
FROM club c JOIN (
  SELECT 'Engineering Builders Society' AS club, 'Robot build night' AS title, 'Bring a laptop; we provide the parts.' AS description, 2 AS days, '18:00:00' AS start_time, '20:30:00' AS end_time, 'Engineering Lab B' AS location UNION ALL
  SELECT 'Computer Science Club', 'Intro to Git workshop', 'Hands-on session for beginners.', 3, '18:30:00', '20:00:00', 'Tech Center 305' UNION ALL
  SELECT 'Computer Science Club', 'Mini hackathon', 'Teams of 2 to 4. Pizza provided.', 10, '10:00:00', '16:00:00', 'Tech Center Lab 110' UNION ALL
  SELECT 'African Student Union', 'Culture night', 'Food, music and a fashion showcase.', 6, '19:00:00', '22:00:00', 'Student Center Ballroom' UNION ALL
  SELECT 'Gospel Choir', 'Open rehearsal', 'Come sing with us, no audition.', 1, '19:30:00', '21:00:00', 'Fine Arts Hall' UNION ALL
  SELECT 'Soccer Club', 'Pickup game', 'All skill levels.', 4, '10:00:00', '12:00:00', 'South Field' UNION ALL
  SELECT 'Running Club', 'Sunrise 5K', 'Easy group pace.', 5, '06:30:00', '07:30:00', 'Track Entrance' UNION ALL
  SELECT 'Habitat Builders', 'Build day', 'Closed-toe shoes required.', 8, '08:00:00', '14:00:00', 'Meet at Student Center' UNION ALL
  SELECT 'Green Campus Club', 'Campus garden workday', 'Gloves provided.', 3, '16:30:00', '18:00:00', 'Campus Garden' UNION ALL
  SELECT 'Anime and Gaming Club', 'Smash tournament', 'Sign up at the door.', 7, '19:00:00', '23:00:00', 'Student Center Game Room'
) e ON e.club = c.club_name;
