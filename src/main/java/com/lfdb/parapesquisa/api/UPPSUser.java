package com.lfdb.parapesquisa.api;

import com.lfdb.parapesquisa.DateParser;
import com.lfdb.parapesquisa.api.descriptors.UserInfo;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Igor on 8/6/13.
 */
public class UPPSUser {

    public int id;
    public String name;
    public String username;
    private String _role = "agent";
    public Date created;
    public String avatar_url;
    public String etag;

    public UPPSUser(UserInfo data) {
        parse(data);
    }

    public UPPSUser() {

    }

    public void parse(UserInfo data) {
        id = data.id;
        name = data.name;
        username = data.username;
        _role = data.role;

        try {
            created = DateParser.parse(data.created_at);
        } catch (Exception ex) {
            created = Calendar.getInstance().getTime();
        }
    }

    public void setRole(String role) {
        _role = role;
    }

    public String getRole() {
        return _role;
    }

    public boolean isCoordinator() {
        return _role != null && (_role.equals("mod") || _role.equals("api"));
    }
}
