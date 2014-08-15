package com.lfdb.parapesquisa;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.text.Html;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSCallback;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.UPPSSubmission;
import com.lfdb.parapesquisa.api.UPPSUser;
import com.lfdb.parapesquisa.api.callbacks.LoginResultCallback;
import com.lfdb.parapesquisa.api.callbacks.SwapSubmissionsCallback;
import com.lfdb.parapesquisa.api.callbacks.SyncResultCallback;
import com.lfdb.parapesquisa.storage.UPPSStorage;
import com.lfdb.parapesquisa.ui.FontTextView;
import com.lfdb.parapesquisa.ui.SwapSubmissionsWindow;
import com.lfdb.parapesquisa.ui.Window;

import java.util.ArrayList;

/**
 * Created by Igor on 8/6/13.
 */
public class Util implements View.OnClickListener, UPPSCallback, View.OnTouchListener {
    static Util sInstance;
    public static final int ICON_LOGS = 1;
    public static final int ICON_ADD = 2;
    public static final int ICON_SEARCH = 4;
    public static final int ICON_REFRESH = 8;
    public static final int ICON_SWAP = 16;
    static boolean isSyncing = false;
    //public static final int ICON_NOTIFICATIONS;

    static Window mLogsWindow;
    static Window mSwapWindow;
    static int mSwapSource;
    static int mSwapDest;

    static View sSyncButton;
    static boolean isPresentingRelogin;

    static ViewGroup sReloginContainer;

    public static void init() {
        sInstance = new Util();
    }

    void onLoginResult(LoginResultCallback result) {
        ProgressBar progress = (ProgressBar)sReloginContainer.findViewById(R.id.relogin_progress);
        progress.setVisibility(View.GONE);

        if(result.result == EResult.OK) {
            UPPSCache.currentUser.id = result.response.user_id;
            UPPSCache.currentUserId = result.response.user_id; // Just in case

            UPPSStorage.setSession(UPPSServer.getActiveServer().getHost(), result.response.session_id, UPPSCache.currentUser.id);
            isPresentingRelogin = false;

            Util.hideOverlay((Activity)sReloginContainer.getContext());
            sReloginContainer = null;
        } else {
            View error = sReloginContainer.findViewById(R.id.relogin_error);
            error.setVisibility(View.VISIBLE);

            EditText txtPassword = (EditText)sReloginContainer.findViewById(R.id.relogin_password);
            View btnOk = sReloginContainer.findViewById(R.id.modal_buttoncontainer);

            txtPassword.setEnabled(true);
            btnOk.setVisibility(View.VISIBLE);

            txtPassword.requestFocus();
            showKeyboard(txtPassword);
        }
        UPPSServer.getActiveServer().unregisterCallback(this);
    }

    public static boolean isOverlayVisible(Activity activity) {
        View overlay = activity.findViewById(R.id.activity_overlay);
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onCallback(int iCallback, Object pParam) {
        if(iCallback == SyncResultCallback.k_iCallback) {
            final SyncResultCallback result = (SyncResultCallback)pParam;

            isSyncing = false;

            ((Activity)sSyncButton.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sSyncButton.clearAnimation();
                    if(result.result == EResult.Fail)
                        ((ImageView)sSyncButton).setImageDrawable(sSyncButton.getContext().getResources().getDrawable(R.drawable.header_bt_sync_verm));
                }
            });

            UPPSServer.getActiveServer().unregisterCallback(this);
        } else if(iCallback == LoginResultCallback.k_iCallback && isPresentingRelogin) {
            final LoginResultCallback result = (LoginResultCallback)pParam;
            ((Activity)sReloginContainer.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onLoginResult(result);
                }
            });
        } else if(iCallback == SwapSubmissionsCallback.k_iCallback) {
            beginSync((View)null);
            UPPSServer.getActiveServer().unregisterCallback(SwapSubmissionsCallback.k_iCallback, this);
        }
    }

    public static void beginSync(Activity view) {
        beginSync(findSyncButton(view));
    }

    public static View findSyncButton(Activity activity) {
        if(activity == null)
            return null;

        return  activity.findViewById(R.id.menubar_refresh);
    }

    public static void animateSyncButton(View view) {
        if(view != null) {
            RotateAnimation animation = new RotateAnimation(0, 360f, 27 * view.getContext().getResources().getDisplayMetrics().density, 30 * view.getContext().getResources().getDisplayMetrics().density);
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.INFINITE);
            animation.setDuration(1000);
            sSyncButton = view;

            view.startAnimation(animation);
            ((ImageView)sSyncButton).setImageDrawable(sSyncButton.getContext().getResources().getDrawable(R.drawable.header_bt_sync));
        }
    }

    public static void animateSyncButton(Activity activity) {
        animateSyncButton(findSyncButton(activity));
    }

    public static void stopAnimatingSyncButton(View view) {
        if(view != null) {
            view.clearAnimation();
        }
    }

    public static void stopAnimatingSyncButton(Activity activity) {
        stopAnimatingSyncButton(findSyncButton(activity));
    }

    public static void beginSync(View view) {
        if(UPPSCache.isSyncing)
            return;

        UPPSServer.getActiveServer().registerCallback(SyncResultCallback.k_iCallback, sInstance);
        isSyncing = true;

        animateSyncButton(view);
        UPPSCache.sync();
    }

    public static void showSyncModal(Activity activity) {
        Util.showOverlay(activity, R.layout.modal_sync);
    }

    public static void showSessionExpiredModal(Activity activity) {
        isPresentingRelogin = true;

        ViewGroup container = showOverlay(activity, R.layout.modal_relogin);
        sReloginContainer = container;

        TextView txt1 = (TextView)container.findViewById(R.id.relogin_text1);
        TextView txt2 = (TextView)container.findViewById(R.id.relogin_text2);
        EditText txtServer = (EditText)container.findViewById(R.id.relogin_server);
        EditText txtUsername = (EditText)container.findViewById(R.id.relogin_username);
        EditText txtPassword = (EditText)container.findViewById(R.id.relogin_password);
        View btnCancel = container.findViewById(R.id.modal_cancel);
        View btnOk = container.findViewById(R.id.modal_buttoncontainer);

        txt1.setText(Html.fromHtml(activity.getString(R.string.relogin_text1).replace("{name}", UPPSCache.currentUser.name)));
        txt2.setText(Html.fromHtml(activity.getString(R.string.relogin_text2)));

        txtServer.setText(UPPSServer.getActiveServer().getHost());
        txtUsername.setText(UPPSCache.currentUser.username);

        btnCancel.setOnClickListener(sInstance);
        btnOk.setOnClickListener(sInstance);

        hookTapEventGroup(container);

        txtPassword.requestFocus();
        showKeyboard(txtPassword);
    }

    public void onClick(View view) {
        if(view.getId() == R.id.modal_close || view.getId() == R.id.modal_cancel) {
            hideOverlay((Activity)view.getContext());
            if(isPresentingRelogin) {
                UPPSStorage.clear();
                UPPSCache.init();
                view.getContext().startActivity(new Intent(view.getContext(), LoginActivity.class));
                isPresentingRelogin = false;

                hideKeyboard(view);
            }
        } else if(view.getId() == R.id.modal_buttoncontainer && isPresentingRelogin) {
            ProgressBar progress = (ProgressBar)((Activity)view.getContext()).findViewById(R.id.relogin_progress);
            progress.setVisibility(View.VISIBLE);

            EditText txtPassword = (EditText)((Activity)view.getContext()).findViewById(R.id.relogin_password);
            txtPassword.setEnabled(false);
            txtPassword.clearFocus();
            hideKeyboard(txtPassword);

            View btnOk = view;
            btnOk.setVisibility(View.GONE);

            String username = UPPSCache.currentUser.username;
            String password = txtPassword.getText().toString();

            UPPSServer.getActiveServer().registerCallback(LoginResultCallback.k_iCallback, sInstance);
            UPPSServer.getActiveServer().tryLogin(username, password);
        } else if(view.getId() == R.id.logs_myforms) {
            LinearLayout layout = new LinearLayout(view.getContext());
            layout.setBackgroundColor(0xffe5e5e5);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(26, 26, 26, 16);

            for(int i = 0; i < UPPSCache.getFormCount(); i++) {
                UPPSForm form = UPPSCache.getFormAt(i);

                RelativeLayout row = new RelativeLayout(view.getContext());
                ((Activity)view.getContext()).getLayoutInflater().inflate(R.layout.modal_logs_form_row, row);

                TextView txtLabel = (TextView)row.findViewById(R.id.logs_row_label);
                txtLabel.setText(form.title);

                row.setId(R.id.logs_row);
                row.setOnClickListener(sInstance);
                row.setTag(R.id.logs_row_formid, form.id);

                layout.addView(row);
            }

            mLogsWindow.pushView(layout, view.getResources().getString(R.string.forms_to_me));
        } else if(view.getId() == R.id.logs_row) {
            UPPSForm form = UPPSCache.getForm((Integer)view.getTag(R.id.logs_row_formid));
            mLogsWindow.pushView(R.layout.modal_logs_form, view.getResources().getString(R.string.log) + ": " + form.title);
            fillFormInfo(mLogsWindow, form);
        } else if(view.getId() == R.id.menubar_refresh) {
            beginSync(view);
        } else if(view.getId() == R.id.menubar_swap) {
            mSwapWindow = new SwapSubmissionsWindow((Activity)view.getContext());
            mSwapWindow.show();
        }
    }

    static void fillFormInfo(Window logsWindow, UPPSForm form) {
        int quota = 0, remaining = 0, approved = 0, other = 0, users = 0;
        if(form == null) { // Global logs
            TextView txtForms = (TextView)logsWindow.findViewById(R.id.logs_formcount);

            txtForms.setText(Integer.toString(UPPSCache.getFormCount()));

            for(int i = 0; i < UPPSCache.getFormCount(); i++) {
                UPPSForm f = UPPSCache.getFormAt(i);
                quota += f.getQuota();
                remaining += f.getRemainingCount();
                approved += f.getStateCount(UPPSSubmission.State.Approved);
                other += f.getStateCount(UPPSSubmission.State.Rejected) + f.getStateCount(UPPSSubmission.State.WaitingForApproval) + f.getStateCount(UPPSSubmission.State.NotSent) + f.getStateCount(UPPSSubmission.State.Rescheduled);
                users += f.users.size();
            }
        } else {
            TextView txtDays = (TextView)logsWindow.findViewById(R.id.logs_days);

            txtDays.setText(Integer.toString(form.getRemainingDays()));

            users = form.users.size();
            quota = form.getQuota();
            remaining = form.getRemainingCount();
            approved = form.getStateCount(UPPSSubmission.State.Approved);
            other = form.getStateCount(UPPSSubmission.State.Rejected) + form.getStateCount(UPPSSubmission.State.WaitingForApproval) + form.getStateCount(UPPSSubmission.State.NotSent) + form.getStateCount(UPPSSubmission.State.Rescheduled);
        }

        TextView txtUserText = (TextView)logsWindow.findViewById(R.id.logs_text_users);
        if(users < 2)
            txtUserText.setText(txtUserText.getResources().getString(R.string.log_users_sing));

        TextView txtUser = (TextView)logsWindow.findViewById(R.id.logs_usercount);
        TextView txtQuota = (TextView)logsWindow.findViewById(R.id.logs_quota);
        TextView txtRemaining = (TextView)logsWindow.findViewById(R.id.logs_remaining);
        TextView txtApproved = (TextView)logsWindow.findViewById(R.id.logs_approved);
        TextView txtOther = (TextView)logsWindow.findViewById(R.id.logs_other);

        txtQuota.setText(Integer.toString(quota));
        txtRemaining.setText(Integer.toString(remaining));
        txtApproved.setText(Integer.toString(approved));
        txtOther.setText(Integer.toString(other));
        txtUser.setText(Integer.toString(users));
    }

    public static Window showLogsWindow(Activity activity) {
        mLogsWindow = new Window(activity, R.drawable.relatorio_icon1);

        mLogsWindow.pushView(R.layout.modal_logs, activity.getResources().getString(R.string.general_log));
        mLogsWindow.findViewById(R.id.logs_myforms).setOnClickListener(sInstance);
        fillFormInfo(mLogsWindow, null);

        mLogsWindow.show();
        return mLogsWindow;
    }

    public static Window showLocalLogsWindow(Activity activity, UPPSForm form) {
        mLogsWindow = showLogsWindow(activity);
        mLogsWindow.pushView(R.layout.modal_logs_form, activity.getResources().getString(R.string.log) + ": " + form.title);
        fillFormInfo(mLogsWindow, form);

        return mLogsWindow;
    }

    public static void setMenuBarVisibleIcons(Activity activity, int icons) {
        View btnLogs = activity.findViewById(R.id.menubar_logs);
        View btnAdd = activity.findViewById(R.id.menubar_add);
        View btnSearch = activity.findViewById(R.id.menubar_search);
        View btnRefresh = activity.findViewById(R.id.menubar_refresh);
        View btnSwap = activity.findViewById(R.id.menubar_swap);

        btnLogs.setVisibility((icons & ICON_LOGS) == ICON_LOGS ? View.VISIBLE : View.GONE);
        btnAdd.setVisibility((icons & ICON_ADD) == ICON_ADD ? View.VISIBLE : View.GONE);
        btnSearch.setVisibility((icons & ICON_SEARCH) == ICON_SEARCH ? View.VISIBLE : View.GONE);
        btnRefresh.setVisibility((icons & ICON_REFRESH) == ICON_REFRESH ? View.VISIBLE : View.GONE);
        btnSwap.setVisibility((icons & ICON_SWAP) == ICON_SWAP ? View.VISIBLE : View.GONE);
    }

    static void initMenuBar(Activity activity) {
        if(UPPSCache.currentUser == null)
            return;

        View btnLogs = activity.findViewById(R.id.menubar_logs);
        View btnAdd = activity.findViewById(R.id.menubar_add);
        View btnSync = activity.findViewById(R.id.menubar_refresh);
        View btnSwap = activity.findViewById(R.id.menubar_swap);

        if(btnLogs != null) {
            btnLogs.setOnClickListener(sInstance);
            btnLogs.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.VISIBLE : View.GONE);
        }
        if(btnAdd != null) {
            btnAdd.setOnClickListener(sInstance);
            btnAdd.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.GONE : View.VISIBLE);
        }
        if(btnSync != null) {
            btnSync.setOnClickListener(sInstance);
        }
        if(btnSwap != null) {
            btnSwap.setOnClickListener(sInstance);
        }
    }

    static void initOverlay(Activity activity) {
        if(activity.findViewById(R.id.activity_root) == null) return;

        FrameLayout activity_overlay = new FrameLayout(activity);
        activity_overlay.setId(R.id.activity_overlay);
        activity_overlay.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        activity_overlay.setBackgroundColor(0xff000000);
        activity_overlay.setVisibility(View.INVISIBLE);
        activity_overlay.setClickable(true);

        FrameLayout activity_overlaycontainer = new FrameLayout(activity);
        activity_overlaycontainer.setId(R.id.activity_overlaycontainer);
        activity_overlaycontainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        activity_overlaycontainer.setVisibility(View.INVISIBLE);

        FrameLayout activity_overlaycontent = new FrameLayout(activity);
        activity_overlaycontent.setId(R.id.activity_overlaycontent);
        activity_overlaycontent.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        activity_overlaycontainer.addView(activity_overlaycontent);

        ViewGroup root = (ViewGroup)activity.findViewById(R.id.activity_root);
        root.addView(activity_overlay);
        root.addView(activity_overlaycontainer);
    }

    public static void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Service.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(activity.findViewById(android.R.id.content).getWindowToken(), 0);
    }

    static ArrayList<View> getChildrenTraverse(ViewGroup root) {
        ArrayList<View> result = new ArrayList<View>();
        for(int i = 0; i < root.getChildCount(); i++) {
            View view = root.getChildAt(i);
            result.add(view);
            if(view instanceof ViewGroup)
                result.addAll(getChildrenTraverse((ViewGroup)view));
        }
        return result;
    }

    public static void initActivity(Activity activity) {
        NotificationCenter.hook(activity);
        initMenuBar(activity);
        initOverlay(activity);

        ViewGroup root = (ViewGroup)activity.getWindow().findViewById(android.R.id.content);
        hookTapEventGroup(root);
    }

    public static void hookTapEventGroup(ViewGroup root) {
        ArrayList<View> allViews = getChildrenTraverse(root);
        for(View view : allViews) {
            if(Build.VERSION.SDK_INT >= 15) {
                if(!(view.hasOnClickListeners()))
                    continue;
            } else {
                if(!view.isClickable())
                    continue;
            }

            view.setOnTouchListener(sInstance);
        }
    }

    public static void hookTapEvent(View view) {
        view.setOnTouchListener(sInstance);
    }

    public static ViewGroup showOverlay(Activity activity) {
        View overlay = activity.findViewById(R.id.activity_overlay);
        overlay.setVisibility(View.VISIBLE);

        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0F, 0.5F);
        alpha.setDuration(500); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        overlay.startAnimation(alpha);

        View overlayContainer = activity.findViewById(R.id.activity_overlaycontainer);
        overlayContainer.setVisibility(View.VISIBLE);

        ViewGroup overlayContent = (ViewGroup)activity.findViewById(R.id.activity_overlaycontent);

        hookTapEventGroup((ViewGroup)overlayContainer);

        return overlayContent;
    }

    public static ViewGroup showOverlay(Activity activity, int contentLayout) {
        View overlay = activity.findViewById(R.id.activity_overlay);
        overlay.setVisibility(View.VISIBLE);

        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0F, 0.5F);
        alpha.setDuration(500); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        overlay.startAnimation(alpha);

        View overlayContainer = activity.findViewById(R.id.activity_overlaycontainer);
        overlayContainer.setVisibility(View.VISIBLE);

        ViewGroup overlayContent = (ViewGroup)activity.findViewById(R.id.activity_overlaycontent);
        overlayContent.removeAllViews();
        activity.getLayoutInflater().inflate(contentLayout, overlayContent);

        hookTapEventGroup((ViewGroup)overlayContainer);

        return overlayContent;
    }

    public static void hideOverlay(Activity activity) {
        View overlay = activity.findViewById(R.id.activity_overlay);
        overlay.setVisibility(View.INVISIBLE);

        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0.5F, 0F);
        alpha.setDuration(500); // Make animation instant
        alpha.setFillAfter(false); // Tell it to persist after the animation ends
        overlay.startAnimation(alpha);

        View overlayContainer = activity.findViewById(R.id.activity_overlaycontainer);
        overlayContainer.setVisibility(View.INVISIBLE);
    }

    public static ViewGroup showDarkModalWindow(Activity activity, String title, int layout) {
        ViewGroup container = showDarkModalWindow(activity, title);
        activity.getLayoutInflater().inflate(layout, container);

        return container;
    }

    public static ViewGroup showDarkModalWindow(Activity activity, String title) {
        View modal = showOverlay(activity, R.layout.modal_window_dark);

        TextView txtTitle = (TextView)modal.findViewById(R.id.modal_title);
        ViewGroup container = (ViewGroup)modal.findViewById(R.id.modal_content);
        View btnClose = modal.findViewById(R.id.modal_close);

        btnClose.setOnClickListener(sInstance);
        txtTitle.setText(title);

        hookTapEventGroup((ViewGroup)modal);

        return container;
    }

    public static TextView showDarkModal(Activity activity, String title, String button, View.OnClickListener buttonListener) {
        View modal = showOverlay(activity, R.layout.modal_dark);

        TextView txtTitle = (TextView)modal.findViewById(R.id.modal_title);
        TextView txtButton = (TextView)modal.findViewById(R.id.modal_button);
        TextView txtText = (TextView)modal.findViewById(R.id.modal_text);
        View btnContainer = modal.findViewById(R.id.modal_buttoncontainer);

        txtTitle.setText(title);
        txtButton.setText(button);
        btnContainer.setOnClickListener(buttonListener);

        hookTapEventGroup((ViewGroup)modal);

        return txtText;
    }

    public static TextView showDarkOkCancelModal(Activity activity, String title, String button, View.OnClickListener buttonListener) {
        View modal = showOverlay(activity, R.layout.modal_dark_okcancel);

        TextView txtTitle = (TextView)modal.findViewById(R.id.modal_title);
        TextView txtButton = (TextView)modal.findViewById(R.id.modal_button);
        TextView txtText = (TextView)modal.findViewById(R.id.modal_text);
        View btnContainer = modal.findViewById(R.id.modal_buttoncontainer);
        View btnCancel = modal.findViewById(R.id.modal_cancel);

        txtTitle.setText(title);
        txtButton.setText(button);
        btnContainer.setOnClickListener(buttonListener);
        btnCancel.setOnClickListener(sInstance);

        hookTapEventGroup((ViewGroup)modal);

        return txtText;
    }

    public static ViewGroup showDarkListModal(Activity activity, String title) {
        View modal = showOverlay(activity, R.layout.modal_dark_list);

        TextView txtTitle = (TextView)modal.findViewById(R.id.modal_title);
        View btnClose = modal.findViewById(R.id.modal_close);
        btnClose.setOnClickListener(sInstance);

        txtTitle.setText(title);

        hookTapEventGroup((ViewGroup)modal);

        return (ViewGroup)modal.findViewById(R.id.modal_content);
    }

    public static ViewGroup showModal(Activity activity, String title, String button, View.OnClickListener buttonListener) {
        View modal = showOverlay(activity, R.layout.modal);

        TextView txtTitle = (TextView)modal.findViewById(R.id.modal_title);
        View scrollView = modal.findViewById(R.id.modal_content);
        TextView txtButton = (TextView)modal.findViewById(R.id.modal_button);
        View btnContainer = modal.findViewById(R.id.modal_button_container);

        txtTitle.setText(title);
        txtButton.setText(button);

        btnContainer.setClickable(true);
        btnContainer.setOnClickListener(buttonListener);

        hookTapEventGroup((ViewGroup)modal);

        return (ViewGroup)scrollView;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            View mask = view;
            float from = 1.0f;
            float to = .5f;
            if(view instanceof ViewGroup && ((ViewGroup)view).findViewById(R.id.click_mask) != null) {
                mask = ((ViewGroup)view).findViewById(R.id.click_mask);
                from = 0;
                to = .5f;
                mask.setVisibility(View.VISIBLE);
            }

            AlphaAnimation anim = new AlphaAnimation(from, to);
            anim.setDuration(100);
            anim.setFillAfter(true);

            mask.startAnimation(anim);
        } else if(motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            View mask = view;
            float from = .5f;
            float to = 1.0f;
            if(view instanceof ViewGroup && ((ViewGroup)view).findViewById(R.id.click_mask) != null) {
                mask = ((ViewGroup)view).findViewById(R.id.click_mask);
                from = .5f;
                to = 0;
                mask.setVisibility(View.GONE);
            }

            AlphaAnimation anim = new AlphaAnimation(from, to);
            anim.setDuration(100);

            mask.startAnimation(anim);
        }
        return false;
    }
}
