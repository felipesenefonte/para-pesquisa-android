package com.lfdb.parapesquisa.api;

import java.util.Date;

/**
 * Created by Igor on 8/4/13.
 */
public class UPPSNotification {
    public int id;
    public String title;
    public String description;
    public Date date;
    public boolean read;

    public UPPSNotification(int id, String title, String description, Date date) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
    }

    public UPPSNotification() {

    }
}
