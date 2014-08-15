package com.lfdb.parapesquisa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSCallback;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.UPPSSubmission;
import com.lfdb.parapesquisa.api.UPPSUser;
import com.lfdb.parapesquisa.api.callbacks.AccessDeniedCallback;
import com.lfdb.parapesquisa.api.callbacks.NotificationCallback;
import com.lfdb.parapesquisa.api.callbacks.SyncResultCallback;
import com.lfdb.parapesquisa.api.callbacks.SyncStartedCallback;
import com.lfdb.parapesquisa.storage.UPPSStorage;
import com.lfdb.parapesquisa.ui.RoundIndicator;
import com.lfdb.parapesquisa.ui.Tab;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Created by Igor on 7/30/13.
 */
public class FormActivity extends Activity implements View.OnClickListener, TextWatcher, UPPSCallback {
    class UserComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer uppsUser, Integer uppsUser2) {
            UPPSUser user1 = UPPSCache.getUser(uppsUser);
            UPPSUser user2 = UPPSCache.getUser(uppsUser2);

            if(user1 == null || user2 == null)
                return 0;

            return user1.name.compareTo(user2.name);
        }
    }

    Hashtable<Integer, View> submissionViewCache = new Hashtable<Integer, View>();
    UPPSForm mForm;
    int mUserId;
    int mPageIndex = 0;
    static int ITEMS_PER_PAGE = 20;
    String mSearchQuery;
    String mMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_form);

        Util.initActivity(this);
        GuidedTour.init(this);

        int formId = getIntent().getIntExtra("id", -1);
        if(formId == -1) {
            finish();
            return;
        }

        mForm = UPPSCache.getForm(formId);

        if(mForm == null || UPPSCache.currentUser == null) {
            finish();
            return;
        }

        int iconFlags = Util.ICON_REFRESH | Util.ICON_SEARCH;
        if(UPPSCache.currentUser.isCoordinator() && mForm.allowSubmissionTransfer) iconFlags |= Util.ICON_SWAP;
        Util.setMenuBarVisibleIcons(this, iconFlags);

        View iconSearch = findViewById(R.id.menubar_search);
        View searchCancel = findViewById(R.id.menu_search_cancel);
        View searchClear = findViewById(R.id.menu_search_clear);
        searchClear.setOnClickListener(this);
        searchCancel.setOnClickListener(this);
        iconSearch.setOnClickListener(this);
        android.widget.EditText txtSearch = (android.widget.EditText)findViewById(R.id.menu_search_text);
        txtSearch.addTextChangedListener(this);

        if(Build.VERSION.SDK_INT >= 11) this.getActionBar().hide();


        init();

        mMode = "all";

        selectFirstTab();
        fillSubmissions(mMode);
        refreshTabs();

        UPPSServer.getActiveServer().registerCallback(SyncStartedCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(SyncResultCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(NotificationCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(AccessDeniedCallback.k_iCallback, this);

        if(GuidedTour.isRunning)
            GuidedTour.restore(this);
    }

    void init() {
        View headerUser = findViewById(R.id.form_header_user);
        View headerCoordinator = findViewById(R.id.form_header_coordinator);
        View tabsUser = findViewById(R.id.form_tabs_user);
        View tabsCoordinator = findViewById(R.id.form_tabs_coordinator);

        tabsUser.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.GONE : View.VISIBLE);
        headerUser.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.GONE : View.VISIBLE);
        tabsCoordinator.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.VISIBLE : View.GONE);
        headerCoordinator.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.VISIBLE : View.GONE);

        ViewGroup header = (ViewGroup)(UPPSCache.currentUser.isCoordinator() ? headerCoordinator : headerUser);

        TextView txtTitle = (TextView)this.findViewById(R.id.form_title);
        TextView txtDescription = (TextView)header.findViewById(R.id.form_description);
        TextView txtDeadline = (TextView)header.findViewById(R.id.form_deadline);
        TextView txtUser = (TextView)header.findViewById(R.id.form_user);
        TextView txtRemaining = (TextView)header.findViewById(R.id.form_remaining);
        RoundIndicator indicator = (RoundIndicator)findViewById(R.id.form_indicator);
        View remaining = findViewById(R.id.form_remaining);

        txtTitle.setText(mForm.title);
        txtDescription.setText(mForm.description);

        int quota = mForm.getQuota();

        if(UPPSCache.currentUser.isCoordinator()) {
            txtUser.setText(getResources().getString(R.string.form_allusers) + " (" + mForm.users.size() + ")");
            txtRemaining.setText(mForm.getRemainingCount() + " " + getResources().getString(R.string.remaining_submissions) + " " + (quota > 0 ? getString(R.string.of) + " " + quota : ""));
        }
        else {
            indicator.setCount(mForm.getRemainingCount());
            txtRemaining.setText(getResources().getString(R.string.remaining_submissions) + " " + (quota > 0 ? getString(R.string.of) + " " + quota : ""));
        }

        indicator.setVisibility(mForm.quota > 0 ? View.VISIBLE : View.GONE);
        remaining.setVisibility(mForm.quota > 0 ? View.VISIBLE : View.GONE);

        View formAlert = findViewById(R.id.alert_form);
        boolean hasPendingSubmission = false;
        for(UPPSSubmission submission : mForm.submissions) {
            if(submission.state == UPPSSubmission.State.NotSent) {
                hasPendingSubmission = true;
                break;
            }
        }

        formAlert.setVisibility(!UPPSCache.currentUser.isCoordinator() && hasPendingSubmission ? View.VISIBLE : View.GONE);

        if(mForm.startTime != null && mForm.endTime != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
            if(mForm.getRemainingDays() > 1)
                txtDeadline.setText(getResources().getString(R.string.form_deadline).replace("{from}", dateFormat.format(mForm.startTime)).replace("{to}", dateFormat.format(mForm.endTime)).replace("{remaining}", ((Integer)mForm.getRemainingDays()).toString()));
            else
                txtDeadline.setText(getResources().getString(R.string.form_deadline_singular).replace("{from}", dateFormat.format(mForm.startTime)).replace("{to}", dateFormat.format(mForm.endTime)).replace("{remaining}", ((Integer)mForm.getRemainingDays()).toString()));
        } else
            txtDeadline.setVisibility(View.GONE);

        if(!UPPSCache.currentUser.isCoordinator() && (mForm.quota == 0 || (!mForm.undefinedMode && !mForm.allowNewSubmissions && mForm.getStateCount(UPPSSubmission.State.New) == 0))) {
            findViewById(R.id.form_newsubmission_container).setVisibility(View.GONE);
        } else {
            findViewById(R.id.form_newsubmission_container).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCallback(int iCallback, Object pParam) {
        if(iCallback == SyncResultCallback.k_iCallback) {
            init();

            selectFirstTab();
            fillSubmissions("all");
            refreshTabs();

            Util.stopAnimatingSyncButton(this);
            Util.hideOverlay(this);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if(iCallback == SyncStartedCallback.k_iCallback) {
            Util.animateSyncButton(this);
            Util.showSyncModal(this);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if(iCallback == NotificationCallback.k_iCallback) {
            NotificationCenter.refreshBadge(this);
        } else if(iCallback == AccessDeniedCallback.k_iCallback) {
            Util.showSessionExpiredModal(this);
        }
    }

    void refreshTabs() {
        if(UPPSCache.currentUser.isCoordinator()) {
            Tab tabReceived = (Tab)findViewById(R.id.form_tab_received);
            Tab tabToReview = (Tab)findViewById(R.id.form_tab_toreview);
            Tab tabRescheduled = (Tab)findViewById(R.id.form_tab_crescheduled);
            Tab tabCCancelled = (Tab)findViewById(R.id.form_tab_ccancelled);
            Tab tabWaitingForCorrection = (Tab)findViewById(R.id.form_tab_waitingforcorrection);
            Tab tabCApproved = (Tab)findViewById(R.id.form_tab_capproved);

            tabReceived.setCount(mForm.getStateCountExcept(mUserId, UPPSSubmission.State.New));
            tabToReview.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.WaitingForApproval));
            tabRescheduled.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Rescheduled));
            tabCCancelled.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Cancelled));
            tabWaitingForCorrection.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Rejected));
            tabCApproved.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Approved));
        } else {
            Tab tabAll = (Tab)findViewById(R.id.form_tab_all);
            Tab tabApproved = (Tab)findViewById(R.id.form_tab_approved);
            Tab tabRejected = (Tab)findViewById(R.id.form_tab_correct);
            Tab tabRescheduled = (Tab)findViewById(R.id.form_tab_rescheduled);
            Tab tabWaiting = (Tab)findViewById(R.id.form_tab_sent);
            Tab tabCancelled = (Tab)findViewById(R.id.form_tab_cancelled);

            tabAll.setCount(mForm.getStateCountExcept(mUserId, UPPSSubmission.State.New));
            tabApproved.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Approved));
            tabRejected.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Rejected));
            tabRescheduled.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Rescheduled));
            tabWaiting.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.WaitingForApproval));
            tabCancelled.setCount(mForm.getStateCount(mUserId, UPPSSubmission.State.Cancelled));
        }
    }

    void filterByUser(int id) {
        if(!UPPSCache.currentUser.isCoordinator())
            return;

        ViewGroup header = (ViewGroup)findViewById(R.id.form_header_coordinator);

        TextView txtUser = (TextView)header.findViewById(R.id.form_user);
        TextView txtRemaining = (TextView)header.findViewById(R.id.form_remaining);

        String name;
        int remaining;
        if(id == 0) {
            name = getResources().getString(R.string.form_allusers) + " (" + mForm.users.size() + ")";
            remaining = mForm.getRemainingCount();
        } else {
            UPPSUser user = UPPSCache.getUser(id);
            name = user.name;

            int quota = mForm.getUserQuota(id);
            if(quota > 0)
                remaining = mForm.getUserRemainingCount(id);
            else
                remaining = mForm.getRemainingCount();
        }

        int quota;
        if(id > 0)
            quota = mForm.getUserQuota(id);
        else
            quota = mForm.getQuota();

        txtUser.setText(name);
        txtRemaining.setText(remaining + " " + getResources().getString(R.string.remaining_submissions) + " " + (quota > 0 ? getString(R.string.of) + " " + quota : ""));

        mUserId = id;
        fillSubmissions(mMode);
        refreshTabs();
    }

    public void showLogs(View view) {
        Util.showLocalLogsWindow(this, mForm);
    }

    public void formBack(View view) {
        this.finish();
    }

    public void hideSearch() {
        View menu_header = findViewById(R.id.menu_header);
        View menu_search = findViewById(R.id.menu_search);
        View menu_icons = findViewById(R.id.menu_icons);

        menu_header.setVisibility(View.VISIBLE);
        menu_search.setVisibility(View.GONE);
        menu_icons.setVisibility(View.VISIBLE);
    }

    public void showSearch() {
        View menu_header = findViewById(R.id.menu_header);
        View menu_search = findViewById(R.id.menu_search);
        View menu_icons = findViewById(R.id.menu_icons);
        View searchClear = findViewById(R.id.menu_search_clear);
        TextView txtSearch = (TextView)findViewById(R.id.menu_search_text);

        menu_header.setVisibility(View.GONE);
        menu_search.setVisibility(View.VISIBLE);
        menu_icons.setVisibility(View.GONE);
        searchClear.setVisibility(View.GONE);
        txtSearch.requestFocus();
    }

    @Override
    public void beforeTextChanged(java.lang.CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(java.lang.CharSequence charSequence, int i, int i1, int i2) {
        mSearchQuery = charSequence.toString();
        mPageIndex = 0;
        fillSubmissions(mMode);
        refreshPaginationButtons();

        View searchClear = findViewById(R.id.menu_search_clear);
        searchClear.setVisibility(charSequence.length() > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void afterTextChanged(android.text.Editable editable) {

    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.modal_button || view.getId() == R.id.modal_buttoncontainer || view.getId() == R.id.modal_button_container) {
            Util.hideOverlay(this);
        }
        else if(view.getId() == R.id.modal_list_item_label && ((String)view.getTag(R.id.listitem_context)).equals("identifier")) {
            Util.hideOverlay(this);

            int id = (Integer)view.getTag(R.id.listitem_value);
            UPPSSubmission submission = mForm.getSubmission(id);
            submission.state = UPPSSubmission.State.NotSent;
            submission.started_at = Calendar.getInstance().getTime();

            UPPSStorage.removeSubmission(submission.id);
            UPPSStorage.addSubmission(submission.form.id, submission);

            selectFirstTab();
            fillSubmissions("all");

            UPPSCache.currentSubmission = submission;

            Intent intent = new Intent(this, SubmissionActivity.class);
            this.startActivityForResult(intent, 1);
        }
        else if(view.getId() == R.id.modal_list_item_label && ((String)view.getTag(R.id.listitem_context)).equals("user")) {
            Util.hideOverlay(this);
            filterByUser((Integer)view.getTag(R.id.user_id));
        }
        else if(view.getId() == R.id.menubar_search) {
            android.widget.EditText txtSearch = (android.widget.EditText)findViewById(R.id.menu_search_text);
            txtSearch.setText("");
            showSearch();
        } else if(view.getId() == R.id.menu_search_cancel) {
            hideSearch();
            clearSearch();
            Util.hideKeyboard(view);
        } else if(view.getId() == R.id.menu_search_clear) {
            android.widget.EditText txtSearch = (android.widget.EditText)findViewById(R.id.menu_search_text);
            txtSearch.setText("");
            clearSearch();
        }
    }

    void createSubmission() {
        UPPSSubmission submission = new UPPSSubmission(this.mForm);
        submission.state = UPPSSubmission.State.NotSent;
        submission.isLocal = true;
        submission.addAction("created", UPPSCache.currentUserId, Calendar.getInstance().getTime());
        submission.id = mForm.getFreeSubmissionId();
        submission.started_at = Calendar.getInstance().getTime();

        mForm.submissions.add(submission);

        UPPSStorage.addSubmission(submission.form.id, submission);

        selectFirstTab();
        fillSubmissions("all");

        UPPSCache.currentSubmission = submission;

        Intent intent = new Intent(this, SubmissionActivity.class);
        this.startActivityForResult(intent, 1);
    }

    void filterSubmissionsByQuery(String text) {
        mSearchQuery = text;
        fillSubmissions(mMode);
        refreshPaginationButtons();
    }

    void clearSearch() {
        mSearchQuery = null;
        fillSubmissions(mMode);
        refreshPaginationButtons();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for(UPPSSubmission submission : mForm.submissions) {
            if(submission.state == UPPSSubmission.State.NotSent && submission.formdata.size() <= submission.getExtraData().length) {
                mForm.submissions.remove(submission);
                UPPSStorage.removeSubmission(submission.id);
                break;
            }
        }

        RoundIndicator indicator = (RoundIndicator)findViewById(R.id.form_indicator);
        indicator.setCount(mForm.getRemainingCount());

        View formAlert = findViewById(R.id.alert_form);
        boolean hasPendingSubmission = false;
        for(UPPSSubmission submission : mForm.submissions) {
            if(submission.state == UPPSSubmission.State.NotSent) {
                hasPendingSubmission = true;
                break;
            }
        }

        formAlert.setVisibility(!UPPSCache.currentUser.isCoordinator() && hasPendingSubmission ? View.VISIBLE : View.GONE);

        selectFirstTab();
        fillSubmissions("all");
        refreshTabs();
    }

    void selectFirstTab() {
        mPageIndex = 0;
        refreshPaginationButtons();

        com.lfdb.parapesquisa.ui.Tab tab;
        if(UPPSCache.currentUser.isCoordinator())
            tab = (com.lfdb.parapesquisa.ui.Tab)findViewById(R.id.form_tab_received);
        else
            tab = (com.lfdb.parapesquisa.ui.Tab)findViewById(R.id.form_tab_all);

        tab.select();
    }

    public void continuePending(View view) {
        for(UPPSSubmission submission : mForm.submissions) {
            if(submission.state == UPPSSubmission.State.NotSent) {
                UPPSCache.currentSubmission = submission;
                break;
            }
        }

        Intent intent = new Intent(this, SubmissionActivity.class);
        this.startActivityForResult(intent, 1);
    }

    public void hideLongName(View view) {
        View container = findViewById(R.id.longname_container);
        container.setVisibility(View.GONE);
    }

    public void showLongName(View view) {
        View container = findViewById(R.id.longname_container);
        TextView text = (TextView)findViewById(R.id.longname_text);

        text.setText(mForm.title);
        container.setVisibility(View.VISIBLE);
    }

    public void stopPending(View view) {
        for(UPPSSubmission submission : mForm.submissions) {
            if(submission.state == UPPSSubmission.State.NotSent) {
                UPPSCache.currentSubmission = submission;
                break;
            }
        }

        Intent intent = new Intent(this, SubmissionActivity.class);
        intent.putExtra("stop", "true");
        this.startActivityForResult(intent, 1);
    }

    public void showSubmission(View view) {
        TextView txtId = (TextView)view.findViewById(R.id.form_submission_id);
        int id = Integer.parseInt(txtId.getText().toString());

        UPPSSubmission submission = mForm.getSubmission(id);
        if(submission == null)
            return;

        UPPSCache.currentSubmission = submission;

        Intent intent = new Intent(this, SubmissionActivity.class);
        this.startActivityForResult(intent, 1);
    }

    void showConcurrentSubmissionAlert() {
        TextView text = Util.showDarkModal(this, "Alerta", "OK", this);
        text.setText(getResources().getString(R.string.form_concurrentsubmission));
    }

    void showIdentifiers() {
        ViewGroup layout = Util.showModal(this, "Identificador", "Cancelar", this);

        UPPSSubmission[] submissions = this.mForm.submissions.toArray(new UPPSSubmission[0]);
        for(UPPSSubmission submission : submissions) {
            if(submission.state != UPPSSubmission.State.New)
                continue;

            UPPSForm.Input identifierInput = mForm.getIdentifierField();
            if(identifierInput == null)
                continue;

            String identifier = submission.formdata.containsKey(identifierInput.name) ? submission.formdata.get(identifierInput.name).toString() : "UNAVAILABLE";

            FrameLayout row = new FrameLayout(this);
            getLayoutInflater().inflate(R.layout.modal_list_item, row);

            TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);
            View separator = row.findViewById(R.id.modal_list_item_separator);

            txtLabel.setText(identifier);
            txtLabel.setTag(R.id.listitem_context, "identifier");
            txtLabel.setTag(R.id.listitem_value, submission.id);
            txtLabel.setClickable(true);
            txtLabel.setOnClickListener(this);

            layout.addView(row);
        }
    }

    public void selectUser(View view) {
        ViewGroup layout = Util.showModal(this, "Selecionar pesquisador", "Cancelar", this);
        {
            FrameLayout row = new FrameLayout(this);
            getLayoutInflater().inflate(R.layout.modal_list_item, row);

            TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);
            txtLabel.setText("Todos os pesquisadores");
            txtLabel.setTag(R.id.listitem_context, "user");
            txtLabel.setTag(R.id.user_id, 0);
            txtLabel.setClickable(true);
            txtLabel.setOnClickListener(this);

            layout.addView(row);
        }

        Collections.sort(mForm.users, new UserComparator());
        for(int userId : mForm.users) {
            UPPSUser user = UPPSCache.getUser(userId);

            FrameLayout row = new FrameLayout(this);
            getLayoutInflater().inflate(R.layout.modal_list_item, row);

            TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);
            View separator = row.findViewById(R.id.modal_list_item_separator);

            txtLabel.setText(user.name);
            txtLabel.setTag(R.id.listitem_context, "user");
            txtLabel.setTag(R.id.user_id, userId);
            txtLabel.setClickable(true);
            txtLabel.setOnClickListener(this);

            layout.addView(row);
        }
    }

    void showNoRemainingSubmissionsAlert() {
        TextView text = Util.showDarkModal(this, "Alerta", "OK", this);
        text.setText(getResources().getString(R.string.form_noremainingsubmissions));
    }

    public void newSubmission(View view) {
        if(mForm.quota == 0 || (mForm.quota > 0 && mForm.getRemainingCount() < 1) || (!mForm.undefinedMode && mForm.getStateCount(UPPSSubmission.State.New) == 0 && !mForm.allowNewSubmissions)) {
            if(mForm.quota > 0 && mForm.getRemainingCount() < 1) {
                showNoRemainingSubmissionsAlert();
            }
            return;
        }

        if(!UPPSCache.currentUser.isCoordinator() && mForm.hasPendingSubmission())
            showConcurrentSubmissionAlert();
        else {
            if(mForm.getStateCount(UPPSSubmission.State.New) > 0 && mForm.getIdentifierField() != null)
                showIdentifiers();
            else
                createSubmission();
        }
    }

    public void filterSubmissions(View view) {
        String mode = "all";
        if(view.getId() == R.id.form_tab_approved)
            mode = "approved";
        else if(view.getId() == R.id.form_tab_correct)
            mode = "rejected";
        else if(view.getId() == R.id.form_tab_rescheduled)
            mode = "rescheduled";
        else if(view.getId() == R.id.form_tab_sent)
            mode = "sent";
        else if(view.getId() == R.id.form_tab_approved)
            mode = "approved";
        else if(view.getId() == R.id.form_tab_cancelled)
            mode = "cancelled";

        else if(view.getId() == R.id.form_tab_received)
            mode = "all";
        else if(view.getId() == R.id.form_tab_toreview)
            mode = "sent";
        else if(view.getId() == R.id.form_tab_crescheduled)
            mode = "rescheduled";
        else if(view.getId() == R.id.form_tab_ccancelled)
            mode = "cancelled";
        else if(view.getId() == R.id.form_tab_waitingforcorrection)
            mode = "rejected";
        else if(view.getId() == R.id.form_tab_capproved)
            mode = "approved";

        mMode = mode;
        mPageIndex = 0;
        refreshPaginationButtons();
        fillSubmissions(mode);

        ScrollView scroll = (ScrollView)this.findViewById(R.id.form_scroll);
        scroll.smoothScrollTo(0, 0);
    }

    int getPageSubmissionCount(int pageIndex) {
        UPPSSubmission[] submissions = mForm.submissions.toArray(new UPPSSubmission[0]);
        int itemIndex = 0;
        int processedItemCount = 0;
        for(int i = 0; i < submissions.length; i++) {
            UPPSSubmission submission = submissions[i];

            if(submission.state == UPPSSubmission.State.New)
                continue;

            int userId = submission.getUserId();

            if(mUserId != 0 && userId != mUserId)
                continue;

            if(!mMode.equals("all")) {
                if(mMode.equals("rejected") && submission.state != UPPSSubmission.State.Rejected)
                    continue;
                else if(mMode.equals("notsent") && submission.state != UPPSSubmission.State.NotSent)
                    continue;
                else if(mMode.equals("sent") && submission.state != UPPSSubmission.State.WaitingForApproval)
                    continue;
                else if(mMode.equals("approved") && submission.state != UPPSSubmission.State.Approved)
                    continue;
                else if(mMode.equals("cancelled") && submission.state != UPPSSubmission.State.Cancelled)
                    continue;
                else if(mMode.equals("rescheduled") && submission.state != UPPSSubmission.State.Rescheduled)
                    continue;
            }

            if(mSearchQuery != null) {
                if(!submission.getTitle(this).toLowerCase().contains(mSearchQuery.toLowerCase()))
                    continue;
            }

            if(itemIndex < pageIndex * ITEMS_PER_PAGE) {
                itemIndex++;
                continue;
            }

            itemIndex++;

            processedItemCount++;
            if(processedItemCount > ITEMS_PER_PAGE)
                break;
        }

        return processedItemCount;
    }

    void refreshPaginationButtons() {
        ViewGroup container = (ViewGroup)findViewById(R.id.form_navigation);
        View btnBack = container.findViewById(R.id.prev_page);
        View btnNext = container.findViewById(R.id.next_page);

        btnBack.setVisibility(mPageIndex > 0 ? View.VISIBLE : View.INVISIBLE);
        btnNext.setVisibility(getPageSubmissionCount(mPageIndex + 1) > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    public void pagination_prevPage(View view) {
        if(mPageIndex == 0)
            return;

        mPageIndex--;
        refreshPaginationButtons();
        fillSubmissions(mMode);

        ScrollView scroll = (ScrollView)this.findViewById(R.id.form_scroll);
        scroll.smoothScrollTo(0, 0);
    }

    public void pagination_nextPage(View view) {
        int count = getPageSubmissionCount(mPageIndex + 1);

        if(count < 1)
            return;

        mPageIndex++;
        refreshPaginationButtons();
        fillSubmissions(mMode);

        ScrollView scroll = (ScrollView)this.findViewById(R.id.form_scroll);
        scroll.smoothScrollTo(0, 0);
    }

    void fillSubmissions(String mode) {
        if(mode == null)
            mode = "all";

        LinearLayout table = (LinearLayout)this.findViewById(R.id.form_table);
        if(table == null)
            return;

        table.removeAllViews();

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
        SimpleDateFormat tformat = new SimpleDateFormat("HH:mm");

        UPPSSubmission[] submissions = mForm.submissions.toArray(new UPPSSubmission[0]);
        LayoutInflater inflater = getLayoutInflater();

        int processedItemCount = 0;
        int itemIndex = 0;

        for(int i = 0; i < submissions.length; i++) {
            UPPSSubmission submission = submissions[i];

            if(submission.state == UPPSSubmission.State.New)
                continue;

            int userId = submission.getUserId();

            if(mUserId != 0 && userId != mUserId)
                continue;

            if(!mode.equals("all")) {
                if(mode.equals("rejected") && submission.state != UPPSSubmission.State.Rejected)
                    continue;
                else if(mode.equals("notsent") && submission.state != UPPSSubmission.State.NotSent)
                    continue;
                else if(mode.equals("sent") && submission.state != UPPSSubmission.State.WaitingForApproval)
                    continue;
                else if(mode.equals("approved") && submission.state != UPPSSubmission.State.Approved)
                    continue;
                else if(mode.equals("cancelled") && submission.state != UPPSSubmission.State.Cancelled)
                    continue;
                else if(mode.equals("rescheduled") && submission.state != UPPSSubmission.State.Rescheduled)
                    continue;
            }

            if(mSearchQuery != null) {
                if(!submission.getTitle(this).toLowerCase().contains(mSearchQuery.toLowerCase()))
                    continue;
            }

            if(itemIndex < mPageIndex * ITEMS_PER_PAGE) {
                itemIndex++;
                continue;
            }

            itemIndex++;

            processedItemCount++;
            if(processedItemCount > ITEMS_PER_PAGE)
                break;

            FrameLayout row;
            if(!submissionViewCache.containsKey(submission.id)) {
                row = new FrameLayout(this);
                inflater.inflate(R.layout.form_submission, row);
                Util.hookTapEvent(row.getChildAt(0));

                submissionViewCache.put(submission.id, row);
            } else {
                row = (FrameLayout)submissionViewCache.get(submission.id);
            }

            TextView s_txtTitle = (TextView)row.findViewById(R.id.submission_title);
            TextView s_txtDescription = (TextView)row.findViewById(R.id.submission_description);
            TextView s_txtState = (TextView)row.findViewById(R.id.submission_state);
            TextView s_txtReview = (TextView)row.findViewById(R.id.submission_review);
            TextView s_txtId = (TextView)row.findViewById(R.id.form_submission_id);
            ImageView s_img = (ImageView)row.findViewById(R.id.submission_stateicon);

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), submission.getStateImageId(UPPSCache.currentUser.isCoordinator()));
            s_img.setImageBitmap(bmp);
            s_txtTitle.setText(submission.getTitle(this));
            if(submission.started_at != null)
                s_txtDescription.setText("Iniciada em " + format.format(submission.started_at) + " - " + tformat.format(submission.started_at));
            else
                s_txtDescription.setText(getString(R.string.started_at_unavailable));
            s_txtState.setText(submission.getStateDescription(UPPSCache.currentUser.isCoordinator(), this));
            s_txtState.setTextColor(submission.getStateColor(UPPSCache.currentUser.isCoordinator()));
            s_txtId.setText(((Integer)submission.id).toString());
            if(!UPPSCache.currentUser.isCoordinator() && submission.state == UPPSSubmission.State.Rejected && submission.getLatestAction("revised") != null) {
                UPPSSubmission.Action action = submission.getLatestAction("revised");

                s_txtReview.setText("Revisado por " + UPPSCache.getUser(action.userId).name + " em " + format.format(action.date));
            } else if(UPPSCache.currentUser.isCoordinator() && submission.getUserId() > 0) {
                UPPSSubmission.Action action = submission.getLatestAction("submitted");
                if(action == null)
                    action = submission.getLatestAction("started");
                if(action == null)
                    action = submission.getLatestAction("created");

                if(action != null)
                    s_txtReview.setText("Enviado por " + UPPSCache.getUser(action.userId).name + " em " + format.format(action.date));
                else
                    s_txtReview.setVisibility(View.GONE);
            }
            else
                s_txtReview.setVisibility(View.GONE);

            table.addView(row);
        }
    }

    @Override
    public void onBackPressed() {
        if(Util.isOverlayVisible(this))
            return;

        finish();
    }
}
