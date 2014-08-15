package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;

/**
 * Created by igorlira on 09/09/13.
 */
public class ModerateSubmissionCallback {
    public static final int k_iCallback = UPPSServer.SubmissionsCallbacks + 6;

    public EResult result;
    public int local_id;
}
