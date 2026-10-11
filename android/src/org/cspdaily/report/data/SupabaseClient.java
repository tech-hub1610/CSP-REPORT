package org.cspdaily.report.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.cspdaily.report.Core;
import org.cspdaily.report.domain.Biz;
import org.cspdaily.report.security.Crypto;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SupabaseClient {
    public static final String DEFAULT_URL = "https://sfegesdzmoyolrrbturx.supabase.co";
    public static final String DEFAULT_KEY = "sb_publishable_Bvigpve6rd7_Ik57XYzxIw_QBvi7UMv";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message, Throwable error);
    }

    public static class SyncResult {
        public boolean success;
        public int pushedCount;
        public int pulledCount;
        public String message = "";

        public SyncResult(boolean success, int pushed, int pulled, String msg) {
            this.success = success;
            this.pushedCount = pushed;
            this.pulledCount = pulled;
            this.message = msg;
        }
    }

    private SupabaseClient() {}

    public static String getProjectUrl(Context context) {
        SQLiteDatabase db = Core.get(context).dbRead();
        String url = Settings.get(db, Settings.SUPABASE_URL, DEFAULT_URL);
        if (url == null || url.trim().isEmpty()) return DEFAULT_URL;
        return url.trim();
    }

    public static String getPublishableKey(Context context) {
        SQLiteDatabase db = Core.get(context).dbRead();
        String key = Settings.get(db, Settings.SUPABASE_KEY, DEFAULT_KEY);
        if (key == null || key.trim().isEmpty()) return DEFAULT_KEY;
        return key.trim();
    }

    public static void setProjectUrl(Context context, String url) {
        SQLiteDatabase db = Core.get(context).db();
        Settings.set(db, Settings.SUPABASE_URL, url == null ? "" : url.trim());
    }

    public static void setPublishableKey(Context context, String key) {
        SQLiteDatabase db = Core.get(context).db();
        Settings.set(db, Settings.SUPABASE_KEY, key == null ? "" : key.trim());
    }

    public static String cleanBaseUrl(String url) {
        if (url == null) return "";
        url = url.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Test connection to Supabase instance.
     */
    public static void testConnection(final Context context, final Callback<String> callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = cleanBaseUrl(getProjectUrl(context));
                    String key = getPublishableKey(context);
                    String endpoint = baseUrl + "/rest/v1/app_user?select=count";

                    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("apikey", key);
                    conn.setRequestProperty("Authorization", "Bearer " + key);
                    conn.setRequestProperty("Prefer", "count=exact");

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        final String res = "Connected successfully (HTTP " + code + ")";
                        postSuccess(callback, res);
                    } else {
                        String body = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
                        if (body.contains("PGRST205") || body.contains("schema cache")) {
                            final String res = "Connected to Supabase! (Note: Run SQL schema to create tables)";
                            postSuccess(callback, res);
                        } else {
                            final String err = "HTTP " + code + ": " + body;
                            postError(callback, err, null);
                        }
                    }
                    conn.disconnect();
                } catch (final Exception e) {
                    postError(callback, "Connection failed: " + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Authenticate directly against Supabase app_user table (Online Only).
     */
    public static void authenticateOnline(final Context context, final String username, final String password, final Callback<User> callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = cleanBaseUrl(getProjectUrl(context));
                    String key = getPublishableKey(context);
                    String u = username.trim();
                    String p = password.trim();

                    // Query Supabase app_user table
                    String endpoint = baseUrl + "/rest/v1/app_user?username=eq." + java.net.URLEncoder.encode(u, "UTF-8") + "&select=*";
                    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);
                    conn.setRequestProperty("apikey", key);
                    conn.setRequestProperty("Authorization", "Bearer " + key);

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        String body = readStream(conn.getInputStream());
                        conn.disconnect();
                        JSONArray arr = new JSONArray(body);
                        if (arr.length() == 0) {
                            postError(callback, "User '" + u + "' does not exist in the Supabase cloud database.", null);
                            return;
                        }
                        JSONObject userObj = arr.getJSONObject(0);
                        String cloudPassword = userObj.optString("password", "");
                        String cloudHash = userObj.optString("password_hash", "");
                        String cloudSalt = userObj.optString("password_salt", "");
                        boolean isDefPw = userObj.optInt("is_default_password", 0) == 1 || (userObj.has("is_default_password") && userObj.optBoolean("is_default_password", false));
                        String status = userObj.optString("status", "ACTIVE");

                        if (!"ACTIVE".equalsIgnoreCase(status)) {
                            postError(callback, "Account '" + u + "' is deactivated / disabled.", null);
                            return;
                        }

                        boolean passwordMatches = false;
                        if (cloudHash != null && !cloudHash.trim().isEmpty() && cloudSalt != null && !cloudSalt.trim().isEmpty()) {
                            passwordMatches = Crypto.verify(p, cloudHash, cloudSalt);
                        } else if (cloudPassword != null && !cloudPassword.isEmpty()) {
                            passwordMatches = p.equals(cloudPassword);
                        }

                        if (!passwordMatches) {
                            postError(callback, "Incorrect password for '" + u + "'.", null);
                            return;
                        }

                        // Save / update user in local SQLite DB
                        SQLiteDatabase db = Core.get(context).db();
                        User user = new User();
                        user.username = userObj.optString("username", u);
                        user.name = userObj.optString("name", u);
                        user.role = userObj.optString("role", User.ROLE_SUPERVISOR);
                        user.mobile = userObj.optString("mobile", "");
                        user.email = userObj.optString("email", "");
                        user.status = status;
                        user.createdAt = userObj.optString("created_at", Biz.now());
                        user.updatedAt = userObj.optString("updated_at", Biz.now());

                        if (cloudHash != null && !cloudHash.trim().isEmpty() && cloudSalt != null && !cloudSalt.trim().isEmpty()) {
                            user.passwordHash = cloudHash;
                            user.passwordSalt = cloudSalt;
                            user.password = "";
                            user.isDefaultPassword = isDefPw;
                        } else {
                            user.setPassword(p);
                            user.isDefaultPassword = isDefPw;
                        }

                        long localId = User.save(db, user);
                        user.id = localId;

                        // Also pull mapped CSPs and parameters from cloud
                        try {
                            pullTable(db, baseUrl, key, "csp");
                            pullTable(db, baseUrl, key, "supervisor_csp_map");
                            pullTable(db, baseUrl, key, "admin_instruction");
                            pullTable(db, baseUrl, key, "daily_csp_target");
                        } catch (Throwable ignored) {}

                        Auth.setCurrentUser(context, user);
                        postSuccess(callback, user);
                    } else {
                        String errBody = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
                        conn.disconnect();
                        if (errBody.contains("PGRST205") || errBody.contains("schema cache") || errBody.contains("Could not find")) {
                            postError(callback, "Supabase Database Error: Tables have not been created yet in your Supabase project! Run the SQL schema script in Supabase SQL Editor.", null);
                        } else {
                            postError(callback, "Supabase HTTP " + code + ": " + errBody, null);
                        }
                    }
                } catch (final Exception e) {
                    postError(callback, "Unable to connect to Supabase (" + e.getMessage() + "). Please check internet connection.", e);
                }
            }
        });
    }

    /**
     * Full bidirectional sync with Supabase.
     */
    public static void syncAll(final Context context, final Callback<SyncResult> callback) {
        notifySyncStarted();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SyncResult push = pushInternal(context);
                    SyncResult pull = pullInternal(context);

                    int totalPushed = push.pushedCount;
                    int totalPulled = pull.pulledCount;

                    SQLiteDatabase db = Core.get(context).db();
                    Settings.set(db, Settings.SUPABASE_LAST_SYNC, Biz.now());

                    final SyncResult result = new SyncResult(true, totalPushed, totalPulled, 
                        "Sync complete: " + totalPushed + " rows pushed, " + totalPulled + " rows pulled.");
                    notifySyncFinished(true, totalPulled, totalPushed, result.message);
                    postSuccess(callback, result);
                } catch (final Exception e) {
                    notifySyncFinished(false, 0, 0, e.getMessage());
                    postError(callback, "Sync failed: " + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Push all local SQLite data to Supabase.
     */
    public static void pushAll(final Context context, final Callback<SyncResult> callback) {
        notifySyncStarted();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SyncResult res = pushInternal(context);
                    SQLiteDatabase db = Core.get(context).db();
                    Settings.set(db, Settings.SUPABASE_LAST_SYNC, Biz.now());
                    notifySyncFinished(true, 0, res.pushedCount, res.message);
                    postSuccess(callback, res);
                } catch (final Exception e) {
                    notifySyncFinished(false, 0, 0, e.getMessage());
                    postError(callback, "Push failed: " + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Non-blocking background push of all local changes to Supabase.
     */
    public static void pushAsync(final Context context) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    pushInternal(context.getApplicationContext());
                    SQLiteDatabase db = Core.get(context.getApplicationContext()).db();
                    Settings.set(db, Settings.SUPABASE_LAST_SYNC, Biz.now());
                } catch (Throwable ignored) {}
            }
        });
    }

    /**
     * Pull all data from Supabase into local SQLite.
     */
    public static void pullAll(final Context context, final Callback<SyncResult> callback) {
        notifySyncStarted();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SyncResult res = pullInternal(context);
                    SQLiteDatabase db = Core.get(context).db();
                    Settings.set(db, Settings.SUPABASE_LAST_SYNC, Biz.now());
                    notifySyncFinished(true, res.pulledCount, 0, res.message);
                    postSuccess(callback, res);
                } catch (final Exception e) {
                    notifySyncFinished(false, 0, 0, e.getMessage());
                    postError(callback, "Pull failed: " + e.getMessage(), e);
                }
            }
        });
    }

    public interface SyncStatusListener {
        void onSyncStarted();
        void onSyncFinished(boolean success, int pulledCount, int pushedCount, String message);
    }

    public interface LiveSyncListener {
        void onDataSynced(int pulledCount, int pushedCount);
    }

    private static final java.util.concurrent.ScheduledExecutorService SYNC_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static java.util.concurrent.ScheduledFuture<?> liveSyncTask = null;
    private static final java.util.concurrent.atomic.AtomicBoolean isSyncRunning = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final List<LiveSyncListener> syncListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final List<SyncStatusListener> statusListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addSyncStatusListener(SyncStatusListener l) {
        if (l != null && !statusListeners.contains(l)) {
            statusListeners.add(l);
        }
    }

    public static void removeSyncStatusListener(SyncStatusListener l) {
        if (l != null) {
            statusListeners.remove(l);
        }
    }

    public static boolean isSyncing() {
        return isSyncRunning.get();
    }

    private static void notifySyncStarted() {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                for (SyncStatusListener l : statusListeners) {
                    try { l.onSyncStarted(); } catch (Throwable ignored) {}
                }
            }
        });
    }

    private static void notifySyncFinished(final boolean success, final int pulled, final int pushed, final String msg) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                for (SyncStatusListener l : statusListeners) {
                    try { l.onSyncFinished(success, pulled, pushed, msg); } catch (Throwable ignored) {}
                }
            }
        });
    }

    public static synchronized void startLiveSync(final Context context, final LiveSyncListener listener) {
        if (listener != null && !syncListeners.contains(listener)) {
            syncListeners.add(listener);
        }
        if (liveSyncTask != null && !liveSyncTask.isCancelled()) {
            return;
        }
        liveSyncTask = SYNC_SCHEDULER.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (isSyncRunning.compareAndSet(false, true)) {
                    try {
                        if (context == null) return;
                        Context appContext = context.getApplicationContext();
                        if (!Auth.isLoggedIn(appContext)) return;

                        SyncResult pushRes = pushInternal(appContext);
                        SyncResult pullRes = pullInternal(appContext);

                        final int pulled = pullRes != null ? pullRes.pulledCount : 0;

                        // Only notify UI if new or modified data actually arrived from cloud
                        if (pulled > 0) {
                            MAIN_HANDLER.post(new Runnable() {
                                @Override
                                public void run() {
                                    for (LiveSyncListener l : syncListeners) {
                                        try {
                                            l.onDataSynced(pulled, 0);
                                        } catch (Throwable ignored) {}
                                    }
                                }
                            });
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        isSyncRunning.set(false);
                    }
                }
            }
        }, 15000, 60000, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public static synchronized void stopLiveSync(LiveSyncListener listener) {
        if (listener != null) {
            syncListeners.remove(listener);
        }
        if (syncListeners.isEmpty() && liveSyncTask != null) {
            liveSyncTask.cancel(true);
            liveSyncTask = null;
        }
    }

    public static String httpGet(String endpointUrl, String apiKey) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            String res = readStream(conn.getInputStream());
            conn.disconnect();
            return res;
        } else {
            String err = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
            conn.disconnect();
            throw new Exception("HTTP " + code + ": " + err);
        }
    }

    public static String httpPost(String endpointUrl, String apiKey, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setRequestProperty("Content-Type", "application/json");
        if (endpointUrl.contains("on_conflict=")) {
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation");
        } else {
            conn.setRequestProperty("Prefer", "return=representation");
        }
        if (jsonBody != null) {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();
        }
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            String res = readStream(conn.getInputStream());
            conn.disconnect();
            return res;
        } else {
            String err = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
            conn.disconnect();
            throw new Exception("HTTP " + code + ": " + err);
        }
    }

    public static String httpPatch(String endpointUrl, String apiKey, String jsonBody) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
        conn.setRequestMethod("PATCH");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "return=representation");
        if (jsonBody != null) {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.flush();
            os.close();
        }
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            String res = readStream(conn.getInputStream());
            conn.disconnect();
            return res;
        } else {
            String err = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
            conn.disconnect();
            throw new Exception("HTTP " + code + ": " + err);
        }
    }

    public static void uploadStorageFile(final Context context, final String bucket, final String filePath, final File file, final Callback<String> callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = cleanBaseUrl(getProjectUrl(context));
                    String key = getPublishableKey(context);
                    String uploadUrl = baseUrl + "/storage/v1/object/" + bucket + "/" + filePath;

                    HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(60000);
                    conn.setDoOutput(true);
                    if (key != null && !key.isEmpty()) {
                        conn.setRequestProperty("apikey", key);
                        conn.setRequestProperty("Authorization", "Bearer " + key);
                    }
                    conn.setRequestProperty("Content-Type", "application/vnd.android.package-archive");
                    conn.setRequestProperty("x-upsert", "true");

                    long fileLength = file.length();
                    conn.setFixedLengthStreamingMode(fileLength);
                    OutputStream os = conn.getOutputStream();
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                    fis.close();
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        String res = readStream(conn.getInputStream());
                        conn.disconnect();
                        postSuccess(callback, res);
                    } else {
                        String err = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
                        conn.disconnect();
                        postError(callback, "Upload failed (HTTP " + code + "): " + err, null);
                    }
                } catch (final Exception e) {
                    postError(callback, "Upload error: " + e.getMessage(), e);
                }
            }
        });
    }

    public static void deleteAsync(final Context context, final String tableName, final String queryParams) {
        if (context == null || tableName == null || queryParams == null) return;
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String baseUrl = cleanBaseUrl(getProjectUrl(context));
                    String key = getPublishableKey(context);
                    if (baseUrl.isEmpty() || key.isEmpty()) return;

                    String endpoint = baseUrl + "/rest/v1/" + tableName + "?" + queryParams;
                    HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
                    conn.setRequestMethod("DELETE");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("apikey", key);
                    conn.setRequestProperty("Authorization", "Bearer " + key);
                    conn.setRequestProperty("Prefer", "return=minimal");

                    int code = conn.getResponseCode();
                    conn.disconnect();
                    android.util.Log.d("SupabaseSync", "Remote delete " + tableName + "?" + queryParams + " -> HTTP " + code);
                } catch (Throwable t) {
                    android.util.Log.e("SupabaseSync", "Error in remote delete: " + t.getMessage());
                }
            }
        });
    }

    // ---------------- Internal Sync Logic ----------------

    private static SyncResult pushInternal(Context context) throws Exception {
        SQLiteDatabase db = Core.get(context).dbRead();
        String baseUrl = cleanBaseUrl(getProjectUrl(context));
        String key = getPublishableKey(context);

        int totalPushed = 0;

        // 1. csp
        totalPushed += pushTable(db, baseUrl, key, "csp", "csp_code", false);
        // 2. app_user
        totalPushed += pushTable(db, baseUrl, key, "app_user", "username", false);
        // 3. daily_report
        totalPushed += pushTable(db, baseUrl, key, "daily_report", "csp_id,business_date", true);
        // 4. hourly_report
        totalPushed += pushTable(db, baseUrl, key, "hourly_report", "csp_id,business_date,hour", true);
        // 5. supervisor_csp_map
        totalPushed += pushTable(db, baseUrl, key, "supervisor_csp_map", "supervisor_id,csp_id", false);
        // 6. admin_instruction
        totalPushed += pushTable(db, baseUrl, key, "admin_instruction", "business_date,sender_name,created_at", true);
        // 7. pending_reason
        totalPushed += pushTable(db, baseUrl, key, "pending_reason", "csp_id,business_date", true);
        // 8. followup_log
        totalPushed += pushTable(db, baseUrl, key, "followup_log", "csp_id,business_date,created_at", true);
        // 9. daily_csp_target
        totalPushed += pushTable(db, baseUrl, key, "daily_csp_target", "csp_id,business_date,field_key", true);
        // 10. office_note
        totalPushed += pushTable(db, baseUrl, key, "office_note", "business_date", false);
        // 11. business_day
        totalPushed += pushTable(db, baseUrl, key, "business_day", "business_date", false);
        // 12. app_updates (only if user is admin)
        if (Auth.isAdmin(context)) {
            totalPushed += pushTable(db, baseUrl, key, "app_updates", "version_code", true);
            totalPushed += pushTable(db, baseUrl, key, "remote_config", "config_key", true);
        }

        return new SyncResult(true, totalPushed, 0, "Pushed " + totalPushed + " rows to Supabase.");
    }

    private static int pushTable(SQLiteDatabase db, String baseUrl, String key, String tableName, String onConflict, boolean omitId) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + tableName, null);
            if (c == null || !c.moveToFirst()) return 0;

            JSONArray array = new JSONArray();
            String[] colNames = c.getColumnNames();

            do {
                JSONObject obj = new JSONObject();
                for (String col : colNames) {
                    if (omitId && "id".equalsIgnoreCase(col)) {
                        continue;
                    }
                    int idx = c.getColumnIndex(col);
                    if (c.isNull(idx)) {
                        obj.put(col, JSONObject.NULL);
                    } else {
                        int type = c.getType(idx);
                        if (type == Cursor.FIELD_TYPE_INTEGER) {
                            obj.put(col, c.getLong(idx));
                        } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                            obj.put(col, c.getDouble(idx));
                        } else {
                            obj.put(col, c.getString(idx));
                        }
                    }
                }
                array.put(obj);
            } while (c.moveToNext());

            if (array.length() == 0) return 0;

            String endpoint = baseUrl + "/rest/v1/" + tableName;
            if (onConflict != null && !onConflict.isEmpty()) {
                endpoint += "?on_conflict=" + onConflict;
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", key);
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates");

            byte[] jsonBytes = array.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(jsonBytes.length);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBytes);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                conn.disconnect();
                return array.length();
            } else {
                String err = readStream(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
                conn.disconnect();
                android.util.Log.e("SupabaseSync", "Push failed for " + tableName + ": HTTP " + code + " -> " + err);
            }
        } catch (Exception e) {
            android.util.Log.e("SupabaseSync", "Exception pushing " + tableName, e);
        } finally {
            if (c != null) c.close();
        }
        return 0;
    }

    private static SyncResult pullInternal(Context context) throws Exception {
        SQLiteDatabase db = Core.get(context).db();
        String baseUrl = cleanBaseUrl(getProjectUrl(context));
        String key = getPublishableKey(context);

        int totalPulled = 0;

        // Pull and upsert each table
        totalPulled += pullTable(db, baseUrl, key, "app_user");
        totalPulled += pullTable(db, baseUrl, key, "csp");
        totalPulled += pullTable(db, baseUrl, key, "supervisor_csp_map");
        totalPulled += pullTable(db, baseUrl, key, "admin_instruction");
        totalPulled += pullTable(db, baseUrl, key, "daily_report");
        totalPulled += pullTable(db, baseUrl, key, "hourly_report");
        totalPulled += pullTable(db, baseUrl, key, "pending_reason");
        totalPulled += pullTable(db, baseUrl, key, "followup_log");
        totalPulled += pullTable(db, baseUrl, key, "daily_csp_target");
        totalPulled += pullTable(db, baseUrl, key, "office_note");
        totalPulled += pullTable(db, baseUrl, key, "business_day");
        totalPulled += pullTable(db, baseUrl, key, "app_updates");
        totalPulled += pullTable(db, baseUrl, key, "remote_config");

        return new SyncResult(true, 0, totalPulled, "Pulled " + totalPulled + " rows from Supabase.");
    }

    private static String[] getTableKeyCols(String tableName) {
        if ("csp".equals(tableName)) {
            return new String[]{"csp_code"};
        } else if ("app_user".equals(tableName)) {
            return new String[]{"username"};
        } else if ("supervisor_csp_map".equals(tableName)) {
            return new String[]{"supervisor_id", "csp_id"};
        } else if ("daily_report".equals(tableName)) {
            return new String[]{"csp_id", "business_date"};
        } else if ("hourly_report".equals(tableName)) {
            return new String[]{"csp_id", "business_date", "hour"};
        } else if ("admin_instruction".equals(tableName)) {
            return new String[]{"business_date", "sender_name", "created_at"};
        } else if ("pending_reason".equals(tableName)) {
            return new String[]{"csp_id", "business_date"};
        } else if ("followup_log".equals(tableName)) {
            return new String[]{"csp_id", "business_date", "created_at"};
        } else if ("daily_csp_target".equals(tableName)) {
            return new String[]{"csp_id", "business_date", "field_key"};
        } else if ("app_updates".equals(tableName)) {
            return new String[]{"version_code"};
        } else if ("remote_config".equals(tableName)) {
            return new String[]{"config_key"};
        } else if ("office_note".equals(tableName) || "business_day".equals(tableName)) {
            return new String[]{"business_date"};
        }
        return new String[]{"id"};
    }

    private static boolean upsertIfChanged(SQLiteDatabase db, String tableName, ContentValues cv) {
        String[] keyCols = getTableKeyCols(tableName);
        StringBuilder where = new StringBuilder();
        String[] args = new String[keyCols.length];
        for (int i = 0; i < keyCols.length; i++) {
            if (i > 0) where.append(" AND ");
            where.append(keyCols[i]).append(" = ?");
            Object v = cv.get(keyCols[i]);
            args[i] = v != null ? v.toString() : "";
        }

        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + tableName + " WHERE " + where.toString() + " LIMIT 1", args);
            if (c == null || !c.moveToFirst()) {
                db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                return true;
            }

            boolean changed = false;
            for (String key : cv.keySet()) {
                int colIdx = c.getColumnIndex(key);
                if (colIdx == -1) continue;

                Object newVal = cv.get(key);
                if (newVal == null) {
                    if (!c.isNull(colIdx)) {
                        changed = true;
                        break;
                    }
                } else {
                    if (c.isNull(colIdx)) {
                        changed = true;
                        break;
                    }
                    String oldStr = c.getString(colIdx);
                    String newStr = newVal.toString();
                    if (!newStr.equals(oldStr)) {
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                return true;
            }
            return false;
        } catch (Exception e) {
            try {
                db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (Throwable ignored) {}
            return true;
        } finally {
            if (c != null) c.close();
        }
    }

    private static int pullTable(SQLiteDatabase db, String baseUrl, String key, String tableName) {
        try {
            String endpoint = baseUrl + "/rest/v1/" + tableName + "?select=*";
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("apikey", key);
            conn.setRequestProperty("Authorization", "Bearer " + key);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                String body = readStream(conn.getInputStream());
                conn.disconnect();
                JSONArray array = new JSONArray(body);
                int count = array.length();
                int changedCount = 0;

                for (int i = 0; i < count; i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ContentValues cv = new ContentValues();
                    java.util.Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        Object val = obj.opt(k);
                        if (val == null || val == JSONObject.NULL) {
                            cv.putNull(k);
                        } else if (val instanceof Integer || val instanceof Long) {
                            cv.put(k, ((Number) val).longValue());
                        } else if (val instanceof Double || val instanceof Float) {
                            cv.put(k, ((Number) val).doubleValue());
                        } else {
                            cv.put(k, val.toString());
                        }
                    }
                    if (upsertIfChanged(db, tableName, cv)) {
                        changedCount++;
                    }
                }
                return changedCount;
            }
            conn.disconnect();
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static <T> void postSuccess(final Callback<T> callback, final T result) {
        if (callback == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result);
            }
        });
    }

    private static <T> void postError(final Callback<T> callback, final String message, final Throwable error) {
        if (callback == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message, error);
            }
        });
    }

    /**
     * Ready-to-execute PostgreSQL DDL script for Supabase SQL Editor.
     */
    public static String getSupabaseSqlSchema() {
        return "-- ==========================================================\n" +
               "-- CSP Daily Report Database Schema for Supabase PostgreSQL\n" +
               "-- Project URL: https://sfegesdzmoyolrrbturx.supabase.co\n" +
               "-- ==========================================================\n\n" +
               "-- 1. CSP Master Table\n" +
               "CREATE TABLE IF NOT EXISTS public.csp (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    csp_code TEXT NOT NULL UNIQUE,\n" +
               "    csp_name TEXT NOT NULL,\n" +
               "    gender TEXT NOT NULL DEFAULT '',\n" +
               "    mobile_number TEXT NOT NULL DEFAULT '',\n" +
               "    branch_name TEXT NOT NULL DEFAULT '',\n" +
               "    branch_code TEXT NOT NULL DEFAULT '',\n" +
               "    status TEXT NOT NULL DEFAULT 'ACTIVE',\n" +
               "    notes TEXT NOT NULL DEFAULT '',\n" +
               "    tgt_inoperative_rekyc INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_qr_code_rekyc INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_pmjjby INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_pmsby INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_pmjdy INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_apy INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_inoperative_rekyc_branch_submit INTEGER NOT NULL DEFAULT 0,\n" +
               "    tgt_whatsapp_banking INTEGER NOT NULL DEFAULT 0,\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'))\n" +
               ");\n\n" +
               "-- 2. App Users & Supervisors\n" +
               "CREATE TABLE IF NOT EXISTS public.app_user (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    username TEXT NOT NULL UNIQUE,\n" +
               "    password TEXT DEFAULT '',\n" +
               "    password_hash TEXT DEFAULT '',\n" +
               "    password_salt TEXT DEFAULT '',\n" +
               "    is_default_password BOOLEAN NOT NULL DEFAULT FALSE,\n" +
               "    name TEXT NOT NULL,\n" +
               "    role TEXT NOT NULL DEFAULT 'SUPERVISOR',\n" +
               "    mobile TEXT NOT NULL DEFAULT '',\n" +
               "    email TEXT NOT NULL DEFAULT '',\n" +
               "    status TEXT NOT NULL DEFAULT 'ACTIVE',\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'))\n" +
               ");\n\n" +
               "-- 3. Supervisor to CSP Mapping\n" +
               "CREATE TABLE IF NOT EXISTS public.supervisor_csp_map (\n" +
               "    supervisor_id BIGINT NOT NULL REFERENCES public.app_user(id) ON DELETE CASCADE,\n" +
               "    csp_id BIGINT NOT NULL REFERENCES public.csp(id) ON DELETE CASCADE,\n" +
               "    assigned_date TEXT NOT NULL DEFAULT '',\n" +
               "    PRIMARY KEY(supervisor_id, csp_id)\n" +
               ");\n\n" +
               "-- 4. Daily Reports\n" +
               "CREATE TABLE IF NOT EXISTS public.daily_report (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    csp_id BIGINT NOT NULL REFERENCES public.csp(id) ON DELETE CASCADE,\n" +
               "    business_date TEXT NOT NULL,\n" +
               "    inoperative_rekyc INTEGER NOT NULL DEFAULT 0,\n" +
               "    qr_code_rekyc INTEGER NOT NULL DEFAULT 0,\n" +
               "    pmjjby INTEGER NOT NULL DEFAULT 0,\n" +
               "    pmsby INTEGER NOT NULL DEFAULT 0,\n" +
               "    pmjdy INTEGER NOT NULL DEFAULT 0,\n" +
               "    apy INTEGER NOT NULL DEFAULT 0,\n" +
               "    inoperative_rekyc_branch_submit INTEGER NOT NULL DEFAULT 0,\n" +
               "    whatsapp_banking INTEGER NOT NULL DEFAULT 0,\n" +
               "    remarks TEXT NOT NULL DEFAULT '',\n" +
               "    submitted_at TEXT,\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    UNIQUE (csp_id, business_date)\n" +
               ");\n\n" +
               "-- 5. Hourly Reports\n" +
               "CREATE TABLE IF NOT EXISTS public.hourly_report (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    csp_id BIGINT NOT NULL REFERENCES public.csp(id) ON DELETE CASCADE,\n" +
               "    business_date TEXT NOT NULL,\n" +
               "    hour INTEGER NOT NULL,\n" +
               "    inoperative_rekyc INTEGER NOT NULL DEFAULT 0,\n" +
               "    qr_code_rekyc INTEGER NOT NULL DEFAULT 0,\n" +
               "    pmjjby INTEGER NOT NULL DEFAULT 0,\n" +
               "    pmsby INTEGER NOT NULL DEFAULT 0,\n" +
               "    pmjdy INTEGER NOT NULL DEFAULT 0,\n" +
               "    apy INTEGER NOT NULL DEFAULT 0,\n" +
               "    inoperative_rekyc_branch_submit INTEGER NOT NULL DEFAULT 0,\n" +
               "    whatsapp_banking INTEGER NOT NULL DEFAULT 0,\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    UNIQUE (csp_id, business_date, hour)\n" +
               ");\n\n" +
               "-- 6. Admin Instructions\n" +
               "CREATE TABLE IF NOT EXISTS public.admin_instruction (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    business_date TEXT NOT NULL,\n" +
               "    sender_name TEXT NOT NULL,\n" +
               "    title TEXT NOT NULL,\n" +
               "    message TEXT NOT NULL,\n" +
               "    priority TEXT NOT NULL DEFAULT 'NORMAL',\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    UNIQUE (business_date, sender_name, created_at)\n" +
               ");\n\n" +
               "-- 7. Pending Reasons\n" +
               "CREATE TABLE IF NOT EXISTS public.pending_reason (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    csp_id BIGINT NOT NULL REFERENCES public.csp(id) ON DELETE CASCADE,\n" +
               "    business_date TEXT NOT NULL,\n" +
               "    reason TEXT NOT NULL,\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    UNIQUE (csp_id, business_date)\n" +
               ");\n\n" +
               "-- 8. Follow-up Logs\n" +
               "CREATE TABLE IF NOT EXISTS public.followup_log (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    csp_id BIGINT NOT NULL REFERENCES public.csp(id) ON DELETE CASCADE,\n" +
               "    business_date TEXT NOT NULL,\n" +
               "    action_status TEXT NOT NULL,\n" +
               "    note TEXT NOT NULL DEFAULT '',\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    UNIQUE (csp_id, business_date, created_at)\n" +
               ");\n\n" +
               "-- 9. Daily CSP Targets\n" +
               "CREATE TABLE IF NOT EXISTS public.daily_csp_target (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    csp_id BIGINT NOT NULL REFERENCES public.csp(id) ON DELETE CASCADE,\n" +
               "    business_date TEXT NOT NULL,\n" +
               "    field_key TEXT NOT NULL,\n" +
               "    target_value INTEGER NOT NULL DEFAULT 0,\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    UNIQUE (csp_id, business_date, field_key)\n" +
               ");\n\n" +
               "-- 10. Office Notes\n" +
               "CREATE TABLE IF NOT EXISTS public.office_note (\n" +
               "    business_date TEXT PRIMARY KEY,\n" +
               "    note TEXT NOT NULL DEFAULT '',\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'))\n" +
               ");\n\n" +
               "-- 11. Business Days\n" +
               "CREATE TABLE IF NOT EXISTS public.business_day (\n" +
               "    business_date TEXT PRIMARY KEY,\n" +
               "    day_type TEXT NOT NULL DEFAULT 'WORKING',\n" +
               "    description TEXT NOT NULL DEFAULT '',\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'))\n" +
               ");\n\n" +
               "-- 12. App Releases & Updates\n" +
               "CREATE TABLE IF NOT EXISTS public.app_updates (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    version_name TEXT NOT NULL,\n" +
               "    version_code INTEGER NOT NULL UNIQUE,\n" +
               "    minimum_supported_version_code INTEGER NOT NULL DEFAULT 0,\n" +
               "    update_type TEXT NOT NULL DEFAULT 'major',\n" +
               "    title TEXT NOT NULL DEFAULT 'Update Available',\n" +
               "    message TEXT NOT NULL DEFAULT '',\n" +
               "    apk_path TEXT NOT NULL DEFAULT '',\n" +
               "    apk_file_name TEXT NOT NULL DEFAULT '',\n" +
               "    apk_file_path TEXT NOT NULL DEFAULT '',\n" +
               "    apk_size_bytes BIGINT NOT NULL DEFAULT 0,\n" +
               "    apk_sha256 TEXT NOT NULL DEFAULT '',\n" +
               "    release_notes JSONB NOT NULL DEFAULT '[]'::jsonb,\n" +
               "    is_mandatory BOOLEAN NOT NULL DEFAULT false,\n" +
               "    is_active BOOLEAN NOT NULL DEFAULT true,\n" +
               "    released_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    created_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')),\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'))\n" +
               ");\n\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS minimum_supported_version_code INTEGER DEFAULT 0;\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS update_type TEXT DEFAULT 'major';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS title TEXT DEFAULT 'Update Available';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS message TEXT DEFAULT '';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_path TEXT DEFAULT '';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_file_name TEXT DEFAULT '';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_file_path TEXT DEFAULT '';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_size_bytes BIGINT DEFAULT 0;\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS apk_sha256 TEXT DEFAULT '';\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS release_notes JSONB DEFAULT '[]'::jsonb;\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS is_mandatory BOOLEAN DEFAULT false;\n" +
               "ALTER TABLE public.app_updates ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;\n\n" +
               "-- 13. Remote Configuration & Feature Flags\n" +
               "CREATE TABLE IF NOT EXISTS public.remote_config (\n" +
               "    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
               "    config_key TEXT NOT NULL UNIQUE,\n" +
               "    config_value TEXT NOT NULL DEFAULT '',\n" +
               "    is_active BOOLEAN NOT NULL DEFAULT true,\n" +
               "    updated_at TEXT NOT NULL DEFAULT (to_char(now(), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'))\n" +
               ");\n\n" +
               "-- Seed Default Remote Config\n" +
               "INSERT INTO public.remote_config (config_key, config_value, is_active) VALUES\n" +
               "    ('maintenance_mode', 'false', true),\n" +
               "    ('maintenance_message', 'System is temporarily undergoing scheduled maintenance. Normal access will resume shortly.', true),\n" +
               "    ('announcement_active', 'false', true),\n" +
               "    ('announcement_title', 'System Update', true),\n" +
               "    ('announcement_message', 'Welcome to CSP Daily Report. Live cloud sync and update systems are online.', true),\n" +
               "    ('announcement_severity', 'info', true),\n" +
               "    ('min_sync_interval_sec', '15', true)\n" +
               "ON CONFLICT (config_key) DO NOTHING;\n\n" +
               "-- 14. Storage Bucket for APK Releases\n" +
               "INSERT INTO storage.buckets (id, name, public) VALUES ('app-releases', 'app-releases', true)\n" +
               "ON CONFLICT (id) DO UPDATE SET public = true;\n\n" +
               "-- Enable Row Level Security & Allow Anon Public Access Policies\n" +
               "ALTER TABLE public.csp ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access csp\" ON public.csp FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.app_user ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access app_user\" ON public.app_user FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.supervisor_csp_map ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access supervisor_csp_map\" ON public.supervisor_csp_map FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.daily_report ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access daily_report\" ON public.daily_report FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.hourly_report ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access hourly_report\" ON public.hourly_report FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.admin_instruction ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access admin_instruction\" ON public.admin_instruction FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.pending_reason ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access pending_reason\" ON public.pending_reason FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.followup_log ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access followup_log\" ON public.followup_log FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.daily_csp_target ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access daily_csp_target\" ON public.daily_csp_target FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.office_note ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access office_note\" ON public.office_note FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.business_day ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Full Access business_day\" ON public.business_day FOR ALL TO anon USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.app_updates ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Read Access app_updates\" ON public.app_updates FOR SELECT TO anon, authenticated USING (true);\n" +
               "CREATE POLICY \"Admin Full Access app_updates\" ON public.app_updates FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);\n\n" +
               "ALTER TABLE public.remote_config ENABLE ROW LEVEL SECURITY;\n" +
               "CREATE POLICY \"Anon Read Access remote_config\" ON public.remote_config FOR SELECT TO anon, authenticated USING (true);\n" +
               "CREATE POLICY \"Admin Full Access remote_config\" ON public.remote_config FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);\n\n" +
               "-- Storage Object Policies for app-releases\n" +
               "CREATE POLICY \"Public Read Access app-releases\" ON storage.objects FOR SELECT TO anon, authenticated USING (bucket_id = 'app-releases');\n" +
               "CREATE POLICY \"Admin Upload Access app-releases\" ON storage.objects FOR INSERT TO anon, authenticated WITH CHECK (bucket_id = 'app-releases');\n";
    }
}
