package org.cspdaily.report.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* loaded from: classes.dex */
public class ReminderReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            Reminders.schedule(context);
        } else if (action != null) {
            Reminders.fire(context, action);
        }
    }
}
