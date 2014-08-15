package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by igorlira on 18/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    public int id;
    public String name;
    public String username;
    public String role;
    public String email;
    public String created_at;
    //public String avatar;
}
