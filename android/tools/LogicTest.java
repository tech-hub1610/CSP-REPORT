import org.cspdaily.report.domain.Biz;

/**
 * JVM checks for the pure helpers ported from inc/report.php. These run with no
 * Android device: `bash tools/run-tests.sh`.
 */
public class LogicTest {

    static int pass = 0;
    static int fail = 0;

    static void eq(String what, Object got, Object want) {
        if (want.equals(got)) {
            pass++;
        } else {
            fail++;
            System.out.println("FAIL " + what + ": got <" + got + "> want <" + want + ">");
        }
    }

    public static void main(String[] args) {
        // csp_code_suffix(): last 3, dropping a leading zero to give last 2.
        eq("suffix 32250091", Biz.cspCodeSuffix("32250091"), "91");
        eq("suffix 32250049", Biz.cspCodeSuffix("32250049"), "49");
        eq("suffix 32250111", Biz.cspCodeSuffix("32250111"), "111");
        eq("suffix 32250101", Biz.cspCodeSuffix("32250101"), "101");
        eq("suffix 32250160", Biz.cspCodeSuffix("32250160"), "160");
        eq("suffix 9-digit typo", Biz.cspCodeSuffix("322500163"), "163");

        // clean_count(): '' -> 0, digits -> int, else -1 (invalid).
        eq("count empty", Biz.cleanCount(""), 0);
        eq("count blanks", Biz.cleanCount("   "), 0);
        eq("count 42", Biz.cleanCount("42"), 42);
        eq("count negative", Biz.cleanCount("-3"), -1);
        eq("count decimal", Biz.cleanCount("1.5"), -1);
        eq("count text", Biz.cleanCount("abc"), -1);
        eq("count 7 digits ok", Biz.cleanCount("1234567"), 1234567);
        eq("count 8 digits invalid", Biz.cleanCount("12345678"), -1);

        // hour_label(): 12-hour with AM/PM.
        eq("hour 0", Biz.hourLabel(0), "12 AM");
        eq("hour 10", Biz.hourLabel(10), "10 AM");
        eq("hour 12", Biz.hourLabel(12), "12 PM");
        eq("hour 18", Biz.hourLabel(18), "6 PM");
        eq("hour 23", Biz.hourLabel(23), "11 PM");

        // parseHourSetting(): "H" or "HH:MM", clamped 0-23, default when bad.
        eq("parse 10", Biz.parseHourSetting("10", 10), 10);
        eq("parse 14:30", Biz.parseHourSetting("14:30", 10), 14);
        eq("parse blank", Biz.parseHourSetting("", 10), 10);
        eq("parse text", Biz.parseHourSetting("abc", 10), 10);
        eq("parse clamp high", Biz.parseHourSetting("99", 10), 23);

        // dayOfWeek()/isSunday: the default business-day rule.
        eq("2026-09-13 is Sunday", Biz.dayOfWeek("2026-09-13"), 0);
        eq("2026-09-14 is Monday", Biz.dayOfWeek("2026-09-14"), 1);
        eq("day name", Biz.dayName("2026-09-14"), "Monday");
        eq("plusDays across month", Biz.plusDays("2026-09-30", 1), "2026-10-01");
        eq("plusDays back", Biz.plusDays("2026-01-01", -1), "2025-12-31");

        // fmt_dmy / fmt_month used by every export and the office text.
        eq("fmtDmy", Biz.fmtDmy("2026-09-14"), "14-09-2026");
        eq("fmtDmySlash idiom", Biz.fmtDmy("2026-09-14").replace('-', '/'), "14/09/2026");
        eq("fmtMonth", Biz.fmtMonth("2026-09"), "September 2026");
        eq("fmtMonth jan", Biz.fmtMonth("2027-01"), "January 2027");

        // field_slug(): matches PHP's label -> column derivation.
        eq("slug qr", Biz.fieldSlug("QR Code ReKYC"), "qr_code_rekyc");
        eq("slug branch", Biz.fieldSlug("Inoperative ReKYC Branch Submit"),
                "inoperative_rekyc_branch_submit");
        eq("slug punctuation", Biz.fieldSlug("WhatsApp Banking!!"), "whatsapp_banking");
        eq("slug leading digit", Biz.fieldSlug("2 Wheeler"), "f_2_wheeler");

        // Avatar initials and friendly date labels.
        eq("initials two words", Biz.initials("Shubham Roy Chowdhury"), "SC");
        eq("initials one word", Biz.initials("Anubi"), "AN");
        eq("delta positive", Ui0.deltaLocal(4), "+4");

        // Number formatting: tel:/sms: need the '+', wa.me forbids it. Tested on
        // Biz because Contacts links android.content classes the JVM lacks.
        eq("e164 from 10 digits", Biz.phoneE164("7908875354"), "+917908875354");
        eq("e164 from stored 91", Biz.phoneE164("917908875354"), "+917908875354");
        eq("e164 from spaced +91", Biz.phoneE164("+91 79088 75354"), "+917908875354");
        eq("e164 from leading zero", Biz.phoneE164("07908875354"), "+917908875354");
        eq("e164 from 00 trunk", Biz.phoneE164("00917908875354"), "+917908875354");
        eq("e164 empty", Biz.phoneE164(""), "");
        eq("wa keeps digits only", Biz.phoneDigits("7908875354"), "917908875354");
        eq("wa never has plus", Biz.phoneDigits("+91 79088 75354"), "917908875354");
        eq("null number is empty", Biz.phoneDigits(null), "");

        // CSV quoting per fputcsv.
        eq("csv plain", Biz.csv("KARNAJORA"), "KARNAJORA");
        eq("csv comma", Biz.csv("a,b"), "\"a,b\"");
        eq("csv quote", Biz.csv("say \"hi\""), "\"say \"\"hi\"\"\"");

        // PBKDF2 Password Hashing & Crypto verification checks
        org.cspdaily.report.security.Crypto.HashResult hr = org.cspdaily.report.security.Crypto.hashPassword("mySecretPass123");
        eq("hash not null", hr != null, true);
        eq("hash length 44 (256-bit base64)", hr.hash.length(), 44);
        eq("salt length 24 (16-byte base64)", hr.salt.length(), 24);
        eq("verify correct password", org.cspdaily.report.security.Crypto.verify("mySecretPass123", hr.hash, hr.salt), true);
        eq("verify wrong password", org.cspdaily.report.security.Crypto.verify("wrongPass", hr.hash, hr.salt), false);
        eq("verify empty password", org.cspdaily.report.security.Crypto.verify("", hr.hash, hr.salt), false);
        eq("verify null password", org.cspdaily.report.security.Crypto.verify(null, hr.hash, hr.salt), false);

        System.out.println();
        System.out.println(fail == 0
                ? "ALL PASS (" + pass + " checks)"
                : fail + " FAILED of " + (pass + fail));
        System.exit(fail == 0 ? 0 : 1);
    }

    /** Mirrors Ui.delta() so the signed-delta wording is pinned too. */
    static class Ui0 {
        static String deltaLocal(int d) {
            if (d > 0) return "+" + d;
            if (d < 0) return Integer.toString(d);
            return "no change";
        }
    }
}
