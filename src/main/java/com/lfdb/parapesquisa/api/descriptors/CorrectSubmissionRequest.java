package com.lfdb.parapesquisa.api.descriptors;

import java.util.ArrayList;

/**
 * Created by igorlira on 20/09/13.
 */
public class CorrectSubmissionRequest {
    public static class Correction {
        public int field_id;
        public String message;
    }

    public ArrayList<Correction> corrections;
    public String date;

    public CorrectSubmissionRequest() {
        corrections = new ArrayList<Correction>();
    }

    public void addCorrection(int field_id, String message) {
        Correction correction = new Correction();
        correction.field_id = field_id;
        correction.message = message;

        corrections.add(correction);
    }
}
