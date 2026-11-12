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
--   * Every active club has contact info, officers and at least one event.
-- Category ids: 1 Academic, 2 Cultural, 3 Sports, 4 Service
-- =====================================================================

USE club_central;

INSERT INTO club (club_name, description, category_id, meeting_time, contact_email, contact_info, instagram_url, website_url, other_social_url, is_active) VALUES
('Chess Club', 'Casual and competitive chess for every level. We run weekly practice games, puzzle nights, and travel to two regional tournaments each semester.', 1, 'Tuesdays 6:00 PM, Library Room 210', 'chess@example.com', 'Stop by any Tuesday, boards provided.', 'https://example.com/instagram/chessclub', NULL, NULL, TRUE),
('Engineering Builders Society', 'Hands-on engineering projects for all majors. Teams design, build and program robots for intercollegiate robotics competitions and community STEM days.', 1, 'Thursdays 5:30 PM, Engineering Lab B', 'builders@example.com', 'Email us or visit the lab.', 'https://example.com/instagram/builders', 'https://example.com/builders', NULL, TRUE),
('Pre-Med Club', 'Support for students preparing for medical school: MCAT study groups, shadowing tips, and talks from health professionals.', 1, 'Every other Monday 7:00 PM, Science Hall 101', 'premed@example.com', 'MCAT study groups meet after every general meeting.', 'https://example.com/instagram/premed', 'https://example.com/premed', NULL, TRUE),
('Computer Science Club', 'Coding nights, hackathon teams, interview practice and tech talks. Beginners welcome, no experience needed.', 1, 'Wednesdays 6:30 PM, Tech Center 305', 'csclub@example.com', 'Join our Discord from the website.', 'https://example.com/instagram/csclub', 'https://example.com/csclub', 'https://example.com/discord/csclub', TRUE),
('Debate and Forensics Team', 'Competitive debate, public speaking and mock trial. Improve your argument skills and compete at regional tournaments.', 1, 'Mondays and Thursdays 4:00 PM, Humanities 120', 'debate@example.com', 'New members welcome at any practice; no experience needed.', NULL, 'https://example.com/debate', NULL, TRUE),
('Math Circle', 'Problem-solving sessions, puzzle competitions and peer tutoring for anyone who enjoys mathematics.', 1, 'Fridays 3:00 PM, Science Hall 214', 'math@example.com', 'Bring a pencil; puzzles provided.', 'https://example.com/instagram/mathcircle', 'https://example.com/mathcircle', NULL, TRUE),
('Business Leaders Club', 'Networking events, resume workshops, case competitions and visits from local entrepreneurs.', 1, 'Tuesdays 7:00 PM, Business Building 150', 'business@example.com', 'Business casual for networking nights.', 'https://example.com/instagram/business', 'https://example.com/business', NULL, TRUE),
('Psychology Club', 'Discussions on current research, graduate school preparation and volunteer opportunities in mental health awareness.', 1, 'Wednesdays 5:00 PM, Social Sciences 230', 'psych@example.com', 'Open to all majors.', 'https://example.com/instagram/psych', 'https://example.com/psych', NULL, TRUE),
('African Student Union', 'Celebrating African cultures through food, music, fashion shows and conversation. Everyone is welcome.', 2, 'Fridays 6:00 PM, Student Center Ballroom', 'asu@example.com', 'Everyone is welcome, no membership fee.', 'https://example.com/instagram/asu', 'https://example.com/asu', NULL, TRUE),
('Caribbean Students Club', 'Bringing Caribbean culture to campus with heritage week, carnival celebrations and cooking nights.', 2, 'Thursdays 7:00 PM, Student Center 204', 'caribbean@example.com', 'Follow us on TikTok for event updates.', 'https://example.com/instagram/caribbean', 'https://example.com/caribbean', 'https://example.com/tiktok/caribbean', TRUE),
('Latin American Student Association', 'Cultural events, dance nights and community service highlighting Latin American heritage.', 2, 'Tuesdays 6:00 PM, Student Center 210', 'lasa@example.com', 'Bilingual meetings in English and Spanish.', 'https://example.com/instagram/lasa', 'https://example.com/lasa', NULL, TRUE),
('Gospel Choir', 'Student-led gospel choir performing at campus events and churches across the region. No audition needed for rehearsals.', 2, 'Mondays and Wednesdays 7:30 PM, Fine Arts Hall', 'gospel@example.com', 'Rehearsals are open, just show up.', 'https://example.com/instagram/gospel', 'https://example.com/gospel', NULL, TRUE),
('Anime and Gaming Club', 'Anime screenings, tabletop games and casual video game tournaments.', 2, 'Fridays 7:00 PM, Student Center Game Room', 'anime@example.com', 'All skill levels welcome, consoles provided.', 'https://example.com/instagram/anime', 'https://example.com/anime', 'https://example.com/discord/anime', TRUE),
('Poetry and Spoken Word Club', 'Open mics, writing workshops and slam poetry competitions. Share your voice or just come listen.', 2, 'Thursdays 8:00 PM, Campus Cafe', 'poetry@example.com', 'Share your voice or just come listen.', 'https://example.com/instagram/poetry', 'https://example.com/poetry', NULL, TRUE),
('International Students Club', 'A home away from home for international students: cultural nights, trips and help adjusting to campus life.', 2, 'Wednesdays 5:00 PM, International Office', 'international@example.com', 'Help with campus life for new international students.', 'https://example.com/instagram/international', 'https://example.com/international', NULL, TRUE),
('Dance Collective', 'Hip hop, contemporary and step. Weekly practice plus two showcases each semester.', 2, 'Tuesdays and Thursdays 8:00 PM, Gym Studio', 'dance@example.com', 'No experience needed for practice.', 'https://example.com/instagram/dance', 'https://example.com/dance', 'https://example.com/tiktok/dance', TRUE),
('Retro Film Society', 'Classic movie screenings and discussion. Currently inactive.', 2, 'Not meeting this semester', 'film@example.com', NULL, NULL, NULL, NULL, FALSE),
('Soccer Club', 'Co-ed recreational soccer with pickup games and intramural league teams.', 3, 'Saturdays 10:00 AM, South Field', 'soccer@example.com', 'Cleats recommended; shin guards required for games.', 'https://example.com/instagram/soccer', 'https://example.com/soccer', NULL, TRUE),
('Basketball Club', 'Pickup games, skills clinics and intramural teams for all skill levels.', 3, 'Mondays and Wednesdays 8:00 PM, Main Gym', 'basketball@example.com', 'Bring sneakers and water.', 'https://example.com/instagram/basketball', 'https://example.com/basketball', NULL, TRUE),
('Running Club', 'Group runs for every pace, from casual 5K to half marathon training.', 3, 'Tuesdays and Thursdays 6:30 AM, Track Entrance', 'running@example.com', 'All paces welcome.', 'https://example.com/instagram/running', 'https://example.com/running', 'https://example.com/strava/running', TRUE),
('Volleyball Club', 'Indoor and sand volleyball practices plus weekend tournaments.', 3, 'Sundays 2:00 PM, Rec Center Court 2', 'volleyball@example.com', 'Knee pads recommended.', 'https://example.com/instagram/volleyball', 'https://example.com/volleyball', NULL, TRUE),
('Martial Arts Club', 'Self-defense, karate and Brazilian jiu-jitsu taught by certified student instructors.', 3, 'Mondays and Thursdays 6:00 PM, Rec Center Mat Room', 'martialarts@example.com', 'Wear comfortable workout clothes.', 'https://example.com/instagram/martialarts', 'https://example.com/martialarts', NULL, TRUE),
('Esports Club', 'Competitive and casual gaming teams competing in collegiate esports leagues.', 3, 'Wednesdays 8:00 PM, Tech Center Lab 110', 'esports@example.com', 'Bring your own headset if you have one.', 'https://example.com/instagram/esports', 'https://example.com/esports', 'https://example.com/twitch/esports', TRUE),
('Swim Club', 'Lap swimming, stroke clinics and friendly meets. Must be able to swim one pool length.', 3, 'Tuesdays 7:00 PM, Aquatic Center', 'swim@example.com', 'Must be able to swim one pool length.', 'https://example.com/instagram/swim', 'https://example.com/swim', NULL, TRUE),
('Tennis Club', 'Rallies, drills and doubles ladders. Rackets available to borrow.', 3, 'Saturdays 9:00 AM, Campus Courts', 'tennis@example.com', 'Rackets available to borrow.', 'https://example.com/instagram/tennis', 'https://example.com/tennis', NULL, TRUE),
('Habitat Builders', 'Volunteer construction days building affordable housing with local partners.', 4, 'Saturdays 8:00 AM, meet at Student Center', 'habitat@example.com', 'No construction experience needed; we train you.', 'https://example.com/instagram/habitat', 'https://example.com/habitat', NULL, TRUE),
('Campus Food Pantry Volunteers', 'Stock shelves, run food drives and help fellow students access free groceries.', 4, 'Flexible shifts, Student Center 101', 'pantry@example.com', 'Sign up for shifts by email.', 'https://example.com/instagram/pantry', 'https://example.com/pantry', NULL, TRUE),
('Tutoring Corps', 'Volunteer tutors helping local middle and high school students with math, reading and science.', 4, 'Weekdays 3:30 PM, local schools', 'tutoring@example.com', 'Training provided for new tutors.', NULL, 'https://example.com/tutoring', NULL, TRUE),
('Green Campus Club', 'Recycling drives, campus garden work days and sustainability awareness events.', 4, 'Thursdays 4:30 PM, Campus Garden', 'green@example.com', 'Gloves and tools provided.', 'https://example.com/instagram/green', 'https://example.com/green', NULL, TRUE),
('Blood Drive Committee', 'Organizes campus blood drives with regional blood banks every semester.', 4, 'First Monday of each month 6:00 PM, Health Center', 'blood@example.com', 'Volunteers and donors both welcome.', 'https://example.com/instagram/blood', 'https://example.com/blood', NULL, TRUE),
('Mentoring Club', 'Upper-class students mentor first-year and transfer students through their first semester.', 4, 'Wednesdays 6:00 PM, Student Center 220', 'mentoring@example.com', 'Mentors are matched by major when possible.', 'https://example.com/instagram/mentoring', 'https://example.com/mentoring', NULL, TRUE),
('Animal Shelter Volunteers', 'Weekend volunteering at the local animal shelter: walking dogs, socializing cats and adoption events.', 4, 'Sundays 11:00 AM, carpool from Lot C', 'animals@example.com', 'Carpool leaves Lot C at 10:45 AM.', 'https://example.com/instagram/animals', 'https://example.com/animals', NULL, TRUE);

-- Officers (FR-13): fictional names and roles for every active club. Chess Club's
-- officers are added by demo mode (the demo leader is its President).
INSERT INTO club_officer (club_id, name, role, display_order)
SELECT c.club_id, o.name, o.role, o.ord FROM club c JOIN (
  SELECT 'Engineering Builders Society' AS club, 'Aisha Johnson' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Engineering Builders Society' AS club, 'Kevin Tran' AS name, 'Build Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Engineering Builders Society' AS club, 'Rachel Ortiz' AS name, 'Safety Officer' AS role, 3 AS ord UNION ALL
  SELECT 'Pre-Med Club' AS club, 'Nia Thompson' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Pre-Med Club' AS club, 'Samuel Okoye' AS name, 'MCAT Study Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Pre-Med Club' AS club, 'Lily Chen' AS name, 'Shadowing Coordinator' AS role, 3 AS ord UNION ALL
  SELECT 'Computer Science Club' AS club, 'Maya Robinson' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Computer Science Club' AS club, 'Daniel Brooks' AS name, 'Vice President' AS role, 2 AS ord UNION ALL
  SELECT 'Computer Science Club' AS club, 'Sofia Alvarez' AS name, 'Hackathon Coordinator' AS role, 3 AS ord UNION ALL
  SELECT 'Debate and Forensics Team' AS club, 'Jamal Carter' AS name, 'Captain' AS role, 1 AS ord UNION ALL
  SELECT 'Debate and Forensics Team' AS club, 'Emily Rivers' AS name, 'Coach Liaison' AS role, 2 AS ord UNION ALL
  SELECT 'Math Circle' AS club, 'Andre Wilson' AS name, 'Organizer' AS role, 1 AS ord UNION ALL
  SELECT 'Math Circle' AS club, 'Hana Suzuki' AS name, 'Tutoring Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Business Leaders Club' AS club, 'Jasmine Reed' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Business Leaders Club' AS club, 'Carlos Mendez' AS name, 'VP of Events' AS role, 2 AS ord UNION ALL
  SELECT 'Business Leaders Club' AS club, 'Ethan Park' AS name, 'Treasurer' AS role, 3 AS ord UNION ALL
  SELECT 'Psychology Club' AS club, 'Grace Adams' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Psychology Club' AS club, 'Omar Haddad' AS name, 'Research Liaison' AS role, 2 AS ord UNION ALL
  SELECT 'African Student Union' AS club, 'Amara Okafor' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'African Student Union' AS club, 'Kwame Mensah' AS name, 'Events Chair' AS role, 2 AS ord UNION ALL
  SELECT 'African Student Union' AS club, 'Zainab Bello' AS name, 'Secretary' AS role, 3 AS ord UNION ALL
  SELECT 'Caribbean Students Club' AS club, 'Andre Baptiste' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Caribbean Students Club' AS club, 'Keisha Morgan' AS name, 'Events Chair' AS role, 2 AS ord UNION ALL
  SELECT 'Latin American Student Association' AS club, 'Valeria Cruz' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Latin American Student Association' AS club, 'Mateo Rojas' AS name, 'Vice President' AS role, 2 AS ord UNION ALL
  SELECT 'Latin American Student Association' AS club, 'Isabella Diaz' AS name, 'Outreach Chair' AS role, 3 AS ord UNION ALL
  SELECT 'Gospel Choir' AS club, 'Naomi Wright' AS name, 'Director' AS role, 1 AS ord UNION ALL
  SELECT 'Gospel Choir' AS club, 'Joshua King' AS name, 'Section Leader' AS role, 2 AS ord UNION ALL
  SELECT 'Anime and Gaming Club' AS club, 'Kenji Watanabe' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Anime and Gaming Club' AS club, 'Chloe Martin' AS name, 'Tournament Organizer' AS role, 2 AS ord UNION ALL
  SELECT 'Poetry and Spoken Word Club' AS club, 'Imani Jackson' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Poetry and Spoken Word Club' AS club, 'Darius Hall' AS name, 'Open Mic Host' AS role, 2 AS ord UNION ALL
  SELECT 'International Students Club' AS club, 'Priyanka Sharma' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'International Students Club' AS club, 'Lucas Ferreira' AS name, 'Buddy Program Lead' AS role, 2 AS ord UNION ALL
  SELECT 'International Students Club' AS club, 'Mei Lin' AS name, 'Events Chair' AS role, 3 AS ord UNION ALL
  SELECT 'Dance Collective' AS club, 'Brianna Lewis' AS name, 'Captain' AS role, 1 AS ord UNION ALL
  SELECT 'Dance Collective' AS club, 'Tyrese Coleman' AS name, 'Choreographer' AS role, 2 AS ord UNION ALL
  SELECT 'Soccer Club' AS club, 'Luis Moreno' AS name, 'Captain' AS role, 1 AS ord UNION ALL
  SELECT 'Soccer Club' AS club, 'Grace Kim' AS name, 'Treasurer' AS role, 2 AS ord UNION ALL
  SELECT 'Basketball Club' AS club, 'Marcus Johnson' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Basketball Club' AS club, 'Tiana Brooks' AS name, 'Intramural Coordinator' AS role, 2 AS ord UNION ALL
  SELECT 'Running Club' AS club, 'Tyler Evans' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Running Club' AS club, 'Sarah Nguyen' AS name, 'Training Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Volleyball Club' AS club, 'Ashley Turner' AS name, 'Captain' AS role, 1 AS ord UNION ALL
  SELECT 'Volleyball Club' AS club, 'Jordan Price' AS name, 'Vice Captain' AS role, 2 AS ord UNION ALL
  SELECT 'Martial Arts Club' AS club, 'David Kim' AS name, 'Head Instructor' AS role, 1 AS ord UNION ALL
  SELECT 'Martial Arts Club' AS club, 'Erica Santos' AS name, 'Assistant Instructor' AS role, 2 AS ord UNION ALL
  SELECT 'Esports Club' AS club, 'Ryan Patel' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Esports Club' AS club, 'Alexis Moore' AS name, 'Team Manager' AS role, 2 AS ord UNION ALL
  SELECT 'Swim Club' AS club, 'Hannah Brooks' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Swim Club' AS club, 'Isaiah Grant' AS name, 'Coach Liaison' AS role, 2 AS ord UNION ALL
  SELECT 'Tennis Club' AS club, 'Sophie Laurent' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Tennis Club' AS club, 'Malik Robinson' AS name, 'Ladder Coordinator' AS role, 2 AS ord UNION ALL
  SELECT 'Habitat Builders' AS club, 'Hannah Lee' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Habitat Builders' AS club, 'Marcus Bell' AS name, 'Volunteer Coordinator' AS role, 2 AS ord UNION ALL
  SELECT 'Campus Food Pantry Volunteers' AS club, 'Danielle Price' AS name, 'Pantry Manager' AS role, 1 AS ord UNION ALL
  SELECT 'Campus Food Pantry Volunteers' AS club, 'Kofi Asante' AS name, 'Food Drive Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Tutoring Corps' AS club, 'Olivia Scott' AS name, 'Coordinator' AS role, 1 AS ord UNION ALL
  SELECT 'Tutoring Corps' AS club, 'Brandon Lee' AS name, 'Math Tutor Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Green Campus Club' AS club, 'Maya Patel' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Green Campus Club' AS club, 'Jonah Fisher' AS name, 'Garden Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Blood Drive Committee' AS club, 'Alicia Ramos' AS name, 'Chair' AS role, 1 AS ord UNION ALL
  SELECT 'Blood Drive Committee' AS club, 'Derek Owens' AS name, 'Volunteer Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Mentoring Club' AS club, 'Christopher Hayes' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Mentoring Club' AS club, 'Ana Gutierrez' AS name, 'Mentor Matching Lead' AS role, 2 AS ord UNION ALL
  SELECT 'Animal Shelter Volunteers' AS club, 'Emma Collins' AS name, 'President' AS role, 1 AS ord UNION ALL
  SELECT 'Animal Shelter Volunteers' AS club, 'Noah Rivera' AS name, 'Carpool Coordinator' AS role, 2 AS ord
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
  SELECT 'Engineering Builders Society' AS club, 'STEM day with local middle schoolers' AS title, 'Volunteers needed to run hands-on stations.' AS description, 13 AS days, '09:00:00' AS start_time, '13:00:00' AS end_time, 'Engineering Building Atrium' AS location UNION ALL
  SELECT 'Pre-Med Club' AS club, 'Talk: a day in the life of an ER doctor' AS title, 'Q&A with a local emergency physician.' AS description, 9 AS days, '19:00:00' AS start_time, '20:30:00' AS end_time, 'Science Hall 101' AS location UNION ALL
  SELECT 'Computer Science Club' AS club, 'Intro to Git workshop' AS title, 'Hands-on session for beginners.' AS description, 3 AS days, '18:30:00' AS start_time, '20:00:00' AS end_time, 'Tech Center 305' AS location UNION ALL
  SELECT 'Computer Science Club' AS club, 'Mini hackathon' AS title, 'Teams of 2 to 4. Pizza provided.' AS description, 10 AS days, '10:00:00' AS start_time, '16:00:00' AS end_time, 'Tech Center Lab 110' AS location UNION ALL
  SELECT 'Debate and Forensics Team' AS club, 'Practice tournament' AS title, 'Two rounds of policy debate; judges provided.' AS description, 6 AS days, '16:00:00' AS start_time, '19:00:00' AS end_time, 'Humanities 120' AS location UNION ALL
  SELECT 'Math Circle' AS club, 'Puzzle hunt' AS title, 'Solve 10 puzzles across campus in teams.' AS description, 5 AS days, '15:00:00' AS start_time, '17:00:00' AS end_time, 'Science Hall 214' AS location UNION ALL
  SELECT 'Business Leaders Club' AS club, 'Resume workshop' AS title, 'Bring a printed resume for feedback.' AS description, 4 AS days, '19:00:00' AS start_time, '20:30:00' AS end_time, 'Business Building 150' AS location UNION ALL
  SELECT 'Business Leaders Club' AS club, 'Networking night with local entrepreneurs' AS title, 'Business casual. Light snacks.' AS description, 15 AS days, '18:00:00' AS start_time, '20:00:00' AS end_time, 'Business Building Atrium' AS location UNION ALL
  SELECT 'Psychology Club' AS club, 'Grad school panel' AS title, 'Current psychology grad students answer your questions.' AS description, 8 AS days, '17:00:00' AS start_time, '18:30:00' AS end_time, 'Social Sciences 230' AS location UNION ALL
  SELECT 'African Student Union' AS club, 'Culture night' AS title, 'Food, music and a fashion showcase.' AS description, 6 AS days, '19:00:00' AS start_time, '22:00:00' AS end_time, 'Student Center Ballroom' AS location UNION ALL
  SELECT 'Caribbean Students Club' AS club, 'Caribbean cooking night' AS title, 'Learn to make jerk chicken and festival.' AS description, 11 AS days, '18:00:00' AS start_time, '20:00:00' AS end_time, 'Student Center Kitchen' AS location UNION ALL
  SELECT 'Latin American Student Association' AS club, 'Salsa night' AS title, 'Free lesson for beginners, then open dancing.' AS description, 5 AS days, '19:30:00' AS start_time, '22:00:00' AS end_time, 'Student Center 210' AS location UNION ALL
  SELECT 'Gospel Choir' AS club, 'Open rehearsal' AS title, 'Come sing with us, no audition.' AS description, 1 AS days, '19:30:00' AS start_time, '21:00:00' AS end_time, 'Fine Arts Hall' AS location UNION ALL
  SELECT 'Gospel Choir' AS club, 'Winter concert' AS title, 'Free and open to everyone.' AS description, 18 AS days, '19:00:00' AS start_time, '20:30:00' AS end_time, 'Fine Arts Auditorium' AS location UNION ALL
  SELECT 'Anime and Gaming Club' AS club, 'Smash tournament' AS title, 'Sign up at the door.' AS description, 7 AS days, '19:00:00' AS start_time, '23:00:00' AS end_time, 'Student Center Game Room' AS location UNION ALL
  SELECT 'Poetry and Spoken Word Club' AS club, 'Open mic night' AS title, 'Sign-up sheet at the door; 5 minutes each.' AS description, 4 AS days, '20:00:00' AS start_time, '22:00:00' AS end_time, 'Campus Cafe' AS location UNION ALL
  SELECT 'International Students Club' AS club, 'Global potluck' AS title, 'Bring a dish from home and share it.' AS description, 9 AS days, '18:00:00' AS start_time, '20:00:00' AS end_time, 'International Office Lounge' AS location UNION ALL
  SELECT 'Dance Collective' AS club, 'Showcase auditions' AS title, 'Prepare a 1-minute routine in any style.' AS description, 12 AS days, '18:00:00' AS start_time, '21:00:00' AS end_time, 'Gym Studio' AS location UNION ALL
  SELECT 'Soccer Club' AS club, 'Pickup game' AS title, 'All skill levels.' AS description, 4 AS days, '10:00:00' AS start_time, '12:00:00' AS end_time, 'South Field' AS location UNION ALL
  SELECT 'Basketball Club' AS club, '3-on-3 tournament' AS title, 'Register your team at the door.' AS description, 10 AS days, '18:00:00' AS start_time, '21:00:00' AS end_time, 'Main Gym' AS location UNION ALL
  SELECT 'Running Club' AS club, 'Sunrise 5K' AS title, 'Easy group pace.' AS description, 5 AS days, '06:30:00' AS start_time, '07:30:00' AS end_time, 'Track Entrance' AS location UNION ALL
  SELECT 'Volleyball Club' AS club, 'Sand volleyball social' AS title, 'Mixed teams, all levels.' AS description, 7 AS days, '14:00:00' AS start_time, '16:30:00' AS end_time, 'Rec Center Sand Courts' AS location UNION ALL
  SELECT 'Martial Arts Club' AS club, 'Self-defense workshop' AS title, 'Open to everyone, no experience needed.' AS description, 6 AS days, '18:00:00' AS start_time, '19:30:00' AS end_time, 'Rec Center Mat Room' AS location UNION ALL
  SELECT 'Esports Club' AS club, 'Tryouts for the varsity roster' AS title, 'Ranked players encouraged; casual teams too.' AS description, 9 AS days, '19:00:00' AS start_time, '22:00:00' AS end_time, 'Tech Center Lab 110' AS location UNION ALL
  SELECT 'Swim Club' AS club, 'Stroke clinic' AS title, 'Improve freestyle and backstroke technique.' AS description, 8 AS days, '19:00:00' AS start_time, '20:30:00' AS end_time, 'Aquatic Center' AS location UNION ALL
  SELECT 'Tennis Club' AS club, 'Doubles ladder kickoff' AS title, 'Partners assigned at the door.' AS description, 3 AS days, '09:00:00' AS start_time, '11:00:00' AS end_time, 'Campus Courts' AS location UNION ALL
  SELECT 'Habitat Builders' AS club, 'Build day' AS title, 'Closed-toe shoes required.' AS description, 8 AS days, '08:00:00' AS start_time, '14:00:00' AS end_time, 'Meet at Student Center' AS location UNION ALL
  SELECT 'Campus Food Pantry Volunteers' AS club, 'Holiday food drive' AS title, 'Help sort and pack donations.' AS description, 14 AS days, '12:00:00' AS start_time, '16:00:00' AS end_time, 'Student Center 101' AS location UNION ALL
  SELECT 'Tutoring Corps' AS club, 'New tutor orientation' AS title, 'Required before your first session.' AS description, 5 AS days, '16:00:00' AS start_time, '17:30:00' AS end_time, 'Library Room 105' AS location UNION ALL
  SELECT 'Green Campus Club' AS club, 'Campus garden workday' AS title, 'Gloves provided.' AS description, 3 AS days, '16:30:00' AS start_time, '18:00:00' AS end_time, 'Campus Garden' AS location UNION ALL
  SELECT 'Blood Drive Committee' AS club, 'Fall blood drive' AS title, 'Walk-ins welcome; bring a photo ID.' AS description, 16 AS days, '10:00:00' AS start_time, '16:00:00' AS end_time, 'Health Center' AS location UNION ALL
  SELECT 'Mentoring Club' AS club, 'Mentor meetup' AS title, 'Meet your mentor over snacks.' AS description, 2 AS days, '18:00:00' AS start_time, '19:30:00' AS end_time, 'Student Center 220' AS location UNION ALL
  SELECT 'Animal Shelter Volunteers' AS club, 'Adoption event' AS title, 'Help families meet their new pets.' AS description, 11 AS days, '11:00:00' AS start_time, '15:00:00' AS end_time, 'City Animal Shelter' AS location
) e ON e.club = c.club_name;
