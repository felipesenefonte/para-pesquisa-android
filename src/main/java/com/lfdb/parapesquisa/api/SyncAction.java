package com.lfdb.parapesquisa.api;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by igorlira on 09/09/13.
 */
public class SyncAction {
    public enum Action {
        SubmissionPublish,
        SubmissionUpdate,
        SubmissionCorrect,
        SubmissionReschedule,
        SubmissionCancel,
        SubmissionModerate,
        SubmissionTransfer
    }

    public Action action;
    public int resourceId;
    public String moderate_action;
    public UPPSSubmission.State update_newState;

    public int transfer_dest;
    public ArrayList<Integer> transfer_ids;

    public Date date;
}
