# Spartan Club Central: Requirements

## Functional Requirements

| ID | Requirement |
|----|-------------|
| FR-01 | The system shall let students search the club directory by keyword. |
| FR-02 | The system shall let students filter clubs by category (academic, cultural, sports, service, and others). |
| FR-03 | The system shall let students send a join/contact request to a club through a form, without logging in. |
| FR-04 | The system shall let club leaders register, log in, and reset their password, and use a login-protected dashboard to create, edit, and deactivate their own club's listing and view requests sent to their club. |
| FR-05 | The system shall show a profile page for each club with its description, meeting times, contact information, and social links. |

## Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | Passwords shall be stored only as salted hashes. |
| NFR-02 | A student's contact information shall be visible only to the leader of the club the request was sent to. |
| NFR-04 | Search results shall be returned within 2 seconds (target: 100 concurrent users). |

## Users

- **Student / Public Visitor:** no account needed.
- **Club Leader:** logs in to manage their own club's listing and requests.
- **Administrator:** supports account recovery and system upkeep.

## Open Items

- Whether keyword search matches club names only, or also descriptions and categories.
- Whether a club has one leader or several.
