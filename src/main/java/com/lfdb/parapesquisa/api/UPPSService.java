package com.lfdb.parapesquisa.api;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Igor on 8/29/13.
 */
public class UPPSService extends Service {
    public static boolean isRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        return START_STICKY;
    }
}
