package org.cspdaily.report.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

public final class ApkIntegrityVerifier {

    public static class VerificationResult {
        public boolean valid;
        public String calculatedSha256 = "";
        public String expectedSha256 = "";
        public String archivePackageName = "";
        public int archiveVersionCode = 0;
        public String archiveVersionName = "";
        public String errorMessage = "";

        public VerificationResult(boolean valid, String error) {
            this.valid = valid;
            this.errorMessage = error != null ? error : "";
        }
    }

    private ApkIntegrityVerifier() {}

    public static VerificationResult verify(Context context, File apkFile, AppRelease release) {
        if (apkFile == null || !apkFile.exists() || apkFile.length() == 0) {
            return new VerificationResult(false, "Downloaded APK file does not exist or is empty.");
        }

        // 1. Calculate SHA-256
        String calculatedHash;
        try {
            calculatedHash = calculateSha256(apkFile);
        } catch (Exception e) {
            return new VerificationResult(false, "Failed to compute SHA-256 hash: " + e.getMessage());
        }

        VerificationResult result = new VerificationResult(true, "");
        result.calculatedSha256 = calculatedHash;
        result.expectedSha256 = release != null && release.apkSha256 != null ? release.apkSha256.trim().toLowerCase(Locale.US) : "";

        // 2. Validate SHA-256 against release metadata (if expected hash is provided)
        if (!result.expectedSha256.isEmpty()) {
            if (!calculatedHash.equalsIgnoreCase(result.expectedSha256)) {
                result.valid = false;
                result.errorMessage = "SHA-256 checksum mismatch!\nExpected: " + result.expectedSha256 + "\nActual: " + calculatedHash;
                return result;
            }
        }

        // 3. Inspect Package Info inside APK
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (info == null) {
                result.valid = false;
                result.errorMessage = "Corrupted APK file: Android system could not parse package archive.";
                return result;
            }

            result.archivePackageName = info.packageName != null ? info.packageName : "";
            result.archiveVersionCode = info.versionCode;
            result.archiveVersionName = info.versionName != null ? info.versionName : "";

            // Validate package name
            String expectedPkg = context.getPackageName();
            if (!expectedPkg.equals(result.archivePackageName)) {
                result.valid = false;
                result.errorMessage = "Package name mismatch! Expected " + expectedPkg + " but found " + result.archivePackageName;
                return result;
            }

            // Validate version code if release metadata is provided
            if (release != null && release.versionCode > 0) {
                if (result.archiveVersionCode != release.versionCode) {
                    result.valid = false;
                    result.errorMessage = "Version code mismatch! Release metadata expects " + release.versionCode + " but APK contains " + result.archiveVersionCode;
                    return result;
                }
            }

        } catch (Exception e) {
            result.valid = false;
            result.errorMessage = "Failed to parse APK archive: " + e.getMessage();
            return result;
        }

        result.valid = true;
        return result;
    }

    public static String calculateSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        is.close();

        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.US);
    }
}
