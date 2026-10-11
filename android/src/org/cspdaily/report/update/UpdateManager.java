package org.cspdaily.report.update;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.cspdaily.report.Core;
import org.cspdaily.report.data.Auth;
import org.cspdaily.report.data.Settings;
import org.cspdaily.report.data.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;

public final class UpdateManager {
    public static final String KEY_LAST_UPDATE_CHECK = "last_update_check_ts";
    public static final String STORAGE_BUCKET = "app-releases";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static AppRelease sLatestActiveRelease = null;

    public interface CheckCallback {
        void onNoUpdate(int currentCode, String currentName);
        void onUpdateAvailable(AppRelease release, boolean mandatory);
        void onRemoteConfigApplied();
        void onError(String message, Throwable error);
    }

    private UpdateManager() {}

    public static String getInstalledVersionName(Context context) {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return p.versionName != null ? p.versionName : "1.0";
        } catch (Exception e) {
            return "1.0";
        }
    }

    public static int getInstalledVersionCode(Context context) {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return p.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    public static AppRelease getLatestCachedRelease(Context context) {
        if (sLatestActiveRelease != null) return sLatestActiveRelease;
        SQLiteDatabase db = Core.get(context).dbRead();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM app_updates WHERE is_active = 1 ORDER BY version_code DESC LIMIT 1", null);
            if (c != null && c.moveToFirst()) {
                sLatestActiveRelease = AppRelease.fromCursor(c);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
        return sLatestActiveRelease;
    }

    public static List<AppRelease> getAllCachedReleases(Context context) {
        List<AppRelease> list = new ArrayList<>();
        SQLiteDatabase db = Core.get(context).dbRead();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM app_updates ORDER BY version_code DESC", null);
            if (c != null && c.moveToFirst()) {
                do {
                    AppRelease r = AppRelease.fromCursor(c);
                    if (r != null) list.add(r);
                } while (c.moveToNext());
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    public static boolean isMandatoryUpdatePending(Context context) {
        AppRelease latest = getLatestCachedRelease(context);
        if (latest == null) return false;
        int installed = getInstalledVersionCode(context);
        return installed < latest.versionCode && latest.isMandatory(installed);
    }

    public static void checkForUpdates(final Context context, final boolean isManual, final CheckCallback callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = SupabaseClient.cleanBaseUrl(SupabaseClient.getProjectUrl(context));
                    String key = SupabaseClient.getPublishableKey(context);

                    // 1. Fetch Remote Config first (Minor Updates)
                    fetchAndApplyRemoteConfig(context, baseUrl, key);

                    // 2. Fetch Latest Active Release from app_updates
                    AppRelease latestCloudRelease = fetchLatestRelease(context, baseUrl, key);

                    final int currentCode = getInstalledVersionCode(context);
                    final String currentName = getInstalledVersionName(context);

                    // Record last checked timestamp
                    SQLiteDatabase db = Core.get(context).db();
                    Settings.set(db, KEY_LAST_UPDATE_CHECK, String.valueOf(System.currentTimeMillis()));

                    if (latestCloudRelease != null && latestCloudRelease.isActive) {
                        sLatestActiveRelease = latestCloudRelease;

                        if (latestCloudRelease.versionCode > currentCode) {
                            final boolean mandatory = latestCloudRelease.isMandatory(currentCode);
                            MAIN_HANDLER.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onUpdateAvailable(latestCloudRelease, mandatory);
                                }
                            });
                            return;
                        }
                    }

                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onRemoteConfigApplied();
                                callback.onNoUpdate(currentCode, currentName);
                            }
                        }
                    });

                } catch (final Exception e) {
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError("Update check failed: " + e.getMessage(), e);
                        }
                    });
                }
            }
        });
    }

    private static void fetchAndApplyRemoteConfig(Context context, String baseUrl, String key) {
        try {
            String endpoint = baseUrl + "/rest/v1/remote_config?is_active=eq.true&select=*";
            String jsonStr = SupabaseClient.httpGet(endpoint, key);
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String k = obj.optString("config_key");
                    String v = obj.optString("config_value");
                    boolean active = obj.optBoolean("is_active", true);
                    String updated = obj.optString("updated_at");
                    if (k != null && !k.isEmpty()) {
                        RemoteConfig.saveConfig(context, k, v, active, updated);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static AppRelease fetchLatestRelease(Context context, String baseUrl, String key) {
        try {
            // Query latest published releases from Supabase
            String endpoint = baseUrl + "/rest/v1/app_updates?order=version_code.desc&limit=25&select=*";
            String jsonStr = SupabaseClient.httpGet(endpoint, key);
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONArray array = new JSONArray(jsonStr);
                SQLiteDatabase db = Core.get(context).db();
                AppRelease latestActive = null;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    AppRelease release = AppRelease.fromJson(obj);
                    if (release != null && release.versionCode > 0) {
                        db.insertWithOnConflict("app_updates", null, release.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
                        if (release.isActive && latestActive == null) {
                            latestActive = release;
                        }
                    }
                }
                if (latestActive != null) {
                    sLatestActiveRelease = latestActive;
                    return latestActive;
                }
            }
        } catch (Throwable tr) {
            android.util.Log.e("UpdateManager", "Failed to fetch releases from Supabase: " + tr.getMessage(), tr);
        }
        return getLatestCachedRelease(context);
    }
}
