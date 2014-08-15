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
public class CheckGroup implements View.OnClickListener, View.OnFocusChangeListener {
    class Item {
        public String label;
        public Object value;
        public LinearLayout view;
        public boolean selected;
    }

    TableLayout mLayout;
    int mColumns;
    ArrayList<Item> mItems;
    ArrayList<TableRow> mRows;
    OnValueChangeListener listener;

    static Drawable imgSelected;
    static Drawable imgClear;

    public CheckGroup(TableLayout root, int columns) {
        mLayout = root;
        mColumns = columns;
        mItems = new ArrayList<Item>();
        mRows = new ArrayList<TableRow>();

        root.setWeightSum(columns);
        root.setOrientation(LinearLayout.VERTICAL);

        if(imgSelected == null)
            imgSelected = root.getContext().getResources().getDrawable(R.drawable.checkitem_selecionado);
        if(imgClear == null)
            imgClear = root.getContext().getResources().getDrawable(R.drawable.checkitem);
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    void deselectAll() {
        for(Item item : mItems) {
            ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
            item.selected = false;
            round.setImageDrawable(imgClear);
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if(b) {
            View v = (View)view.getParent().getParent();
            Item item = (Item)v.getTag(R.id.radio_item);

            item.selected = true;

            ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
            round.setImageDrawable(imgSelected);
        }
    }

    public void onClick(View view) {
        if(view.getId() == R.id.form_fill_radio_text) {
            View v = (View)view.getParent().getParent();
            Item item = (Item)v.getTag(R.id.radio_item);

            item.selected = !item.selected;

            if(listener != null)
                listener.onValueChanged();
        } else {
            Item item = (Item)view.getTag(R.id.radio_item);
            item.selected = !item.selected;
            ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
            round.setImageDrawable(item.selected ? imgSelected : imgClear);

            InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            if(listener != null)
                listener.onValueChanged();
        }
    }

    public boolean hasValue() {
        for(Item item : mItems) {
            String value = (String)item.value;
            if(value != null && value.equals("other"))
                value = ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).getText().toString();

            if(item.selected && value.length() > 0)
                return true;
        }
        return false;
    }

    public boolean hasValue(Object value) {
        for(Item item : mItems) {
            String val = (String)item.value;
            if(val != null && val.equals("other"))
                val = ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).getText().toString();

            if(item.selected && val.equals(value))
                return true;
        }
        return false;
    }

    public Object[] getValues() {
        if(!hasValue())
            return null;

        int selectedCount = 0;
        for(Item item : mItems) {
            if(item.selected)
                selectedCount++;
        }

        Object result[] = new Object[selectedCount];
        int i = 0;
        for(Item item : mItems) {
            if(item.selected)
                if(item.value.equals("other")) {
                    String text = ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).getText().toString();
                    if(text.length() > 0)
                        result[i++] = text;
                }
                else
                    result[i++] = item.value;
        }
        return result;
    }

    public void setValues(Object[] values) {
        deselectAll();

        ArrayList<Object> foundItems = new ArrayList<Object>();
        for(Item item : mItems) {
            for(Object obj : values) {
                if(item.value.equals(obj)) {
                    item.selected = true;

                    ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
                    round.setImageDrawable(imgSelected);
                    foundItems.add(obj);
                    break;
                }
            }
        }

        if(foundItems.size() < values.length) {
            for(Object obj : values) {
                if(foundItems.contains(obj))
                    continue;

                for(Item item : mItems) {
                    if(item.value.equals("other")) {
                        item.selected = true;

                        ImageView round = (ImageView)item.view.findViewById(R.id.form_fill_radio_round);
                        round.setImageDrawable(imgSelected);

                        if(obj != null)
                            ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).setText(obj.toString());
                        else
                            ((EditText)item.view.findViewById(R.id.form_fill_radio_text)).setText("");
                        break;
                    }
                }

                break; // This should only happen once
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
        } else {
            ((Activity)mLayout.getContext()).getLayoutInflater().inflate(R.layout.form_fill_radio_item, column);
            TextView txtLabel = (TextView)column.findViewById(R.id.form_fill_radio_text);
            txtLabel.setText(label);
        }
        column.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
        lastRow.addView(column);

        ImageView round = (ImageView)column.findViewById(R.id.form_fill_radio_round);
        round.setImageDrawable(imgClear);

        Item item = new Item();
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
