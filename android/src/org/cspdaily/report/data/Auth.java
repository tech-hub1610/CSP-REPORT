package org.cspdaily.report.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import org.cspdaily.report.Core;

import org.cspdaily.report.security.Crypto;

public final class Auth {
    private static final String PREF_NAME = "csp_auth";
    private static final String KEY_USER_ID = "logged_user_id";
    private static final String KEY_USERNAME = "logged_username";
    private static final String KEY_LAST_USER_ID = "last_user_id";
    private static final String KEY_LAST_USERNAME = "last_username";
    private static User cachedUser;

    private Auth() {}

    public static synchronized void ensureDefaultSuperAdmin(SQLiteDatabase db) {
        try {
            User admin = User.byUsername(db, "admin");
            if (admin == null) {
                User superAdmin = new User();
                superAdmin.username = "admin";
                superAdmin.name = "Super Admin";
                superAdmin.role = User.ROLE_SUPERADMIN;
                superAdmin.mobile = "";
                superAdmin.email = "admin@cspdaily.local";
                superAdmin.status = "ACTIVE";
                superAdmin.setPassword("admin123");
                superAdmin.isDefaultPassword = true;
                User.save(db, superAdmin);
            }
        } catch (Throwable ignored) {}
    }

    public static synchronized User currentUser(Context context) {
        if (cachedUser != null) {
            return cachedUser;
        }
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String username = sp.getString(KEY_USERNAME, null);
        SQLiteDatabase db = Core.get(context).dbRead();
        if (username != null && !username.trim().isEmpty()) {
            cachedUser = User.byUsername(db, username.trim());
        }
        if (cachedUser == null) {
            long userId = sp.getLong(KEY_USER_ID, 0);
            if (userId > 0) {
                cachedUser = User.byId(db, userId);
                if (cachedUser != null && cachedUser.username != null) {
                    sp.edit().putString(KEY_USERNAME, cachedUser.username).apply();
                }
            }
        }
        return cachedUser;
    }

    public static synchronized User lastUser(Context context) {
        User cur = currentUser(context);
        if (cur != null) return cur;

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastU = sp.getString(KEY_LAST_USERNAME, null);
        SQLiteDatabase db = Core.get(context).dbRead();
        if (lastU != null && !lastU.trim().isEmpty()) {
            User u = User.byUsername(db, lastU.trim());
            if (u != null) return u;
        }
        long lastId = sp.getLong(KEY_LAST_USER_ID, 0);
        if (lastId > 0) {
            return User.byId(db, lastId);
        }
        return null;
    }

    public static synchronized boolean login(Context context, String username, String password) {
        if (username == null || password == null) return false;
        SQLiteDatabase db = Core.get(context).db();
        ensureDefaultSuperAdmin(db);
        User user = User.byUsername(db, username.trim());
        if (user == null) return false;

        String plain = password.trim();

        if (user.hasPasswordHash()) {
            if (Crypto.verify(plain, user.passwordHash, user.passwordSalt)) {
                if ("admin".equalsIgnoreCase(user.username) && "admin123".equals(plain)) {
                    user.isDefaultPassword = true;
                    User.save(db, user);
                }
                setCurrentUser(context, user);
                return true;
            }
            return false;
        }

        if (user.password != null && !user.password.isEmpty()) {
            if (user.password.equals(plain)) {
                user.setPassword(plain);
                if ("admin".equalsIgnoreCase(user.username) && "admin123".equals(plain)) {
                    user.isDefaultPassword = true;
                }
                User.save(db, user);
                setCurrentUser(context, user);
                return true;
            }
        }

        return false;
    }

    public static synchronized void setCurrentUser(Context context, User user) {
        cachedUser = user;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (user != null) {
            SharedPreferences.Editor ed = sp.edit();
            if (user.id > 0) {
                ed.putLong(KEY_USER_ID, user.id);
                ed.putLong(KEY_LAST_USER_ID, user.id);
            }
            if (user.username != null && !user.username.isEmpty()) {
                ed.putString(KEY_USERNAME, user.username);
                ed.putString(KEY_LAST_USERNAME, user.username);
            }
            ed.apply();
        } else {
            sp.edit().remove(KEY_USER_ID).remove(KEY_USERNAME).apply();
        }
    }

    public static synchronized void logout(Context context) {
        cachedUser = null;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_USER_ID).remove(KEY_USERNAME).apply();
    }

    public static boolean isLoggedIn(Context context) {
        return currentUser(context) != null;
    }

    public static boolean isSuperAdmin(Context context) {
        User u = currentUser(context);
        return u != null && u.isSuperAdmin();
    }

    public static boolean isAdmin(Context context) {
        User u = currentUser(context);
        return u != null && u.isAdmin();
    }

    public static boolean isSupervisor(Context context) {
        User u = currentUser(context);
        return u != null && u.isSupervisor();
    }
}
