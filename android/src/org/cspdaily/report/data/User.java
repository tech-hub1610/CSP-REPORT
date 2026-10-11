package org.cspdaily.report.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.security.Crypto;

public class User {
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPERVISOR = "SUPERVISOR";

    public long id;
    public String username;
    public String passwordHash;
    public String passwordSalt;
    public boolean isDefaultPassword;
    public String password; // Transient reference for legacy migration
    public String name;
    public String role;
    public String mobile;
    public String email;
    public String status;
    public String createdAt;
    public String updatedAt;

    public User() {
        this.username = "";
        this.passwordHash = "";
        this.passwordSalt = "";
        this.isDefaultPassword = false;
        this.password = "";
        this.name = "";
        this.role = ROLE_SUPERVISOR;
        this.mobile = "";
        this.email = "";
        this.status = "ACTIVE";
        this.createdAt = "";
        this.updatedAt = "";
    }

    public boolean hasPasswordHash() {
        return this.passwordHash != null && !this.passwordHash.trim().isEmpty() &&
               this.passwordSalt != null && !this.passwordSalt.trim().isEmpty();
    }

    public void setPassword(String plainPassword) {
        if (plainPassword != null && !plainPassword.trim().isEmpty()) {
            Crypto.HashResult hr = Crypto.hashPassword(plainPassword.trim());
            this.passwordHash = hr.hash;
            this.passwordSalt = hr.salt;
            this.password = "";
            this.isDefaultPassword = false;
        }
    }

    public boolean isSuperAdmin() {
        return ROLE_SUPERADMIN.equalsIgnoreCase(this.role);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(this.role) || isSuperAdmin();
    }

    public boolean isSupervisor() {
        return ROLE_SUPERVISOR.equalsIgnoreCase(this.role);
    }

    public String roleLabel() {
        if (isSuperAdmin()) return "Super Admin";
        if (ROLE_ADMIN.equalsIgnoreCase(this.role)) return "Admin";
        return "Supervisor";
    }

    public static User fromCursor(Cursor c) {
        User u = new User();
        u.id = c.getLong(c.getColumnIndex("id"));
        u.username = c.getString(c.getColumnIndex("username"));

        int hashIdx = c.getColumnIndex("password_hash");
        if (hashIdx >= 0 && !c.isNull(hashIdx)) {
            u.passwordHash = c.getString(hashIdx);
        }
        int saltIdx = c.getColumnIndex("password_salt");
        if (saltIdx >= 0 && !c.isNull(saltIdx)) {
            u.passwordSalt = c.getString(saltIdx);
        }
        int defIdx = c.getColumnIndex("is_default_password");
        if (defIdx >= 0 && !c.isNull(defIdx)) {
            u.isDefaultPassword = c.getInt(defIdx) == 1;
        }

        int pwIdx = c.getColumnIndex("password");
        if (pwIdx >= 0 && !c.isNull(pwIdx)) {
            String legacyPw = c.getString(pwIdx);
            if (legacyPw != null && !legacyPw.isEmpty()) {
                if (!u.hasPasswordHash()) {
                    u.password = legacyPw;
                }
            }
        }

        u.name = c.getString(c.getColumnIndex("name"));
        u.role = c.getString(c.getColumnIndex("role"));
        u.mobile = c.getString(c.getColumnIndex("mobile"));
        u.email = c.getString(c.getColumnIndex("email"));
        u.status = c.getString(c.getColumnIndex("status"));
        u.createdAt = c.getString(c.getColumnIndex("created_at"));
        u.updatedAt = c.getString(c.getColumnIndex("updated_at"));
        return u;
    }

    public static User byId(SQLiteDatabase db, long id) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM app_user WHERE id = ?", new String[]{String.valueOf(id)});
            if (c != null && c.moveToFirst()) {
                return fromCursor(c);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return null;
    }

    public static User byUsername(SQLiteDatabase db, String username) {
        if (username == null) return null;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM app_user WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()});
            if (c != null && c.moveToFirst()) {
                return fromCursor(c);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return null;
    }

    public static List<User> all(SQLiteDatabase db, String roleFilter) {
        List<User> list = new ArrayList<>();
        Cursor c = null;
        try {
            String sql = "SELECT * FROM app_user";
            String[] args = null;
            if (roleFilter != null && !roleFilter.isEmpty() && !"ALL".equalsIgnoreCase(roleFilter)) {
                sql += " WHERE role = ?";
                args = new String[]{roleFilter};
            }
            sql += " ORDER BY name ASC, username ASC";
            c = db.rawQuery(sql, args);
            while (c != null && c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return list;
    }

    public static List<User> supervisors(SQLiteDatabase db) {
        return all(db, ROLE_SUPERVISOR);
    }

    public static List<User> admins(SQLiteDatabase db) {
        return all(db, ROLE_ADMIN);
    }

    public static long save(SQLiteDatabase db, User user) {
        String now = Biz.now();
        ContentValues cv = new ContentValues();
        cv.put("username", user.username.trim());

        if (!user.hasPasswordHash() && user.password != null && !user.password.trim().isEmpty()) {
            Crypto.HashResult hr = Crypto.hashPassword(user.password.trim());
            user.passwordHash = hr.hash;
            user.passwordSalt = hr.salt;
            user.password = "";
        }

        cv.put("password_hash", user.passwordHash != null ? user.passwordHash.trim() : "");
        cv.put("password_salt", user.passwordSalt != null ? user.passwordSalt.trim() : "");
        cv.put("is_default_password", user.isDefaultPassword ? 1 : 0);
        cv.put("password", "");

        cv.put("name", user.name.trim());
        cv.put("role", user.role);
        cv.put("mobile", user.mobile == null ? "" : user.mobile.trim());
        cv.put("email", user.email == null ? "" : user.email.trim());
        cv.put("status", user.status == null ? "ACTIVE" : user.status);
        cv.put("updated_at", now);

        if (user.id > 0) {
            db.update("app_user", cv, "id = ?", new String[]{String.valueOf(user.id)});
            return user.id;
        } else {
            cv.put("created_at", now);
            long id = db.insertWithOnConflict("app_user", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            user.id = id;
            return id;
        }
    }

    public static boolean changePassword(SQLiteDatabase db, long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) return false;
        Crypto.HashResult hr = Crypto.hashPassword(newPassword.trim());
        ContentValues cv = new ContentValues();
        cv.put("password_hash", hr.hash);
        cv.put("password_salt", hr.salt);
        cv.put("is_default_password", 0);
        cv.put("password", "");
        cv.put("updated_at", Biz.now());
        return db.update("app_user", cv, "id = ?", new String[]{String.valueOf(userId)}) > 0;
    }

    public static boolean delete(SQLiteDatabase db, long id) {
        try {
            db.delete("supervisor_csp_map", "supervisor_id = ?", new String[]{String.valueOf(id)});
            int deleted = db.delete("app_user", "id = ?", new String[]{String.valueOf(id)});
            return deleted > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
