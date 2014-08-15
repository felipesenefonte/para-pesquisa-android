package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.descriptors.FormInfo;
import com.lfdb.parapesquisa.api.descriptors.FormsResponse;

/**
 * Created by Igor on 8/18/13.
 */
public class FormsCallback {
    public static final int k_iCallback = UPPSServer.FormsCallbacks + 1;

    public EResult result;
    public int userId;
    public FormsResponse.Assignment forms[];

    public String etag;
}
