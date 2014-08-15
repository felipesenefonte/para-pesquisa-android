package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.descriptors.SubmissionInfo;

import java.util.Hashtable;
import java.util.LinkedHashMap;

/**
 * Created by Igor on 9/6/13.
 */
public class NewSubmissionsCallback {
    public static final int k_iCallback = UPPSServer.SubmissionsCallbacks + 9;

    public EResult result;
    public LinkedHashMap items[];
}
