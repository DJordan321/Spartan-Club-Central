# Spartan Club Central

## Project Description

At NSU, students often struggle to find clubs and organizations that match their interests. Information is scattered across Instagram pages, outdated Google Forms, printed flyers, and a once-a-semester club fair that is easy to miss. New students, especially first-years and transfers, have no single reliable place to see what is available.

Spartan Club Central is a web application that serves as one centralized, searchable directory of student clubs. Students can search and filter clubs, read club profiles, and send a join or contact request directly to club leadership, with no account required. Club leaders log in to a protected dashboard to create, edit, and deactivate their own listings without needing developer support.

## Team Members

| Name | Roles |
|---|---|
| Dennis Jordan | Project coordinator, Design/UML lead, Documentation lead |
| Thomas Chappell | Requirements lead, Presentation lead, Testing lead |
| Ryan Bowens | Data lead, Implementation lead |

Communication: Discord. Meetings: every Wednesday.

## Major Features

- Searchable club directory with keyword search
- Category filtering (academic, cultural, sports, service, and others)
- Individual club profile pages with description, meeting times, contact information, social links, and the club's officers
- Join/contact request form that students can use without logging in
- Club leader registration, login, and password reset
- Login-protected dashboard where leaders create, edit, and deactivate their own club's listing and view requests sent to their club
- Student contact information visible only to the leader of the club the request was sent to
- Passwords stored only as salted hashes
- Responsive layout for desktop, tablet, and mobile browsers

## Technologies Used

- **Frontend:** HTML, CSS, light JavaScript, Thymeleaf templates
- **Backend:** Java 17, Spring Boot, Spring Security, Spring Data JPA
- **Database:** MySQL
- **Build tool:** Maven
- **Testing:** JUnit
- **Version control and collaboration:** Git, GitHub, Discord

## Setup and Installation

Spartan Club Central is a web application, so there is nothing to install. You use it in your browser.

**What you need:** a phone, tablet, or computer with an internet connection and a current web browser (Chrome, Firefox, or Safari).

**Students:** no setup and no account. Open the site and start browsing.

**Club leaders:** create an account once, then log in whenever you want to manage your club's listing.

1. Open the site: **[SITE URL]**
2. Select **Register**.
3. Enter your name, email, password, and the club you lead.
4. Submit the form. If the email is already registered, use **Log In** or **Forgot Password** instead.
5. Log in with your email and password. You are taken to your dashboard.

## Running It Locally (Developers and Demos)

See [docs/run-locally.md](docs/run-locally.md) for installing MySQL, loading the sample data, setting environment variables, and running the app and tests on your laptop.

## How to Run the Project

**As a student**

1. Open the site: **[SITE URL]**. The club directory is the home page.
2. Search by typing a keyword into the search box.
3. Filter by choosing a category. Choose **All Categories** to clear the filter.
4. Select a club to open its profile page.
5. To contact a club, select **Join / Contact**, fill in your name, email, and message, and submit. You will see a confirmation message. If a required field is missing or invalid, the form tells you what to fix and nothing is sent.

**As a club leader**

1. Log in with your email and password.
2. Create your listing the first time: select **Create New Listing**, fill in the club details, and save.
3. Edit your listing any time from your dashboard.
4. View join and contact requests sent to your club on your dashboard.
5. Deactivate your listing if your club is no longer active.
6. If you forget your password, select **Forgot Password** on the login page and follow the emailed link.
7. Log out when you are done. You are also logged out automatically after 30 minutes of inactivity.

You can edit and view requests only for your own club.

## Current Project Status

**Feature-complete and tested; ready for local demonstrations.** All functional requirements (FR-01 to FR-12) and security requirements (SPR-01 to SPR-08) are built. There are 134 automated tests, and the system has been checked on MySQL 8 in a real browser.

- [x] Requirements, user stories, use cases, and UML diagrams
- [x] Architecture and class design ([docs/architecture.md](docs/architecture.md))
- [x] Club directory with keyword search (name and description) and category filter
- [x] Club profile pages with contact info and social links
- [x] Join/contact request form with validation, emailed to the club (Mailpit locally)
- [x] Club leader registration, login, logout, and password reset by email
- [x] Leader dashboard: create, edit, hide/show, and delete your own club; read its join requests
- [x] Administrator page: all clubs and leaders, hide clubs, block accounts, send reset links, security log
- [x] Security: BCrypt passwords, ownership checks, CSRF, session timeout, security headers, audit log
- [x] Automated tests and requirements traceability ([docs/traceability.md](docs/traceability.md))
- [x] Performance check: 100 concurrent users, 95% of searches under 1 second on a test machine
- [ ] Manual screen-reader pass and Firefox/Safari check
- [ ] Pilot test with a small group of students
- [ ] Initial club data collection from student organizations
- [ ] Hosting deployment and campus-wide launch

## Documentation

| Document | What's in it |
|---|---|
| [docs/run-locally.md](docs/run-locally.md) | Install, run, test, reset, troubleshoot, and the demo script |
| [docs/local-run.html](docs/local-run.html) | Open in any browser: clickable offline preview of the whole site plus the setup guide |
| [docs/architecture.md](docs/architecture.md) | How the code is organized and why |
| [docs/traceability.md](docs/traceability.md) | Every requirement → code → tests |
| [docs/requirements.md](docs/requirements.md), [docs/security-privacy-checklist.md](docs/security-privacy-checklist.md) | Requirements and security checklist |

## Known Issues and Limitations

Decided during the build (were open questions):
- Keyword search matches club names **and** descriptions, combined with the category filter.
- Each club has one leader account, and each leader leads one club.
- Sessions time out after 30 minutes; passwords are 8 to 72 characters; text limits match the database (for example, a 1,000-character message).

Still true:
- Anyone can register as a leader (an approval workflow was out of scope); the administrator can block a fake account and hide its club.
- Club information must be entered by leaders, since there is no connection to a university club database.
- Emails go to Mailpit on the demo laptop; a real deployment needs a real mail server and HTTPS.
- 99% uptime (NFR-05) can only be measured once the site is hosted.
- The site is web only. There is no native mobile app, though the layout works on phones down to 320 px wide.
- Individual club chats and a Student Affairs approval workflow were cut from scope.
