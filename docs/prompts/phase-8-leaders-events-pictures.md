# Build Prompt: Phase 8: Club Leaders, Events and Pictures

## How to use this

1. Check out the latest branch (`phase-7-final-polish`, or `main` once it's merged).
2. Paste the **Master Prompt** (Part A) into your AI coding tool once.
3. Then paste **one part prompt at a time** (Part B: 8A, then 8B, then 8C). Each is its own branch, pull request and demo checkpoint.
4. After each part: `./mvnw test`, the manual checks it lists, and a commit.

Why three parts: each feature touches the database, a service, the dashboard, the public club page, tests and the offline preview. Doing them separately keeps every pull request reviewable and every step demo-ready.

---

## Part A: Master Prompt (paste once)

```text
You are a senior Java/Spring Boot developer adding three features to an
existing, finished student project: "Spartan Club Central" (NSU Club Finder).
Read docs/architecture.md, docs/traceability.md and docs/run-locally.md
FIRST, then inspect the code. Build on what exists; do not restructure it.

=== WHAT EXISTS (do not change these decisions) ===
- Spring Boot 4.1.x, Java 17+, Maven wrapper, Thymeleaf, Spring Security,
  Spring Data JPA, MySQL 8. Package: com.spartanclubcentral.Spartan.Club
- Layers: templates -> controllers -> services -> repositories -> MySQL.
  Controllers never use repositories. All rules live in services.
- Hosting: LOCAL laptop only, for class demos. $0 budget. No cloud storage,
  no CDNs, no external images or fonts. Mailpit for email.
- database/schema.sql is the only place tables are defined;
  spring.jpa.hibernate.ddl-auto=validate. FieldLimits.java holds every max
  length and matches the column sizes.
- Ownership rule: every leader action on a club calls
  ClubService.requireOwnedClub(leaderId, clubId) first. Leader id comes from
  @AuthenticationPrincipal AccountPrincipal, never from the URL or a form.
  Violations -> 403 page + OWNERSHIP_DENIED audit entry.
- Every change is a POST with a CSRF token (th:action). Templates use th:text
  only (never th:utext). Errors: summary at top + message next to each field
  with aria-describedby; typed values kept; passwords never re-rendered.
- Strict Content-Security-Policy: default-src 'self'; img-src 'self' data:;
  no inline scripts or styles anywhere. Keep it that way.
- Audit log (AuditLogger): account ids and actions only, never names,
  emails, messages or file names typed by users.
- One leader account per club (club_leader.club_id). Demo mode
  (SPRING_PROFILES_ACTIVE=demo) seeds demo.leader@clubcentral.local, who leads
  Chess Club.
- Tests: JUnit 5, Mockito, MockMvc, H2 (MySQL mode). Patterns to copy:
  DashboardControllerTest (ownership: try every action as another leader),
  PageRenderWithoutTransactionTest (render pages without a test transaction
  to catch lazy-loading bugs), ClubValidatorTest, SecurityHeadersTest.
- Offline preview docs/local-run.html mirrors the site in plain JS.

=== THE THREE FEATURES ===
8A  Club leaders/officers shown on the club page          (new FR-13)
8B  Club events on the club page, managed by the leader   (new FR-14)
8C  Relevant pictures: a club photo/logo uploaded by the  (new FR-15)
    leader, with a built-in picture for each category as the default

=== RULES FOR ALL THREE ===
1. Schema changes go in BOTH database/schema.sql (fresh installs) and a new
   database/migrations/002-leaders-events-pictures.sql that upgrades an
   existing database without losing data (CREATE TABLE / ALTER TABLE ADD
   COLUMN only). Update database/reset-demo.sql and database/sample-data.sql.
   Explain in docs/run-locally.md how to run the migration in Workbench and
   from the command line.
2. New lengths go in FieldLimits and match the columns exactly.
3. Leaders manage these only for their own club, through the dashboard,
   with requireOwnedClub() before every read or write. Add each new route
   to the ownership test: Leader A tries it on Leader B's club -> 403,
   nothing changes, OWNERSHIP_DENIED logged.
4. Hidden (inactive) clubs: their officers, events and pictures are not
   visible to students either (404, like the club page).
5. Deleting a club deletes its officers, events and picture file too.
6. Accessibility (NFR-06): labels on every input, real <time> elements for
   dates, alt text on every image, keyboard-usable buttons, no information
   shown by color alone. Pages must fit 320 px wide with no sideways scroll.
7. Privacy (NFR-02): never show any person's email address publicly
   (the club's contact_email is the only public email). Officer names are
   shown only if the leader adds them.
8. Audit new leader actions: OFFICERS_UPDATED, EVENT_CREATED, EVENT_EDITED,
   EVENT_DELETED, CLUB_IMAGE_UPLOADED, CLUB_IMAGE_REMOVED (target CLUB #id).
9. Update: docs/architecture.md (tables, routes), docs/traceability.md (new
   FR-13 to FR-15 rows with code + tests), docs/run-locally.md (checklist
   table for the new features, new test count), README status, and
   docs/local-run.html (the offline preview must show and simulate the new
   features with the same rules and messages).
10. If the existing code or my requirements conflict with these
    instructions, stop and tell me before choosing.

=== HOW TO WORK ===
- One part at a time, on its own branch from the previous part.
- For each part give me: files changed, full code (no "..." placeholders),
  the SQL, the commands to build/test/run, a manual check list, and the
  requirement/test IDs covered.
- Prove it works: ./mvnw clean verify passes; run it against real MySQL
  (load schema or migration + sample data) and walk the manual checks in a
  browser, including at 320 px; confirm no CSP errors in the browser console.

Reply now with: your summary of the current code relevant to these three
features, any conflicts you see, and your proposed table designs. No code yet.
```

---

## Part B: Part prompts (paste one at a time)

### 8A: Club leaders / officers on the club page

```text
PART 8A: Show who leads each club (FR-13: "The club profile page shall show
the names and roles of the club's officers").

Design:
- New table club_officer:
    officer_id BIGINT PK AUTO_INCREMENT,
    club_id BIGINT NOT NULL FK -> club ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL           (e.g. President, Treasurer),
    display_order INT NOT NULL,
    INDEX (club_id, display_order)
  Up to 10 officers per club. Names and roles only: no emails, phone
  numbers or photos of people (privacy).
- When a leader creates their club, add them automatically as the first
  officer (their account name and position, or "Club leader" if position is
  blank). They can edit or remove that row like any other.
- Entity ClubOfficer; OfficerRepository; OfficerForm; OfficerValidator
  (name required <= 100, role required <= 50); methods in ClubService:
  listOfficers(clubId) for the public page (active clubs only),
  addOfficer / updateOfficer / removeOfficer / moveOfficer(up|down), each
  calling requireOwnedClub first; the officer must belong to that club
  (an officer id from another club -> 403, logged).
- Dashboard: an "Officers" card on /dashboard and a page
  GET /dashboard/clubs/{id}/officers with the list, an add form, edit and
  remove buttons (POST), and move up/down buttons (POST). Remove asks
  for confirmation on a separate page, like club delete.
- Public club page: an "Officers" section in the Details card:
  "Name, Role" list in display order. Hidden when the club has none.
- Sample data: 2 to 3 fictional officers for about 10 sample clubs.
  Demo mode: Chess Club gets "Demo Leader, President" plus 2 more.
- Admin page: an "Officers" count column in the Clubs table.

Tests: officer validator; public page shows officers in order and never an
email; ownership test for every new route (including using an officer id
from another club); delete club removes officers; render the officers page
in PageRenderWithoutTransactionTest; 10-officer limit message.

Manual checks: demo leader adds "Priya Patel, Treasurer", moves her up,
public Chess Club page shows the new order; second leader trying
/dashboard/clubs/1/officers gets 403.
```

### 8B: Club events on the club page

```text
PART 8B: Club events (FR-14: "Club leaders shall be able to post events,
and the club profile page shall show upcoming events").

Design:
- New table club_event:
    event_id BIGINT PK AUTO_INCREMENT,
    club_id BIGINT NOT NULL FK -> club ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NULL,
    location VARCHAR(150) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX (club_id, starts_at), INDEX (starts_at)
  Times are campus local time (LocalDateTime). Use DATETIME, not TIMESTAMP,
  so MySQL doesn't shift times by time zone.
- Entity ClubEvent; EventRepository with
  findByClub_ClubIdAndStartsAtGreaterThanEqualOrderByStartsAt (upcoming) and
  a past-events query; EventForm (separate date and time inputs: <input
  type="date"> and <input type="time">, which work without JavaScript);
  EventValidator: title required <= 100, description <= 1000, location <=
  150, start date/time required and valid, end must be after start, start
  no more than 1 year ahead; new events can't start in the past (editing an
  existing past event is allowed).
- EventService (or methods in ClubService): createEvent, updateEvent,
  deleteEvent, each with requireOwnedClub and a check that the event belongs
  to that club; upcomingForClub(clubId, limit 5) for active clubs only.
- Dashboard: an "Events" card with the next event and a link to
  GET /dashboard/clubs/{id}/events (list of upcoming and past events with
  Edit and Delete), GET .../events/new, POST .../events, GET
  .../events/{eventId}/edit, POST .../events/{eventId},
  GET+POST .../events/{eventId}/delete (confirm page).
- Public club page: "Upcoming events" section above the join form showing
  up to 5 future events: title, <time datetime="..."> formatted like
  "Thu, Nov 20 · 6:00 PM to 8:00 PM", location, description (th:text,
  white-space: pre-line). "No upcoming events" when empty. Past events never
  shown publicly.
- Homepage: optional "Coming up this week" strip with the next 6 events
  across all ACTIVE clubs (title, club name link, date). Skip if it makes
  the homepage cluttered; ask me.
- Sample data: events relative to the load date so they're never in the
  past, e.g. starts_at = DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 18
  HOUR, for about 10 clubs; 2 for Chess Club.

Tests: validator (end before start, past start, too far ahead, lengths);
upcoming list excludes past events and hidden clubs and is sorted; ownership
test for every event route, including editing another club's event id
through your own club's URL; delete club removes events; render tests
without a transaction; escaping of <script> in title/description.

Manual checks: demo leader adds "Blitz tournament" next Friday 6-8 PM;
it appears on the public Chess Club page in the right place in the list;
editing it to end before it starts shows the error; past events don't show.
```

### 8C: Relevant pictures

```text
PART 8C: Pictures (FR-15: "Each club page shall show a picture: a photo or
logo uploaded by the club leader, or a default picture for its category").

Design: two layers, so every club has a relevant picture even before
leaders upload anything.

1. Default category pictures (no upload needed):
   - Four simple, original SVG illustrations in
     src/main/resources/static/images/categories/ (academic.svg,
     cultural.svg, sports.svg, service.svg), flat shapes in the site's green
     and gold, e.g. a book, a globe, a ball, a helping hand. Create them as
     SVG code yourself. No downloaded or copyrighted images, no external
     URLs. Mark them aria-hidden/alt="" where purely decorative.
   - Category is chosen by name, falling back to a generic picture for any
     new category.
   - Use them on homepage category cards, directory club cards (small
     thumbnail) and the club page header when the club has no photo.

2. Club photo or logo uploaded by the leader:
   - Columns on club: image_filename VARCHAR(64) NULL (server-generated
     name, e.g. a UUID + ".jpg"), image_alt VARCHAR(150) NULL.
     FieldLimits: IMAGE_ALT = 150, IMAGE_MAX_BYTES = 2 MB.
   - Files are stored on disk OUTSIDE the project and outside static/, in a
     folder from an environment variable: UPLOAD_DIR (default
     ./uploads, which must be in .gitignore). Never trust the uploaded
     file name; never store the image in the database or in Git.
   - ClubImageService:
       upload(leaderId, clubId, MultipartFile, alt): requireOwnedClub;
       reject empty files, > 2 MB, and anything that isn't really a JPEG or
       PNG (check the file's first bytes, not the extension or the browser's
       content type); read it with ImageIO and RE-ENCODE it, at most
       1200 px wide, as JPEG. Re-encoding removes hidden data such as GPS
       location and anything malicious inside the file. Alt text required
       (1 to 150 chars) for accessibility. Replacing deletes the old file.
       remove(leaderId, clubId): requireOwnedClub, delete the file, clear
       the columns.
   - GET /clubs/{id}/image serves the file for ACTIVE clubs only (404
     otherwise), with Content-Type image/jpeg,
     X-Content-Type-Options: nosniff and Cache-Control max-age=3600. The URL
     uses the club id, never a file path, so nobody can request
     ../../anything.
   - Dashboard: "Club picture" card with a preview, an upload form
     (enctype="multipart/form-data", th:action so the CSRF token is
     included; confirm the token is accepted for multipart forms and fix it
     properly if not, without disabling CSRF), the alt text field with a hint
     ("Describe the picture, e.g. 'Chess Club members at the fall
     tournament'"), and a Remove button.
   - application.properties: spring.servlet.multipart.max-file-size=2MB,
     max-request-size=3MB; a too-large upload shows a friendly message on
     the dashboard, not an error page.
   - Club page and directory cards show the uploaded picture with its alt
     text; otherwise the category picture.
   - Guidance shown under the upload field: "Use a logo or a group photo
     everyone in it agreed to share. Don't upload photos of people without
     their permission."
   - Deleting a club deletes its picture file. reset-demo.sql can't delete
     files, so document "delete the uploads folder" in the reset steps.
   - Admin page: a "Picture" column (yes/no) and a "Remove picture" button
     for inappropriate images (AdminService, audit ADMIN as actor).
   - Demo mode: Chess Club gets a generated sample logo (draw a simple
     chess-piece logo with Java2D at startup into UPLOAD_DIR) so the demo
     shows an uploaded picture.

CSP stays as it is (img-src 'self' data:): all pictures come from this site.

Tests: magic-byte check rejects a PNG renamed .exe and a text file named
.jpg; > 2 MB rejected; re-encoded output is a valid JPEG at most 1200 px wide
with no EXIF; alt text required; ownership test for upload/remove;
/clubs/{id}/image for a hidden club -> 404; path traversal attempts like
/clubs/1/image/../../etc -> 404; delete club removes the file (use a JUnit
@TempDir as UPLOAD_DIR); CSRF required on the multipart form; render tests
with and without a picture.

Manual checks: upload a phone photo (check it's resized and the file in
UPLOAD_DIR has no GPS data), see it on the club page and directory card,
remove it and see the category picture return; try a 5 MB file and a .txt
renamed .jpg; admin removes a picture.
```

---

## Part C: Updating the offline preview (paste after each part)

```text
Update docs/local-run.html to match what you just built, following how the
file already simulates the site (state in sessionStorage, same validation
messages as the Java validators, ownership checks with forbiddenPage() and
an OWNERSHIP_DENIED audit entry):
- 8A: officers on club pages; dashboard officers page with add/edit/remove/
  move; sample officers.
- 8B: upcoming events on club pages (and the homepage strip if built);
  dashboard events pages with the same date/time rules; sample events
  relative to today.
- 8C: inline the four category SVGs; show them on cards and club pages;
  simulate uploads with <input type="file"> + FileReader into a data: URL
  (2 MB limit, JPEG/PNG only, alt text required; preview only, nothing
  leaves the browser), and say so on the page.
Then test every new screen in a browser at 1280 px and 320 px with an
accessibility scan, and update the "Run the real app" checklist and test count.
```

---

## Part D: Checklist after each part

- [ ] `./mvnw clean verify` passes; new tests cover validation, ownership, hidden clubs, deletion and rendering
- [ ] Migration script tested on a database that already has data (nothing lost)
- [ ] Manual checks done on real MySQL, including 320 px width and no console errors
- [ ] No emails of people on public pages; no user-typed text or file names in the audit log
- [ ] Docs, traceability and `local-run.html` updated
- [ ] Branch pushed, pull request opened, a teammate reviewed it
