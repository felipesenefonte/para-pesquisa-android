package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;

/**
 * Created by Igor on 9/6/13.
 */
public class PublishSubmissionCallback {
    public static final int k_iCallback = UPPSServer.SubmissionsCallbacks + 2;

    public EResult result;
    public int local_id;
    public int submission_id;
}
