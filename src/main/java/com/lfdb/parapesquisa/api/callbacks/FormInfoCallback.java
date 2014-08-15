package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;

/**
 * Created by Igor on 8/18/13.
 */
public class FormInfoCallback {
    public class Section {
        public class Field {
            public int id;
            public String label;
            public String description;
            public boolean identifier;
            public boolean read_only;
            public String type;

            public String data;
        }

        public int id;
        public String name;
        public int order;
        public Field fields[];
        int i_fIndex;

        public void initFields(int count) {
            fields = new Field[count];
        }

        public Field createField() {
            Field field = new Field();
            fields[i_fIndex++] = field;

            return field;
        }
    }

    public class StopReason {
        public int id;
        public String reason;
        public boolean reschedule;
    }

    public int requested_id;

    public int id;
    public String name;
    public String subtitle;
    public int quota;
    public String pub_start;
    public String pub_end;
    public boolean restricted_to_users;
    public int max_reschedules;
    public StopReason stop_reasons[];
    public int users[];
    public Section sections[];

    int i_sIndex;
    int i_rIndex;

    public void initStopReasons(int count) {
        stop_reasons = new StopReason[count];
    }

    public StopReason createStopReason() {
        StopReason reason = new StopReason();
        stop_reasons[i_rIndex++]  = reason;

        return reason;
    }

    public void initSections(int count) {
        sections = new Section[count];
    }

    public Section createSession() {
        Section sect = new Section();
        sections[i_sIndex++] = sect;

        return sect;
    }

    public static final int k_iCallback = UPPSServer.FormsCallbacks + 2;

    public EResult result;
    public String etag;
}
