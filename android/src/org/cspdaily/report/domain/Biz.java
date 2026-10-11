package org.cspdaily.report.domain;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/* loaded from: classes.dex */
public final class Biz {
    public static final TimeZone TZ = TimeZone.getTimeZone("Asia/Kolkata");
    private static final ThreadLocal<SimpleDateFormat> FMT_DT = new ThreadLocal<SimpleDateFormat>() { // from class: org.cspdaily.report.domain.Biz.1
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // java.lang.ThreadLocal
        public SimpleDateFormat initialValue() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            simpleDateFormat.setTimeZone(Biz.TZ);
            return simpleDateFormat;
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FMT_D = new ThreadLocal<SimpleDateFormat>() { // from class: org.cspdaily.report.domain.Biz.2
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // java.lang.ThreadLocal
        public SimpleDateFormat initialValue() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            simpleDateFormat.setTimeZone(Biz.TZ);
            return simpleDateFormat;
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FMT_DMY = new ThreadLocal<SimpleDateFormat>() { // from class: org.cspdaily.report.domain.Biz.3
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // java.lang.ThreadLocal
        public SimpleDateFormat initialValue() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            simpleDateFormat.setTimeZone(Biz.TZ);
            return simpleDateFormat;
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FMT_HM = new ThreadLocal<SimpleDateFormat>() { // from class: org.cspdaily.report.domain.Biz.4
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // java.lang.ThreadLocal
        public SimpleDateFormat initialValue() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.US);
            simpleDateFormat.setTimeZone(Biz.TZ);
            return simpleDateFormat;
        }
    };

    private Biz() {
    }

    public static String now() {
        return FMT_DT.get().format(new Date());
    }

    public static String today() {
        return FMT_D.get().format(new Date());
    }

    public static String dateOf(long j) {
        return FMT_D.get().format(new Date(j));
    }

    public static String fmtDmy(String str) {
        return (str == null || str.length() < 10) ? String.valueOf(str) : str.substring(8, 10) + "-" + str.substring(5, 7) + "-" + str.substring(0, 4);
    }

    public static String fmtMonth(String str) {
        try {
            return new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"}[Math.max(0, Math.min(11, Integer.parseInt(str.substring(5, 7)) - 1))] + " " + Integer.parseInt(str.substring(0, 4));
        } catch (Exception e) {
            return String.valueOf(str);
        }
    }

    public static String shortWhen(String str) {
        return (str == null || str.length() < 16) ? "" : str.substring(8, 10) + "-" + str.substring(5, 7) + " " + str.substring(11, 16);
    }

    public static String clockOf(String str) {
        return (str == null || str.length() < 16) ? "" : str.substring(11, 16);
    }

    public static int hourOfNow() {
        return Integer.parseInt(now().substring(11, 13));
    }

    public static int dayOfWeek(String str) {
        return cal(str).get(7) - 1;
    }

    public static String dayName(String str) {
        return new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"}[cal(str).get(7) - 1];
    }

    public static boolean isToday(String str) {
        return str != null && str.equals(today());
    }

    public static boolean isTomorrow(String str) {
        return str != null && str.equals(plusDays(today(), 1));
    }

    public static boolean isYesterday(String str) {
        return str != null && str.equals(plusDays(today(), -1));
    }

    public static String friendlyDate(String str) {
        return isToday(str) ? "Today" : isYesterday(str) ? "Yesterday" : isTomorrow(str) ? "Tomorrow" : fmtDmy(str);
    }

    public static String plusDays(String str, int i) {
        Calendar cal = cal(str);
        cal.add(5, i);
        return FMT_D.get().format(cal.getTime());
    }

    public static Calendar cal(String str) {
        Calendar calendar = Calendar.getInstance(TZ);
        calendar.clear();
        try {
            calendar.set(Integer.parseInt(str.substring(0, 4)), Integer.parseInt(str.substring(5, 7)) - 1, Integer.parseInt(str.substring(8, 10)));
        } catch (Exception e) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(11, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
        }
        return calendar;
    }

    public static long millisOf(String str) {
        long time;
        if (str == null || str.length() < 10) {
            return 0L;
        }
        try {
            if (str.length() <= 10) {
                time = cal(str).getTimeInMillis();
            } else {
                time = FMT_DT.get().parse(str).getTime();
            }
            return time;
        } catch (Exception e) {
            return 0L;
        }
    }

    public static int cleanCount(String str) {
        if (str == null) {
            return 0;
        }
        String trim = str.trim();
        if (trim.isEmpty()) {
            return 0;
        }
        if (trim.length() > 7) {
            return -1;
        }
        for (int i = 0; i < trim.length(); i++) {
            if (trim.charAt(i) < '0' || trim.charAt(i) > '9') {
                return -1;
            }
        }
        long parseLong = Long.parseLong(trim);
        if (parseLong > 2147483647L) {
            return -1;
        }
        return (int) parseLong;
    }

    public static String cspCodeSuffix(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (str.length() > 3) {
            str = str.substring(str.length() - 3);
        }
        if (str.length() == 3 && str.charAt(0) == '0') {
            return str.substring(1);
        }
        return str;
    }

    public static String hourLabel(int i) {
        return (i % 12 == 0 ? 12 : i % 12) + (i < 12 ? " AM" : " PM");
    }

    public static int parseHourSetting(String str, int i) {
        if (str != null) {
            String trim = str.trim();
            if (!trim.isEmpty()) {
                int indexOf = trim.indexOf(58);
                if (indexOf >= 0) {
                    trim = trim.substring(0, indexOf);
                }
                if (trim.length() != 0 && trim.length() <= 2) {
                    for (int i2 = 0; i2 < trim.length(); i2++) {
                        if (trim.charAt(i2) < '0' || trim.charAt(i2) > '9') {
                            return i;
                        }
                    }
                    return Math.max(0, Math.min(23, Integer.parseInt(trim)));
                }
                return i;
            }
            return i;
        }
        return i;
    }

    public static String fieldSlug(String str) {
        String lowerCase = str == null ? "" : str.trim().toLowerCase(Locale.US);
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (int i = 0; i < lowerCase.length(); i++) {
            char charAt = lowerCase.charAt(i);
            if ((charAt >= 'a' && charAt <= 'z') || (charAt >= '0' && charAt <= '9')) {
                if (z && sb.length() > 0) {
                    sb.append('_');
                }
                sb.append(charAt);
                z = false;
            } else {
                z = true;
            }
        }
        String sb2 = sb.toString();
        if (sb2.startsWith("_")) {
            sb2 = sb2.substring(1);
        }
        if (sb2.endsWith("_")) {
            sb2 = sb2.substring(0, sb2.length() - 1);
        }
        if (sb2.isEmpty() || (sb2.charAt(0) >= '0' && sb2.charAt(0) <= '9')) {
            sb2 = "f" + (sb2.isEmpty() ? "" : "_" + sb2);
        }
        if (sb2.length() > 31) {
            return sb2.substring(0, 31);
        }
        return sb2;
    }

    public static String initials(String str) {
        if (str == null) {
            return "?";
        }
        String trim = str.trim();
        if (trim.isEmpty()) {
            return "?";
        }
        String[] split = trim.split("\\s+");
        if (split.length == 1) {
            return split[0].substring(0, Math.min(2, split[0].length())).toUpperCase(Locale.US);
        }
        return ("" + split[0].charAt(0) + split[split.length - 1].charAt(0)).toUpperCase(Locale.US);
    }

    public static int nz(Integer num) {
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    public static String nz(String str) {
        return str == null ? "" : str;
    }

    public static String csv(String str) {
        if (str == null) {
            str = "";
        }
        if (str.indexOf(44) >= 0 || str.indexOf(34) >= 0 || str.indexOf(10) >= 0 || str.indexOf(13) >= 0) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    public static String phoneDigits(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char charAt = str.charAt(i);
            if (charAt >= '0' && charAt <= '9') {
                sb.append(charAt);
            }
        }
        String sb2 = sb.toString();
        if (sb2.startsWith("00")) {
            sb2 = sb2.substring(2);
        } else if (sb2.startsWith("0") && sb2.length() == 11) {
            sb2 = sb2.substring(1);
        }
        if (sb2.length() == 10) {
            return "91" + sb2;
        }
        return sb2;
    }

    public static String phoneE164(String str) {
        String phoneDigits = phoneDigits(str);
        return phoneDigits.isEmpty() ? "" : "+" + phoneDigits;
    }
}
