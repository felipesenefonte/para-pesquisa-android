package com.lfdb.parapesquisa.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lfdb.parapesquisa.InvalidDateException;
import com.lfdb.parapesquisa.R;
import com.lfdb.parapesquisa.Util;
import com.lfdb.parapesquisa.api.SyncAction;
import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSSubmission;
import com.lfdb.parapesquisa.api.UPPSUser;

import java.util.ArrayList;

/**
 * Created by igorlira on 05/10/13.
 */
public class SwapSubmissionsWindow extends Window {
    Activity activity;
    int mSource;
    int mDest;
    String mState;
    boolean mAll;
    ArrayList<Integer> mSubmissions = new ArrayList<Integer>();

    public SwapSubmissionsWindow(Activity activity) {
        super(activity, 0);
        this.activity = activity;

        init();
    }

    void init() {
        LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createDescriptionLabel(R.string.submissionswap_selectsource));

        for(int userId : UPPSCache.currentForm.users) {
            UPPSUser user = UPPSCache.getUser(userId);

            layout.addView(createListItem(user.name, "swap_source", user.id, false));
        }

        pushView(layout, activity.getString(R.string.submissionswap_title));
    }

    View createListItem(String text, String context, Object value, boolean check) {
        LinearLayout row = new LinearLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.modal_list_item, row, true);

        TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);
        txtLabel.setText(text);
        txtLabel.setTag(R.id.listitem_context, context);
        txtLabel.setTag(R.id.listitem_value, value);
        txtLabel.setOnClickListener(this);

        ImageView checkImg = (ImageView)row.findViewById(R.id.modal_list_item_check);
        checkImg.setVisibility(check ? View.VISIBLE : View.GONE);
        checkImg.setTag(false);

        return row;
    }

    View createDescriptionLabel(int text) {
        return createDescriptionLabel(activity.getString(text));
    }

    View createDescriptionLabel(String text) {
        LinearLayout row = new LinearLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.modal_list_item, row, true);

        FontTextView txtLabel = (FontTextView)row.findViewById(R.id.modal_list_item_label);
        txtLabel.setText(text);
        txtLabel.setWeight(700);

        return row;
    }

    ViewGroup createStep2() {
        UPPSUser sourceUser = UPPSCache.getUser(mSource);

        LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        String text_all = activity.getString(R.string.submissionswap_type_all) + " (" + (UPPSCache.getSubmissionCountByState(mSource, UPPSSubmission.State.New) + UPPSCache.getSubmissionCountByState(mSource, UPPSSubmission.State.Rescheduled) + UPPSCache.getSubmissionCountByState(mSource, UPPSSubmission.State.Rejected)) + ")";
        String text_new = activity.getString(R.string.submissionswap_type_new) + " (" + UPPSCache.getSubmissionCountByState(mSource, UPPSSubmission.State.New) + ")";
        String text_rescheduled = activity.getString(R.string.submissionswap_type_rescheduled) + " (" + UPPSCache.getSubmissionCountByState(mSource, UPPSSubmission.State.Rescheduled) + ")";
        String text_rejected = activity.getString(R.string.submissionswap_type_rejected) + " (" + UPPSCache.getSubmissionCountByState(mSource, UPPSSubmission.State.Rejected) + ")";

        layout.addView(createDescriptionLabel(activity.getString(R.string.submissionswap_selecttype).replace("{name}", sourceUser.name)));
        layout.addView(createListItem(text_all, "swap_state", "all", false));
        layout.addView(createListItem(text_new, "swap_state", "new", false));
        layout.addView(createListItem(text_rescheduled, "swap_state", "rescheduled", false));
        layout.addView(createListItem(text_rejected, "swap_state", "rejected", false));

        return layout;
    }

    ViewGroup createStep3() {
        UPPSSubmission.State state = UPPSSubmission.State.Invalid;
        if(mState.equals("new"))
            state = UPPSSubmission.State.New;
        else if(mState.equals("rescheduled"))
            state = UPPSSubmission.State.Rescheduled;
        else if(mState.equals("rejected"))
            state = UPPSSubmission.State.Rejected;

        LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createListItem(activity.getString(R.string.submissionswap_transferall) + " (" + UPPSCache.getSubmissionCountByState(mSource, state) + ")", "swap_submissions", "all", false));
        for(int i = 0; i < UPPSCache.getFormCount(); i++) {
            for(int j = 0; j < UPPSCache.getFormAt(i).submissions.size(); j++) {
                UPPSSubmission submission = UPPSCache.getFormAt(i).submissions.get(j);
                if(submission.state != state || submission.getUserId() != mSource)
                    continue;

                View row = createListItem(submission.getTitle(activity), "swap_submission", submission.id, true);
                row.setTag("swap_submission_container");
                layout.addView(row);
            }
        }

        return layout;
    }

    ViewGroup createStep4() {
        UPPSUser sourceUser = UPPSCache.getUser(mSource);

        LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(createDescriptionLabel(activity.getString(R.string.submissionswap_selectdest).replace("{name}", sourceUser.name)));

        for(int userId : UPPSCache.currentForm.users) {
            UPPSUser user = UPPSCache.getUser(userId);
            if(user.id == mSource)
                continue;

            layout.addView(createListItem(user.name, "swap_dest", user.id, false));
        }

        return layout;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.modal_list_item_label) {
            if("swap_source".equals(view.getTag(R.id.listitem_context))) {
                mSource = (Integer)view.getTag(R.id.listitem_value);
                pushView(createStep2(), activity.getString(R.string.submissionswap_title));
            } else if("swap_state".equals(view.getTag(R.id.listitem_context))) {
                mState = (String)view.getTag(R.id.listitem_value);
                if(mState.equals("all"))
                    pushView(createStep4(), activity.getString(R.string.submissionswap_title));
                else
                    pushView(createStep3(), activity.getString(R.string.submissionswap_title));
            } else if("swap_submission".equals(view.getTag(R.id.listitem_context))) {
                ViewGroup parent = (ViewGroup)view.getParent();
                ImageView checkImage = (ImageView)parent.findViewById(R.id.modal_list_item_check);

                boolean selected = (Boolean)checkImage.getTag();
                selected = !selected;

                checkImage.setImageDrawable(activity.getResources().getDrawable(selected ? R.drawable.checkitem_selecionado : R.drawable.checkitem));
                checkImage.setTag(selected);

                if(step3_hasAnySelectedItem())
                    showButton();
                else
                    hideButton();
            } else if("swap_submissions".equals(view.getTag(R.id.listitem_context))) {
                if("all".equals(view.getTag(R.id.listitem_value))) {
                    mAll = true;
                    pushView(createStep4(), activity.getString(R.string.submissionswap_title));
                }
            } else if("swap_dest".equals(view.getTag(R.id.listitem_context))) {
                mDest = (Integer)view.getTag(R.id.listitem_value);
                finish();
            }
        } else if(view.getId() == R.id.modal_buttoncontainer) {
            mSubmissions = step3_getSelectedItems();
            mAll = false;
            hideButton();
            pushView(createStep4(), activity.getString(R.string.submissionswap_title));
        } else
            super.onClick(view);
    }

    void finish() {
        Util.hideOverlay(activity);

        ArrayList<Integer> transferSubmissions = new ArrayList<Integer>();

        UPPSSubmission.State state = UPPSSubmission.State.Invalid;
        if(mState.equals("new"))
            state = UPPSSubmission.State.New;
        else if(mState.equals("rescheduled"))
            state = UPPSSubmission.State.Rescheduled;
        else if(mState.equals("rejected"))
            state = UPPSSubmission.State.Rejected;

        for(int i = 0; i < UPPSCache.getFormCount(); i++) {
            for(UPPSSubmission submission : UPPSCache.getFormAt(i).submissions) {
                if(submission.getUserId() != mSource)
                    continue;

                if(mState.equals("all"))
                    transferSubmissions.add(submission.id);
                else if(submission.state == state) {
                    if(mAll)
                        transferSubmissions.add(submission.id);
                    else if(mSubmissions.contains(submission.id))
                        transferSubmissions.add(submission.id);
                }
            }
        }

        SyncAction action = UPPSCache.createSyncAction(SyncAction.Action.SubmissionTransfer, 0);
        action.transfer_dest = mDest;
        action.transfer_ids = transferSubmissions;
        UPPSCache.queueSyncAction(action);

        UPPSCache.sync();
    }

    @Override
    void onBack() {
        hideButton();
    }

    boolean step3_hasAnySelectedItem() {
        ViewGroup currentViewController = (ViewGroup)mViewHistory.get(viewIndex);

        for(int i = 0; i < currentViewController.getChildCount(); i++) {
            View child = currentViewController.getChildAt(i);
            if(!"swap_submission_container".equals(child.getTag()))
                continue;

            ImageView checkImage = (ImageView)child.findViewById(R.id.modal_list_item_check);
            if((Boolean)checkImage.getTag() == true)
                return true;
        }

        return false;
    }

    ArrayList<Integer> step3_getSelectedItems() {
        ViewGroup currentViewController = (ViewGroup)mViewHistory.get(viewIndex);
        ArrayList<Integer> result = new ArrayList<Integer>();

        for(int i = 0; i < currentViewController.getChildCount(); i++) {
            View child = currentViewController.getChildAt(i);
            if(!"swap_submission_container".equals(child.getTag()))
                continue;

            ImageView checkImage = (ImageView)child.findViewById(R.id.modal_list_item_check);
            View label = child.findViewById(R.id.modal_list_item_label);

            if((Boolean)checkImage.getTag() == true)
                result.add((Integer)label.getTag(R.id.listitem_value));
        }

        return result;
    }
}
