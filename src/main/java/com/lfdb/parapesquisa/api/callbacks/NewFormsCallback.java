package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.descriptors.FormsResponse;

import java.util.LinkedHashMap;

/**
 * Created by Igor on 8/18/13.
 */
public class NewFormsCallback {
    public static final int k_iCallback = UPPSServer.FormsCallbacks + 3;

    public EResult result;
    public int userId;
    public LinkedHashMap forms[];

    public String etag;
}
