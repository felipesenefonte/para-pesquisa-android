package com.lfdb.parapesquisa.api.callbacks;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lfdb.parapesquisa.api.*;
import com.lfdb.parapesquisa.api.descriptors.LoginResponse;

/**
 * Created by Igor on 8/17/13.
 */
public class LoginResultCallback {
    public static final int k_iCallback = UPPSServer.UserCallbacks + 2;

    public EResult result;
    //public UPPSUser user;
    //public int userId;
    //public String token;
    public LoginResponse.Response response;
}