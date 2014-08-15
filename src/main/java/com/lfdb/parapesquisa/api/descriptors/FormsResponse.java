package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by igorlira on 18/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormsResponse {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assignment {
        public int quota;
        public FormInfo form;
    }
    public Assignment[] response;
}
