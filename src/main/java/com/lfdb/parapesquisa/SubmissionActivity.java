package com.lfdb.parapesquisa;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.HttpAuthHandler;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.EditText;

import com.lfdb.parapesquisa.api.SyncAction;
import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSCallback;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.UPPSSubmission;
import com.lfdb.parapesquisa.api.callbacks.AccessDeniedCallback;
import com.lfdb.parapesquisa.api.callbacks.NotificationCallback;
import com.lfdb.parapesquisa.storage.UPPSStorage;
import com.lfdb.parapesquisa.ui.CheckGroup;
import com.lfdb.parapesquisa.ui.CurrencyControl;
import com.lfdb.parapesquisa.ui.DragGroup;
import com.lfdb.parapesquisa.ui.FontTextView;
import com.lfdb.parapesquisa.ui.OnValueChangeListener;
import com.lfdb.parapesquisa.ui.RadioGroup;
import com.lfdb.parapesquisa.ui.RoundIndicator;
import com.lfdb.parapesquisa.ui.SelectGroup;
import com.lfdb.parapesquisa.util.Validator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by Igor on 8/2/13.
 */
public class SubmissionActivity extends FragmentActivity implements Runnable, View.OnClickListener, OnValueChangeListener, UPPSCallback, ViewTreeObserver.OnGlobalLayoutListener {
    @Override
    public void onValueChanged() {
        saveData();

        boolean removedAny = false;

        ArrayList<String> hiddenFields = getHiddenFields();
        for(String name : hiddenFields) {
            if(UPPSCache.currentSubmission.formdata.containsKey(name)) {
                UPPSCache.currentSubmission.formdata.remove(name);
                removedAny = true;
            }
        }

        if(removedAny) {
            refreshPage();
        }

        checkActions();
    }

    @Override
    public void onCallback(int iCallback, Object pParam) {
        if(iCallback == NotificationCallback.k_iCallback) {
            NotificationCenter.refreshBadge(this);
        } else if(iCallback == AccessDeniedCallback.k_iCallback) {
            Util.showSessionExpiredModal(this);
        }
    }

    @Override
    public void onGlobalLayout() {
        if(!needsScroll)
            return;

        needsScroll = false;

        if(correctionMode) {
            String correctionInputName = this.getCorrectionFieldName();
            if(correctionInputName == null)
                return;

            View inputView = this.getInputCommentView(correctionInputName);
            if(inputView == null)
                return;

            int top = inputView.getTop();
            ScrollView scrollView = (ScrollView)findViewById(R.id.fillform_scroll);
            scrollView.smoothScrollTo(0, top);
        } else if(mValidations.size() > 0 && (UPPSCache.currentSubmission.state == UPPSSubmission.State.NotSent || UPPSCache.currentSubmission.state == UPPSSubmission.State.Rescheduled)) {
            String inputName = null;
            for(UPPSForm.Input input : UPPSCache.currentSubmission.form.pages.get(mPageIndex).inputs) {
                if(mValidations.containsKey(input.name)) {
                    inputName = input.name;
                    break;
                }
            }
            if(inputName == null)
                return;

            View inputView = getInputView(inputName);

            int top = inputView.getTop();
            ScrollView scrollView = (ScrollView)findViewById(R.id.fillform_scroll);
            scrollView.smoothScrollTo(0, top);
        }
    }

    class BoldTypefaceSpan extends TypefaceSpan {
        private final Typeface newType;

        public BoldTypefaceSpan(Typeface typeface) {
            super("bold");
            newType = typeface;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            applyCustomTypeFace(ds, newType);
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            applyCustomTypeFace(paint, newType);
        }

        private void applyCustomTypeFace(Paint paint, Typeface tf) {
            int oldStyle;
            Typeface old = paint.getTypeface();
            if (old == null) {
                oldStyle = 0;
            } else {
                oldStyle = old.getStyle();
            }

            int fake = oldStyle & ~tf.getStyle();
            if ((fake & Typeface.BOLD) != 0) {
                paint.setFakeBoldText(true);
            }

            if ((fake & Typeface.ITALIC) != 0) {
                paint.setTextSkewX(-0.25f);
            }

            paint.setTypeface(tf);
        }
    }

    class DatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        boolean fired = false;
        @Override
        public void onDateSet(android.widget.DatePicker datePicker, int year,int monthOfYear, int dayOfMonth) {
            if(fired)
                return;

            fired = true;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            String str = calendar.toString();

            if(getTag() != null && getTag().equals("input")) {
                setInputDate(calendar);
            } else {
                saveRescheduleDate(Integer.parseInt(getTag()), calendar.getTime());
            }
        }
    }

    public class ExtraData_t {
        public String name;
        public String value;
    }

    enum Validation {
        None,
        Required,
        OutOfRange,
        Invalid
    }

    class FormPageComparator implements Comparator<UPPSForm.Page> {

        @Override
        public int compare(UPPSForm.Page page, UPPSForm.Page page2) {
            return ((Integer)page.order).compareTo(page2.order);
        }
    }

    class FormInputComparator implements Comparator<UPPSForm.Input> {

        @Override
        public int compare(UPPSForm.Input page, UPPSForm.Input page2) {
            return ((Integer)page.order).compareTo(page2.order);
        }
    }

    int mPageIndex;
    int mCorrectIndex;
    boolean correctionMode = false;
    boolean presentingCorrectModal = false;
    boolean presentingConfirmModal = false;
    ViewGroup mCommentModal;
    ViewGroup editingDateInput;
    Hashtable<String, Validation> mValidations = new Hashtable<String, Validation>();
    Hashtable<String, View> mViewCache = new Hashtable<String, View>();
    boolean needsScroll = false;
    Thread pageRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(UPPSCache.currentSubmission == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_fillform);
        mViewCache = new Hashtable<String, View>();

        TextView txtPageTitle = (TextView)findViewById(R.id.form_page_title);
        txtPageTitle.setText("");

        if(savedInstanceState != null)
            savedInstanceState.clear();

        Util.initActivity(this);
        GuidedTour.init(this);

        Util.setMenuBarVisibleIcons(this, 0);

        if(Build.VERSION.SDK_INT >= 11) this.getActionBar().hide();

        TextView txtTitle = (TextView)findViewById(R.id.form_title);
        TextView txtDescription = (TextView)findViewById(R.id.form_description);
        TextView txtState = (TextView)findViewById(R.id.form_state);

        UPPSSubmission submission = UPPSCache.currentSubmission;
        txtTitle.setText(submission.getTitle(this));
        txtState.setText(submission.getStateDescription(UPPSCache.currentUser.isCoordinator(), this));
        txtState.setTextColor(submission.getStateColor(UPPSCache.currentUser.isCoordinator()));

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
        SimpleDateFormat tformat = new SimpleDateFormat("HH:mm");

        if(submission.started_at != null)
            txtDescription.setText("Iniciada em " + format.format(submission.started_at) + " - " + tformat.format(submission.started_at));
        else
            txtDescription.setText(getString(R.string.started_at_unavailable));

        RoundIndicator indicator = (RoundIndicator)findViewById(R.id.form_indicator);
        indicator.setFillColor(UPPSCache.currentSubmission.getStateColor(UPPSCache.currentUser.isCoordinator()));
        if(UPPSCache.currentSubmission.state != UPPSSubmission.State.NotSent && UPPSCache.currentSubmission.state != UPPSSubmission.State.Rescheduled) {
            indicator.setImage(UPPSCache.currentSubmission.getStateImageId(UPPSCache.currentUser.isCoordinator()));
        }

        View progress = findViewById(R.id.form_progress);
        progress.setVisibility(UPPSCache.currentSubmission.state == UPPSSubmission.State.NotSent || UPPSCache.currentSubmission.state == UPPSSubmission.State.Rescheduled ? View.VISIBLE : View.INVISIBLE);
        progress.setBackgroundColor(UPPSCache.currentSubmission.getStateColor(UPPSCache.currentUser.isCoordinator()));

        View coordinatorActions = findViewById(R.id.submission_coordinatoractions);
        coordinatorActions.setVisibility(UPPSCache.currentUser.isCoordinator() && submission.state == UPPSSubmission.State.WaitingForApproval ? View.VISIBLE : View.GONE);

        View userActions = findViewById(R.id.submission_useractions);
        userActions.setVisibility(!UPPSCache.currentUser.isCoordinator() && submission.state == UPPSSubmission.State.Rejected ? View.VISIBLE : View.GONE);
        setMode(!UPPSCache.currentUser.isCoordinator() && submission.state == UPPSSubmission.State.Rejected);

        View stop = findViewById(R.id.stop_filling);
        stop.setVisibility(!UPPSCache.currentUser.isCoordinator() && submission.form.canStopFilling && (submission.state == UPPSSubmission.State.NotSent || submission.state == UPPSSubmission.State.Rescheduled) ? View.VISIBLE : View.GONE);

        TextView review = (TextView)findViewById(R.id.form_review);
        if(UPPSCache.currentUser.isCoordinator() && submission.getLatestAction("submitted") != null) {
            review.setVisibility(View.VISIBLE);

            UPPSSubmission.Action action = submission.getLatestAction("submitted");
            review.setText(getResources().getString(R.string.fillform_sentby).replace("{name}", UPPSCache.getUser(action.userId).name).replace("{date}", format.format(action.date)));
        } else if(submission.state == UPPSSubmission.State.Rejected && submission.getLatestAction("revised") != null) {
            review.setVisibility(View.VISIBLE);

            UPPSSubmission.Action action = submission.getLatestAction("revised");
            review.setText(getResources().getString(R.string.fillform_reviewedby).replace("{name}", UPPSCache.getUser(action.userId).name).replace("{date}", format.format(action.date)));
        }

        findViewById(R.id.submission_xtradata).setVisibility(getExtraData().length > 0 ? View.VISIBLE : View.GONE);

        ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
        ViewTreeObserver observer = layout.getViewTreeObserver();
        if(observer.isAlive()) {
            observer.addOnGlobalLayoutListener(this);
        }

        Collections.sort(UPPSCache.currentSubmission.form.pages, new FormPageComparator());

        if(submission.state == UPPSSubmission.State.NotSent && submission.currentPageIndex >= 0 && submission.currentPageIndex < submission.form.pages.size())
        {
            mPageIndex = submission.currentPageIndex;
            if(mPageIndex < 0 || mPageIndex >= submission.form.pages.size())
                mPageIndex = 0;
        }
        else
            mPageIndex = 0;

        firstRefreshPage();

        if(getIntent().hasExtra("stop") && getIntent().getStringExtra("stop").equals("true"))
            stopFilling(null);

        if(submission.state == UPPSSubmission.State.Rejected && !UPPSCache.currentUser.isCoordinator()) {
            showCorrectionModal();
            mCorrectIndex = 0;
            String name = getCorrectionFieldName();
            mPageIndex = UPPSCache.currentSubmission.form.getInputPageIndex(name);
            needsScroll = true;

            refreshPage();
        }

        UPPSServer.getActiveServer().registerCallback(NotificationCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(AccessDeniedCallback.k_iCallback, this);

        if(GuidedTour.isRunning)
            GuidedTour.restore(this);
    }

    public void run() {
        final Activity activity = this;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
                layout.removeAllViews();

                activity.findViewById(R.id.async_loading).setVisibility(View.VISIBLE);
            }
        });

        try {
            Thread.sleep(100); // Wait for message loop...
        } catch (InterruptedException ex) {
            return;
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshPage();
                activity.findViewById(R.id.async_loading).setVisibility(View.GONE);

                pageRenderer = null;
            }
        });
    }

    void firstRefreshPage() {
        if(pageRenderer != null)
            return;

        pageRenderer = new Thread(this);
        pageRenderer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mViewCache.clear();
    }

    public ExtraData_t[] getExtraData() {
        int count = 0;
        for(UPPSForm.Page page : UPPSCache.currentSubmission.form.pages)
            for(UPPSForm.Input input : page.inputs)
                if(input.readonly)
                    count++;

        ExtraData_t result[] = new ExtraData_t[count];
        int i = 0;
        for(UPPSForm.Page page : UPPSCache.currentSubmission.form.pages)
            for(UPPSForm.Input input : page.inputs)
                if(input.readonly) {
                    ExtraData_t data = new ExtraData_t();
                    data.name = input.label;
                    if(UPPSCache.currentSubmission.formdata.get(input.name) != null)
                        data.value = UPPSCache.currentSubmission.formdata.get(input.name).toString();
                    else
                        data.value = getString(R.string.unavailable);

                    result[i++] = data;
                }

        return result;
    }

    void showCorrectionModal() {
        presentingCorrectModal = true;
        ViewGroup table = Util.showModal(this, getResources().getString(R.string.fields_to_correct), getString(R.string.cancel), this);

        int i = 0;
        for(UPPSSubmission.ReviewData data : UPPSCache.currentSubmission.reviewdata) {
            LinearLayout row = new LinearLayout(this);
            getLayoutInflater().inflate(R.layout.modal_list_item, row, true);

            TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);

            UPPSForm.Input input = UPPSCache.currentSubmission.form.getInput(data.fieldName);
            int page = UPPSCache.currentSubmission.form.getInputPageIndex(input.name);
            UPPSForm.Page formPage = UPPSCache.currentSubmission.form.pages.get(page);

            txtLabel.setText((page + 1) + "." + (formPage.getInputIndex(input.name) + 1) + " " + input.label);
            txtLabel.setTag(R.id.listitem_value, i++);
            txtLabel.setTag(R.id.listitem_context, "correct");
            txtLabel.setOnClickListener(this);

            table.addView(row);
        }
    }

    public void showExtraData(View view) {
        ViewGroup layout = Util.showDarkListModal(this, getResources().getString(R.string.extra_info));
        for(ExtraData_t xtra : getExtraData()) {
            String name = xtra.name;
            String value = xtra.value;

            SpannableString str = new SpannableString(name + ": " + value);
            str.setSpan(new BoldTypefaceSpan(FontTextView.getTypeface(this.getAssets(), 700)), 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            LinearLayout row = new LinearLayout(this);
            getLayoutInflater().inflate(R.layout.modal_list_item, row, true);

            TextView txtText = (TextView)row.findViewById(R.id.modal_list_item_label);
            txtText.setText(str);

            layout.addView(row);
        }
    }

    void beforeExiting() {
        Util.beginSync(this.getParent());
    }

    public void hideLongName(View view) {
        View container = findViewById(R.id.longname_container);
        container.setVisibility(View.GONE);
    }

    public void showLongName(View view) {
        View container = findViewById(R.id.longname_container);
        TextView text = (TextView)findViewById(R.id.longname_text);

        text.setText(UPPSCache.currentSubmission.getTitle(this));
        container.setVisibility(View.VISIBLE);
    }

    void saveMe() {
        UPPSStorage.removeSubmission(UPPSCache.currentSubmission.id);
        UPPSStorage.addSubmission(UPPSCache.currentSubmission.form.id, UPPSCache.currentSubmission);
    }

    void saveRescheduleDate(int reason, Date date) {
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(date);

        Calendar dayStart = Calendar.getInstance();
        dayStart.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH));
        dayStart.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
        dayStart.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
        dayStart.set(Calendar.HOUR, 0);
        dayStart.set(Calendar.MINUTE, 0);

        if(dateCalendar.compareTo(dayStart) < 0) {
            final Activity activity = this;
            TextView txt = Util.showDarkModal(this, getString(R.string.warning), getString(R.string.ok), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Util.hideOverlay(activity);
                }
            });
            txt.setText(getString(R.string.invaliddate));
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

        UPPSCache.currentSubmission.rescheduleDate = date;
        UPPSCache.currentSubmission.stopReason = reason;
        UPPSCache.currentSubmission.timesRescheduled++;
        UPPSCache.currentSubmission.state = UPPSSubmission.State.WaitingForSync;

        saveData();
        saveMe();

        UPPSCache.queueSyncAction(SyncAction.Action.SubmissionReschedule, UPPSCache.currentSubmission.id);
        beforeExiting();

        this.setResult(2, getIntent());
        this.finish();
    }

    void cancelSubmission(UPPSForm.StopReason_t reason, boolean maxReschedules) {
        if(UPPSCache.currentSubmission.alternatives.size() > 0) {
            UPPSSubmission alternative = UPPSCache.currentSubmission.alternatives.get(0);
            alternative.state = UPPSSubmission.State.NotSent;

            UPPSCache.currentSubmission.form.submissions.add(alternative);
            UPPSCache.currentSubmission.alternatives.remove(0);
        }

        UPPSCache.currentSubmission.state = UPPSSubmission.State.WaitingForSync;
        UPPSCache.currentSubmission.stopReason = reason.id;
        UPPSCache.currentSubmission.addAction("canceled", UPPSCache.currentUserId, Calendar.getInstance().getTime()).reasonId = reason.id;

        saveData();
        saveMe();

        UPPSCache.queueSyncAction(SyncAction.Action.SubmissionReschedule, UPPSCache.currentSubmission.id);
        beforeExiting();

        this.setResult(2, getIntent());
        this.finish();
    }

    public void onClick(View view) {
        if(view.getId() == R.id.modal_button_container) {
            Util.hideOverlay(this);
        }
        else if(view.getId() == R.id.modal_list_item_label) {
            if(((String)view.getTag(R.id.listitem_context)).equals("cancel")) {
                UPPSForm.StopReason_t reason = (UPPSForm.StopReason_t)view.getTag(R.id.listitem_value);

                if(reason.reschedule) {
                    if(UPPSCache.currentSubmission.form.maxReschedules > 0 && UPPSCache.currentSubmission.timesRescheduled >= UPPSCache.currentSubmission.form.maxReschedules) {
                        cancelSubmission(reason, true);
                    } else {
                        DatePicker picker = new DatePicker();
                        picker.show(getSupportFragmentManager(), Integer.toString(reason.id));
                    }
                } else {
                    cancelSubmission(reason, false);
                }

            } else if(((String)view.getTag(R.id.listitem_context)).equals("correct")) {
                correctionMode = true;
                mCorrectIndex = (Integer)view.getTag(R.id.listitem_value);
                String name = getCorrectionFieldName();
                mPageIndex = UPPSCache.currentSubmission.form.getInputPageIndex(name);
                needsScroll = true;

                refreshPage();
                Util.hideOverlay(this);
            }
        } else if(view.getId() == R.id.form_input_baloon) {
            String inputName = (String)view.getTag();
            ViewGroup container = Util.showOverlay(this, R.layout.modal_comment);

            EditText editText = (EditText)container.findViewById(R.id.form_comment_text);
            View btnCancel = container.findViewById(R.id.modal_cancel);
            View btnOk = container.findViewById(R.id.modal_ok);

            editText.requestFocus();
            Util.showKeyboard(editText);
            btnOk.setTag(inputName);

            btnCancel.setOnClickListener(this);
            btnOk.setOnClickListener(this);
            mCommentModal = container;
            Util.hookTapEventGroup(container);
        } else if(view.getId() == R.id.modal_ok) {
            String inputName = (String)view.getTag();

            EditText txtComment = (EditText)mCommentModal.findViewById(R.id.form_comment_text);
            String comment = txtComment.getText().toString();
            txtComment.clearFocus();
            Util.hideKeyboard(txtComment);

            UPPSCache.currentSubmission.addReviewData(inputName, comment, UPPSCache.currentUserId, Calendar.getInstance().getTime());//reviewdata.put(inputName, comment);
            Util.hideOverlay(this);
            mCommentModal = null;
            refreshPage();
        } else if(view.getId() == R.id.modal_cancel) {
            Util.hideKeyboard(this);
            Util.hideOverlay(this);
        } else if(view.getId() == R.id.form_fill_editcomment) {
            String inputName = (String)view.getTag();

            UPPSSubmission.ReviewData data = UPPSCache.currentSubmission.getReviewData(inputName);

            ViewGroup container = Util.showOverlay(this, R.layout.modal_comment);

            View btnCancel = container.findViewById(R.id.modal_cancel);
            View btnOk = container.findViewById(R.id.modal_ok);
            EditText txtComment = (EditText)container.findViewById(R.id.form_comment_text);
            txtComment.requestFocus();
            Util.showKeyboard(txtComment);

            btnOk.setTag(inputName);

            btnCancel.setOnClickListener(this);
            btnOk.setOnClickListener(this);
            txtComment.setText(data.message);
            mCommentModal = container;
            Util.hookTapEventGroup(container);
        } else if(view.getId() == R.id.form_fill_removecomment) {
            String inputName = (String)view.getTag();
            UPPSCache.currentSubmission.removeReviewData(inputName);
            refreshPage();
        } else if(view.getId() == R.id.modal_buttoncontainer) {
            if(presentingConfirmModal) {
                endSubmission();
                presentingConfirmModal = false;
            } else {
                UPPSCache.currentSubmission.state = UPPSSubmission.State.WaitingForSync;
                UPPSCache.currentSubmission.addAction("rejected", UPPSCache.currentUserId, Calendar.getInstance().getTime());

                saveData();
                saveMe();

                SyncAction syncAction = UPPSCache.createSyncAction(SyncAction.Action.SubmissionModerate, UPPSCache.currentSubmission.id);
                syncAction.moderate_action = "reprove";
                UPPSCache.queueSyncAction(syncAction);

                beforeExiting();

                this.setResult(2, getIntent());
                this.finish();
            }
        } else if(view.getId() == R.id.form_input_date_mask) {
            DatePicker picker = new DatePicker();
            editingDateInput = (ViewGroup)view.getTag();
            picker.show(this.getSupportFragmentManager(), "input");
        } else if(view.getId() == R.id.form_input_private_button) {
            ViewGroup row = (ViewGroup)view.getTag();
            View mask = row.findViewById(R.id.form_input_private_mask);
            View button = row.findViewById(R.id.form_input_private_button);

            EditText txt = (EditText)row.findViewById(R.id.form_input_value);
            if(txt.length() > 0) {
                txt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                txt.clearFocus();
                txt.setEnabled(false);

                mask.setVisibility(View.VISIBLE);
                button.setVisibility(View.GONE);
            }
            else {
                TextView textView = Util.showDarkModal(this, getString(R.string.alert), getString(R.string.ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Util.hideOverlay(SubmissionActivity.this);
                    }
                });
                textView.setText(getString(R.string.fillform_private_notext));
            }
        }
    }

    void setInputDate(Calendar date) {
        EditText txtDay = (EditText)editingDateInput.findViewById(R.id.form_input_day);
        EditText txtMonth = (EditText)editingDateInput.findViewById(R.id.form_input_month);
        EditText txtYear = (EditText)editingDateInput.findViewById(R.id.form_input_year);

        String day = Integer.toString(date.get(Calendar.DAY_OF_MONTH));
        String month = Integer.toString(date.get(Calendar.MONTH) + 1);
        String year = Integer.toString(date.get(Calendar.YEAR));

        if(day.length() < 2)
            day = "0" + day;
        if(month.length() < 2)
            month = "0" + month;
        if(year.length() < 2)
            year = "0" + year;

        txtDay.setText(day);
        txtMonth.setText(month);
        txtYear.setText(year);
    }

    void setMode(boolean correction) {
        int activeColor = 0xff666666;
        int inactiveColor = 0xff999999;

        this.correctionMode = correction;

        View vView = findViewById(R.id.submission_viewmode);
        View vCorrect = findViewById(R.id.submission_correctmode);
        View vNavigation = findViewById(R.id.submission_navigation);
        View vViewAll = findViewById(R.id.submission_useractions_viewall);
        View vNav = findViewById(R.id.submission_useractions_nav);

        if(correction) {
            vView.setBackgroundColor(inactiveColor);
            vCorrect.setBackgroundColor(activeColor);
            vNavigation.setVisibility(View.GONE);
            vViewAll.setVisibility(View.VISIBLE);
            vNav.setVisibility(View.VISIBLE);
        } else {
            vView.setBackgroundColor(activeColor);
            vCorrect.setBackgroundColor(inactiveColor);
            vNavigation.setVisibility(View.VISIBLE);
            vViewAll.setVisibility(View.GONE);
            vNav.setVisibility(View.GONE);
        }
    }

    void showStopModal() {
        ViewGroup layout = (ViewGroup)Util.showModal(this, getResources().getString(R.string.stop_filling_title), getResources().getString(R.string.cancel), this);

        int i = 0;
        for(UPPSForm.StopReason_t reason : UPPSCache.currentSubmission.form.stopReasons) {
            FrameLayout row = new FrameLayout(this);
            getLayoutInflater().inflate(R.layout.modal_list_item, row);

            TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);
            View separator = row.findViewById(R.id.modal_list_item_separator);

            if(i == UPPSCache.currentSubmission.form.stopReasons.size() - 1) separator.setVisibility(View.INVISIBLE);

            txtLabel.setText(reason.reason);
            txtLabel.setClickable(true);
            txtLabel.setOnClickListener(this);
            txtLabel.setTag(R.id.listitem_context, "cancel");
            txtLabel.setTag(R.id.listitem_value, reason);

            layout.addView(row);
            i++;
        }
    }

    public void back(View view) {
        saveData();
        saveMe();
        this.setResult(1, getIntent());
        this.finish();
    }

    public void stopFilling(View view) {
        showStopModal();
    }

    public void prevPage(View view) {
        if(pageRenderer != null)
            return;

        if(mPageIndex == 0)
            return;

        ScrollView scrollView = (ScrollView)findViewById(R.id.fillform_scroll);
        scrollView.smoothScrollTo(0, 0);

        saveData();

        mPageIndex--;
        UPPSCache.currentSubmission.currentPageIndex = mPageIndex;
        refreshPage();
    }

    boolean validateFields() {
        saveData();
        mValidations.clear();

        UPPSForm.Page currentPage = UPPSCache.currentSubmission.form.pages.get(this.mPageIndex);
        ArrayList<String> hiddenFields = getHiddenFields();
        boolean allValid = true;
        for(UPPSForm.Input input : currentPage.inputs) {
            if(hiddenFields.contains(input.name))
                continue;

            int range[] = null;
            boolean required = false;

            boolean isValid = true;
            Validation result = Validation.None;

            if(UPPSCache.currentSubmission.formdata.containsKey(input.name)) {
                Object value = UPPSCache.currentSubmission.formdata.get(input.name);
                if(input.getType() == UPPSForm.InputType.Cpf) {
                    isValid = Validator.isCPFValid((String)UPPSCache.currentSubmission.formdata.get(input.name));
                } else if(input.getType() == UPPSForm.InputType.Email)
                    isValid = Validator.isEmailValid((String)value);
                else if(input.getType() == UPPSForm.InputType.Url)
                    isValid = Validator.isURLValid((String)value);

                if(!isValid)
                    result = Validation.Invalid;
            }

            required = input.data.validations.required;

            if(range != null && UPPSCache.currentSubmission.formdata.containsKey(input.name)) {
                if(input.getType() == UPPSForm.InputType.Number) {
                    Integer value = (Integer)UPPSCache.currentSubmission.formdata.get(input.name);
                    if(value < range[0] || value > range[1]) {
                        isValid = false;
                        result = Validation.OutOfRange;
                    }
                } else if(input.getType() != UPPSForm.InputType.OrderedList && input.getType() != UPPSForm.InputType.Date && input.getType() != UPPSForm.InputType.Check && input.getType() != UPPSForm.InputType.Radio) {
                    String value = (String)UPPSCache.currentSubmission.formdata.get(input.name);
                    if(value.length() < range[0] || value.length() > range[1]) {
                        isValid = false;
                        result = Validation.OutOfRange;
                    }
                }
            }
            if(required) {
                if(!UPPSCache.currentSubmission.formdata.containsKey(input.name) || UPPSCache.currentSubmission.formdata.get(input.name).toString().length() < 1) {
                    isValid = false;
                    result = Validation.Required;
                }
            }

            if(!isValid) {
                allValid = false;
                this.mValidations.put(input.name, result);
            }
        }
        return allValid;
    }

    void finishSubmission() {
        TextView txt = Util.showDarkOkCancelModal(this, "Alerta", "OK", this);
        txt.setText(getResources().getString(R.string.send_submission));
        presentingConfirmModal = true;
    }

    void endSubmission() {
        UPPSCache.currentSubmission.state = UPPSSubmission.State.WaitingForSync;
        UPPSCache.currentSubmission.addAction("submitted", UPPSCache.currentUserId, Calendar.getInstance().getTime());
        saveData();
        saveMe();

        if((UPPSCache.currentSubmission.id & 0x80000000) == 0x80000000) {
            SyncAction action = UPPSCache.createSyncAction(SyncAction.Action.SubmissionPublish, UPPSCache.currentSubmission.id);
            UPPSCache.queueSyncAction(action);
        } else {
            SyncAction syncAction = UPPSCache.createSyncAction(SyncAction.Action.SubmissionUpdate, UPPSCache.currentSubmission.id);
            syncAction.update_newState = UPPSSubmission.State.WaitingForApproval;
            UPPSCache.queueSyncAction(syncAction);
        }

        beforeExiting();

        this.setResult(2, getIntent());
        this.finish();
    }

    public void nextPage(View view) {
        if(pageRenderer != null)
            return;

        ScrollView scrollView = (ScrollView)findViewById(R.id.fillform_scroll);
        scrollView.smoothScrollTo(0, 0);

        if(!UPPSCache.currentUser.isCoordinator() && (UPPSCache.currentSubmission.state == UPPSSubmission.State.NotSent || UPPSCache.currentSubmission.state == UPPSSubmission.State.Rescheduled) && !validateFields()) {
            needsScroll = true;
            refreshPage();
            return;
        }
        UPPSForm form = UPPSCache.currentSubmission.form;
        if(mPageIndex == form.pages.size() - 1) {
            if(UPPSCache.currentUser.isCoordinator() || (UPPSCache.currentSubmission.state != UPPSSubmission.State.NotSent && UPPSCache.currentSubmission.state != UPPSSubmission.State.Rescheduled))
                return;

            finishSubmission();
            saveMe();
            return;
        }

        saveData();
        saveMe();

        mPageIndex++;
        UPPSCache.currentSubmission.currentPageIndex = mPageIndex;
        refreshPage();
    }

    public void requestCorrection(View view) {
        final Activity activity = this;

        if(UPPSCache.currentSubmission.reviewdata.size() > 0) {
            TextView txt = Util.showDarkOkCancelModal(this, getString(R.string.alert), getString(R.string.ok), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(view.getId() == R.id.modal_buttoncontainer) {
                        UPPSCache.currentSubmission.state = UPPSSubmission.State.WaitingForSync;
                        UPPSCache.currentSubmission.addAction("revised", UPPSCache.currentUserId, Calendar.getInstance().getTime());

                        saveData();
                        saveMe();

                        UPPSCache.queueSyncAction(SyncAction.Action.SubmissionCorrect, UPPSCache.currentSubmission.id);
                        beforeExiting();

                        setResult(2, getIntent());
                        finish();
                    } else if(view.getId() == R.id.modal_cancel) {
                        Util.hideOverlay(activity);
                    }
                }
            });
            txt.setText(getString(R.string.confirm_correct));
        } else {
            TextView txt = Util.showDarkOkCancelModal(this, getString(R.string.alert), getString(R.string.ok), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Util.hideOverlay(activity);
                }
            });
            txt.setText(getString(R.string.no_corrections));
        }
    }

    public void reject(View view) {
        TextView textView = (TextView)Util.showDarkOkCancelModal(this, "Alerta", "OK", this);
        textView.setText(getResources().getString(R.string.reject_confirm));
    }

    public void approve(View view) {
        final Activity activity = this;
        TextView txt = Util.showDarkOkCancelModal(this, getString(R.string.alert), getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view.getId() == R.id.modal_buttoncontainer) {
                    UPPSCache.currentSubmission.state = UPPSSubmission.State.WaitingForSync;
                    UPPSCache.currentSubmission.addAction("approved", UPPSCache.currentUserId, Calendar.getInstance().getTime());

                    saveData();
                    saveMe();

                    SyncAction syncAction = UPPSCache.createSyncAction(SyncAction.Action.SubmissionModerate, UPPSCache.currentSubmission.id);
                    syncAction.moderate_action = "approve";
                    UPPSCache.queueSyncAction(syncAction);

                    beforeExiting();

                    setResult(2, getIntent());
                    finish();
                } else if(view.getId() == R.id.modal_cancel) {
                    Util.hideOverlay(activity);
                }
            }
        });
        txt.setText(getString(R.string.confirm_approve));
    }

    public void viewMode(View view) {
        setMode(false);
        mPageIndex = 0;
        refreshPage();
    }

    public void correctMode(View view) {
        setMode(true);
        mPageIndex = 0;
        mCorrectIndex = 0;
        needsScroll = true;
        refreshPage();
    }

    public void showCorrectionModal(View view) {
        showCorrectionModal();
    }

    public void prevCorrection(View view) {
        if(mCorrectIndex == 0)
            return;

        saveData();

        mCorrectIndex--;
        String name = getCorrectionFieldName();
        mPageIndex = UPPSCache.currentSubmission.form.getInputPageIndex(name);
        needsScroll = true;

        refreshPage();
    }

    public void nextCorrection(View view) {
        if(mCorrectIndex == UPPSCache.currentSubmission.reviewdata.size() - 1) {
            finishSubmission();
            saveMe();
            return;
        }

        saveData();

        mCorrectIndex++;
        String name = getCorrectionFieldName();
        mPageIndex = UPPSCache.currentSubmission.form.getInputPageIndex(name);
        needsScroll = true;

        refreshPage();
    }

    void saveData() {
        UPPSForm form = UPPSCache.currentSubmission.form;
        if(form.pages.size() < 1)
            return;

        UPPSForm.Page currentPage = form.pages.get(mPageIndex);

        ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
        for(int i = 0; i < layout.getChildCount(); i++)
        {
            View child = layout.getChildAt(i);
            if(!(child instanceof ViewGroup) || child.getTag(R.id.submission_input_name) == null)
                continue;

            ViewGroup row = (ViewGroup)child;

            String inputName = (String)child.getTag(R.id.submission_input_name);
            UPPSForm.Input input = form.getInput(inputName);
            Object value = null;

            switch(input.getType()) {
                case Cpf:
                case Email:
                case Url:
                case Private:
                case Text:
                    value = ((EditText)row.findViewById(R.id.form_input_value)).getText().toString();
                    if(((String)value).length() < 1)
                        value = null;

                    break;
                case Age:
                case Number:
                    try {
                        value = Integer.parseInt(((EditText)row.findViewById(R.id.form_input_value)).getText().toString());
                    } catch(NumberFormatException ex) {
                        value = null;
                    }
                    break;
                case Date:
                    EditText txtDay   = (EditText)row.findViewById(R.id.form_input_day);
                    EditText txtMonth = (EditText)row.findViewById(R.id.form_input_month);
                    EditText txtYear  = (EditText)row.findViewById(R.id.form_input_year);

                    if(txtDay.getText().length() < 1 && txtMonth.getText().length() < 1 && txtYear.getText().length() < 1)
                        value = null;
                    else
                        value = txtDay.getText().toString() + "-" + txtMonth.getText().toString() + "-" + txtYear.getText().toString();
                    break;
                case Radio:
                {
                    Object obj = row.findViewById(R.id.form_input_radio_table).getTag();
                    value = ((RadioGroup)row.findViewById(R.id.form_input_radio_table).getTag()).getValue();
                    break;
                }
                case Check:
                {
                    Object obj = row.findViewById(R.id.form_input_radio_table).getTag();
                    value = ((CheckGroup)row.findViewById(R.id.form_input_radio_table).getTag()).getValues();
                    break;
                }
                case OrderedList:
                {
                    Object obj = row.findViewById(R.id.form_input_radio_table).getTag();
                    value = ((DragGroup)row.findViewById(R.id.form_input_radio_table).getTag()).getValues();
                    break;
                }
                case Select:
                {
                    Object obj = row.findViewById(R.id.form_input_value).getTag();
                    value = ((SelectGroup)obj).getValue();
                    break;
                }
                case Currency:
                {
                    Object obj = row.findViewById(R.id.form_input_value).getTag();
                    double val = ((CurrencyControl)obj).getValue();

                    if(val > 0)
                        value = val;
                    else
                        value = null;
                    break;
                }
            }

            if(value != null)
                UPPSCache.currentSubmission.formdata.put(inputName, value);
            else if(UPPSCache.currentSubmission.formdata.containsKey(inputName))
                UPPSCache.currentSubmission.formdata.remove(inputName);
        }
    }

    String getCorrectionFieldName() {
        for(int i = 0; i < UPPSCache.currentSubmission.reviewdata.size(); i++) {
            UPPSSubmission.ReviewData data = UPPSCache.currentSubmission.reviewdata.get(i);
            if(i == mCorrectIndex)
                return data.fieldName;
        }

        return null;
    }

    ViewGroup getInputView(String name) {
        ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
        for(int i = 0; i < layout.getChildCount(); i++) {
            ViewGroup row = (ViewGroup)layout.getChildAt(i);
            String inputName = (String)row.getTag(R.id.submission_input_name);
            if(inputName != null && inputName.equals(name))
                return row;
        }

        return null;
    }

    ViewGroup getInputCommentView(String name) {
        ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
        for(int i = 0; i < layout.getChildCount(); i++) {
            ViewGroup row = (ViewGroup)layout.getChildAt(i);
            String inputName = (String)row.getTag(R.id.submission_comment_input_name);
            if(inputName != null && inputName.equals(name))
                return row;
        }

        return null;
    }

    ArrayList<String> getHiddenFields() {
        UPPSForm form = UPPSCache.currentSubmission.form;
        ArrayList<String> hiddenFields = new ArrayList<String>();

        for(UPPSForm.Page page : form.pages) {
            for(UPPSForm.Input input : page.inputs) {
                ViewGroup inputView = getInputView(input.name);
                if(inputView == null) // Not on this page
                    continue;

                for(UPPSForm.InputAction action : input.actions) {
                    Object[] conditions = action.when;
                    boolean allConditionsMet = true;

                    for(Object condition : conditions) {
                        switch (input.getType()) {
                            case Radio:
                            {
                                TableLayout table = (TableLayout)inputView.findViewById(R.id.form_input_radio_table);
                                RadioGroup group = (RadioGroup)table.getTag();

                                allConditionsMet = group.getValue() != null && group.getValue().length > 0 && group.getValue()[0].equals(condition);
                                break;
                            }
                            case Check:
                            {
                                TableLayout table = (TableLayout)inputView.findViewById(R.id.form_input_radio_table);
                                CheckGroup group = (CheckGroup)table.getTag();

                                allConditionsMet = group.hasValue(condition);
                                break;
                            }
                            case Select:
                            {
                                TextView txt = (TextView)inputView.findViewById(R.id.form_input_value);
                                SelectGroup group = (SelectGroup)txt.getTag();

                                allConditionsMet = group.getValue() != null && group.getValue().length > 0 && group.getValue()[0].equals(condition);
                                break;
                            }
                            default:
                                allConditionsMet = false;
                                break;
                        }

                        if(!allConditionsMet)
                            break;
                    }

                    if(!allConditionsMet)
                        continue;

                    for(String disable : action.disable)
                        hiddenFields.add(disable);
                }
            }
        }

        return hiddenFields;
    }

    void checkActions() {
        ArrayList<String> hiddenFields = getHiddenFields();

        ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
        for(int i = 0; i < layout.getChildCount(); i++) {
            ViewGroup row = (ViewGroup)layout.getChildAt(i);
            row.setVisibility(View.VISIBLE);
        }

        for(String hidden : hiddenFields) {
            ViewGroup inputView = getInputView(hidden);
            if(inputView != null)
                inputView.setVisibility(View.GONE);
        }
    }

    public void refreshPage() {
        UPPSForm form = UPPSCache.currentSubmission.form;
        if(mPageIndex < 0 || mPageIndex >= form.pages.size()) {
            finish();
            return;
        }

        UPPSForm.Page currentPage = form.pages.get(mPageIndex);

        if(correctionMode) {
            View btnBack = findViewById(R.id.submission_useractions_nav_prev);
            View btnNext = findViewById(R.id.submission_useractions_nav_next);
            View btnSend = findViewById(R.id.submission_useractions_nav_send);

            btnBack.setVisibility(mCorrectIndex > 0 ? View.VISIBLE : View.GONE);
            btnNext.setVisibility(mCorrectIndex + 1 < UPPSCache.currentSubmission.reviewdata.size() ? View.VISIBLE : View.GONE);
            btnSend.setVisibility(mCorrectIndex + 1 < UPPSCache.currentSubmission.reviewdata.size() ? View.GONE : View.VISIBLE);
        }

        Collections.sort(currentPage.inputs, new FormInputComparator());

        View progress = findViewById(R.id.form_progress);
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams((int)Math.ceil(((float)mPageIndex / (float)form.pages.size()) * findViewById(R.id.form_progress_container).getMeasuredWidth()), 5);
        progress.setLayoutParams(lparams);

        RoundIndicator indicator = (RoundIndicator)findViewById(R.id.form_indicator);
        int percent = Math.round(((float)mPageIndex / (float)form.pages.size()) * (float)100);
        indicator.setText(percent + "%");

        TextView txtPageTitle = (TextView)findViewById(R.id.form_page_title);
        txtPageTitle.setText((mPageIndex + 1) + " " + currentPage.title);

        ViewGroup layout = (ViewGroup)findViewById(R.id.form_fill_table);
        layout.removeAllViews();

        View btnBack = findViewById(R.id.prev_page);
        View btnNext = findViewById(R.id.next_page);
        TextView txtNext = (TextView)findViewById(R.id.submission_next_text);

        btnBack.setVisibility(mPageIndex > 0 ? View.VISIBLE : View.INVISIBLE);
        txtNext.setText(getResources().getString(R.string.next_page));

        if(UPPSCache.currentSubmission.state != UPPSSubmission.State.NotSent && UPPSCache.currentSubmission.state != UPPSSubmission.State.Rescheduled)
            btnNext.setVisibility(mPageIndex < form.pages.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        else if(mPageIndex == form.pages.size() - 1)
            txtNext.setText(getResources().getString(R.string.submission_send));

        if(mValidations.size() > 0) {
            RelativeLayout warning = new RelativeLayout(this);
            getLayoutInflater().inflate(R.layout.form_fill_warning, warning, true);

            layout.addView(warning);
        }

        int i = 0;
        int verificationI = 0;
        for(UPPSForm.Input input : currentPage.inputs) {
            if(input.readonly)
                continue;

            boolean disabled = UPPSCache.currentSubmission.state != UPPSSubmission.State.NotSent && UPPSCache.currentSubmission.state != UPPSSubmission.State.Rescheduled;

            if(UPPSCache.currentUser.isCoordinator())
                disabled = true;

            UPPSSubmission.ReviewData reviewData = UPPSCache.currentSubmission.getReviewData(input.name);
            if(UPPSCache.currentUser.isCoordinator() && UPPSCache.currentSubmission.state == UPPSSubmission.State.WaitingForApproval && reviewData != null) {
                FrameLayout comment = new FrameLayout(this);
                getLayoutInflater().inflate(R.layout.form_fill_comment_coordinator, comment);

                TextView txtComment = (TextView)comment.findViewById(R.id.form_fill_comment);
                View btnEdit = comment.findViewById(R.id.form_fill_editcomment);
                View btnRemove = comment.findViewById(R.id.form_fill_removecomment);

                txtComment.setText(UPPSCache.currentUser.name + ": " + reviewData.message);
                btnEdit.setOnClickListener(this);
                btnRemove.setOnClickListener(this);

                btnEdit.setTag(input.name);
                btnRemove.setTag(input.name);
                layout.addView(comment);
            }

            if(UPPSCache.currentSubmission.state == UPPSSubmission.State.Rejected && !UPPSCache.currentUser.isCoordinator()) {
                disabled = !(correctionMode && input.name.equals(getCorrectionFieldName()));

                if(!disabled && reviewData != null) {
                    FrameLayout comment = new FrameLayout(this);
                    getLayoutInflater().inflate(R.layout.form_fill_comment, comment);
                    TextView txtComment = (TextView)comment.findViewById(R.id.form_fill_comment);
                    txtComment.setText(UPPSCache.getUser(reviewData.userId).name + ": " + reviewData.message);

                    comment.setTag(R.id.submission_comment_input_name, input.name);
                    layout.addView(comment);
                }
            } else if(UPPSCache.currentUser.isCoordinator() && UPPSCache.currentSubmission.state != UPPSSubmission.State.WaitingForApproval && reviewData != null) {
                FrameLayout comment = new FrameLayout(this);
                getLayoutInflater().inflate(R.layout.form_fill_comment, comment);
                TextView txtComment = (TextView)comment.findViewById(R.id.form_fill_comment);
                txtComment.setText(UPPSCache.getUser(reviewData.userId).name + ": " + reviewData.message);

                layout.addView(comment);
            }

            FrameLayout row;
            if(mViewCache.containsKey(input.name)) {
                row = (FrameLayout)mViewCache.get(input.name);
            } else {
                row = new FrameLayout(this);

                row.setTag(R.id.submission_input_name, input.name);

                Object savedValue = UPPSCache.currentSubmission.formdata.get(input.name);

                int layoutId = 0;
                switch (input.getType()) {
                    case Text:
                    case Email:
                    case Url:
                        layoutId = R.layout.form_fill_text;
                        break;
                    case Age:
                        layoutId = R.layout.form_fill_age;
                        break;
                    case Date:
                        layoutId = R.layout.form_fill_date;
                        break;
                    case Radio:
                        layoutId = R.layout.form_fill_radio;
                        break;
                    case Cpf:
                        layoutId = R.layout.form_fill_cpf;
                        break;
                    case Number:
                        layoutId = R.layout.form_fill_number;
                        break;
                    case OrderedList:
                        layoutId = R.layout.form_fill_radio;
                        break;
                    case Check:
                        layoutId = R.layout.form_fill_radio;
                        break;
                    case Private:
                        layoutId = R.layout.form_fill_text_private;
                        break;
                    case Select:
                        layoutId = R.layout.form_fill_select;
                        break;
                    case Label:
                        layoutId = R.layout.form_fill_label;
                        break;
                    case Currency:
                        layoutId = R.layout.form_fill_currency;
                        break;
                }

                getLayoutInflater().inflate(layoutId, row);

                TextView txtNumber = (TextView)row.findViewById(R.id.form_input_number);
                TextView txtLabel = (TextView)row.findViewById(R.id.form_input_label);
                TextView txtHint = (TextView)row.findViewById(R.id.form_input_hint);
                View baloon = row.findViewById(R.id.form_input_baloon);

                // Restore saved data
                    switch (input.getType()) {
                        case Text:
                        case Cpf:
                        case Email:
                        case Url:
                        case Private:
                        {
                            final EditText txtValue = (EditText)row.findViewById(R.id.form_input_value);
                            txtValue.setText(savedValue != null ? (String)savedValue : "");

                            if(input.getType() == UPPSForm.InputType.Private) {
                                final View button = row.findViewById(R.id.form_input_private_button);
                                button.setTag(row);
                                button.setOnClickListener(this);

                                EditText txt = (EditText)row.findViewById(R.id.form_input_value);
                                View mask = row.findViewById(R.id.form_input_private_mask);
                                if(txt.getText().length() > 0 && !UPPSCache.currentUser.isCoordinator()) {
                                    txt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                    txt.setEnabled(false);
                                    mask.setVisibility(View.VISIBLE);
                                    button.setVisibility(View.GONE);
                                } else if(UPPSCache.currentUser.isCoordinator()) {
                                    button.setVisibility(View.GONE);
                                }
                            }
                            break;
                        }
                        case Age:
                        case Number:
                        {
                            EditText txtValue = (EditText)row.findViewById(R.id.form_input_value);
                            txtValue.setText(savedValue != null ? Integer.toString((Integer)savedValue) : "");
                            break;
                        }
                        case Date:
                        {
                            String date = (String)savedValue;
                            if(date != null) {
                                String chunks[] = date.split("-");

                                EditText txtDay   = (EditText)row.findViewById(R.id.form_input_day);
                                EditText txtMonth = (EditText)row.findViewById(R.id.form_input_month);
                                EditText txtYear  = (EditText)row.findViewById(R.id.form_input_year);

                                txtDay.setText(chunks[0]);
                                txtMonth.setText(chunks[1]);
                                txtYear.setText(chunks[2]);
                            }

                            View mask = row.findViewById(R.id.form_input_date_mask);
                            mask.setTag(row);
                            mask.setOnClickListener(this);
                            break;
                        }
                        case Radio:
                        {
                            TableLayout table = (TableLayout)row.findViewById(R.id.form_input_radio_table);
                            RadioGroup group = new RadioGroup(table, input.layout == UPPSForm.InputLayout.MultipleColumns ? 2 : 1);
                            UPPSForm.RadioInput radioInput = (UPPSForm.RadioInput)input;
                            for(UPPSForm.RadioOption option : radioInput.options) {
                                group.addItem(option.label, option.value);
                            }

                            group.setValue((Object[])savedValue);

                            group.setOnValueChangeListener(this);

                            table.setTag(group);
                            break;
                        }
                        case OrderedList:
                        {
                            UPPSForm.OrderedListInput orderedList = (UPPSForm.OrderedListInput)input;
                            TableLayout table = (TableLayout)row.findViewById(R.id.form_input_radio_table);
                            DragGroup group = new DragGroup(table);

                            for(UPPSForm.OrderedListOption option : orderedList.options) {
                                group.addItem(option.label, option.value);
                            }

                            if(savedValue != null)
                                group.setValues((Object[])savedValue);

                            table.setTag(group);
                            break;
                        }
                        case Check:
                        {
                            TableLayout table = (TableLayout)row.findViewById(R.id.form_input_radio_table);
                            CheckGroup group = new CheckGroup(table, input.layout == UPPSForm.InputLayout.MultipleColumns ? 2 : 1);

                            UPPSForm.CheckInput checkInput = (UPPSForm.CheckInput)input;
                            for(UPPSForm.CheckOption option : checkInput.options) {
                                group.addItem(option.label, option.value);
                            }

                            if(savedValue != null)
                                group.setValues((Object[])savedValue);

                            group.setOnValueChangeListener(this);

                            table.setTag(group);
                            break;
                        }
                        case Select:
                        {
                            TextView txt = (TextView)row.findViewById(R.id.form_input_value);
                            SelectGroup group = new SelectGroup(txt);

                            UPPSForm.SelectInput checkInput = (UPPSForm.SelectInput)input;
                            for(UPPSForm.SelectOption option : checkInput.options) {
                                group.addItem(option.label, option.value);
                            }

                            if(savedValue != null)
                                group.setValue((Object[])savedValue);

                            group.setOnValueChangeListener(this);

                            txt.setTag(group);
                            break;
                        }
                        case Currency:
                        {
                            EditText txt = (EditText)row.findViewById(R.id.form_input_value);

                            CurrencyControl control = new CurrencyControl(txt);

                            if(savedValue != null)
                                control.setValue(savedValue instanceof Double ? (Double)savedValue : (Integer)savedValue);

                            txt.setTag(control);
                        }
                    }

                txtNumber.setText((mPageIndex + 1) + "." + (i + 1));
                txtLabel.setText(input.label);
                if(input.hint != null) {
                    txtHint.setText(input.hint);
                    txtHint.setVisibility(View.VISIBLE);
                } else {
                    txtHint.setVisibility(View.INVISIBLE);
                }

                if(UPPSCache.currentUser.isCoordinator() && UPPSCache.currentSubmission.state == UPPSSubmission.State.WaitingForApproval && reviewData == null) {
                    baloon.setVisibility(View.VISIBLE);
                    baloon.setTag(input.name);
                    baloon.setOnClickListener(this);
                } else if(!UPPSCache.currentUser.isCoordinator() && UPPSCache.currentSubmission.state == UPPSSubmission.State.Rejected && reviewData != null && !correctionMode) {
                    baloon.setVisibility(View.VISIBLE);
                    ((ImageView)baloon).setImageDrawable(getResources().getDrawable(R.drawable.modovisualizacao_icon_correcao));
                } else
                    baloon.setVisibility(View.INVISIBLE);
            }

            View maskDisabled = row.findViewById(R.id.form_input_disabledmask);
            // Gambiarra para versões que não possuem View::setAlpha
            if(disabled)
            {
                maskDisabled.setVisibility(View.VISIBLE);
                android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0.5F, 0.5F);
                alpha.setDuration(0); // Make animation instant
                alpha.setFillAfter(true); // Tell it to persist after the animation ends
                maskDisabled.startAnimation(alpha);
            }
            else
            {
                android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0F, 0F);
                alpha.setDuration(0); // Make animation instant
                alpha.setFillAfter(true); // Tell it to persist after the animation ends
                maskDisabled.startAnimation(alpha);
                maskDisabled.setVisibility(View.GONE);
            }

            layout.addView(row);
            int top = row.getHeight();

            if(mValidations.containsKey(input.name)) {
                LinearLayout validationRow = new LinearLayout(this);
                getLayoutInflater().inflate(R.layout.form_fill_error, validationRow);

                TextView txtError = (TextView)validationRow.findViewById(R.id.form_input_label);
                txtError.setText(getValidationText(mValidations.get(input.name), input));
                layout.addView(validationRow);
            }

            i++;
        }

        FrameLayout spacer = new FrameLayout(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 20));
        layout.addView(spacer);

        checkActions();
    }

    String getValidationText(Validation validation, UPPSForm.Input input) {
        Object value = UPPSCache.currentSubmission.formdata.get(input.name);
        if(validation == Validation.Required)
            return getResources().getString(R.string.validation_required);
        else if(validation == Validation.OutOfRange && input.getType() == UPPSForm.InputType.Number)
            return getResources().getString(R.string.validation_range_number);
        else if(validation == Validation.OutOfRange)
            return getResources().getString(R.string.validation_range_text);

        return "Erro";
    }

    private int getRelativeTop(View myView) {
        if (myView.getParent() == findViewById(R.id.fillform_scroll))
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View)myView.getParent());
    }

    @Override
    public void onBackPressed() {
        if(Util.isOverlayVisible(this))
            return;
        saveData();
        saveMe();
        this.setResult(1, getIntent());
        this.finish();
    }
}
