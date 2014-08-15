package com.lfdb.parapesquisa.api.descriptors;

/**
 * Created by igorlira on 21/11/13.
 */
public class GenericItemsRequest {
    public static class Item {
        public int id;
        public long timestamp;
    }

    public Item[] items;
}
