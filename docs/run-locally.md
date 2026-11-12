# Running Spartan Club Central Locally (Demo Setup)

> Prefer a web page? Open [`docs/local-run.html`](local-run.html) in any browser. It has these steps with Windows/Mac tabs and copy buttons, plus a clickable offline preview of the student pages (homepage, directory, club profiles) that needs no Java or MySQL.

The whole system runs on one laptop. Nothing is hosted online and no real emails are sent.

| Piece | Tool (all free) | Address |
|---|---|---|
| Web app | Spring Boot (`./mvnw spring-boot:run`) | http://localhost:8080 |
| Database | MySQL 8 Community Server | localhost:3306, database `club_central` |
| Email | Mailpit (fake mail server with a web inbox) | SMTP localhost:1025, inbox http://localhost:8025 |

> **Phase 2 status:** the student pages are live:
> - `/` homepage: search box plus one card per category (one click to filtered results, NFR-03)
> - `/clubs` directory: keyword + category search (FR-01, FR-02)
> - `/clubs/{id}` club profile: description, meeting time, contact, social links (FR-05, FR-06)
>
> The join request form arrives in Phase 3 and leader login in Phase 4.
>
> ![Club directory](screenshots/phase2-directory.png)

---

## 1. Install the tools (once per laptop)

1. **Java 17 or newer**: Eclipse Temurin JDK (adoptium.net). Check with `java -version`.
2. **MySQL 8 Community Server**: dev.mysql.com/downloads. Remember the **root** password you choose. MySQL Workbench is optional but useful for looking at tables.
3. **Mailpit**: download from its GitHub releases page (github.com/axllent/mailpit), or on Mac run `brew install mailpit`. Needed from Phase 3 on.
4. **Git**, plus IntelliJ IDEA Community or VS Code.

You do **not** need to install Maven: the project includes the Maven wrapper (`mvnw` / `mvnw.cmd`).

## 2. Create the database (once)

From the project folder, run the schema as **root**:

```bash
mysql -u root -p < database/schema.sql
```

Then open a MySQL prompt (`mysql -u root -p`) and create the app's own user. It can read and write data but cannot drop or change tables (least privilege). Pick your own password and keep it out of Git:

```sql
CREATE USER 'club_app'@'localhost' IDENTIFIED BY 'pick-a-local-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON club_central.* TO 'club_app'@'localhost';
```

Load the 32 sample clubs:

```bash
mysql -u root -p club_central < database/sample-data.sql
```

**Windows:** if `mysql` isn't recognized, use the full path, e.g. `"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"`, or run the same scripts from MySQL Workbench (*File → Open SQL Script*, then the lightning bolt).

## 3. Set environment variables (secrets never go in Git)

`application.properties` only contains placeholders. The real values come from your environment.

| Variable | Required? | Example | Default if not set |
|---|---|---|---|
| `DB_PASSWORD` | **Yes** | your `club_app` password | none (startup fails) |
| `DB_USER` | no | `club_app` | `club_app` |
| `DB_URL` | no | `jdbc:mysql://localhost:3306/club_central` | same |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | from Phase 4 | `admin@clubcentral.local` | admin not created |
| `APP_BASE_URL` | no | `http://192.168.1.20:8080` for phone demos | `http://localhost:8080` |
| `MAIL_HOST` / `MAIL_PORT` | no | | `localhost` / `1025` (Mailpit) |

**IntelliJ (easiest):** *Run → Edit Configurations → SpartanClubApplication → Environment variables* → add `DB_PASSWORD=...`.

**Mac / Linux terminal** (lasts until you close the terminal):

```bash
export DB_PASSWORD="pick-a-local-password"
```

**Windows PowerShell** (lasts until you close the window):

```powershell
$env:DB_PASSWORD="pick-a-local-password"
```

## 4. Run it

```bash
./mvnw spring-boot:run
```

On Windows: `mvnw.cmd spring-boot:run`. Wait for `Started SpartanClubApplication`, then open http://localhost:8080 (the homepage). Stop the app with **Ctrl+C**. From Phase 3 on, also start Mailpit first (run `mailpit` in another terminal).

## 5. Run the tests

Normal tests use an in-memory database, so they need no MySQL:

```bash
./mvnw test
```

Optional: check that **your** MySQL schema matches the code and that the sample data supports the demo searches. This only reads data:

```bash
RUN_MYSQL_TESTS=true DB_PASSWORD="pick-a-local-password" ./mvnw test -Dtest=MySqlSampleDataTest
```

PowerShell: set `$env:RUN_MYSQL_TESTS="true"` and `$env:DB_PASSWORD="..."`, then `mvnw.cmd test -Dtest=MySqlSampleDataTest`.

## 6. Reset before every demo

This removes all leader accounts, requests and log entries, then reloads the sample clubs, so every demo starts the same:

```bash
mysql -u root -p club_central < database/reset-demo.sql
```

```bash
mysql -u root -p club_central < database/sample-data.sql
```

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user 'club_app' (using password: NO)` | `DB_PASSWORD` isn't set in the window or run configuration that started the app. |
| `Access denied ... (using password: YES)` | Wrong password, or the user was created for a different host. Re-run the `CREATE USER` / `GRANT` step. |
| `Communications link failure` | MySQL isn't running. Start the MySQL service (Windows *Services*, Mac System Settings → MySQL, or `brew services start mysql`). |
| `Schema-validation: missing column [...]` or `missing table` | Your database is from an older schema. Drop it (`DROP DATABASE club_central;`) and redo step 2. |
| `Unknown database 'club_central'` | Step 2 wasn't run. |
| `Port 8080 was already in use` | Another copy of the app is running. Stop it, or run with `--server.port=8081`. |
| `./mvnw: Permission denied` (Mac/Linux) | `chmod +x mvnw` |
| Emails don't appear (Phase 3+) | Start Mailpit and open http://localhost:8025. |

## What to check in the browser (Phase 2)

| Do this | You should see | Proves |
|---|---|---|
| Open http://localhost:8080 | 4 category cards: Academic 8, Cultural 8, Service 7, Sports 8 | NFR-03 |
| Click **Sports** | 8 Sports clubs, one click from the homepage | FR-02, NFR-03 |
| Search **robotics** | Only Engineering Builders Society (the word is only in its description) | FR-01, TC-02 |
| Keyword **club** + category **Academic** | 5 Academic clubs, keyword and category still filled in | TC-05 |
| Search **zzzzqq** | "No clubs found" message | TC-03 |
| Open any club | Meeting time, contact email, contact info and social links filled in | FR-05, FR-06, TC-06 |
| Go to http://localhost:8080/clubs/17 | "Page not found" (club 17 is inactive) | FR-12, SPR-06 |
| Shrink the window to phone width | Everything fits, no sideways scrolling | NFR-08 |

Automated tests: `./mvnw test` should report 39 passing and 1 skipped.

## Demo checklist

Filled in during Phase 7.
