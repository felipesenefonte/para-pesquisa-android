package com.lfdb.parapesquisa.api.descriptors;

import android.app.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.util.List;

/**
 * Created by igorlira on 18/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormInfo {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StopReason {
        public int id;
        public String reason;
        public boolean reschedule;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Section {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Field {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Validation {
                //public int[] range;
                public boolean required;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Action {
                public String[] when;
                public int[] disable;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Option {
                public String label;
                public String value;
            }

            public int id;
            public String label;
            public String description;
            public boolean identifier;
            public boolean read_only;
            public String type;
            public Validation validations;
            public Action[] actions;

            public String layout;
            public int order;
            public Option[] options;
        }

        public int id;
        public String name;
        public int order;
        public Field[] fields;
    }


    public int id;
    public String name;
    public String subtitle;
    public int quota;
    public String pub_start;
    public String pub_end;
    public boolean restricted_to_users;
    public boolean required_approval;
    public int max_reschedules;
    public StopReason[] stop_reasons;
    public boolean allow_transfer;
    public String created_at;
    public String updated_at;
    public Section[] sections;
    public boolean allow_new_submissions;
    public boolean undefined_mode;
}
