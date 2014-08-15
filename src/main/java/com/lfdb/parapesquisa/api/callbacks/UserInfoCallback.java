package com.lfdb.parapesquisa.api.callbacks;

import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.descriptors.UserInfo;

/**
 * Created by Igor on 8/26/13.
 */
public class UserInfoCallback {
    public static final int k_iCallback = UPPSServer.UserCallbacks + 3;

    public EResult result;
    public UserInfo response;
    /*public int id;
    public String name;
    public String username;
    public String role;
    public String email;
    public String avatar_url;
    public long created_at;*/

    public String etag;
}
