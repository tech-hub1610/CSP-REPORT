package org.cspdaily.report.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ApkDownloader {

    public interface DownloadListener {
        void onDownloadStarted(long totalBytes);
        void onDownloadProgress(long bytesRead, long totalBytes, int percent);
        void onDownloadSuccess(File apkFile);
        void onDownloadError(String message, Throwable error);
        void onDownloadCancelled();
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean CANCEL_FLAG = new AtomicBoolean(false);
    private static volatile boolean sDownloading = false;

    private ApkDownloader() {}

    public static boolean isDownloading() {
        return sDownloading;
    }

    public static void cancel() {
        CANCEL_FLAG.set(true);
    }

    public static File getUpdatesDir(Context context) {
        File dir = new File(context.getCacheDir(), "updates");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void cleanOldDownloads(Context context) {
        try {
            File dir = getUpdatesDir(context);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void download(final Context context, final String downloadUrl, final String targetFileName, final DownloadListener listener) {
        if (sDownloading) {
            if (listener != null) listener.onDownloadError("A download is already in progress.", null);
            return;
        }

        sDownloading = true;
        CANCEL_FLAG.set(false);

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                File outputFile = null;
                File tempFile = null;
                HttpURLConnection conn = null;
                InputStream is = null;
                FileOutputStream fos = null;

                try {
                    File dir = getUpdatesDir(context);
                    String fname = targetFileName != null && !targetFileName.trim().isEmpty()
                            ? targetFileName.trim()
                            : "CSPDailyReport-update.apk";

                    outputFile = new File(dir, fname);
                    tempFile = new File(dir, fname + ".part");
                    if (tempFile.exists()) tempFile.delete();

                    URL url = new URL(downloadUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "CSPDailyReport-Updater/1.0");
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode < 200 || responseCode >= 300) {
                        throw new Exception("Server returned HTTP " + responseCode + ": " + conn.getResponseMessage());
                    }

                    final long contentLength = conn.getContentLengthLong();
                    postStarted(listener, contentLength);

                    is = conn.getInputStream();
                    fos = new FileOutputStream(tempFile);

                    byte[] buffer = new byte[16384];
                    long totalBytesRead = 0;
                    int read;
                    long lastPostTime = 0;

                    while ((read = is.read(buffer)) != -1) {
                        if (CANCEL_FLAG.get()) {
                            fos.close();
                            is.close();
                            tempFile.delete();
                            sDownloading = false;
                            postCancelled(listener);
                            return;
                        }

                        fos.write(buffer, 0, read);
                        totalBytesRead += read;

                        long now = System.currentTimeMillis();
                        if (now - lastPostTime > 100 || totalBytesRead == contentLength) {
                            lastPostTime = now;
                            final int percent = contentLength > 0 ? (int) ((totalBytesRead * 100) / contentLength) : -1;
                            final long bytesCurrent = totalBytesRead;
                            postProgress(listener, bytesCurrent, contentLength, percent);
                        }
                    }

                    fos.flush();
                    fos.close();
                    fos = null;

                    is.close();
                    is = null;

                    if (outputFile.exists()) outputFile.delete();
                    if (!tempFile.renameTo(outputFile)) {
                        throw new Exception("Failed to rename temporary download file to target APK.");
                    }

                    sDownloading = false;
                    final File finalFile = outputFile;
                    postSuccess(listener, finalFile);

                } catch (final Exception e) {
                    sDownloading = false;
                    if (tempFile != null && tempFile.exists()) {
                        tempFile.delete();
                    }
                    postError(listener, e.getMessage(), e);
                } finally {
                    try { if (fos != null) fos.close(); } catch (Throwable ignored) {}
                    try { if (is != null) is.close(); } catch (Throwable ignored) {}
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    private static void postStarted(final DownloadListener listener, final long totalBytes) {
        if (listener == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onDownloadStarted(totalBytes);
            }
        });
    }

    private static void postProgress(final DownloadListener listener, final long bytesRead, final long totalBytes, final int percent) {
        if (listener == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onDownloadProgress(bytesRead, totalBytes, percent);
            }
        });
    }

    private static void postSuccess(final DownloadListener listener, final File file) {
        if (listener == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onDownloadSuccess(file);
            }
        });
    }

    private static void postError(final DownloadListener listener, final String msg, final Throwable e) {
        if (listener == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onDownloadError(msg != null ? msg : "Download failed", e);
            }
        });
    }

    private static void postCancelled(final DownloadListener listener) {
        if (listener == null) return;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onDownloadCancelled();
            }
        });
    }
}
