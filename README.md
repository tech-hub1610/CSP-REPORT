# CSP Daily Report

Android field-reporting application for CSP (Credit Service Point) network management.
Native Java, API 24+ target, synced to Supabase.

---

## Build

```bash
# Prerequisites: Android SDK with build-tools, Java 11+
cd android
./build.sh
```

**Version:** `2.0.54` · `versionCode 20260973`

## Source Layout

| Directory | Contents |
|-----------|----------|
| `android/src/org/cspdaily/report/security/` | Crypto, SessionManager, BiometricHelper |
| `android/src/org/cspdaily/report/data/` | SQLite DB, Supabase sync, Auth, User, Settings |
| `android/src/org/cspdaily/report/domain/` | Business logic (Biz, Day, CspDay, Backup, Agg) |
| `android/src/org/cspdaily/report/ui/screen/` | All screen implementations (29 files) |
| `android/src/org/cspdaily/report/ui/widget/` | Reusable UI components (14 files) |
| `android/src/org/cspdaily/report/notify/` | Reminder receiver, notification manager |
| `android/src/org/cspdaily/report/update/` | OTA update, APK downloader, integrity verifier |
| `android/res/` | Drawables, layouts, styles, colors, animations |
| `android/tools/` | Build helper scripts |

Cloud backend: **Supabase** (`sfegesdzmoyolrrbturx.supabase.co`)

## Audit Reports

| File | Description |
|------|-------------|
| `CSP_Daily_Report_Security_Audit.pdf` | Full source-code security audit — 16 findings (4 Critical, 4 High, 4 Medium, 4 Low) |
| `security_audit_report.html` | Same audit in HTML format |
| `UI_UX_Design_Restrictions_and_Limitations.md` | UI/UX design restrictions and limitations — 18 findings |

## Key Security Concerns

See the PDF audit for full details. Top findings:

1. **Hardcoded Supabase publishable key** in `SupabaseClient.java`
2. **Default super-admin account** `admin` / `admin123` auto-created on install
3. **Permissive RLS policies** — anonymous users have full read/write on all tables
4. **Plaintext password fallback** for legacy accounts
5. **No certificate pinning** on any HTTP connection
6. **Unencrypted local SQLite database**
7. **APK download URL not host-validated**

## License

Proprietary — CSP Daily Report.
