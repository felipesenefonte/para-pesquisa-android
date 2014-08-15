package com.lfdb.parapesquisa.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.lfdb.parapesquisa.NotificationCenter;
import com.lfdb.parapesquisa.R;
import com.lfdb.parapesquisa.api.UPPSCallback;

import java.util.ArrayList;

/**
 * Created by Igor on 9/5/13.
 */
public class DragGroup implements View.OnClickListener {
    @Override
    public void onClick(View view) {
        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        Item_t item = (Item_t)view.getTag();
        ViewGroup parent = (ViewGroup)item.view.getParent();
        int index = parent.indexOfChild(item.view);

        if(view.getId() == R.id.dragitem_up && index > 0) {
            View view2 = index - 1 >= 0 ? parent.getChildAt(index - 1) : null;

            parent.removeView(item.view);
            parent.addView(item.view, index - 1);

            TranslateAnimation animation = new TranslateAnimation(0, 0, item.view.getHeight(), 0);
            animation.setDuration(300);
            item.view.startAnimation(animation);

            if(view2 != null) {
                animation = new TranslateAnimation(0, 0, -view2.getHeight(), 0);
                animation.setDuration(300);
                view2.startAnimation(animation);
            }
        } else if(view.getId() == R.id.dragitem_down && index < mItems.size() - 1) {
            View view2 = parent.getChildCount() > index + 1 ? parent.getChildAt(index + 1) : null;

            parent.removeView(item.view);
            parent.addView(item.view, index + 1);

            TranslateAnimation animation = new TranslateAnimation(0, 0, -item.view.getHeight(), 0);
            animation.setDuration(300);
            item.view.startAnimation(animation);

            if(view2 != null) {
                animation = new TranslateAnimation(0, 0, view2.getHeight(), 0);
                animation.setDuration(300);
                view2.startAnimation(animation);
            }
        }
    }

    ArrayList<Item_t> mItems = new ArrayList<Item_t>();

    class Item_t {
        public String label;
        public Object value;

        public View view;
    }

    ViewGroup mLayout;

    public DragGroup(ViewGroup root) {
        mLayout = root;
        ((Activity)root.getContext()).getLayoutInflater().inflate(R.layout.draggroup, root);
    }

    boolean compareArrays(Object[] a, Object[] b) {
        if(a.length != b.length)
            return false;

        for(int i = 0; i < a.length; i++) {
            if((a[i] == null && b[i] != null) || (b[i] == null && a[i] != null))
                return false;

            if(!a[i].equals(b[i]))
                return false;
        }

        return true;
    }

    public Object[] getValues() {
        Object[] result = new Object[mItems.size()];
        Object[] defaultResult = new Object[mItems.size()];

        for(int i = 0; i < mItems.size(); i++) {
            defaultResult[i] = mItems.get(i).value;
        }

        ViewGroup table = (ViewGroup)mLayout.findViewById(R.id.draggroup_table);
        for(int i = 0; i < table.getChildCount(); i++) {
            View row = table.getChildAt(i);
            Item_t item = (Item_t)row.getTag();

            result[i] = item.value;
        }

        if(compareArrays(result, defaultResult))
            return null;

        return result;
    }

    public void setValues(Object[] values) {
        ViewGroup table = (ViewGroup)mLayout.findViewById(R.id.draggroup_table);
        table.removeAllViews();

        for(Object obj : values) {
            for(Item_t item : mItems) {
                if(item.value.equals(obj)) {
                    table.addView(item.view);
                    break;
                }
            }
        }
    }

    public void addItem(String label, Object value) {
        FrameLayout layout = new FrameLayout(mLayout.getContext());
        ((Activity)mLayout.getContext()).getLayoutInflater().inflate(R.layout.dragitem, layout, true);

        Item_t item = new Item_t();
        item.label = label;
        item.value = value;
        item.view = layout;

        mItems.add(item);

        layout.setTag(item);

        TextView txtLabel = (TextView)layout.findViewById(R.id.dragitem_label);
        txtLabel.setText(label);

        View btnUp = layout.findViewById(R.id.dragitem_up);
        View btnDown = layout.findViewById(R.id.dragitem_down);

        btnUp.setTag(item);
        btnDown.setTag(item);

        btnUp.setOnClickListener(this);
        btnDown.setOnClickListener(this);

        ViewGroup table = (ViewGroup)mLayout.findViewById(R.id.draggroup_table);
        table.addView(layout);
    }
}
