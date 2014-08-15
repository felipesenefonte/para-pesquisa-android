package com.lfdb.parapesquisa.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.lfdb.parapesquisa.R;
import com.lfdb.parapesquisa.Util;

import java.util.ArrayList;

/**
 * Created by Igor on 8/28/13.
 */
public class SelectGroup implements View.OnClickListener {
    class Item {
        public String label;
        public Object value;
        public boolean selected;
    }

    TextView mLayout;
    ArrayList<Item> mItems;
    OnValueChangeListener listener;

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public SelectGroup(TextView root) {
        mLayout = root;
        root.setOnClickListener(this);

        mItems = new ArrayList<Item>();
    }

    void deselectAll() {
        for(Item item : mItems) {
            item.selected = false;
        }
    }

    public void onClick(View view) {
        if(view.getId() == R.id.form_input_value) {
            InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            ViewGroup layout = Util.showModal((Activity)view.getContext(), "", "Cancelar", this);

            for(Item item : mItems) {
                FrameLayout row = new FrameLayout(view.getContext());
                ((Activity)view.getContext()).getLayoutInflater().inflate(R.layout.modal_list_item, row);

                TextView txtLabel = (TextView)row.findViewById(R.id.modal_list_item_label);
                View separator = row.findViewById(R.id.modal_list_item_separator);

                txtLabel.setText(item.label);
                txtLabel.setTag(R.id.listitem_value, item);
                txtLabel.setClickable(true);
                txtLabel.setOnClickListener(this);

                layout.addView(row);
            }
        }
        else if(view.getId() == R.id.modal_list_item_label) {
            Item item = (Item)view.getTag(R.id.listitem_value);
            deselectAll();
            item.selected = true;

            mLayout.setText(item.label);
            Util.hideOverlay((Activity)view.getContext());

            if(listener != null)
                listener.onValueChanged();
        } else if(view.getId() == R.id.modal_button_container)
            Util.hideOverlay((Activity)view.getContext());
    }

    public Object[] getValue() {
        for(Item item : mItems) {
            if(item.selected)
                return new Object[] { item.value };
        }
        return null;
    }

    public void setValue(Object[] valuec) {
        if(valuec == null || valuec.length < 1)
            return;

        Object value = valuec[0];

        deselectAll();
        for(Item item : mItems) {
            if(item.value.equals(value)) {
                item.selected = true;
                mLayout.setText(item.label);
            }
        }

    }

    public void addItem(String label, Object value) {
        Item item = new Item();
        item.selected = false;
        item.label = label;
        item.value = value;

        mItems.add(item);
    }
}
