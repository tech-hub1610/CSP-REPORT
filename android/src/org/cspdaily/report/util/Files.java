package org.cspdaily.report.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/* loaded from: classes.dex */
public final class Files {
    public static final int REQ_CREATE = 300;
    public static final int REQ_OPEN = 301;
    public static final int REQ_OPEN_CSV = 302;
    public static String pendingName;
    public static String pendingText;

    private Files() {
    }

    public static void saveText(Activity activity, String str, String str2, String str3) {
        pendingName = str;
        pendingText = str3;
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        if (str2 == null) {
            str2 = "application/octet-stream";
        }
        intent.setType(str2);
        intent.putExtra("android.intent.extra.TITLE", str);
        activity.startActivityForResult(Intent.createChooser(intent, "Save " + str), REQ_CREATE);
    }

    public static void pickJson(Activity activity) {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        activity.startActivityForResult(Intent.createChooser(intent, "Choose a backup"), REQ_OPEN);
    }

    public static void pickCsv(Activity activity) {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        activity.startActivityForResult(Intent.createChooser(intent, "Choose CSV file to import"), REQ_OPEN_CSV);
    }

    public static boolean write(Activity activity, Uri uri, String str) {
        boolean z = false;
        OutputStream outputStream = null;
        try {
            OutputStream openOutputStream = activity.getContentResolver().openOutputStream(uri, "wt");
            if (openOutputStream != null) {
                openOutputStream.write(str.getBytes("UTF-8"));
                openOutputStream.flush();
                z = true;
                if (openOutputStream != null) {
                    try {
                        openOutputStream.close();
                    } catch (Exception e) {
                    }
                }
            } else if (openOutputStream != null) {
                try {
                    openOutputStream.close();
                } catch (Exception e2) {
                }
            }
        } catch (Exception e3) {
            if (0 != 0) {
                try {
                    outputStream.close();
                } catch (Exception e4) {
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    outputStream.close();
                } catch (Exception e5) {
                }
            }
            throw th;
        }
        return z;
    }

    public static String read(Activity activity, Uri uri) throws Exception {
        InputStream openInputStream = activity.getContentResolver().openInputStream(uri);
        if (openInputStream == null) {
            throw new IOException("Could not open the file");
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(openInputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    sb.append(readLine).append('\n');
                } else {
                    return sb.toString();
                }
            }
        } finally {
            openInputStream.close();
        }
    }
}
