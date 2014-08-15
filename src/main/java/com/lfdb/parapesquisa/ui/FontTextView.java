package com.lfdb.parapesquisa.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import com.lfdb.parapesquisa.R;

import java.util.Hashtable;

/**
 * Created by Igor on 9/3/13.
 */
public class FontTextView extends TextView {
    static Hashtable<Integer, Typeface> typefaces = new Hashtable<Integer, Typeface>();
    int weight = 400;

    public FontTextView(Context context) {
        super(context);

        init();
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray arr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FontTextView, 0, 0);
        try {
            weight = arr.getInt(R.styleable.FontTextView_font_weight, 400);
        } finally {
            arr.recycle();
        }
        init();
    }

    public FontTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray arr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FontTextView, 0, 0);
        try {
            weight = arr.getInt(R.styleable.FontTextView_font_weight, 400);
        } finally {
            arr.recycle();
        }
        init();
    }

    public void setWeight(int weight) {
        this.weight = weight;
        init();
    }

    public static Typeface getTypeface(AssetManager assets, int weight) {
        if(!typefaces.containsKey((Integer)weight))
            typefaces.put(weight, Typeface.createFromAsset(assets, "fonts/OpenSans-" + weight + ".ttf"));

        return typefaces.get(weight);
    }

    void init() {
        if(isInEditMode())
            return;

        if(!typefaces.containsKey((Integer)weight))
            typefaces.put(weight, Typeface.createFromAsset(getContext().getAssets(), "fonts/OpenSans-" + weight + ".ttf"));

        this.setTypeface(typefaces.get(weight));
    }
}
