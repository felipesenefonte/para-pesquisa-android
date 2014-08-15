package com.lfdb.parapesquisa.api;

import android.graphics.Color;
import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfdb.parapesquisa.DateParser;
import com.lfdb.parapesquisa.InvalidDateException;
import com.lfdb.parapesquisa.R;
import com.lfdb.parapesquisa.api.descriptors.SubmissionInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Created by Igor on 7/30/13.
 */
public class UPPSSubmission
{
    public enum State
    {
        Invalid,
        New,
        NotSent,
        Cancelled,
        WaitingForSync,
        WaitingForApproval,
        Rejected,
        Approved,
        Rescheduled,
        Reproved
    }
    public class ReviewData {
        public long local_id;

        public String fieldName;
        public String message;
        public int userId;
        public Date date;
    }
    public class Action {
        public long local_id;

        public int id;
        public String action;
        public int userId;
        public int reasonId;
        public Date date;
    }
    public class ExtraData_t {
        public String name;
        public String value;
    }

    public int id;
    public Date started_at;
    public State state;
    public UPPSForm form;
    public Hashtable<String, Object> formdata;
    public ArrayList<ReviewData> reviewdata;
    public ArrayList<Action> actionLog;
    public ArrayList<UPPSSubmission> alternatives;

    public boolean isLocal;
    public int stopReason;
    public Date rescheduleDate;
    public int timesRescheduled;
    public int currentPageIndex;

    public Date updated_at;

    public String etag;

    public UPPSSubmission(UPPSForm form) {
        this.form = form;
        this.formdata = new Hashtable<String, Object>(form.getInputCount());
        this.reviewdata = new ArrayList<ReviewData>(form.getInputCount());
        this.actionLog = new ArrayList<Action>();
        this.alternatives = new ArrayList<UPPSSubmission>();
    }

    public int getUserId() {
        if(getLatestAction("submitted") != null)
            return getLatestAction("submitted").userId;
        else if(getLatestAction("started") != null)
            return getLatestAction("started").userId;
        else if(getLatestAction("created") != null)
            return getLatestAction("created").userId;
        else
            return 0;
    }

    public Action addAction(String action, int userId, Date date) {
        Action a = new Action();
        a.action = action;
        a.userId = userId;
        a.date = date;

        actionLog.add(a);
        return a;
    }

    public Action getAction(int id) {
        for(int i = 0; i < actionLog.size(); i++) {
            Action act = actionLog.get(i);
            if(act.id == id)
                return act;
        }
        return null;
    }

    public Action getLatestAction(String action) {
        for(int i = actionLog.size() - 1; i >= 0; i--) {
            Action act = actionLog.get(i);
            if(act.action.equals(action))
                return act;
        }
        return null;
    }

    public ReviewData getReviewData(String fieldName) {
        for(ReviewData data : reviewdata)
            if(data.fieldName.equals(fieldName))
                return data;

        return null;
    }

    public void removeReviewData(String fieldName) {
        for(ReviewData d : reviewdata) {
            if(d.fieldName.equals(fieldName)) {
                reviewdata.remove(d);
                break;
            }
        }
    }

    public ReviewData addReviewData(String fieldName, String message, int userId, Date date) {
        removeReviewData(fieldName);

        ReviewData data = new ReviewData();
        data.fieldName = fieldName;
        data.message = message;
        data.userId = userId;
        data.date = date;

        reviewdata.add(data);

        return data;
    }

    public void setValue(String fieldName, Object value) {
        this.formdata.put(fieldName, value);
    }

    public void deserializeData(String json) {
        Hashtable<String, Object> data = new Hashtable<String, Object>();

        ObjectMapper mapper = new ObjectMapper();

        try {
            data = mapper.readValue(json, data.getClass());
            Enumeration<String> keys = data.keys();
            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                deserializeField(key, data.get(key));
            }
        } catch (IOException ex) {
            return;
        }
    }

    public String serializeData() {
        try {
            JSONStringer stringer = new JSONStringer();
            stringer.object();
            Enumeration<String> keys = formdata.keys();
            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                stringer.key(key);
                serializeField(key, stringer);
            }
            stringer.endObject();

            return stringer.toString();
        } catch (JSONException ex) {
            return "";
        }
    }

    public void deserializeField(String key, Object value) {
        UPPSForm.Input input = form.getInput(key);
        if(input == null)
            return;

        if(formdata.containsKey(key))
            formdata.remove(key);

        switch (input.getType()) {
            case Age:
            case Number:
            case Text:
            case Cpf:
            case Email:
            case Url:
            case Private:
            case Date:
            case Currency:
                formdata.put(key, value);
                break;

            case Check:
            {
                ArrayList val = (ArrayList)value;
                Object setval[] = new Object[val.size()];
                for(int i = 0; i < val.size(); i++) {
                    setval[i] = val.get(i);
                }
                formdata.put(key, setval);
                break;
            }

            case Select:
            {
                ArrayList val = (ArrayList)value;
                Object setval[] = new Object[val.size()];
                for(int i = 0; i < val.size(); i++) {
                    setval[i] = val.get(i);
                }

                if(setval.length > 0)
                    formdata.put(key, setval);
                break;
            }
            case Radio:
            {
                ArrayList val = (ArrayList)value;
                Object setval[] = new Object[val.size()];
                for(int i = 0; i < val.size(); i++) {
                    setval[i] = val.get(i);
                }

                if(setval.length > 0)
                    formdata.put(key, setval);
                break;
            }

            case OrderedList:
            {
                ArrayList val = (ArrayList)value;
                Object setval[] = new Object[val.size()];
                for(int i = 0; i < val.size(); i++) {
                    setval[i] = val.get(i);
                }
                formdata.put(key, setval);
                break;
            }
        }
    }

    void serializeField(String key, JSONStringer stringer) throws JSONException {
        UPPSForm.Input input = form.getInput(key);
        if(input == null)
            return;
        switch (input.getType()) {
            case Age:
            case Number:
            {
                stringer.value((Integer)formdata.get(key));
                break;
            }

            case Currency:
            {
                stringer.value(formdata.get(key));
                break;
            }

            case Text:
            case Cpf:
            case Email:
            case Url:
            case Private:
            case Date:
            {
                stringer.value((String) formdata.get(key));
                break;
            }

            case Check:
            {
                Object[] value = (Object[])formdata.get(key);
                stringer.array();
                for(Object obj : value) {
                    stringer.value(obj);
                }
                stringer.endArray();
                break;
            }

            case Select:
            {
                Object[] value = (Object[])formdata.get(key);
                stringer.array();
                if(value.length > 0)
                    stringer.value(value[0]);
                stringer.endArray();
                break;
            }
            case Radio:
            {
                Object[] value = (Object[])formdata.get(key);
                stringer.array();
                if(value.length > 0)
                    stringer.value(value[0]);
                stringer.endArray();
                break;
            }

            case OrderedList:
            {
                Object[] value = (Object[])formdata.get(key);
                stringer.array();
                for(Object obj : value) {
                    stringer.value(obj);
                }
                stringer.endArray();
                break;
            }
        }
    }

    Object serializeField(String key) {
        UPPSForm.Input input = form.getInput(key);
        if(input == null)
            return null;

        switch (input.getType()) {
            case Age:
            case Number:
                return formdata.get(key);

            case Currency:
                return formdata.get(key);

            case Text:
            case Cpf:
            case Email:
            case Url:
            case Private:
            case Date:
                return formdata.get(key);

            case Check:
            {
                Object[] value = (Object[])formdata.get(key);
                return value;
            }

            case Select:
            {
                Object value = formdata.get(key);
                return value;
            }

            case Radio:
            {
                Object value = formdata.get(key);
                return value;
            }

            case OrderedList:
            {
                Object[] value = (Object[])formdata.get(key);
                return value;
            }
        }

        return null;
    }

    public ExtraData_t[] getExtraData() {
        int count = 0;
        for(UPPSForm.Page page : form.pages)
            for(UPPSForm.Input input : page.inputs)
                if(input.readonly)
                    count++;

        ExtraData_t result[] = new ExtraData_t[count];
        int i = 0;
        for(UPPSForm.Page page : form.pages)
            for(UPPSForm.Input input : page.inputs)
                if(input.readonly) {
                    ExtraData_t data = new ExtraData_t();
                    data.name = input.label;
                    if(formdata.get(input.name) != null)
                        data.value = formdata.get(input.name).toString();
                    else
                        data.value = "-";

                    result[i++] = data;
                }

        return result;
    }

    /*public String getTitle(Context context) {
        return getTitle(context, new SimpleDateFormat("dd.MM.yy"));
    }*/

    public String getTitle(Context context) {
        String defTitle = context.getResources().getString(R.string.submission_title).replace("{number}", ((Integer)id).toString());
        if(!form.hasIdentifierField()) {
            return defTitle;
        } else {
            UPPSForm.Input identifier = form.getIdentifierField();
            Object value = this.formdata.get(identifier.name);
            if(value != null && value.toString().length() > 0)
                return value.toString();
            else
                return defTitle;
        }
    }

    public int getStateImageId(boolean coordinator) {
        if(coordinator) {
            switch (state) {
                case NotSent:
                    return R.drawable.status_icon_incompleto;
                case WaitingForSync:
                    return R.drawable.status_icon_sincronizacao;
                case WaitingForApproval:
                    return R.drawable.status_icon_correcao;
                case Rejected:
                    return R.drawable.status_icon_aguardando;
                case Approved:
                    return R.drawable.status_icon_aprovado;
                case Cancelled:
                case Reproved:
                    return R.drawable.status_icon_cancelado;
                case Rescheduled:
                    return R.drawable.status_icon_incompleto;
            }
        }
        else {
            switch (state) {
                case NotSent:
                    return R.drawable.status_icon_incompleto;
                case WaitingForSync:
                    return R.drawable.status_icon_sincronizacao;
                case WaitingForApproval:
                    return R.drawable.status_icon_aguardando;
                case Rejected:
                    return R.drawable.status_icon_correcao;
                case Approved:
                    return R.drawable.status_icon_aprovado;
                case Cancelled:
                case Reproved:
                    return R.drawable.status_icon_cancelado;
                case Rescheduled:
                    return R.drawable.status_icon_incompleto;
            }
        }

        return 0;
    }

    public String getStateDescription(boolean coordinator, Context context) {
        if(coordinator) {
            switch (state)
            {
                case NotSent:
                    return context.getResources().getString(R.string.submission_state_coordinator_notsent);
                case WaitingForSync:
                    return context.getResources().getString(R.string.submission_state_coordinator_waitingforsync);
                case WaitingForApproval:
                    return context.getResources().getString(R.string.submission_state_coordinator_waitingforapproval);
                case Rejected:
                    return context.getResources().getString(R.string.submission_state_coordinator_rejected);
                case Approved:
                    return context.getResources().getString(R.string.submission_state_coordinator_approved);
                case Cancelled:
                {
                    String reason = "";
                    if(getLatestAction("canceled") != null) {
                        UPPSForm.StopReason_t r = form.getStopReason(getLatestAction("canceled").reasonId);
                        if(r != null)
                            reason = r.reason;
                    }
                    return context.getResources().getString(R.string.submission_state_coordinator_cancelled).replace("{reason}", reason);
                }
                case Rescheduled:
                {
                    if(rescheduleDate != null) {
                        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
                        return context.getResources().getString(R.string.submission_state_rescheduled).replace("{date}", format.format(rescheduleDate));
                    } else {
                        return context.getResources().getString(R.string.submission_state_rescheduled).replace("{date}", "");
                    }
                }
                case Reproved:
                    return context.getResources().getString(R.string.submission_state_coordinator_reproved);
            }
        }
        else {
            switch (state)
            {
                case NotSent:
                    return context.getResources().getString(R.string.submission_state_notsent);
                case WaitingForSync:
                    return context.getResources().getString(R.string.submission_state_waitingforsync);
                case WaitingForApproval:
                    return context.getResources().getString(R.string.submission_state_waitingforapproval);
                case Rejected:
                    return context.getResources().getString(R.string.submission_state_rejected);
                case Approved:
                    return context.getResources().getString(R.string.submission_state_approved);
                case Cancelled:
                {
                    String reason = "";
                    if(getLatestAction("canceled") != null) {
                        UPPSForm.StopReason_t r = form.getStopReason(getLatestAction("canceled").reasonId);
                        if(r != null)
                            reason = r.reason;
                    }
                    return context.getResources().getString(R.string.submission_state_cancelled).replace("{reason}", reason);
                }
                case Rescheduled:
                {
                    if(rescheduleDate != null) {
                        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
                        return context.getResources().getString(R.string.submission_state_rescheduled).replace("{date}", format.format(rescheduleDate));
                    } else {
                        return context.getResources().getString(R.string.submission_state_rescheduled).replace("{date}", "");
                    }
                }
                case Reproved:
                    return context.getResources().getString(R.string.submission_state_reproved);
            }
        }

        return "Estado inválido";
    }

    public int getStateColor(boolean coordinator) {
        if(coordinator) {
            switch (state)
            {
                case NotSent:
                    return Color.argb(255, 255, 158, 35);
                case WaitingForSync:
                    return Color.argb(255, 204, 204, 204);
                case WaitingForApproval:
                    return Color.argb(255, 255, 52, 1);
                case Rejected:
                    return Color.argb(255, 123, 193, 226);
                case Approved:
                    return Color.argb(255, 155, 210, 35);
                case Cancelled:
                    return 0xff666666;
                case Rescheduled:
                    return Color.argb(255, 255, 158, 35);
            }
        }
        else {
            switch (state)
            {
                case NotSent:
                    return Color.argb(255, 255, 158, 35);
                case WaitingForSync:
                    return Color.argb(255, 204, 204, 204);
                case WaitingForApproval:
                    return Color.argb(255, 123, 193, 226);
                case Rejected:
                    return Color.argb(255, 255, 52, 1);
                case Approved:
                    return Color.argb(255, 155, 210, 35);
                case Cancelled:
                    return 0xff666666;
                case Rescheduled:
                    return Color.argb(255, 255, 158, 35);
            }
        }

        return 0xff000000;
    }

    public void parse(SubmissionInfo submissiondata) {
        UPPSSubmission submission = this;

        submission.id = submissiondata.id;
        UPPSSubmission.State state = UPPSSubmission.State.Invalid;
        if(submissiondata.status.equals("new"))
            state = UPPSSubmission.State.New;
        else if(submissiondata.status.equals("waiting_approval"))
            state = UPPSSubmission.State.WaitingForApproval;
        else if(submissiondata.status.equals("waiting_correction"))
            state = UPPSSubmission.State.Rejected;
        else if(submissiondata.status.equals("approved"))
            state = UPPSSubmission.State.Approved;
        else if(submissiondata.status.equals("canceled"))
            state = UPPSSubmission.State.Cancelled;
        else if (submissiondata.status.equals("rescheduled"))
            state = UPPSSubmission.State.Rescheduled;
        else if (submissiondata.status.equals("reproved"))
            state = UPPSSubmission.State.Reproved;
        else {
            state = UPPSSubmission.State.Invalid;
        }

        submission.state = state;
        submission.started_at = null;
        if(submissiondata.last_reschedule_date != null && submissiondata.last_reschedule_date.length() > 0) {
            try {
                submission.rescheduleDate = DateParser.parse(submissiondata.last_reschedule_date);
            } catch (Exception ex) { }
        }

        try {
            this.updated_at = DateParser.parse(submissiondata.updated_at);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        submission.reviewdata.clear();
        for(SubmissionInfo.Correction correction : submissiondata.corrections) {
            submission.addReviewData(Integer.toString(correction.field_id), correction.message, correction.user_id, Calendar.getInstance().getTime());
        }

        submission.formdata.clear();
        for(int i = 0; i < submissiondata.answers.length; i++) {
            Object[] arr = submissiondata.answers[i];
            int fieldId = (Integer)arr[0];
            Object value = arr[1];

            submission.deserializeField(Integer.toString(fieldId), value);
        }

        submission.actionLog.clear();
        for(SubmissionInfo.LogAction log : submissiondata.log) {
            Date date = null;
            try {
                if(log.when != null)
                    date = DateParser.parse(log.when);
            } catch (ParseException ex) { }
            Action action = submission.addAction(log.action, log.user_id, date);
            action.id = log.id;
            action.reasonId = log.reason_id;
        }

        submission.alternatives.clear();
        for(SubmissionInfo alternative : submissiondata.alternatives) {
            UPPSSubmission alternativesubmission = new UPPSSubmission(UPPSCache.getForm(alternative.form_id));
            alternativesubmission.parse(alternative);
            submission.alternatives.add(alternativesubmission);
        }

        if(getLatestAction("started") != null && getLatestAction("started").date != null) {
            submission.started_at = getLatestAction("started").date;
        } else if(getLatestAction("created") != null && getLatestAction("created").date != null) {
            submission.started_at = getLatestAction("created").date;
        }
    }

    public void updateData(LinkedHashMap edict) {
        Integer id = (Integer)edict.get("id");
        String status = (String)edict.get("status");
        String last_reschedule_date = (String)edict.get("last_reschedule_date");
        ArrayList<LinkedHashMap> corrections = (ArrayList<LinkedHashMap>)edict.get("corrections");
        ArrayList<Object> answers = (ArrayList<Object>)edict.get("answers");
        ArrayList<LinkedHashMap> log = (ArrayList<LinkedHashMap>)edict.get("log");
        String updated_at = (String)edict.get("updated_at");

        UPPSSubmission submission = this;

        if(id != null)
            submission.id = id;

        if(status != null) {
            UPPSSubmission.State state;
            if(status.equals("new"))
                state = UPPSSubmission.State.New;
            else if(status.equals("waiting_approval"))
                state = UPPSSubmission.State.WaitingForApproval;
            else if(status.equals("waiting_correction"))
                state = UPPSSubmission.State.Rejected;
            else if(status.equals("approved"))
                state = UPPSSubmission.State.Approved;
            else if(status.equals("canceled"))
                state = UPPSSubmission.State.Cancelled;
            else if (status.equals("rescheduled"))
                state = UPPSSubmission.State.Rescheduled;
            else if (status.equals("reproved"))
                state = UPPSSubmission.State.Reproved;
            else {
                state = UPPSSubmission.State.Invalid;
            }

            submission.state = state;
        }

        submission.started_at = null;
        if(last_reschedule_date != null && last_reschedule_date.length() > 0) {
            try {
                submission.rescheduleDate = DateParser.parse(last_reschedule_date);
            } catch (Exception ex) { }
        }

        if(updated_at != null) {
            try {
                this.updated_at = DateParser.parse(updated_at);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(corrections != null) {
            for(LinkedHashMap correction : corrections) {
                Integer field_id = (Integer)correction.get("field_id");
                String message = (String)correction.get("message");
                Integer user_id = (Integer)correction.get("user_id");
                Boolean $keep = (Boolean)correction.get("$keep");
                if($keep == null)
                    $keep = true;

                ReviewData data = submission.getReviewData(Integer.toString(field_id));
                if(data == null && $keep) {
                    submission.addReviewData(Integer.toString(field_id), message, user_id, Calendar.getInstance().getTime());
                } else if($keep) {
                    if(message != null)
                        data.message = message;
                    if(user_id != null)
                        data.userId = user_id;
                } else {
                    submission.removeReviewData(Integer.toString(field_id));
                }
            }
        }

        if(answers != null) {
            for(int i = 0; i < answers.size(); i++) {
                ArrayList<Object> arr = (ArrayList<Object>)answers.get(i);

                int fieldId = (Integer)arr.get(0);
                Object value = arr.get(1);

                if(value != null)
                    submission.deserializeField(Integer.toString(fieldId), value);
                else
                    submission.formdata.remove(Integer.toString(fieldId));
            }
        }

        if(log != null) {
            for(LinkedHashMap logdata : log) {
                Integer logid = (Integer)logdata.get("id");
                if(logid == null)
                    continue;

                String when = (String)logdata.get("when");
                String action_s = (String)logdata.get("action");
                Integer user_id = (Integer)logdata.get("user_id");
                Integer reason_id = (Integer)logdata.get("reason_id");
                Boolean $keep = (Boolean)logdata.get("$keep");
                if($keep == null)
                    $keep = true;

                Date date = null;
                try {
                    if(when != null)
                        date = DateParser.parse(when);
                } catch (ParseException ex) { }

                Action action = submission.getAction(logid);
                if(action == null && $keep) {
                    action = submission.addAction(action_s, user_id, date);
                    if(reason_id != null)
                        action.reasonId = reason_id;
                } else if($keep) {
                    if(when != null)
                        action.date = date;
                    if(action_s != null)
                        action.action = action_s;
                    if(user_id != null)
                        action.userId = user_id;
                    if(reason_id != null)
                        action.reasonId = reason_id;
                } else {
                    submission.actionLog.remove(action);
                }
            }
        }

        /*submission.alternatives.clear();
        for(SubmissionInfo alternative : submissiondata.alternatives) {
            UPPSSubmission alternativesubmission = new UPPSSubmission(UPPSCache.getForm(alternative.form_id));
            alternativesubmission.parse(alternative);
            submission.alternatives.add(alternativesubmission);
        }*/

        if(getLatestAction("started") != null && getLatestAction("started").date != null) {
            submission.started_at = getLatestAction("started").date;
        } else if(getLatestAction("created") != null && getLatestAction("created").date != null) {
            submission.started_at = getLatestAction("created").date;
        }
    }
}
