package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;

/**
 * Created by Igor on 9/5/13.
 */
public class SyncResultCallback {
    public static final int k_iCallback = UPPSServer.UtilCallbacks + 1;

    public EResult result;
}
