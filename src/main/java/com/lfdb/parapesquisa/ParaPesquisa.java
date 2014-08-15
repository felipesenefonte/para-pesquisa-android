package com.lfdb.parapesquisa;

import android.app.Application;
import android.content.Context;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.lfdb.parapesquisa.util.SentrySender;
import com.lfdb.parapesquisa.util.Validator;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by igorlira on 09/09/13.
 */
@ReportsCrashes(formKey = "0AmFX5dA5eXradHhqdklKWG1tYV9oT3JPREwwVk5uTmc")
public class ParaPesquisa extends Application {
    static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
        SentrySender sentry = new SentrySender("https://c7884e8ccd774e06971eccb3ddcba7f0:7295c26c720848bb9564157455e0ea24@app.getsentry.com/13146");
        ACRA.getErrorReporter().setReportSender(sentry);
        ACRA.getErrorReporter().checkReportsOnApplicationStart();

        context = this.getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
