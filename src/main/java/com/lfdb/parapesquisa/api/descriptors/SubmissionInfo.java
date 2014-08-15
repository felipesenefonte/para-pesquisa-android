package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by igorlira on 18/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionInfo {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LogAction {
        public int id;
        public String action;
        public String when;
        public int user_id;
        public int reason_id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Correction {
        public int field_id;
        public String message;
        public int user_id;
        public int created_at;
    }

    public int id;
    public int form_id;
    public String last_reschedule_date;
    public String status;
    public LogAction[] log;
    public Correction[] corrections;
    public Object[][] answers;
    public SubmissionInfo[] alternatives;
    public String updated_at;
}
