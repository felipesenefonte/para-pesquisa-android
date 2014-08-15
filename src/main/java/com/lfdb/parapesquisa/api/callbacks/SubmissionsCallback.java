package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.descriptors.SubmissionInfo;

/**
 * Created by Igor on 9/6/13.
 */
public class SubmissionsCallback {
    public static final int k_iCallback = UPPSServer.SubmissionsCallbacks + 1;

    public EResult result;
    public SubmissionInfo submissions[];
}
