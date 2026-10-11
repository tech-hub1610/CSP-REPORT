package org.cspdaily.report.ui.screen;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.LinearLayout;
import java.util.Iterator;
import java.util.List;
import org.cspdaily.report.Core;
import org.cspdaily.report.R;
import org.cspdaily.report.data.Journal;
import org.cspdaily.report.ui.Screen;
import org.cspdaily.report.ui.Ui;
import org.cspdaily.report.ui.widget.Cards;
import org.cspdaily.report.ui.widget.Empty;
import org.cspdaily.report.ui.widget.Timeline;

/* loaded from: classes.dex */
public class ActivityScreen extends Screen {
    private final Empty empty;
    private final LinearLayout list;

    public ActivityScreen(Context context, Screen.Host host) {
        super(context, host);
        title("Activity");
        this.list = Cards.cardFlat(context);
        add(this.list, 0);
        this.empty = new Empty(context);
        add(this.empty, 0);
    }

    @Override // org.cspdaily.report.ui.Screen
    public String headline() {
        return "Activity";
    }

    @Override // org.cspdaily.report.ui.Screen
    public void refreshContent() {
        SQLiteDatabase dbRead = Core.get(getContext()).dbRead();
        Context context = getContext();
        this.list.removeAllViews();
        List<Journal.Entry> recent = Journal.recent(dbRead, 120, null);
        if (recent.isEmpty()) {
            this.empty.show(R.drawable.ic_history, R.color.line_2, "No activity yet", "Reports and follow-ups you log will be listed here.");
            Ui.gone(this.list, true);
            return;
        }
        Ui.gone(this.list, false);
        this.empty.hide();
        Iterator<Journal.Entry> it = recent.iterator();
        while (it.hasNext()) {
            this.list.addView(Timeline.row(context, it.next(), false), new LinearLayout.LayoutParams(-1, -2));
        }
    }
}
