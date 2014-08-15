package com.lfdb.parapesquisa.api.callbacks;

import android.graphics.Bitmap;

import com.lfdb.parapesquisa.api.UPPSServer;

/**
 * Created by Igor on 9/7/13.
 */
public class BitmapLoadedCallback {
    public static final int k_iCallback = UPPSServer.UtilCallbacks + 4;

    public Object tag;
    public Bitmap bitmap;
}
