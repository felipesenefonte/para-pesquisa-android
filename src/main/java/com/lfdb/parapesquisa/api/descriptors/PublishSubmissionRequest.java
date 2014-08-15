package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

/**
 * Created by igorlira on 19/09/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublishSubmissionRequest {
    public ArrayList<Object[]> answers;
    public String started_at;
    public String status;

    public PublishSubmissionRequest() {
        answers = new ArrayList<Object[]>();
    }

    public void addAnswer(int key, Object value) {
        Object[] obj = new Object[] { key, value };
        answers.add(obj);
    }
}
