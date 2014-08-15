package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Created by igorlira on 18/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewSubmissionsResponse {
    /*public static class Response {
        public Hashtable<String, Object> items[];
    }
    public Response response;*/

    public /*Hashtable<String, Object>*/LinkedHashMap response[];
}
