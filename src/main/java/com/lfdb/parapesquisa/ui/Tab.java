package com.lfdb.parapesquisa.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.lfdb.parapesquisa.FormActivity;
import com.lfdb.parapesquisa.R;

/**
 * Created by Igor on 7/30/13.
 */
public class Tab extends View implements View.OnClickListener
{
    String mText;
    int mCount;
    boolean mSelected;

    public Tab(Context ctx)
    {
        super(ctx);
        this.setOnClickListener(this);
    }

    public Tab(Context ctx, AttributeSet attr)
    {
        super(ctx, attr);
        this.setOnClickListener(this);

        TypedArray a = ctx.getTheme().obtainStyledAttributes(attr, R.styleable.Tab, 0, 0);

        try {
            mText = a.getString(R.styleable.Tab_text);
        } finally {
            a.recycle();
        }
    }

    public Tab(Context ctx, AttributeSet attr, int defStyle)
    {
        super(ctx, attr, defStyle);
        this.setOnClickListener(this);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if(this.mSelected)
            paint.setARGB(255, 51, 51, 51);
        else
            paint.setARGB(255, 237, 237, 237);

        canvas.drawRect(1, 0, this.getWidth(), this.getHeight(), paint);

        if(this.mSelected)
            paint.setARGB(255, 255, 255, 255);
        else
            paint.setARGB(255, 157, 157, 157);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12.0f);
        if(!isInEditMode())
            paint.setTypeface(FontTextView.getTypeface(getContext().getAssets(), 400));

        int totalHeight = 16;

        if(this.mText != null)
            canvas.drawText(mText, this.getWidth() / 2, (this.getHeight() / 2) - (paint.descent() + paint.ascent() / 2) - (totalHeight / 2), paint);

        canvas.drawText("(" + mCount + ")", this.getWidth() / 2, (this.getHeight() / 2) - (paint.descent() + paint.ascent() / 2) + (totalHeight / 2), paint);
    }

    public void setCount(int c) {
        mCount = c;
        invalidate();
    }

    public void setText(String text) {
        mText = text;
        invalidate();
    }

    public void select()
    {
        if(this.getParent() != null)
        {
            ViewGroup parent = (ViewGroup)this.getParent();
            for(int i = 0; i < parent.getChildCount(); i++)
            {
                View child = parent.getChildAt(i);
                if(child instanceof Tab)
                    child.setSelected(false);
            }
        }

        this.setSelected(true);
    }

    public void setSelected(boolean value)
    {
        this.mSelected = value;
        this.invalidate();
    }

    @Override
    public void onClick(View view)
    {
        Activity form = (Activity)getContext();
        if(form instanceof FormActivity)
            ((FormActivity)form).filterSubmissions(view);

        this.select();
    }
}
