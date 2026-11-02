COMPLAINT MANAGEMENT PORTAL - WINDOWS APPLICATION

1. Install MySQL 8 or later.
2. Copy application.properties.example to ComplaintPortal\application.properties.
3. Edit application.properties and set db.username and db.password for your MySQL account.
   The configured account must be able to create the complaint_portal database on first run.
4. Double-click ComplaintPortal\ComplaintPortal.exe.

The app bundle includes its own Java runtime. It initializes its MySQL schema and default complaint categories on startup.
Default administrator: admin@complaints.local / Admin@123
Change the demonstration password after signing in.

If MySQL is already configured and the schema was initialized, db.initialize=false can be set.
Do not share or commit application.properties; it contains your database credentials.
