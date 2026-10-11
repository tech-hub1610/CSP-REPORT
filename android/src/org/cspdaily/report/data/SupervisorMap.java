package org.cspdaily.report.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.cspdaily.report.domain.Biz;

public final class SupervisorMap {

    private SupervisorMap() {}

    public static class SupervisorStats {
        public User supervisor;
        public int totalCsps;
        public int reportedCsps;
        public int pendingCsps;
        public int nilCsps;
        public int totalCalls;
        public int totalBusiness;
        public int totalTarget;
        public Map<String, Integer> paramTotals = new LinkedHashMap<>();
        public Map<String, Integer> paramTargets = new LinkedHashMap<>();
        public List<String> branches = new ArrayList<>();
    }

    public static class SupervisorFollowupItem {
        public long id;
        public long cspId;
        public String cspCode;
        public String cspName;
        public String branchName;
        public String mobileNumber;
        public String businessDate;
        public String actionStatus;
        public String note;
        public String createdAt;
        public long supervisorId;
        public String supervisorName;
        public String supervisorUsername;

        public boolean isCall() {
            String s = ((actionStatus != null ? actionStatus : "") + " " + (note != null ? note : "")).toLowerCase(Locale.US);
            return s.contains("call") || s.contains("talking time") || s.contains("dial") || s.contains("answered");
        }

        public boolean isWhatsApp() {
            String s = ((actionStatus != null ? actionStatus : "") + " " + (note != null ? note : "")).toLowerCase(Locale.US);
            return s.contains("whatsapp");
        }

        public String formattedTime() {
            return Biz.shortWhen(createdAt);
        }
    }

    public static class SupervisorReportItem {
        public long id;
        public long cspId;
        public String cspCode;
        public String cspName;
        public String branchName;
        public String mobileNumber;
        public String businessDate;
        public String submittedAt;
        public String remarks;
        public int totalBusiness;
        public long supervisorId;
        public String supervisorName;
        public String supervisorUsername;
        public Map<String, Integer> paramValues = new LinkedHashMap<>();

        public boolean isNil() {
            return totalBusiness <= 0;
        }

        public String formattedTime() {
            return Biz.shortWhen(submittedAt);
        }
    }

    public static class SupervisorPendingItem {
        public long cspId;
        public String cspCode;
        public String cspName;
        public String branchName;
        public String mobileNumber;
        public String pendingReason;
        public int pendingDays;
        public long supervisorId;
        public String supervisorName;
        public String supervisorUsername;
        public String followupStatus;
        public String latestNote;
    }

    public static class Instruction {
        public long id;
        public String businessDate;
        public String senderName;
        public String title;
        public String message;
        public String priority;
        public String createdAt;
    }

    public static Set<Long> getCspIdsForSupervisor(SQLiteDatabase db, long supervisorId) {
        Set<Long> set = new HashSet<>();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT csp_id FROM supervisor_csp_map WHERE supervisor_id = ?", new String[]{String.valueOf(supervisorId)});
            while (c != null && c.moveToNext()) {
                set.add(c.getLong(0));
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return set;
    }

    public static long getSupervisorIdForCsp(SQLiteDatabase db, long cspId) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT supervisor_id FROM supervisor_csp_map WHERE csp_id = ? LIMIT 1", new String[]{String.valueOf(cspId)});
            if (c != null && c.moveToFirst()) {
                return c.getLong(0);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return 0;
    }

    public static void mapCspToSupervisor(SQLiteDatabase db, long supervisorId, long cspId) {
        try {
            db.delete("supervisor_csp_map", "csp_id = ?", new String[]{String.valueOf(cspId)});
            if (supervisorId > 0) {
                ContentValues cv = new ContentValues();
                cv.put("supervisor_id", supervisorId);
                cv.put("csp_id", cspId);
                cv.put("assigned_date", Biz.today());
                db.insertWithOnConflict("supervisor_csp_map", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Throwable ignored) {}
    }

    public static void mapBranchToSupervisor(SQLiteDatabase db, long supervisorId, String branchName) {
        if (branchName == null || branchName.isEmpty()) return;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT id FROM csp WHERE LOWER(branch_name) = LOWER(?)", new String[]{branchName.trim()});
            while (c != null && c.moveToNext()) {
                long cspId = c.getLong(0);
                mapCspToSupervisor(db, supervisorId, cspId);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
    }

    public static void setSupervisorCspBatch(SQLiteDatabase db, long supervisorId, List<Long> cspIds) {
        try {
            db.beginTransaction();
            db.delete("supervisor_csp_map", "supervisor_id = ?", new String[]{String.valueOf(supervisorId)});
            for (Long cspId : cspIds) {
                if (cspId != null && cspId > 0) {
                    ContentValues cv = new ContentValues();
                    cv.put("supervisor_id", supervisorId);
                    cv.put("csp_id", cspId);
                    cv.put("assigned_date", Biz.today());
                    db.insertWithOnConflict("supervisor_csp_map", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable ignored) {
        } finally {
            try { db.endTransaction(); } catch (Throwable ignored) {}
        }
    }

    public static List<SupervisorStats> getAllSupervisorsStats(SQLiteDatabase db, String date) {
        List<SupervisorStats> list = new ArrayList<>();
        List<User> supervisors = User.supervisors(db);
        for (User s : supervisors) {
            list.add(getStatsForSupervisor(db, s, date));
        }
        return list;
    }

    public static SupervisorStats getStatsForSupervisor(SQLiteDatabase db, User supervisor, String date) {
        SupervisorStats stats = new SupervisorStats();
        stats.supervisor = supervisor;
        Set<Long> cspIds = getCspIdsForSupervisor(db, supervisor.id);
        stats.totalCsps = cspIds.size();

        if (cspIds.isEmpty()) {
            return stats;
        }

        StringBuilder inClause = new StringBuilder("(");
        int idx = 0;
        for (Long id : cspIds) {
            if (idx > 0) inClause.append(",");
            inClause.append(id);
            idx++;
        }
        inClause.append(")");

        Cursor c = null;
        try {
            // Count reported and nil figures
            String sumExpr = Params.sumSql(db, "r");
            c = db.rawQuery("SELECT count(DISTINCT r.csp_id), COALESCE(SUM(" + sumExpr + "), 0), " +
                    "COUNT(CASE WHEN (" + sumExpr + ") <= 0 THEN 1 END) " +
                    "FROM daily_report r WHERE r.business_date = ? AND r.csp_id IN " + inClause, new String[]{date});
            if (c != null && c.moveToFirst()) {
                stats.reportedCsps = c.getInt(0);
                stats.totalBusiness = c.getInt(1);
                stats.nilCsps = c.getInt(2);
            }
            if (c != null) c.close();

            stats.pendingCsps = Math.max(0, stats.totalCsps - stats.reportedCsps);

            // Follow-up calls
            c = db.rawQuery("SELECT count(*) FROM followup_log WHERE business_date = ? AND csp_id IN " + inClause, new String[]{date});
            if (c != null && c.moveToFirst()) {
                stats.totalCalls = c.getInt(0);
            }
            if (c != null) c.close();

            // Total target and parameter targets
            c = db.rawQuery("SELECT field_key, SUM(target_value) FROM daily_csp_target WHERE business_date = ? AND csp_id IN " + inClause + " GROUP BY field_key", new String[]{date});
            while (c != null && c.moveToNext()) {
                String k = c.getString(0);
                int val = c.getInt(1);
                stats.paramTargets.put(k, val);
                stats.totalTarget += val;
            }
            if (c != null) c.close();

            // Parameter actual totals
            List<Params.P> paramList = Params.all(db);
            if (!paramList.isEmpty()) {
                StringBuilder selectCols = new StringBuilder();
                for (int i = 0; i < paramList.size(); i++) {
                    if (i > 0) selectCols.append(", ");
                    selectCols.append("COALESCE(SUM(r.").append(paramList.get(i).key).append("), 0)");
                }
                c = db.rawQuery("SELECT " + selectCols + " FROM daily_report r WHERE r.business_date = ? AND r.csp_id IN " + inClause, new String[]{date});
                if (c != null && c.moveToFirst()) {
                    for (int i = 0; i < paramList.size(); i++) {
                        stats.paramTotals.put(paramList.get(i).key, c.getInt(i));
                    }
                }
                if (c != null) c.close();
            }

            // Branches
            c = db.rawQuery("SELECT DISTINCT branch_name FROM csp WHERE id IN " + inClause + " AND branch_name != '' ORDER BY branch_name ASC", null);
            while (c != null && c.moveToNext()) {
                String b = c.getString(0);
                if (b != null && !b.isEmpty()) stats.branches.add(b);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }

        return stats;
    }

    public static List<SupervisorFollowupItem> getFollowups(SQLiteDatabase db, String date, Long supervisorId, String typeFilter) {
        List<SupervisorFollowupItem> list = new ArrayList<>();
        Cursor c = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT f.id, f.csp_id, c.csp_code, c.csp_name, c.branch_name, c.mobile_number, ")
              .append("f.business_date, f.action_status, f.note, f.created_at, ")
              .append("COALESCE(u.id, 0) AS supervisor_id, COALESCE(u.name, 'Unassigned') AS supervisor_name, COALESCE(u.username, '') AS supervisor_username ")
              .append("FROM followup_log f ")
              .append("JOIN csp c ON c.id = f.csp_id ")
              .append("LEFT JOIN supervisor_csp_map m ON m.csp_id = c.id ")
              .append("LEFT JOIN app_user u ON u.id = m.supervisor_id ")
              .append("WHERE f.business_date = ? ");

            List<String> args = new ArrayList<>();
            args.add(date);

            if (supervisorId != null && supervisorId > 0) {
                sb.append("AND m.supervisor_id = ? ");
                args.add(String.valueOf(supervisorId));
            }

            sb.append("ORDER BY f.id DESC LIMIT 150");

            c = db.rawQuery(sb.toString(), args.toArray(new String[0]));
            while (c != null && c.moveToNext()) {
                SupervisorFollowupItem item = new SupervisorFollowupItem();
                item.id = c.getLong(0);
                item.cspId = c.getLong(1);
                item.cspCode = c.getString(2);
                item.cspName = c.getString(3);
                item.branchName = c.getString(4);
                item.mobileNumber = c.getString(5);
                item.businessDate = c.getString(6);
                item.actionStatus = c.getString(7);
                item.note = c.getString(8);
                item.createdAt = c.getString(9);
                item.supervisorId = c.getLong(10);
                item.supervisorName = c.getString(11);
                item.supervisorUsername = c.getString(12);

                if ("CALLS".equalsIgnoreCase(typeFilter) && !item.isCall()) continue;
                if ("WHATSAPP".equalsIgnoreCase(typeFilter) && !item.isWhatsApp()) continue;
                if ("NOTES".equalsIgnoreCase(typeFilter) && (item.isCall() || item.isWhatsApp())) continue;

                list.add(item);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return list;
    }

    public static List<SupervisorReportItem> getReports(SQLiteDatabase db, String date, Long supervisorId, String typeFilter) {
        List<SupervisorReportItem> list = new ArrayList<>();
        Cursor c = null;
        try {
            List<Params.P> paramList = Params.all(db);
            StringBuilder paramCols = new StringBuilder();
            for (Params.P p : paramList) {
                paramCols.append(", COALESCE(r.").append(p.key).append(", 0)");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("SELECT r.id, r.csp_id, c.csp_code, c.csp_name, c.branch_name, c.mobile_number, ")
              .append("r.business_date, r.submitted_at, r.remarks, (").append(Params.sumSql(db, "r")).append(") AS total_business, ")
              .append("COALESCE(u.id, 0) AS supervisor_id, COALESCE(u.name, 'Unassigned') AS supervisor_name, COALESCE(u.username, '') AS supervisor_username ")
              .append(paramCols)
              .append(" FROM daily_report r ")
              .append("JOIN csp c ON c.id = r.csp_id ")
              .append("LEFT JOIN supervisor_csp_map m ON m.csp_id = c.id ")
              .append("LEFT JOIN app_user u ON u.id = m.supervisor_id ")
              .append("WHERE r.business_date = ? ");

            List<String> args = new ArrayList<>();
            args.add(date);

            if (supervisorId != null && supervisorId > 0) {
                sb.append("AND m.supervisor_id = ? ");
                args.add(String.valueOf(supervisorId));
            }

            sb.append("ORDER BY r.submitted_at DESC, r.id DESC LIMIT 150");

            c = db.rawQuery(sb.toString(), args.toArray(new String[0]));
            while (c != null && c.moveToNext()) {
                SupervisorReportItem item = new SupervisorReportItem();
                item.id = c.getLong(0);
                item.cspId = c.getLong(1);
                item.cspCode = c.getString(2);
                item.cspName = c.getString(3);
                item.branchName = c.getString(4);
                item.mobileNumber = c.getString(5);
                item.businessDate = c.getString(6);
                item.submittedAt = c.getString(7);
                item.remarks = c.getString(8);
                item.totalBusiness = c.getInt(9);
                item.supervisorId = c.getLong(10);
                item.supervisorName = c.getString(11);
                item.supervisorUsername = c.getString(12);

                int colOffset = 13;
                for (Params.P p : paramList) {
                    item.paramValues.put(p.key, c.getInt(colOffset++));
                }

                if ("NIL".equalsIgnoreCase(typeFilter) && !item.isNil()) continue;
                if ("BUSINESS".equalsIgnoreCase(typeFilter) && item.isNil()) continue;

                list.add(item);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return list;
    }

    public static List<SupervisorPendingItem> getPendingCsps(SQLiteDatabase db, String date, Long supervisorId) {
        List<SupervisorPendingItem> list = new ArrayList<>();
        Cursor c = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT c.id, c.csp_code, c.csp_name, c.branch_name, c.mobile_number, ")
              .append("COALESCE(pr.reason, '') AS pending_reason, ")
              .append("COALESCE(u.id, 0) AS supervisor_id, COALESCE(u.name, 'Unassigned') AS supervisor_name, COALESCE(u.username, '') AS supervisor_username, ")
              .append("(SELECT action_status FROM followup_log WHERE csp_id = c.id AND business_date = ? ORDER BY id DESC LIMIT 1) AS followup_status, ")
              .append("(SELECT note FROM followup_log WHERE csp_id = c.id AND business_date = ? ORDER BY id DESC LIMIT 1) AS latest_note ")
              .append("FROM csp c ")
              .append("LEFT JOIN daily_report r ON r.csp_id = c.id AND r.business_date = ? ")
              .append("LEFT JOIN pending_reason pr ON pr.csp_id = c.id AND pr.business_date = ? ")
              .append("LEFT JOIN supervisor_csp_map m ON m.csp_id = c.id ")
              .append("LEFT JOIN app_user u ON u.id = m.supervisor_id ")
              .append("WHERE r.id IS NULL AND c.status = 'ACTIVE' ");

            List<String> args = new ArrayList<>();
            args.add(date);
            args.add(date);
            args.add(date);
            args.add(date);

            if (supervisorId != null && supervisorId > 0) {
                sb.append("AND m.supervisor_id = ? ");
                args.add(String.valueOf(supervisorId));
            }

            sb.append("ORDER BY c.branch_name ASC, c.csp_name ASC");

            c = db.rawQuery(sb.toString(), args.toArray(new String[0]));
            while (c != null && c.moveToNext()) {
                SupervisorPendingItem item = new SupervisorPendingItem();
                item.cspId = c.getLong(0);
                item.cspCode = c.getString(1);
                item.cspName = c.getString(2);
                item.branchName = c.getString(3);
                item.mobileNumber = c.getString(4);
                item.pendingReason = c.getString(5);
                item.supervisorId = c.getLong(6);
                item.supervisorName = c.getString(7);
                item.supervisorUsername = c.getString(8);
                item.followupStatus = c.getString(9);
                item.latestNote = c.getString(10);
                item.pendingDays = 1;
                Cursor c2 = null;
                try {
                    c2 = db.rawQuery("SELECT max(business_date) FROM daily_report WHERE csp_id = ? AND business_date < ?", new String[]{String.valueOf(item.cspId), date});
                    if (c2 != null && c2.moveToFirst()) {
                        String lastDate = c2.getString(0);
                        if (lastDate != null && !lastDate.isEmpty()) {
                            long diff = (Biz.millisOf(date) - Biz.millisOf(lastDate)) / 86400000L;
                            item.pendingDays = (int) Math.max(1, diff);
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (c2 != null) try { c2.close(); } catch (Throwable ignored) {}
                }
                list.add(item);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return list;
    }

    public static List<Instruction> getInstructions(SQLiteDatabase db, String date) {
        List<Instruction> list = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM admin_instruction WHERE business_date = ? ORDER BY id DESC", new String[]{date});
            while (c != null && c.moveToNext()) {
                Instruction inst = new Instruction();
                inst.id = c.getLong(c.getColumnIndex("id"));
                inst.businessDate = c.getString(c.getColumnIndex("business_date"));
                inst.senderName = c.getString(c.getColumnIndex("sender_name"));
                inst.title = c.getString(c.getColumnIndex("title"));
                inst.message = c.getString(c.getColumnIndex("message"));
                inst.priority = c.getString(c.getColumnIndex("priority"));
                inst.createdAt = c.getString(c.getColumnIndex("created_at"));
                list.add(inst);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return list;
    }

    public static long addInstruction(SQLiteDatabase db, String date, String senderName, String title, String message, String priority) {
        ContentValues cv = new ContentValues();
        cv.put("business_date", date);
        cv.put("sender_name", senderName);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("priority", priority == null ? "NORMAL" : priority);
        cv.put("created_at", Biz.now());
        return db.insert("admin_instruction", null, cv);
    }

    public static boolean deleteInstruction(SQLiteDatabase db, long id) {
        return db.delete("admin_instruction", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }
}
