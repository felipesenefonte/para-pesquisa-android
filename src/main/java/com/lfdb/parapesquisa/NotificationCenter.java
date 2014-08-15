package com.lfdb.parapesquisa;

import android.app.Activity;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSNotification;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.callbacks.NotificationCallback;
import com.lfdb.parapesquisa.storage.UPPSStorage;
import com.lfdb.parapesquisa.ui.FontTextView;
import com.lfdb.parapesquisa.ui.RoundIndicator;

import java.util.Calendar;

/**
 * Created by Igor on 8/4/13.
 */
public class NotificationCenter implements View.OnClickListener {
    static NotificationCenter sListener;
    static Activity sCurrentActivity;

    public static void init() {
        sListener = new NotificationCenter();
    }

    static void refreshBadge(Activity activity) {
        if(activity == null || activity.findViewById(R.id.notification_badge) == null) return;

        int newNotifications = 0;
        for(int i = 0; i < UPPSCache.getNotificationCount(); i++) {
            UPPSNotification notification = UPPSCache.getNotificationAt(i);
            if(!notification.read)
                newNotifications++;
        }

        RoundIndicator badge = (RoundIndicator)activity.findViewById(R.id.notification_badge);
        badge.setCount(newNotifications);
        badge.setVisibility(newNotifications > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    public static void postNotification(String title, String description) {
        UPPSNotification n = new UPPSNotification();
        n.title = title;
        n.description = description;
        n.date = Calendar.getInstance().getTime();
        n.read = false;

        UPPSCache.addNotification(n);
        sCurrentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshBadge(sCurrentActivity);
            }
        });
        UPPSServer.getActiveServer().postCallback(NotificationCallback.k_iCallback, new NotificationCallback());
    }

    public static void hook(Activity activity) {
        View button = activity.findViewById(R.id.notifications_button);
        RelativeLayout container = (RelativeLayout)activity.findViewById(R.id.activity_root);
        if(button == null || container == null) return;

        sCurrentActivity = activity;

        button.setOnClickListener(sListener);

        FrameLayout panel = new FrameLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.notification_panel, panel);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 60, 9, 0);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        panel.setLayoutParams(params);

        View badge = activity.findViewById(R.id.notification_badge);
        badge.setVisibility(View.INVISIBLE);

        View n_panel = panel.findViewById(R.id.notification_panel);
        n_panel.setVisibility(View.INVISIBLE);

        refreshBadge(sCurrentActivity);

        container.addView(panel);
    }

    static void fillNotifications(View view) {
        ViewGroup table;
        if(view.getId() == R.id.notification_panel_table)
            table = (ViewGroup)view;
        else
            table = (ViewGroup)view.findViewById(R.id.notification_panel_table);

        table.removeAllViews();

        Activity activity = (Activity)view.getContext();

        for(int i = 0; i < UPPSCache.getNotificationCount(); i++) {
            UPPSNotification notification = UPPSCache.getNotificationAt(i);

            FrameLayout row = new FrameLayout(view.getContext());
            activity.getLayoutInflater().inflate(R.layout.notification, row);

            FontTextView txtTitle = (FontTextView)row.findViewById(R.id.notification_title);
            FontTextView txtDescription = (FontTextView)row.findViewById(R.id.notification_desc);
            FontTextView txtTime = (FontTextView)row.findViewById(R.id.notification_time);

            txtTitle.setText(notification.title);
            txtTitle.setWeight(notification.read ? 400 : 700);

            txtTime.setWeight(notification.read ? 400 : 700);
            txtDescription.setText(notification.description);

            txtTime.setText(DateUtils.getRelativeTimeSpanString(notification.date.getTime()));

            table.addView(row);
        }
    }

    static void onOpenPanel(View panel) {
        fillNotifications(panel);

        for(int i = 0; i < UPPSCache.getNotificationCount(); i++) {
            UPPSNotification notification = UPPSCache.getNotificationAt(i);
            notification.read = true;
        }

        UPPSStorage.markNotificationsAsRead();

        refreshBadge(sCurrentActivity);
        UPPSServer.getActiveServer().postCallback(NotificationCallback.k_iCallback, new NotificationCallback());
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.notifications_button) {
            Activity activity = (Activity)view.getContext();
            View panel = activity.findViewById(R.id.notification_panel);
            if(panel.getVisibility() == View.INVISIBLE)
            {
                panel.setVisibility(View.VISIBLE);
                onOpenPanel(panel);
            }
            else
                panel.setVisibility(View.INVISIBLE);
        }
    }
}
