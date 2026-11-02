# Complaint Management Portal

A Java 17+ desktop complaint-management application built with Swing, JDBC, MySQL, Maven, and FlatLaf. The code is organized into MVC-style models, DAOs, services, controllers, and views. Dashboard and report values are queried from MySQL; no in-memory or hardcoded statistics are used.

## Requirements

- JDK 17 or later
- Maven 3.9+
- MySQL 8.0+

## Configure and run

1. Create a MySQL user with permission to create/use the application database, or create the database yourself:
   ```sql
   CREATE DATABASE complaint_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
2. Copy `application.properties.example` to `application.properties` in the project root and set `db.username` and `db.password`. Alternatively configure `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`, `APP_DB_NAME`, `APP_ADMIN_EMAIL`, and `APP_ADMIN_PASSWORD`.
3. Keep `db.initialize=true` to create/update the schema and add default categories on startup. The configured database account must have database-creation permission if the database does not already exist. To manage schema manually, run `database/schema.sql` and `database/sample_data.sql` in MySQL and set `db.initialize=false`.
4. Build and launch:
   ```sh
   mvn clean package
   java -jar target/complaint-portal-1.0.0.jar
   ```
   A native Windows application is also assembled in `dist/ComplaintPortal-Final/`. Configure
   `dist/ComplaintPortal-Final/application.properties` using
   `dist/ComplaintPortal-Final/application.properties.example`, then double-click
   `dist/ComplaintPortal-Final/ComplaintPortal.exe`. This app bundle includes its own Java runtime.
   Run `.\package-windows.ps1` to rebuild it.

Database connection failures are reported in an error dialog at startup; the application does not silently switch to mock data.
On startup, the application safely adds missing columns to legacy user/category tables and migrates
legacy complaint fields/statuses without deleting existing complaints. Legacy SHA-256 password
hashes are upgraded to BCrypt after a successful login.

## Initial administrator

By default, the application bootstraps `admin@complaints.local` with password `Admin@123` when no administrator exists. Passwords are BCrypt hashes, not plaintext. Change the first administrator through `admin.email` and `admin.password` or the corresponding `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` environment variables before first launch. If you manually imported `database/sample_data.sql`, it already created the demonstration administrator, so sign in with the documented demo credentials and change the password from **My profile**. Do not expose the demonstration credentials in a production deployment.

Users can create accounts from the login screen. Administrators can manage all complaints, assign complaints to themselves, change status/priority/category, and add remarks. Complaint owners can review their own complaint history, update pending complaints, and delete pending complaints.

## Included functionality

- Secure login, user registration, profile editing, and password changes.
- Database-persisted complaint create/read/update/delete with owner/admin authorization.
- Complaint search and category, priority, and status filters.
- Complaint details, assignment, administrative remarks, and status history.
- Administrator user and category management.
- Dashboard KPI cards, status/category distribution, recent complaints, and activity.
- Database-backed category, priority, monthly, and resolution reporting.
- Asynchronous database loading for desktop screens.

`application.properties` is intentionally ignored by Git; do not commit credentials.
