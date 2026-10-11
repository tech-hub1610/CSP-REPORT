package org.cspdaily.report.update;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class AppRelease {
    public static final String TYPE_MINOR = "minor";
    public static final String TYPE_MAJOR = "major";
    public static final String TYPE_MANDATORY = "mandatory";

    public long id;
    public String versionName = "1.0";
    public int versionCode = 0;
    public int minimumSupportedVersionCode = 0;
    public String updateType = TYPE_MAJOR;
    public String title = "New Version Available";
    public String message = "";
    public String apkPath = "";
    public String apkSha256 = "";
    public List<String> releaseNotes = new ArrayList<>();
    public boolean isActive = true;
    public String releasedAt = "";
    public String createdAt = "";
    public String updatedAt = "";

    public AppRelease() {}

    public boolean isMandatory(int installedVersionCode) {
        if (TYPE_MANDATORY.equalsIgnoreCase(updateType)) {
            return true;
        }
        return installedVersionCode < minimumSupportedVersionCode;
    }

    public String getResolvedDownloadUrl(String supabaseBaseUrl, String bucketName) {
        if (apkPath == null || apkPath.trim().isEmpty()) {
            return "";
        }
        String path = apkPath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        // Build Supabase storage public URL
        String base = supabaseBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        // Strip duplicate bucket names if present in path (e.g. app-releases/..., apk-release/...)
        if (bucketName != null && path.startsWith(bucketName + "/")) {
            path = path.substring(bucketName.length() + 1);
        } else if (path.startsWith("app-releases/")) {
            path = path.substring("app-releases/".length());
        } else if (path.startsWith("apk-release/")) {
            path = path.substring("apk-release/".length());
        } else if (path.startsWith("apk-releases/")) {
            path = path.substring("apk-releases/".length());
        }
        return base + "/storage/v1/object/public/" + (bucketName != null ? bucketName : "app-releases") + "/" + path;
    }

    public static AppRelease fromJson(JSONObject json) {
        if (json == null) return null;
        AppRelease r = new AppRelease();
        r.id = json.optLong("id", 0);
        r.versionName = json.optString("version_name", json.optString("version", "1.0"));
        r.versionCode = json.optInt("version_code", json.optInt("versionCode", 0));
        r.minimumSupportedVersionCode = json.optInt("minimum_supported_version_code", json.optInt("min_version_code", 0));
        
        boolean isMandatoryFlag = json.optBoolean("is_mandatory", false);
        r.updateType = json.optString("update_type", isMandatoryFlag ? TYPE_MANDATORY : TYPE_MAJOR);
        r.title = json.optString("title", "Update Available: v" + r.versionName);
        r.message = json.optString("message", json.optString("release_notes", ""));

        // Support various column alias names in Supabase
        r.apkPath = json.optString("apk_path", "");
        if (r.apkPath.isEmpty()) {
            r.apkPath = json.optString("apk_file_path", "");
        }
        if (r.apkPath.isEmpty()) {
            r.apkPath = json.optString("apk_file_name", "");
        }

        r.apkSha256 = json.optString("apk_sha256", json.optString("sha256", ""));

        // Support boolean, integer, or string for is_active
        if (json.has("is_active")) {
            Object act = json.opt("is_active");
            if (act instanceof Boolean) {
                r.isActive = (Boolean) act;
            } else if (act instanceof Number) {
                r.isActive = ((Number) act).intValue() == 1;
            } else if (act instanceof String) {
                r.isActive = "true".equalsIgnoreCase((String) act) || "1".equals(act);
            } else {
                r.isActive = true;
            }
        } else {
            r.isActive = true;
        }

        r.releasedAt = json.optString("released_at", json.optString("published_at", json.optString("created_at", "")));
        r.createdAt = json.optString("created_at", "");
        r.updatedAt = json.optString("updated_at", "");

        r.releaseNotes = new ArrayList<>();
        Object notesObj = json.opt("release_notes");
        if (notesObj instanceof JSONArray) {
            JSONArray arr = (JSONArray) notesObj;
            for (int i = 0; i < arr.length(); i++) {
                r.releaseNotes.add(arr.optString(i));
            }
        } else if (notesObj instanceof String) {
            String str = (String) notesObj;
            try {
                JSONArray arr = new JSONArray(str);
                for (int i = 0; i < arr.length(); i++) {
                    r.releaseNotes.add(arr.optString(i));
                }
            } catch (Exception e) {
                if (!str.trim().isEmpty()) {
                    for (String line : str.split("\n")) {
                        if (!line.trim().isEmpty()) {
                            r.releaseNotes.add(line.replace("•", "").trim());
                        }
                    }
                }
            }
        }
        return r;
    }

    public static AppRelease fromCursor(Cursor c) {
        if (c == null) return null;
        AppRelease r = new AppRelease();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.versionName = c.getString(c.getColumnIndexOrThrow("version_name"));
        r.versionCode = c.getInt(c.getColumnIndexOrThrow("version_code"));
        r.minimumSupportedVersionCode = c.getInt(c.getColumnIndexOrThrow("minimum_supported_version_code"));
        r.updateType = c.getString(c.getColumnIndexOrThrow("update_type"));
        r.title = c.getString(c.getColumnIndexOrThrow("title"));
        r.message = c.getString(c.getColumnIndexOrThrow("message"));
        r.apkPath = c.getString(c.getColumnIndexOrThrow("apk_path"));
        r.apkSha256 = c.getString(c.getColumnIndexOrThrow("apk_sha256"));
        r.isActive = c.getInt(c.getColumnIndexOrThrow("is_active")) == 1;
        r.releasedAt = c.getString(c.getColumnIndexOrThrow("released_at"));
        r.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        r.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));

        r.releaseNotes = new ArrayList<>();
        int notesIdx = c.getColumnIndex("release_notes");
        if (notesIdx != -1 && !c.isNull(notesIdx)) {
            String notesStr = c.getString(notesIdx);
            try {
                JSONArray arr = new JSONArray(notesStr);
                for (int i = 0; i < arr.length(); i++) {
                    r.releaseNotes.add(arr.optString(i));
                }
            } catch (Exception e) {
                if (!notesStr.trim().isEmpty()) {
                    r.releaseNotes.add(notesStr.trim());
                }
            }
        }
        return r;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        if (id > 0) cv.put("id", id);
        cv.put("version_name", versionName);
        cv.put("version_code", versionCode);
        cv.put("minimum_supported_version_code", minimumSupportedVersionCode);
        cv.put("update_type", updateType);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("apk_path", apkPath);
        cv.put("apk_sha256", apkSha256);
        cv.put("release_notes", new JSONArray(releaseNotes).toString());
        cv.put("is_active", isActive ? 1 : 0);
        cv.put("released_at", releasedAt);
        cv.put("created_at", createdAt);
        cv.put("updated_at", updatedAt);
        return cv;
    }
}
