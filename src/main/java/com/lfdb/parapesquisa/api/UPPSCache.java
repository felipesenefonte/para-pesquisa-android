package com.lfdb.parapesquisa.api;

import android.os.Debug;

import com.lfdb.parapesquisa.DateParser;
import com.lfdb.parapesquisa.NotificationCenter;
import com.lfdb.parapesquisa.ParaPesquisa;
import com.lfdb.parapesquisa.R;
import com.lfdb.parapesquisa.api.callbacks.CorrectSubmissionCallback;
import com.lfdb.parapesquisa.api.callbacks.FormInfoCallback;
import com.lfdb.parapesquisa.api.callbacks.FormsCallback;
import com.lfdb.parapesquisa.api.callbacks.ModerateSubmissionCallback;
import com.lfdb.parapesquisa.api.callbacks.NewFormsCallback;
import com.lfdb.parapesquisa.api.callbacks.NewSubmissionsCallback;
import com.lfdb.parapesquisa.api.callbacks.PublishSubmissionCallback;
import com.lfdb.parapesquisa.api.callbacks.RescheduleSubmissionCallback;
import com.lfdb.parapesquisa.api.callbacks.SubmissionsCallback;
import com.lfdb.parapesquisa.api.callbacks.SyncResultCallback;
import com.lfdb.parapesquisa.api.callbacks.SyncStartedCallback;
import com.lfdb.parapesquisa.api.callbacks.TextsCallback;
import com.lfdb.parapesquisa.api.callbacks.TransferSubmissionsCallback;
import com.lfdb.parapesquisa.api.callbacks.UpdateSubmissionCallback;
import com.lfdb.parapesquisa.api.callbacks.UserInfoCallback;
import com.lfdb.parapesquisa.api.callbacks.UsersCallback;
import com.lfdb.parapesquisa.api.descriptors.FormInfo;
import com.lfdb.parapesquisa.api.descriptors.FormsResponse;
import com.lfdb.parapesquisa.api.descriptors.SubmissionInfo;
import com.lfdb.parapesquisa.api.descriptors.TextsResponse;
import com.lfdb.parapesquisa.api.descriptors.UserInfo;
import com.lfdb.parapesquisa.api.descriptors.UsersResponse;
import com.lfdb.parapesquisa.storage.UPPSStorage;
import com.lfdb.parapesquisa.util.SentrySender;

import org.acra.ACRA;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;

/**
 * Created by Igor on 7/30/13.
 */
public class UPPSCache implements UPPSCallback {
    static ArrayList<SyncAction> sSyncQueue = new ArrayList<SyncAction>();
    static ArrayList<UPPSForm> sForms = new ArrayList<UPPSForm>();
    static ArrayList<UPPSUser> sUsers = new ArrayList<UPPSUser>();
    static ArrayList<UPPSNotification> sNotifications = new ArrayList<UPPSNotification>();
    static ArrayList<UPPSText> sTexts = new ArrayList<UPPSText>();
    public static UPPSForm currentForm;
    public static UPPSSubmission currentSubmission;
    public static UPPSUser currentUser;
    public static int currentUserId;
    public static String currentToken;
    public static UPPSText currentText;

    public static boolean isSyncing = false;
    static int sync_requestedActions = 0;
    static int sync_doneActions = 0;
    static boolean sync_requestedUsers = false;
    static boolean sync_isSecondStep = false;

    public static UPPSCache sInstance = new UPPSCache();
    static boolean isSaving = false;
    static boolean isLoading = false;

    public static void init() {
        sSyncQueue.clear();
        sTexts.clear();
        sForms.clear();
        sUsers.clear();
        sNotifications.clear();
        currentSubmission = null;
        currentUser = null;
        currentUserId = 0;
        isSyncing = false;
        sync_requestedActions = 0;
        sync_doneActions = 0;
        sync_requestedUsers = false;
        sync_isSecondStep = false;

        load();
    }

    public static boolean hasSyncActionRelatedTo(int resourceId) {
        for(SyncAction action : sSyncQueue) {
            if(action.resourceId == resourceId)
                return true;
        }
        return false;
    }

    static void sync_secondStep() {
        sync_isSecondStep = true;

        for(UPPSUser user : sUsers) {
            // Request data, download if etag is newer
            UPPSServer.getActiveServer().requestUserInfo(user.id, user.etag);
            sync_requestedActions++;
        }

        UPPSServer.getActiveServer().requestSubmissions(UPPSCache.currentUserId);
        sync_requestedActions++;

        if(UPPSCache.currentUser.isCoordinator()) {
            UPPSServer.getActiveServer().requestUsers(UPPSCache.currentUserId);
            sync_requestedActions++;
        }
    }

    public static void sync() {
        UPPSServer.getActiveServer().postCallback(SyncStartedCallback.k_iCallback, new SyncStartedCallback());

        sync_requestedUsers = false;
        sync_requestedActions = 0;
        sync_doneActions = 0;
        sync_isSecondStep = false;

        isSyncing = true;

        for(SyncAction action : sSyncQueue) {
            switch (action.action) {
                case SubmissionPublish:
                {
                    UPPSSubmission submission = getSubmission(action.resourceId);
                    if(submission != null && (submission.id & 0x80000000) == 0x80000000) {
                        UPPSServer.getActiveServer().publishSubmission(submission);
                        sync_requestedActions++;
                    }
                    break;
                }
                case SubmissionUpdate:
                {
                    UPPSSubmission submission = getSubmission(action.resourceId);
                    if(submission != null) {
                        UPPSServer.getActiveServer().updateSubmission(submission, action.update_newState, action.date);
                        sync_requestedActions++;
                    }
                    break;
                }
                case SubmissionCorrect:
                {
                    UPPSSubmission submission = getSubmission(action.resourceId);
                    if(submission != null) {
                        UPPSServer.getActiveServer().correctSubmission(submission, action.date);
                        sync_requestedActions++;
                    }
                    break;
                }
                case SubmissionReschedule:
                {
                    UPPSSubmission submission = getSubmission(action.resourceId);
                    if(submission != null) {
                        UPPSServer.getActiveServer().rescheduleSubmission(submission);
                        sync_requestedActions++;
                    }
                    break;
                }
                case SubmissionModerate:
                {
                    UPPSSubmission submission = getSubmission(action.resourceId);
                    if(submission != null) {
                        UPPSServer.getActiveServer().moderateSubmission(submission, action.moderate_action, action.date);
                        sync_requestedActions++;
                    }
                    break;
                }
                case SubmissionTransfer:
                {
                    UPPSServer.getActiveServer().transferSubmissions(action.transfer_dest, action.transfer_ids, action.date);
                    sync_requestedActions++;
                    break;
                }
            }
        }

        UPPSServer.getActiveServer().requestForms(UPPSCache.currentUserId);
        sync_requestedActions++;
    }

    public static UPPSSubmission getSubmission(int id) {
        for(UPPSForm form : sForms) {
            if(form.getSubmission(id) != null)
                return form.getSubmission(id);
        }
        return null;
    }

    public static int getSubmissionCount() {
        int count = 0;

        for(UPPSForm form : sForms) {
            count += form.submissions.size();
        }
        return count;
    }

    public static int getSubmissionCount(int userId) {
        int count = 0;

        for(UPPSForm form : sForms) {
            for(UPPSSubmission submission : form.submissions)
                if(submission.getUserId() == userId)
                    count++;
        }
        return count;
    }

    public static int getSubmissionCountByState(int userId, UPPSSubmission.State state) {
        int count = 0;

        for(UPPSForm form : sForms) {
            count += form.getStateCount(userId, state);
        }
        return count;
    }

    public static SyncAction createSyncAction(SyncAction.Action action, int resourceId) {
        SyncAction a = new SyncAction();
        a.action = action;
        a.resourceId = resourceId;
        a.date = Calendar.getInstance().getTime();

        return a;
    }

    public static void queueSyncAction(SyncAction a) {
        sSyncQueue.add(a);
        UPPSStorage.addSyncQueueAction(a);
    }

    public static SyncAction queueSyncAction(SyncAction.Action action, int resourceId) {
        SyncAction a = new SyncAction();
        a.action = action;
        a.resourceId = resourceId;
        a.date = Calendar.getInstance().getTime();

        sSyncQueue.add(a);
        UPPSStorage.addSyncQueueAction(a);

        return a;
    }

    static void syncFail() {
        if(!isSyncing)
            return;

        ACRA.getErrorReporter().handleException(new Exception("Sync Failed"));

        isSyncing = false;

        SyncResultCallback callback = new SyncResultCallback();
        callback.result = EResult.Fail;
        UPPSServer.getActiveServer().postCallback(callback.k_iCallback, callback);
    }

    static void refreshSync() {
        if(sync_requestedActions == sync_doneActions) {
            if(sync_isSecondStep) {
                isSyncing = false;
                sync_isSecondStep = false;
                SyncResultCallback callback = new SyncResultCallback();
                callback.result = EResult.OK;
                UPPSServer.getActiveServer().postCallback(callback.k_iCallback, callback);
            }
            else {
                if(areAllUsersLoaded()) {
                    UPPSStorage.clearSyncQueue();
                    sSyncQueue.clear();

                    sync_secondStep();
                } else if(!sync_requestedUsers) {
                    sync_requestNotLoadedUsers();
                    sync_requestedUsers = true;
                }
            }
        }
    }

    @Override
    public void onCallback(int iCallback, Object pParam) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        if(iCallback == TextsCallback.k_iCallback) {
            TextsCallback result = (TextsCallback)pParam;
            if(result.result == EResult.OK) {
                for(TextsResponse.Text text : result.texts) {
                    UPPSText t = new UPPSText();
                    t.title = text.title;
                    t.content = text.content;
                    t.description = text.subtitle;

                    sTexts.add(t);
                }
            }
        }

        if(!isSyncing)
            return;

        try {

            if(iCallback == UserInfoCallback.k_iCallback) {
            UserInfoCallback result = (UserInfoCallback)pParam;
            if (result.result == EResult.OK) {
                UPPSUser localUser = getUser(result.response.id);
                if(localUser == null) {
                    localUser = new UPPSUser(result.response);
                    addUser(localUser);
                }
                localUser.parse(result.response);
                localUser.etag = result.etag;

                UPPSStorage.removeUser(result.response.id);
                UPPSStorage.addUser(localUser);
            } else if(result.result == EResult.NotModified) {

            } else {
                syncFail();
                return;
            }

            sync_doneActions++;
            refreshSync();
        }
        else if(iCallback == UsersCallback.k_iCallback) {
            UsersCallback result = (UsersCallback)pParam;
            if(result.result == EResult.OK) {
                for(int i = 0; i < UPPSCache.getFormCount(); i++) {
                    UPPSForm form = UPPSCache.getFormAt(i);
                    form.users.clear();
                    form.quotas.clear();
                }
                for(UsersResponse.Assignment assignment : result.assignments) {
                    UserInfo userdata = assignment.user;

                    UPPSUser user;
                    if(UPPSCache.getUser(userdata.id) != null) {
                        user = UPPSCache.getUser(userdata.id);
                        user.parse(userdata);
                    } else {
                        user = new UPPSUser(userdata);
                        UPPSCache.addUser(user);
                    }

                    UPPSStorage.removeUser(user.id);
                    UPPSStorage.addUser(user);

                    UPPSForm form = UPPSCache.getForm(assignment.form_id);
                    if(form != null) {
                        form.users.add(user.id);
                        form.quotas.put(user.id, assignment.quota);
                    }
                }
                for(int i = 0; i < UPPSCache.getFormCount(); i++) {
                    UPPSForm form = UPPSCache.getFormAt(i);
                    UPPSStorage.updateForm(form);
                    //UPPSStorage.removeForm(form.id);
                    //UPPSStorage.addForm(form);
                }
            } else {
                syncFail();
                return;
            }
            sync_doneActions++;
            refreshSync();
        }
            else if(iCallback == FormsCallback.k_iCallback) {
                FormsCallback result = (FormsCallback)pParam;
                if(result.result == EResult.OK) {
                    ArrayList<Integer> formsToRemove = new ArrayList<Integer>();
                    for(int i = 0; i < getFormCount(); i++) {
                        formsToRemove.add(getFormAt(i).id);
                    }

                    for(FormsResponse.Assignment assignment : result.forms) {
                        FormInfo formdata = assignment.form;

                        if(formdata == null)
                            continue;

                        UPPSForm localForm = getForm(formdata.id);
                        if(localForm == null) {
                            localForm = new UPPSForm(formdata);
                            addForm(localForm);
                        } else {
                            formsToRemove.remove((Object)localForm.id);
                            localForm.parse(formdata);
                        }

                        if(assignment.quota > 0)
                            localForm.quota = assignment.quota;

                        UPPSStorage.updateForm(localForm);
                        //UPPSStorage.removeForm(localForm.id);
                        //UPPSStorage.addForm(localForm);
                    }

                    for(int i = 0; i < formsToRemove.size(); i++) {
                        removeForm(formsToRemove.get(i));
                        UPPSStorage.removeForm(formsToRemove.get(i));
                    }
                } else if(result.result == EResult.Fail) {
                    syncFail();
                    return;
                }

                sync_doneActions++;
                refreshSync();
            }
            else if(iCallback == NewFormsCallback.k_iCallback) {
                NewFormsCallback result = (NewFormsCallback)pParam;
                if(result.result == EResult.OK) {
                    ArrayList<Integer> formsToRemove = new ArrayList<Integer>();

                    for(LinkedHashMap assignment : result.forms) {
                        LinkedHashMap formdata = (LinkedHashMap)assignment.get("form");
                        Integer assignment_id = (Integer)assignment.get("id");
                        //Integer id = (Integer)formdata.get("id");
                        Integer quota = (Integer)assignment.get("quota");
                        Boolean $keep = (Boolean)assignment.get("$keep");
                        if($keep == null)
                            $keep = true;


                        UPPSForm localForm = getFormByAssignmentId(assignment_id);
                        int oldQuota = 0;
                        if(localForm != null)
                            oldQuota = localForm.quota;

                        if(localForm == null) {
                            if(formdata !=
                                    null) {
                                localForm = new UPPSForm(formdata);
                                localForm.assignmentId = assignment_id;
                                addForm(localForm);

                                UPPSStorage.addForm(localForm);
                            }
                        } else if($keep) {
                            if(formdata != null)
                                localForm.update(formdata);
                        } else {
                            formsToRemove.add(localForm.id);
                        }

                        if(localForm != null && quota != null)
                            localForm.quota = quota;
                        else if(localForm != null)
                            localForm.quota = oldQuota;

                        /*if(localForm != null && quota != null && quota > 0)
                            localForm.quota = quota;
                        else if(localForm != null) {
                            int oldQuota = localForm.quota;
                            localForm.quota = oldQuota;
                        }*/

                        if(localForm != null)
                            UPPSStorage.updateForm(localForm);
                    }

                    for(int i = 0; i < formsToRemove.size(); i++) {
                        removeForm(formsToRemove.get(i));
                        UPPSStorage.removeForm(formsToRemove.get(i));
                    }
                } else if(result.result == EResult.Fail) {
                    syncFail();
                    return;
                }

                sync_doneActions++;
                refreshSync();
            }
        else if(iCallback == FormInfoCallback.k_iCallback) {
            FormInfoCallback result = (FormInfoCallback)pParam;
            if (result.result == EResult.OK) {
                UPPSForm localForm = getForm(result.id);
                if(localForm == null) {
                    localForm = new UPPSForm();
                    localForm.id = result.id;
                    addForm(localForm);
                }
                if(localForm != null && localForm.startTime != null && result.pub_start != null && localForm.startTime.getTime() != DateParser.parse(result.pub_start).getTime()) {
                    NotificationCenter.postNotification(result.name, ParaPesquisa.getContext().getString(R.string.notification_description_startchange).replace("{date}", format.format(DateParser.parse(result.pub_start))));
                }
                if(localForm != null && localForm.endTime != null && result.pub_end != null && localForm.endTime.getTime() != DateParser.parse(result.pub_end).getTime()) {
                    NotificationCenter.postNotification(result.name, ParaPesquisa.getContext().getString(R.string.notification_description_endchange).replace("{date}", format.format(DateParser.parse(result.pub_end))));
                }

                localForm.etag = result.etag;

                UPPSStorage.updateForm(localForm);
                //UPPSStorage.removeForm(localForm.id);
                //UPPSStorage.addForm(localForm);
            } else if(result.result == EResult.NotModified) {

            } else if(result.result == EResult.NotFound) {
                for(UPPSForm form : sForms)
                    if(form.id == result.requested_id) {
                        sForms.remove(form);
                        break;
                    }
            } else {
                syncFail();
                return;
            }

            sync_doneActions++;
            refreshSync();
        } else if (iCallback == PublishSubmissionCallback.k_iCallback) {
            PublishSubmissionCallback result = (PublishSubmissionCallback)pParam;
            if(result.result == EResult.OK) {
                UPPSSubmission submission = null;
                for(UPPSForm form : sForms) {
                    if(submission != null)
                        break;
                    for(UPPSSubmission sub : form.submissions) {
                        if(sub.id == result.submission_id) {
                            submission = sub;
                            break;
                        }
                    }
                }

                int oldId = result.local_id;
                submission.isLocal = false;
                submission.id = result.submission_id;
                submission.state = UPPSSubmission.State.WaitingForApproval;

                NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_sent));

                UPPSStorage.updateSubmission(oldId, submission.form.id, submission);
                //UPPSStorage.removeSubmission(submission.id);
                //UPPSStorage.addSubmission(submission.form.id, submission);
                sync_doneActions++;
                refreshSync();
            } else {
                UPPSSubmission submission = getSubmission(result.local_id);
                if(submission != null)
                    NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_syncerror));

                syncFail();
                return;
            }
        } else if (iCallback == CorrectSubmissionCallback.k_iCallback) {
            CorrectSubmissionCallback result = (CorrectSubmissionCallback)pParam;
            if(result.result == EResult.OK) {
                UPPSSubmission submission = getSubmission(result.local_id);
                if(submission != null)
                    NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_coordinator_sentforcorrection));

                sync_doneActions++;
                refreshSync();
            } else {
                syncFail();
            }
        } else if (iCallback == UpdateSubmissionCallback.k_iCallback) {
            UpdateSubmissionCallback result = (UpdateSubmissionCallback)pParam;
            if(result.result == EResult.OK) {
                sync_doneActions++;
                refreshSync();
            } else {
                UPPSSubmission submission = getSubmission(result.local_id);
                if(submission != null)
                    NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_syncerror));

                syncFail();
            }
        }  else if (iCallback == RescheduleSubmissionCallback.k_iCallback) {
            RescheduleSubmissionCallback result = (RescheduleSubmissionCallback)pParam;
            if(result.result == EResult.OK) {
                UPPSSubmission submission = UPPSCache.getSubmission(result.local_id);
                if(submission != null && submission.state == UPPSSubmission.State.Rescheduled) {
                    NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_rescheduled).replace("{date}", format.format(submission.rescheduleDate)));
                } else if(submission != null && submission.state == UPPSSubmission.State.Cancelled) {
                    UPPSForm.StopReason_t reason = submission.form.getStopReason(submission.stopReason);
                    if(reason != null)
                        NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_cancelled).replace("{reason}", reason.reason));
                    else
                        NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_cancelled_maxreschedules));
                }
                sync_doneActions++;
                refreshSync();
            } else {
                UPPSSubmission submission = getSubmission(result.local_id);
                if(submission != null)
                    NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_syncerror));

                syncFail();
            }
        } else if (iCallback == ModerateSubmissionCallback.k_iCallback) {
            ModerateSubmissionCallback result = (ModerateSubmissionCallback)pParam;
            if(result.result == EResult.OK) {
                UPPSSubmission submission = getSubmission(result.local_id);
                if(submission != null) {
                    switch (submission.state) {
                        case Approved:
                        {
                            NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_coordinator_approved));
                            break;
                        }
                        case Reproved:
                        {
                            NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_coordinator_reproved));
                            break;
                        }
                    }
                }
                sync_doneActions++;
                refreshSync();
            } else {
                UPPSSubmission submission = getSubmission(result.local_id);
                if(submission != null)
                    NotificationCenter.postNotification(submission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getResources().getString(R.string.notification_description_syncerror));

                syncFail();
            }
        } else if(iCallback == SubmissionsCallback.k_iCallback) {
                SubmissionsCallback result = (SubmissionsCallback)pParam;
                if(result.result == EResult.OK) {
                    ArrayList<Integer> toRemove = new ArrayList<Integer>();
                    for(UPPSForm form : sForms) {
                        for(UPPSSubmission submission : form.submissions)
                            if(submission.state != UPPSSubmission.State.NotSent)
                                toRemove.add(submission.id);
                    }

                    for(SubmissionInfo submissiondata : result.submissions) {
                        toRemove.remove((Object)submissiondata.id);

                        if(UPPSCache.getForm(submissiondata.form_id) == null)
                            continue;

                        UPPSSubmission localSubmission = getSubmission(submissiondata.id);
                        if(localSubmission != null && (localSubmission.state == UPPSSubmission.State.NotSent || hasSyncActionRelatedTo(localSubmission.id)))
                            continue; // u can't touch this

                        UPPSSubmission.State oldState = localSubmission != null ? localSubmission.state : UPPSSubmission.State.Invalid;

                        UPPSSubmission submission = localSubmission != null ? localSubmission : new UPPSSubmission(UPPSCache.getForm(submissiondata.form_id));
                        submission.parse(submissiondata);

                        if(localSubmission != null && oldState != submission.state) {
                            postSubmissionNotifications(submission, oldState);
                        }

                        if(getSubmission(submissiondata.id) == null)
                            UPPSCache.getForm(submissiondata.form_id).submissions.add(submission);

                        if(localSubmission != null)
                            UPPSStorage.updateSubmission(localSubmission.id, submissiondata.form_id, submission);
                        else
                            UPPSStorage.addSubmission(submissiondata.form_id, submission);
                        //UPPSStorage.removeSubmission(submissiondata.id);
                        //UPPSStorage.addSubmission(submission.form.id, submission);
                    }

                    for(Integer id : toRemove) {
                        UPPSSubmission submission = getSubmission(id);
                        submission.form.submissions.remove(submission);
                        UPPSStorage.removeSubmission(id);
                    }

                    sync_requestNotLoadedUsers();

                    sync_doneActions++;
                    refreshSync();
                } else {
                    syncFail();
                }
            } else if(iCallback == NewSubmissionsCallback.k_iCallback) {
                NewSubmissionsCallback result = (NewSubmissionsCallback)pParam;
                if(result.result == EResult.OK) {
                    ArrayList<Integer> toRemove = new ArrayList<Integer>();

                    for(LinkedHashMap submissiondata : result.items) {
                        Integer id = (Integer)submissiondata.get("id");
                        Boolean keep = (Boolean)submissiondata.get("$keep");
                        if(keep == null)
                            keep = true;

                        boolean isANewSubmission = false;

                        if(!keep) {
                            toRemove.add(id);
                            continue;
                        }

                        UPPSSubmission localSubmission = getSubmission(id);
                        if(localSubmission == null) {
                            Integer form_id = (Integer)submissiondata.get("form_id");
                            if(form_id == null)
                                continue;

                            UPPSForm form = getForm(form_id);
                            if(form == null)
                                continue;

                            localSubmission = new UPPSSubmission(form);
                            isANewSubmission = true;
                        } else if(localSubmission.state == UPPSSubmission.State.NotSent || hasSyncActionRelatedTo(localSubmission.id))
                            continue; // u can't touch this

                        UPPSSubmission.State oldState = localSubmission.state;

                        localSubmission.updateData(submissiondata);

                        if(!isANewSubmission && oldState != localSubmission.state) {
                            postSubmissionNotifications(localSubmission, oldState);
                        }

                        if(isANewSubmission)
                            localSubmission.form.submissions.add(localSubmission);

                        if(!isANewSubmission)
                            UPPSStorage.updateSubmission(localSubmission.id, localSubmission.form.id, localSubmission);
                        else
                            UPPSStorage.addSubmission(localSubmission.form.id, localSubmission);
                    }

                    for(Integer id : toRemove) {
                        UPPSSubmission submission = getSubmission(id);
                        if(submission != null) {
                            submission.form.submissions.remove(submission);
                            UPPSStorage.removeSubmission(id);
                        }
                    }

                    sync_requestNotLoadedUsers();

                    sync_doneActions++;
                    refreshSync();
                } else {
                    syncFail();
                }
            } else if (iCallback == TransferSubmissionsCallback.k_iCallback) {
                TransferSubmissionsCallback result = (TransferSubmissionsCallback)pParam;
                if(result.result == EResult.OK) {
                    sync_doneActions++;
                    refreshSync();
                } else {
                    syncFail();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            syncFail();
        }
    }

    static void postSubmissionNotifications(UPPSSubmission localSubmission, UPPSSubmission.State oldState) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

        if(UPPSCache.currentUser.isCoordinator()) {
            switch (localSubmission.state) {
                case WaitingForApproval:
                {
                    UPPSUser user = getUser(localSubmission.getUserId());
                    if(user != null)
                        NotificationCenter.postNotification(localSubmission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_coordinator_waitingforcorrection).replace("{user}", user.name));
                    break;
                }
                case Rescheduled:
                {
                    UPPSForm.StopReason_t reason = localSubmission.form.getStopReason(localSubmission.stopReason);
                    if(reason != null && localSubmission.rescheduleDate != null)
                        NotificationCenter.postNotification(localSubmission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_coordinator_rescheduled).replace("{date}", format.format(localSubmission.rescheduleDate)).replace("{reason}", reason.reason));
                    break;
                }
                case Cancelled:
                {
                    UPPSForm.StopReason_t reason = localSubmission.form.getStopReason(localSubmission.stopReason);
                    if(reason == null)
                        NotificationCenter.postNotification(localSubmission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_coordinator_cancelled_maxreschedules));
                    break;
                }
            }
        } else {
            switch (localSubmission.state) {
                case Approved:
                {
                    NotificationCenter.postNotification(localSubmission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_approved));
                    break;
                }
                case Reproved:
                case New:
                {
                    NotificationCenter.postNotification(localSubmission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_reproved));
                    break;
                }
                case Rejected:
                {
                    NotificationCenter.postNotification(localSubmission.getTitle(ParaPesquisa.getContext()), ParaPesquisa.getContext().getString(R.string.notification_description_rejected));
                    break;
                }
            }
        }
    }

    static void load() {
        if(isLoading)
            return;;

        isLoading = true;

        sSyncQueue.clear();
        sTexts.clear();
        sForms.clear();
        sUsers.clear();
        sNotifications.clear();
        currentSubmission = null;
        currentUser = null;
        currentUserId = 0;
        isSyncing = false;
        sync_requestedActions = 0;
        sync_doneActions = 0;
        sync_requestedUsers = false;

        UPPSForm[] forms = UPPSStorage.getForms();
        for(UPPSForm f : forms)
            sForms.add(f);

        UPPSUser[] users = UPPSStorage.getUsers();
        for(UPPSUser f : users)
            sUsers.add(f);

        UPPSNotification[] notifications = UPPSStorage.getNotifications();
        for(UPPSNotification f : notifications)
            sNotifications.add(f);

        SyncAction[] syncActions = UPPSStorage.getSyncQueue();
        for(SyncAction s : syncActions)
            sSyncQueue.add(s);

        currentUserId = UPPSStorage.getUserId();
        currentToken = UPPSStorage.getToken();
        currentUser = getUser(currentUserId);

        isLoading = false;
    }

    public static void save() {
        if(isSaving)
            return;

        isSaving = true;

        UPPSStorage.clear();

        for(UPPSForm f : sForms)
            UPPSStorage.addForm(f);

        for(UPPSUser user : sUsers)
            UPPSStorage.addUser(user);

        for(UPPSNotification notification : sNotifications)
            UPPSStorage.addNotification(notification);

        for(SyncAction syncAction : sSyncQueue)
            UPPSStorage.addSyncQueueAction(syncAction);

        UPPSStorage.setSession(UPPSServer.getActiveServer().getHost(), currentToken, currentUserId);

        isSaving = false;
    }

    public static boolean areAllUsersLoaded() {
        for(UPPSForm form : sForms) {
            for(int user : form.users)
                if(getUser(user) == null)
                    return false;

            for(UPPSSubmission submission : form.submissions) {
                if(submission.getUserId() != 0 && getUser(submission.getUserId()) == null)
                    return false;

                for(UPPSSubmission.Action action : submission.actionLog)
                    if(action.userId != 0 && getUser(action.userId) == null)
                        return false;

                for(UPPSSubmission.ReviewData reviewData : submission.reviewdata)
                    if(getUser(reviewData.userId) == null)
                        return false;
            }
        }

        return true;
    }

    public static void requestNotLoadedUsers() {
        ArrayList<Integer> requestedUsers = new ArrayList<Integer>();

        for(UPPSForm form : sForms) {
            for(int user : form.users)
                if(getUser(user) == null && !requestedUsers.contains(user)) {
                    UPPSServer.getActiveServer().requestUserInfo(user);
                    requestedUsers.add(user);
                }

            for(UPPSSubmission submission : form.submissions) {
                if(submission.getUserId() != 0 && getUser(submission.getUserId()) == null && !requestedUsers.contains(submission.getUserId())) {
                    UPPSServer.getActiveServer().requestUserInfo(submission.getUserId());
                    requestedUsers.add(submission.getUserId());
                }

                for(UPPSSubmission.Action action : submission.actionLog)
                    if(action.userId != 0 && getUser(action.userId) == null && !requestedUsers.contains(action.userId)) {
                        UPPSServer.getActiveServer().requestUserInfo(action.userId);
                        requestedUsers.add(action.userId);
                    }

                for(UPPSSubmission.ReviewData reviewData : submission.reviewdata)
                    if(getUser(reviewData.userId) == null && !requestedUsers.contains(reviewData.userId)) {
                        UPPSServer.getActiveServer().requestUserInfo(reviewData.userId);
                        requestedUsers.add(reviewData.userId);
                    }
            }
        }
    }

    public static void sync_requestNotLoadedUsers() {
        for(UPPSForm form : sForms) {
            for(int user : form.users)
                if(getUser(user) == null) {
                    UPPSServer.getActiveServer().requestUserInfo(user);
                    sync_requestedActions++;
                }

            for(UPPSSubmission submission : form.submissions) {
                if(submission.getUserId() != 0 && getUser(submission.getUserId()) == null) {
                    UPPSServer.getActiveServer().requestUserInfo(submission.getUserId());
                    sync_requestedActions++;
                }

                for(UPPSSubmission.Action action : submission.actionLog)
                    if(action.userId != 0 && getUser(action.userId) == null) {
                        UPPSServer.getActiveServer().requestUserInfo(action.userId);
                        sync_requestedActions++;
                    }

                for(UPPSSubmission.ReviewData reviewData : submission.reviewdata)
                    if(getUser(reviewData.userId) == null) {
                        UPPSServer.getActiveServer().requestUserInfo(reviewData.userId);
                        sync_requestedActions++;
                    }
            }
        }
    }

    public static void removeForm(int id) {
        for(UPPSForm form : sForms) {
            if(form.id == id) {
                sForms.remove(form);
                return;
            }
        }
    }

    public static void addForm(UPPSForm form) {
        sForms.add(form);
    }

    public static void addUser(UPPSUser user) {
        if(getUser(user.id) != null)
            return;

        sUsers.add(user);
    }

    public static int getUserCount() {
        return  sUsers.size();
    }

    public static UPPSUser getUserAt(int index) {
        return sUsers.get(index);
    }

    public static int getFormCount() {
        return sForms.size();
    }

    public static UPPSForm getFormAt(int index) {
        return sForms.get(index);
    }

    public static UPPSForm getFormByAssignmentId(int assignment_id) {
        for(UPPSForm form : sForms) {
            if(form.assignmentId == assignment_id)
                return form;
        }

        return null;
    }

    public static UPPSForm getForm(int id) {
        for(UPPSForm form : sForms) {
            if(form.id == id)
                return form;
        }

        return null;
    }

    public static UPPSUser getUser(int id) {
        for(UPPSUser user : sUsers) {
            if(user.id == id)
                return user;
        }

        return null;
    }

    public static int getNotificationCount() {
        return sNotifications.size();
    }

    public static UPPSNotification getNotificationAt(int i) {
        return sNotifications.get(i);
    }

    public static void addNotification(UPPSNotification notification) {
        sNotifications.add(0, notification);
        UPPSStorage.addNotification(notification);
    }

    public static int getTextCount() {
        return sTexts.size();
    }

    public static UPPSText getTextAt(int index) {
        return sTexts.get(index);
    }
}
