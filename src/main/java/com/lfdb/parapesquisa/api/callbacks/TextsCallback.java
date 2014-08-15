package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.descriptors.TextsResponse;

/**
 * Created by Igor on 9/5/13.
 */
public class TextsCallback {
    public static final int k_iCallback = UPPSServer.UtilCallbacks + 2;

    public EResult result;
    public TextsResponse.Text texts[];
}
