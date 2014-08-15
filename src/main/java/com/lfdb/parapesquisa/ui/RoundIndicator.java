package com.lfdb.parapesquisa.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.lfdb.parapesquisa.R;

/**
 * Created by Igor on 7/29/13.
 */
public class RoundIndicator extends View
{
    String mText = "0";
    Bitmap mImage;
    int mBgColor[];

    public RoundIndicator(Context ctx)
    {
        super(ctx);
        mBgColor = new int[4];
        mBgColor[0] = 255;
    }

    void init(Context ctx, AttributeSet attrs)
    {
        TypedArray a = ctx.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundIndicator, 0, 0);

        try {
            int color = a.getColor(R.styleable.RoundIndicator_color, 0xff000000);

            mBgColor[0] = (color >> 24) & 0xFF;
            mBgColor[1] = (color >> 16) & 0xFF;
            mBgColor[2] = (color >> 8) & 0xFF;
            mBgColor[3] = (color) & 0xFF;
        }
        finally {
            a.recycle();
        }
    }

    public RoundIndicator(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mBgColor = new int[4];
        mBgColor[0] = 255;

        init(context, attrs);
    }
    public RoundIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mBgColor = new int[4];
        mBgColor[0] = 255;

        init(context, attrs);
    }

    public void setSize(int size)
    {
        this.setLayoutParams(new ViewGroup.LayoutParams(size, size));
    }

    public void setCount(int count)
    {
        this.mText = ((Integer)count).toString();
        this.invalidate();
    }

    public void setFillColor(int a, int r, int g, int b)
    {
        mBgColor[0] = a;
        mBgColor[1] = r;
        mBgColor[2] = g;
        mBgColor[3] = b;
        this.invalidate();
    }

    public void setFillColor(int color)
    {
        mBgColor[0] = Color.alpha(color);
        mBgColor[1] = Color.red(color);
        mBgColor[2] = Color.green(color);
        mBgColor[3] = Color.blue(color);
        this.invalidate();
    }

    public void setText(String text) {
        this.mText = text;
        this.invalidate();
    }

    public void setImage(int id) {
        mImage = BitmapFactory.decodeResource(getResources(), id);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(mImage != null) {
            Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
            pt.setARGB(255, 255, 255, 255);
            canvas.drawBitmap(mImage, 0, 0, pt);
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setARGB(mBgColor[0], mBgColor[1], mBgColor[2], mBgColor[3]);
        paint.setStrokeWidth(10);

        canvas.drawCircle(this.getWidth() / 2, this.getWidth() / 2, this.getHeight() / 2, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        float textSize = this.getHeight() / 3f;
        paint.setTextSize(textSize);
        paint.setARGB(255, 255, 255, 255);
        paint.setStrokeWidth(5);
        paint.setFakeBoldText(true);
        canvas.drawText(mText, this.getWidth() / 2, (this.getHeight() / 2) - ((paint.ascent() + paint.descent()) / 2), paint);
    }
}
