package com.lfdb.parapesquisa.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.lfdb.parapesquisa.R;

import java.util.ArrayList;

/**
 * Created by Igor on 8/28/13.
 */
public class RadioGroup implements View.OnClickListener, View.OnFocusChangeListener {
    @Override
    public void onFocusChange(View view, boolean b) {
        if(b) {
            View v = (View)view.getParent().getParent();
            RadioItem item = (RadioItem)v.getTag(R.id.radio_item);
            EditText editText = (EditText)v.findViewById(R.id.form_fill_radio_text);

            setValue(new Object[] { editText.getText() });
        }
    }

    class RadioItem {
        public String label;
        public Object value;
        public LinearLayout view;
        public boolean selected;
    }

    TableLayout mLayout;
    int mColumns;
    ArrayList<RadioItem> mItems;
    ArrayList<TableRow> mRows;
    OnValueChangeListener listener;

    static Drawable imgSelected;
    static Drawable imgClear;

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public RadioGroup(TableLayout root, int columns) {
        mLayout = root;
        mColumns = columns;
        mItems = new ArrayList<RadioItem>();
        mRows = new ArrayList<TableRow>();

        root.setWeightSum(columns);
        root.setOrientation(LinearLayout.VERTICAL);

        if(imgSelected == null)
            imgSelected = root.getContext().getResources().getDrawable(R.drawable.bolinha_selecao_selecionado);
        if(imgClear == null)
            imgClear = root.getContext().getResources().getDrawable(R.drawable.bolinha_selecao);
    }

    void deselectAll() {
        for(RadioItem item : mItems) {
            ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
            item.selected = false;
            round.setImageDrawable(imgClear);
        }
    }

    public void onClick(View view) {
        if(view.getId() == R.id.form_fill_radio_text) {
            View v = (View)view.getParent().getParent();
            RadioItem item = (RadioItem)v.getTag(R.id.radio_item);
            EditText editText = (EditText)v.findViewById(R.id.form_fill_radio_text);

            setValue(new Object[] { editText.getText() });

            if(listener != null)
                listener.onValueChanged();
        } else {
            deselectAll();

            RadioItem item = (RadioItem)view.getTag(R.id.radio_item);
            item.selected = true;
            ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
            round.setImageDrawable(imgSelected);

            InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            if(listener != null)
                listener.onValueChanged();
        }
    }

    public boolean hasValue() {
        for(RadioItem item : mItems) {
            ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
            if(item.selected)
                return true;
        }
        return false;
    }

    public Object[] getValue() {
        for(RadioItem item : mItems) {
            if(item.selected)
                if(item.value != null && item.value.equals("other")) {
                    String text = ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).getText().toString();
                    if(text.length() < 1)
                        return null;

                    return new Object[] { text };
                } else
                    return new Object[] { item.value };
        }
        return null;
    }

    public void setValue(Object[] valuec) {
        deselectAll();

        if(valuec == null || valuec.length < 1)
            return;

        Object value = valuec[0];

        boolean didMatch = false;
        for(RadioItem item : mItems) {
            if(item.value.equals(value)) {
                item.selected = true;

                ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
                round.setImageDrawable(imgSelected);

                didMatch = true;
                break;
            }
        }

        if(!didMatch) {
            for(RadioItem item : mItems) {
                if(item.value.equals("other")) {
                    item.selected = true;

                    ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
                    round.setImageDrawable(imgSelected);

                    ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).setText(value.toString());
                    break;
                }
            }
        }
    }

    public void addItem(String label, Object value) {
        TableRow lastRow = null;
        boolean newRow = false;
        if(mRows.size() > 0) {
            lastRow = mRows.get(mRows.size() - 1);
            if(lastRow.getChildCount() >= mColumns)
                lastRow = null;
        }

        if(lastRow == null) {
            lastRow = new TableRow(mLayout.getContext());
            lastRow.setOrientation(LinearLayout.HORIZONTAL);
            lastRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            lastRow.setWeightSum(mColumns);
            newRow = true;
        }

        LinearLayout column = new LinearLayout(mLayout.getContext());
        if(value.equals("other")) {
            ((Activity)mLayout.getContext()).getLayoutInflater().inflate(R.layout.form_fill_radio_item_other, column);
            EditText txt = (EditText)column.findViewById(R.id.form_fill_radio_text);
            txt.setOnClickListener(this);
            txt.setOnFocusChangeListener(this);
        }
        else {
            ((Activity)mLayout.getContext()).getLayoutInflater().inflate(R.layout.form_fill_radio_item, column);
            TextView txtLabel = (TextView)column.findViewById(R.id.form_fill_radio_text);
            txtLabel.setText(label);
        }
        column.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
        lastRow.addView(column);

        RadioItem item = new RadioItem();
        item.label = label;
        item.value = value;
        item.view = column;
        mItems.add(item);

        column.setTag(R.id.radio_item, item);
        column.setClickable(true);
        column.setOnClickListener(this);

        if(newRow) {
            mLayout.addView(lastRow);
            mRows.add(lastRow);
        }
    }
}
