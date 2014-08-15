package com.lfdb.parapesquisa.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfdb.parapesquisa.api.SyncAction;
import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSNotification;
import com.lfdb.parapesquisa.api.UPPSSubmission;
import com.lfdb.parapesquisa.api.UPPSUser;
import com.lfdb.parapesquisa.api.descriptors.FormInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Igor on 8/18/13.
 */
public class UPPSStorage {
    static LocalStorageOpenHelper helper;
    static SQLiteDatabase sReadableDatabase;
    static SQLiteDatabase sWritableDatabase;
    
    public static void init(android.content.Context context) {
        helper = new LocalStorageOpenHelper(context);
        sReadableDatabase = helper.getReadableDatabase();
        sWritableDatabase = helper.getWritableDatabase();
    }

    public static void clear() {
        helper.clear(sWritableDatabase);
    }

    public static void clearSyncQueue() {
        sWritableDatabase.execSQL("DELETE FROM sync_queue");
    }

    public static void markNotificationsAsRead() {
        sWritableDatabase.execSQL("UPDATE notifications SET read=1;");
    }

    public static void addSyncQueueAction(SyncAction action) {
        ContentValues cv = new ContentValues();
        cv.put("action", action.action.ordinal());
        cv.put("resource_id", action.resourceId);
        cv.put("moderate_action", action.moderate_action);
        if(action.update_newState != null)
            cv.put("update_newstate", action.update_newState.ordinal());
        else
            cv.put("update_newstate", 0);
        if(action.date != null)
            cv.put("created_at", action.date.getTime());
        else
            cv.put("created_at", 0);
        sWritableDatabase.insert("sync_queue", null, cv);
    }

    public static SyncAction[] getSyncQueue() {
        Cursor cursor = sReadableDatabase.rawQuery("SELECT action, resource_id, moderate_action, update_newstate, created_at FROM sync_queue", null);
        SyncAction result[] = new SyncAction[cursor.getCount()];
        int i = 0;
        while(cursor.moveToNext()) {
            SyncAction action = new SyncAction();
            action.action = SyncAction.Action.values()[cursor.getInt(0)];
            action.resourceId = cursor.getInt(1);
            action.moderate_action = cursor.getString(2);
            action.update_newState = UPPSSubmission.State.values()[cursor.getInt(3)];
            if(cursor.getLong(4) > 0) {
                action.date = new Date(cursor.getLong(4));
            }

            result[i++] = action;
        }

        return result;
    }

    public static void setSession(String server, String token, int userId) {
        sWritableDatabase.execSQL("DELETE FROM session;");

        ContentValues cv = new ContentValues();
        cv.put("server", server);
        cv.put("token", token);
        cv.put("user_id", userId);
        sWritableDatabase.insert("session", null, cv);
    }

    public static void setAppData(String title_line_1, String title_line_2, byte[] logo) {
        sWritableDatabase.execSQL("DELETE FROM appdata;");

        ContentValues cv = new ContentValues();
        cv.put("title_line_1", title_line_1);
        cv.put("title_line_2", title_line_2);
        cv.put("logo", logo);
        sWritableDatabase.insert("appdata", null, cv);
    }

    public static String[] getAppTitles() {
        Cursor cursor = sReadableDatabase.rawQuery("SELECT title_line_1, title_line_2 FROM appdata LIMIT 1;", null);
        if(!cursor.moveToNext())
            return null;

        String[] result = new String[] { cursor.getString(0), cursor.getString(1) };
        return result;
    }

    public static byte[] getAppLogo() {
        Cursor cursor = sReadableDatabase.rawQuery("SELECT logo FROM appdata;", null);
        if(!cursor.moveToNext())
            return null;

        return cursor.getBlob(0);
    }

    public static void addUser(UPPSUser user) {
        ContentValues cv = new ContentValues();
        cv.put("id", user.id);
        cv.put("name", user.name);
        cv.put("username", user.username);
        cv.put("role", user.getRole());
        cv.put("avatar_url", user.avatar_url);
        cv.put("etag", user.etag);
        sWritableDatabase.insert("users", null, cv);
    }

    public static void removeUser(int id) {
        sWritableDatabase.execSQL("DELETE FROM users WHERE id=" + id);
    }

    public static UPPSUser[] getUsers() {
        Cursor result = sReadableDatabase.rawQuery("SELECT id, name, username, role, created, avatar_url, etag FROM users", null);
        UPPSUser[] ret = new UPPSUser[result.getCount()];

        int i = 0;
        while(result.moveToNext()) {
            UPPSUser user = new UPPSUser();
            user.id = result.getInt(0);
            user.name = result.getString(1);
            user.username = result.getString(2);
            user.setRole(result.getString(3));
            user.avatar_url = result.getString(5);
            user.etag = result.getString(6);

            ret[i++] = user;
        }

        return ret;
    }

    public static UPPSNotification[] getNotifications() {
        Cursor result = sReadableDatabase.rawQuery("SELECT title, description, date, read FROM notifications", null);
        UPPSNotification[] ret = new UPPSNotification[result.getCount()];

        int i = 0;
        while(result.moveToNext()) {
            UPPSNotification notification = new UPPSNotification();
            notification.title = result.getString(0);
            notification.description = result.getString(1);
            notification.date = new Date(result.getLong(2));
            notification.read = result.getInt(3) == 1;

            ret[i++] = notification;
        }

        return ret;
    }

    public static void addNotification(UPPSNotification notification) {
        ContentValues cv = new ContentValues();
        cv.put("title", notification.title);
        cv.put("description", notification.description);
        cv.put("date", notification.date.getTime());
        cv.put("read", notification.read ? 1 : 0);
        sWritableDatabase.insert("notifications", null, cv);
    }

    public static void removeSubmission(int id) {
        UPPSSubmission submission = UPPSCache.getSubmission(id);

        sWritableDatabase.execSQL("DELETE FROM submissions WHERE id=" + id);
        sWritableDatabase.execSQL("DELETE FROM submissions_reviewdata WHERE submission_id=" + id);
        sWritableDatabase.execSQL("DELETE FROM submissions_log WHERE submission_id=" + id);
        sWritableDatabase.execSQL("DELETE FROM submissions_alternatives WHERE submission_id=" + id);
    }

    static ContentValues createSubmissionContentValues(int formId, UPPSSubmission submission) {
        ContentValues cv = new ContentValues();
        cv.put("id", submission.id);
        cv.put("form_id", formId);
        if(submission.started_at != null)
            cv.put("started_at", submission.started_at.getTime());
        else
            cv.put("started_at", 0);
        cv.put("state", submission.state.ordinal());
        cv.put("data", submission.serializeData());
        if(submission.rescheduleDate != null)
            cv.put("reschedule_date", submission.rescheduleDate.getTime());
        else
            cv.put("reschedule_date", 0);

        cv.put("stop_reason", submission.stopReason);
        cv.put("page_index", submission.currentPageIndex);
        cv.put("updated_at", submission.updated_at != null ? submission.updated_at.getTime() : 0);

        cv.put("etag", submission.etag);

        return cv;
    }

    static ContentValues createSubmissionReviewDataContentValues(UPPSSubmission submission, UPPSSubmission.ReviewData review) {
        ContentValues cv = new ContentValues();
        cv.put("submission_id", submission.id);
        cv.put("field_name", review.fieldName);
        cv.put("message", review.message);
        cv.put("user_id", review.userId);
        cv.put("date", review.date.getTime());

        return cv;
    }

    static ContentValues createSubmissionActionContentValues(UPPSSubmission submission, UPPSSubmission.Action action) {
        ContentValues cv = new ContentValues();
        cv.put("id", action.id);
        cv.put("submission_id", submission.id);
        cv.put("action", action.action);
        cv.put("user_id", action.userId);
        cv.put("reason_id", action.reasonId);
        if(action.date != null)
            cv.put("date", action.date.getTime());
        else
            cv.put("date", 0);

        return cv;
    }

    static ContentValues createSubmissionAlternativeContentValues(UPPSSubmission submission, UPPSSubmission alternative) {
        ContentValues cv = new ContentValues();
        cv.put("submission_id", submission.id);
        cv.put("id", alternative.id);
        cv.put("data", alternative.serializeData());
        cv.put("etag", alternative.etag);

        return cv;
    }

    public static void addSubmission(int formId, UPPSSubmission submission) {
        sWritableDatabase.insert("submissions", null, createSubmissionContentValues(formId, submission));

        for(UPPSSubmission.ReviewData review : submission.reviewdata) {
            review.local_id = sWritableDatabase.insert("submissions_reviewdata", null, createSubmissionReviewDataContentValues(submission, review));
        }

        for(UPPSSubmission.Action action : submission.actionLog) {
            action.local_id = sWritableDatabase.insert("submissions_log", null, createSubmissionActionContentValues(submission, action));
        }

        for(UPPSSubmission alternative : submission.alternatives) {
            sWritableDatabase.insert("submissions_alternatives", null, createSubmissionAlternativeContentValues(submission, alternative));

            for(UPPSSubmission.Action action : alternative.actionLog) {
                action.local_id = sWritableDatabase.insert("submissions_log", null, createSubmissionActionContentValues(alternative, action));
            }
        }
    }

    public static void updateSubmission(int id, int formId, UPPSSubmission submission) {
        sWritableDatabase.update("submissions", createSubmissionContentValues(formId, submission), "id=?", new String[]{Integer.toString(id)});

        for(UPPSSubmission.ReviewData review : submission.reviewdata) {
            sWritableDatabase.update("submissions_reviewdata", createSubmissionReviewDataContentValues(submission, review), "rowid=?", new String[]{ Long.toString(review.local_id)});
        }

        for(UPPSSubmission.Action action : submission.actionLog) {
            sWritableDatabase.update("submissions_log", createSubmissionActionContentValues(submission, action), "rowid=?", new String[]{ Long.toString(action.local_id)});
        }

        for(UPPSSubmission alternative : submission.alternatives) {
            sWritableDatabase.update("submissions_alternatives", createSubmissionAlternativeContentValues(submission, alternative), "submission_id=? AND id=?", new String[]{ Integer.toString(submission.id), Integer.toString(alternative.id)});

            for(UPPSSubmission.Action action : alternative.actionLog) {
                sWritableDatabase.update("submissions_log", createSubmissionActionContentValues(alternative, action), "rowid=?", new String[]{ Long.toString(action.local_id)});
            }
        }
    }

    static ContentValues createFormContentValues(UPPSForm form) {
        String users = "";
        if(form.users != null) {
            for(int i = 0; i < form.users.size(); i++) {
                if(i > 0)
                    users += ",";

                users += Integer.toString(form.users.get(i));
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("id", form.id);
        cv.put("title", form.title);
        cv.put("description", form.description);
        if(form.startTime != null)
            cv.put("start_time", form.startTime.getTime());
        else
            cv.put("start_time", 0);
        if(form.endTime != null)
            cv.put("end_time", form.endTime.getTime());
        else
            cv.put("end_time", 0);
        cv.put("quota", form.quota);
        cv.put("max_reschedules", form.maxReschedules);
        cv.put("users", users);
        cv.put("allow_transfer", form.allowSubmissionTransfer ? 1 : 0);
        cv.put("allow_new_submissions", form.allowNewSubmissions ? 1 : 0);
        cv.put("undefined_mode", form.undefinedMode ? 1 : 0);
        cv.put("assignment_id", form.assignmentId);
        cv.put("updated_at", form.updated_at != null ? form.updated_at.getTime() : 0);
        cv.put("etag", form.etag);

        return cv;
    }

    static ContentValues createFormPageContentValues(UPPSForm form, UPPSForm.Page page) {
        ContentValues cv = new ContentValues();
        cv.put("id", page.id);
        cv.put("form_id", form.id);
        cv.put("name", page.title);
        cv.put("order_id", page.order);

        return cv;
    }

    static ContentValues createFormPageFieldContentValues(UPPSForm form, UPPSForm.Page page, UPPSForm.Input input, ObjectMapper mapper) {
        String rawData = "{}";
        try {
            rawData = mapper.writeValueAsString(input.data);
        } catch (Exception ex) { }

        ContentValues cv = new ContentValues();
        cv.put("id", Integer.parseInt(input.name));
        cv.put("form_id", form.id);
        cv.put("section_id", page.id);
        cv.put("rawdata", rawData);

        return cv;
    }

    static ContentValues createFormStopReasonContentValues(UPPSForm form, UPPSForm.StopReason_t reason) {
        ContentValues cv = new ContentValues();
        cv.put("form_id", form.id);
        cv.put("id", reason.id);
        cv.put("reason", reason.reason);
        cv.put("reschedule", reason.reschedule ? 1 : 0);

        return cv;
    }

    static ContentValues createFormQuotaContentValues(UPPSForm form, int userId, int quota) {
        ContentValues cv = new ContentValues();
        cv.put("form_id", form.id);
        cv.put("user_id", userId);
        cv.put("quota", quota);

        return cv;
    }

    public static void addForm(UPPSForm form) {
        if(sReadableDatabase.rawQuery("SELECT id FROM forms WHERE id=" + form.id + " LIMIT 1", null).getCount() > 0) {
            return;
        }

        sWritableDatabase.insert("forms", null, createFormContentValues(form));

        ObjectMapper mapper = new ObjectMapper();
        for(int i = 0; i < form.pages.size(); i++) {
            UPPSForm.Page page = form.pages.get(i);
            page.local_id = sWritableDatabase.insert("forms_sections", null, createFormPageContentValues(form, page));

            for(int j = 0; j < page.inputs.size(); j++) {
                UPPSForm.Input input = page.inputs.get(j);
                input.local_id = sWritableDatabase.insert("forms_fields", null, createFormPageFieldContentValues(form, page, input, mapper));
            }
        }

        for(int i = 0; i < form.stopReasons.size(); i++) {
            UPPSForm.StopReason_t reason = form.stopReasons.get(i);
            reason.local_id = sWritableDatabase.insert("forms_stopreasons", null, createFormStopReasonContentValues(form, reason));
        }

        for(Integer userId : form.quotas.keySet()) {
            Integer quota = form.quotas.get(userId);
            sWritableDatabase.insert("forms_quotas", null, createFormQuotaContentValues(form, userId, quota));
        }

        for(UPPSSubmission submission : form.submissions) {
            addSubmission(form.id, submission);
        }
    }

    public static void updateForm(UPPSForm form) {
        // id, title, description, start_time, end_time, quota, max_reschedules, users, allow_transfer, allow_new_submissions, undefined_mode, etag
        //UPPSForm form = UPPSCache.getForm(id);

        sWritableDatabase.update("forms", createFormContentValues(form), "id=?", new String[] { Integer.toString(form.id) });
        sWritableDatabase.delete("forms_sections", "form_id=?", new String[] { Integer.toString(form.id) });
        sWritableDatabase.delete("forms_fields", "form_id=?", new String[] { Integer.toString(form.id) });
        sWritableDatabase.delete("forms_stopreasons", "form_id=?", new String[] { Integer.toString(form.id) });
        sWritableDatabase.delete("forms_quotas", "form_id=?", new String[] { Integer.toString(form.id) });

        ObjectMapper mapper = new ObjectMapper();
        for(int i = 0; i < form.pages.size(); i++) {
            UPPSForm.Page page = form.pages.get(i);
            //sWritableDatabase.update("forms_sections", createFormPageContentValues(form, page), "rowid=?", new String[] { Long.toString(page.local_id) });
            sWritableDatabase.insert("forms_sections", null, createFormPageContentValues(form, page));

            for(int j = 0; j < page.inputs.size(); j++) {
                UPPSForm.Input input = page.inputs.get(j);
                //sWritableDatabase.update("forms_fields", createFormPageFieldContentValues(form, page, input, mapper), "rowid=?", new String[] { Long.toString(input.local_id) });
                sWritableDatabase.insert("forms_fields", null, createFormPageFieldContentValues(form, page, input, mapper));
            }
        }

        for(int i = 0; i < form.stopReasons.size(); i++) {
            UPPSForm.StopReason_t reason = form.stopReasons.get(i);
            //sWritableDatabase.update("forms_stopreasons", createFormStopReasonContentValues(form, reason), "rowid=?", new String[] { Long.toString(reason.local_id) });
            sWritableDatabase.insert("forms_stopreasons", null, createFormStopReasonContentValues(form, reason));
        }

        for(Integer userId : form.quotas.keySet()) {
            Integer quota = form.quotas.get(userId);
            //sWritableDatabase.update("forms_quotas", createFormQuotaContentValues(form, userId, quota), "form_id=? AND user_id=?", new String[] { Integer.toString(form.id), Integer.toString(userId) });
            sWritableDatabase.insert("forms_quotas", null, createFormQuotaContentValues(form, userId, quota));
        }

        ArrayList<Integer> idsNotToRemove = new ArrayList<Integer>();
        for(UPPSSubmission submission : form.submissions) {
            updateSubmission(submission.id, form.id, submission);
            idsNotToRemove.add(submission.id);
        }

        String whereClause = "";
        int i = 0;
        for(Integer id : idsNotToRemove)
        {
            whereClause += "submission_id != " + id + " ";
            if(i + 1 < idsNotToRemove.size())
                whereClause += "AND ";

            i++;
        }

        if(whereClause.length() > 0)
        {
            sWritableDatabase.execSQL("DELETE FROM submissions WHERE " + whereClause.replace("submission_id", "id"));
            sWritableDatabase.execSQL("DELETE FROM submissions_reviewdata WHERE " + whereClause);
            sWritableDatabase.execSQL("DELETE FROM submissions_log WHERE " + whereClause);
            sWritableDatabase.execSQL("DELETE FROM submissions_alternatives WHERE " + whereClause);
        }
    }

    public static void removeForm(int id) {
        UPPSForm form = UPPSCache.getForm(id);

        sWritableDatabase.execSQL("DELETE FROM forms WHERE id=" + id);
        sWritableDatabase.execSQL("DELETE FROM forms_sections WHERE form_id=" + id);
        sWritableDatabase.execSQL("DELETE FROM forms_fields WHERE form_id=" + id);
        sWritableDatabase.execSQL("DELETE FROM forms_stopreasons WHERE form_id=" + id);
        sWritableDatabase.execSQL("DELETE FROM forms_quotas WHERE form_id=" + id);
        if(form != null) {
            for(UPPSSubmission submission : form.submissions)
                removeSubmission(submission.id);
        }
    }

    public static UPPSForm[] getForms() {
        Cursor result = sReadableDatabase.rawQuery("SELECT id, title, description, start_time, end_time, quota, max_reschedules, users, allow_transfer, allow_new_submissions, undefined_mode, assignment_id, updated_at, etag FROM forms", null);
        UPPSForm[] ret = new UPPSForm[result.getCount()];

        ObjectMapper mapper = new ObjectMapper();

        int i = 0;
        while(result.moveToNext()) {
            UPPSForm form = new UPPSForm();
            form.id = result.getInt(0);
            form.title = result.getString(1);
            form.description = result.getString(2);
            if(result.getLong(3) > 0)
                form.startTime = new Date(result.getLong(3));
            if(result.getLong(4) > 0)
                form.endTime = new Date(result.getLong(4));
            form.quota = result.getInt(5);
            form.maxReschedules = result.getInt(6);
            form.allowSubmissionTransfer = result.getInt(8) > 0;
            form.allowNewSubmissions = result.getInt(9) > 0;
            form.undefinedMode = result.getInt(10) > 0;
            form.assignmentId = result.getInt(11);
            if(result.getLong(12) > 0)
                form.updated_at = new Date(result.getLong(12));
            form.etag = result.getString(13);

            String users[] = result.getString(7).split(",");
            for(String s : users) {
                try {
                    form.users.add(Integer.parseInt(s));
                } catch (NumberFormatException ex) { }
            }

            Cursor sections = sReadableDatabase.rawQuery("SELECT id, name, order_id, rowid FROM forms_sections WHERE form_id=" + form.id + " ORDER BY `order_id` ASC", null);
            while(sections.moveToNext()) {
                UPPSForm.Page page = form.createPage(sections.getString(1));
                page.id = sections.getInt(0);
                page.order = sections.getInt(2);
                page.local_id = sections.getLong(3);

                Cursor fields = sReadableDatabase.rawQuery("SELECT id, rawdata, rowid FROM forms_fields WHERE form_id=" + form.id + " AND section_id=" + sections.getInt(0), null);
                while(fields.moveToNext()) {
                    int fieldId = fields.getInt(0);
                    String fieldData = fields.getString(1);
                    FormInfo.Section.Field data;
                    try {
                        data = mapper.readValue(fieldData, FormInfo.Section.Field.class);
                        UPPSForm.Input input = page.addInput(Integer.toString(fieldId), data);
                        if(input != null)
                            input.local_id = fields.getLong(2);
                    } catch (Exception ex) {
                        continue;
                    }
                }
            }

            Cursor quotas = sReadableDatabase.rawQuery("SELECT user_id, quota FROM forms_quotas WHERE form_id=" + form.id, null);
            while(quotas.moveToNext()) {
                form.quotas.put(quotas.getInt(0), quotas.getInt(1));
            }

            Cursor submissions = sReadableDatabase.rawQuery("SELECT id, started_at, state, `data`, reschedule_date, stop_reason, page_index, updated_at, etag FROM submissions WHERE form_id=" + form.id, null);
            while(submissions.moveToNext()) {
                UPPSSubmission submission = new UPPSSubmission(form);
                submission.id = submissions.getInt(0);

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(submissions.getLong(1));
                if(submissions.getLong(1) > 0)
                    submission.started_at = new Date(submissions.getLong(1));

                submission.state = UPPSSubmission.State.values()[submissions.getInt(2)];
                submission.deserializeData(submissions.getString(3));
                if(submissions.getLong(4) > 0) {
                    cal = Calendar.getInstance();
                    cal.setTimeInMillis(submissions.getLong(4));
                    submission.rescheduleDate = cal.getTime();
                }
                submission.stopReason = submissions.getInt(5);
                submission.currentPageIndex = submissions.getInt(6);
                if(submissions.getLong(7) > 0)
                    submission.updated_at = new Date(submissions.getLong(7));
                submission.etag = submissions.getString(8);

                form.submissions.add(submission);

                Cursor reviewdata = sReadableDatabase.rawQuery("SELECT field_name, message, user_id, date, rowid FROM submissions_reviewdata WHERE submission_id=" + submission.id, null);
                while(reviewdata.moveToNext()) {
                    String fieldName = reviewdata.getString(0);
                    String message = reviewdata.getString(1);
                    int userId = reviewdata.getInt(2);
                    Date date = null;
                    if(reviewdata.getLong(3) > 0)
                        date = new Date(reviewdata.getLong(3));

                    UPPSSubmission.ReviewData data = submission.addReviewData(fieldName, message, userId, date);
                    data.local_id = reviewdata.getLong(4);
                }

                Cursor log = sReadableDatabase.rawQuery("SELECT action, user_id, reason_id, date, rowid, id FROM submissions_log WHERE submission_id=" + submission.id + " ORDER BY date ASC", null);
                while(log.moveToNext()) {
                    String action = log.getString(0);
                    int userId = log.getInt(1);
                    int reasonId = log.getInt(2);
                    Date date = new Date(log.getLong(3));
                    UPPSSubmission.Action act = submission.addAction(action, userId, date);
                    act.reasonId = reasonId;
                    act.local_id = log.getLong(4);
                    act.id = log.getInt(5);
                }

                Cursor alternatives = sReadableDatabase.rawQuery("SELECT id, data, etag FROM submissions_alternatives WHERE submission_id=" + submission.id, null);
                while (alternatives.moveToNext()) {
                    UPPSSubmission alternative = new UPPSSubmission(form);
                    alternative.id = alternatives.getInt(0);
                    alternative.state = UPPSSubmission.State.New;
                    alternative.deserializeData(alternatives.getString(1));
                    submission.etag = submissions.getString(2);

                    submission.alternatives.add(submission);

                    log = sReadableDatabase.rawQuery("SELECT action, user_id, date FROM submissions_log WHERE submission_id=" + alternative.id + " ORDER BY date ASC", null);
                    while(log.moveToNext()) {
                        String action = log.getString(0);
                        int userId = log.getInt(1);
                        Date date = new Date(log.getLong(2));
                        alternative.addAction(action, userId, date);
                    }
                }
            }

            Cursor stopReasons = sReadableDatabase.rawQuery("SELECT id, reason, reschedule, rowid FROM forms_stopreasons WHERE form_id=" + form.id, null);
            while(stopReasons.moveToNext()) {
                UPPSForm.StopReason_t reason = form.addStopReason(stopReasons.getInt(0), stopReasons.getString(1), stopReasons.getInt(2) == 1);
                reason.local_id = stopReasons.getLong(3);
            }
            ret[i++] = form;
        }

        return ret;
    }

    public static String getToken() {
        android.database.Cursor result = sReadableDatabase.rawQuery("SELECT token FROM session LIMIT 1;", null);
        if(!result.moveToNext())
            return null;

        return result.getString(0);
    }

    public static String getServer() {
        android.database.Cursor result = sReadableDatabase.rawQuery("SELECT server FROM session LIMIT 1;", null);
        if(!result.moveToNext())
            return null;

        return result.getString(0);
    }

    public static int getUserId() {
        android.database.Cursor result = sReadableDatabase.rawQuery("SELECT user_id FROM session LIMIT 1;", null);
        if(!result.moveToNext())
            return 0;

        return result.getInt(0);
    }
}
