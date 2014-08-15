package com.lfdb.parapesquisa.api.callbacks;

import android.graphics.Bitmap;

import com.lfdb.parapesquisa.api.EResult;
import com.lfdb.parapesquisa.api.UPPSServer;

/**
 * Created by Igor on 9/7/13.
 */
public class AppInfoCallback {
    public static final int k_iCallback = UPPSServer.UtilCallbacks + 3;

    public EResult result;
    public String title_line1;
    public String title_line2;
    public String header_url;
}
