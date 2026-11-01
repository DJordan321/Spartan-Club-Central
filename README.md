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
- Individual club profile pages with description, meeting times, contact information, and social links
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

**Near completion.** Planning and design are finished, the core features are built, and the project is in testing and polish ahead of a pilot.

- [x] Requirements, user stories, use cases, and UML diagrams
- [x] Architecture and class design
- [x] Security and privacy checklist and security test cases
- [x] Club directory with keyword search and category filter
- [x] Club profile pages
- [x] Join/contact request form with validation
- [x] Club leader registration, login, and dashboard
- [x] Create, edit, and deactivate listings
- [ ] Final run of all security test cases
- [ ] Pilot test with a small group of students
- [ ] Initial club data collection from student organizations
- [ ] Hosting deployment and campus-wide launch

## Known Issues and Limitations

- Whether keyword search matches club names only, or also descriptions and categories, still needs to be confirmed.
- Whether a club has one leader or several is still to be finalized.
- The 30-minute session timeout, minimum password length, and maximum text lengths are placeholder values until the team confirms them.
- Club information must be collected from leaders by hand, since there is no connection to a university club database.
- Listings stay accurate only if club leaders keep them updated.
- Request notifications and password reset emails require mail configuration.
- A free or low-cost hosting tier may limit performance and uptime.
- The 2-second search target for 100 concurrent users has not yet been verified with a full load test.
- The site is web only. There is no native mobile app, though the layout is responsive.
- Individual club chats and a Student Affairs approval workflow were cut from scope.
- Long-term maintenance after the founding team graduates will need documentation and a hand-off plan.
