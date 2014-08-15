package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.descriptors.UsersResponse;

/**
 * Created by igorlira on 17/09/13.
 */
public class UsersCallback {
    public static final int k_iCallback = UPPSServer.UserCallbacks + 4;

    public EResult result;
    public UsersResponse.Assignment assignments[];
}
