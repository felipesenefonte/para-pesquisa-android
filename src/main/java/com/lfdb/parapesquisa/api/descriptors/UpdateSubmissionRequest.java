package com.lfdb.parapesquisa.api.descriptors;

import java.util.ArrayList;

/**
 * Created by igorlira on 19/09/13.
 */
public class UpdateSubmissionRequest {
    public ArrayList<Object[]> answers;
    public String started_at;
    public String status;
    public String date;

    public UpdateSubmissionRequest() {
        answers = new ArrayList<Object[]>();
        started_at = null;
        status = null;
    }

    public void addAnswer(int key, Object value) {
        Object[] obj = new Object[] { key, value };
        answers.add(obj);
    }
}
