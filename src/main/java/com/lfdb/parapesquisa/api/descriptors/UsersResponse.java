package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by igorlira on 20/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsersResponse {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assignment {
        public int id;
        public int form_id;
        public int quota;

        public UserInfo user;
    }
    public Assignment[] response;
}
