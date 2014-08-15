package com.lfdb.parapesquisa.api.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by igorlira on 20/09/13.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextsResponse {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Text {
        public String title;
        public String subtitle;
        public String content;
    }

    public Text[] response;
}
