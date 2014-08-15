package com.lfdb.parapesquisa.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import com.lfdb.parapesquisa.*;
import java.util.ArrayList;
import android.widget.RelativeLayout;
import android.widget.*;

/**
 * Created by Igor on 8/19/13.
 */
public class Window implements View.OnClickListener {
    Activity mActivity;
    ArrayList<View> mViewHistory;
    ArrayList<String> mViewTitles;
    ViewGroup mContainer;
    int viewIndex;
    int mIcon;

    public Window(Activity activity, int icon) {
        mActivity = activity;
        mViewTitles = new ArrayList<String>();
        mViewHistory = new ArrayList<View>();
        mContainer = new RelativeLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.modal_window_dark, mContainer, true);

        View btnClose = mContainer.findViewById(R.id.modal_close);
        btnClose.setOnClickListener(this);

        View btnIcon = mContainer.findViewById(R.id.modal_icon);
        btnIcon.setOnClickListener(this);

        View btn = mContainer.findViewById(R.id.modal_buttoncontainer);
        btn.setOnClickListener(this);

        viewIndex = -1;
        mIcon = icon;
    }

    public void onClick(View view) {
        if(view.getId() == R.id.modal_close) {
            this.hide();
        } else if(view.getId() == R.id.modal_icon) {
            this.back();
        }
    }

    void refreshLayout() {
        View view = mViewHistory.get(viewIndex);
        String title = mViewTitles.get(viewIndex);

        ViewGroup contentContainer = (ViewGroup)mContainer.findViewById(R.id.modal_content);
        contentContainer.removeAllViews();
        contentContainer.addView(view);

        TextView txtTitle = (TextView)mContainer.findViewById(R.id.modal_title);
        txtTitle.setText(title);

        ImageView btnIcon = (ImageView)mContainer.findViewById(R.id.modal_icon);
        if(mIcon > 0)
            btnIcon.setImageDrawable(viewIndex == 0 ? mActivity.getResources().getDrawable(mIcon) : mActivity.getResources().getDrawable(R.drawable.header_bt_voltar));
    }

    void back() {
        if(viewIndex < 1)
            return;

        mViewHistory.remove(viewIndex);
        viewIndex--;
        refreshLayout();

        onBack();
    }

    void showButton() {
        View view = mContainer.findViewById(R.id.modal_buttoncontainer);
        view.setVisibility(View.VISIBLE);
    }

    void hideButton() {
        View view = mContainer.findViewById(R.id.modal_buttoncontainer);
        view.setVisibility(View.GONE);
    }

    public void pushView(View view, String title) {
        mViewHistory.add(view);
        mViewTitles.add(title);
        viewIndex++;

        refreshLayout();
        Util.hookTapEventGroup(mContainer);
    }

    public void pushView(int layout, String title) {
        ViewGroup view = new android.widget.FrameLayout(mActivity);
        mActivity.getLayoutInflater().inflate(layout, view, true);

        pushView(view, title);
    }

    public View findViewById(int id) {
        ViewGroup contentContainer = (ViewGroup)mContainer.findViewById(R.id.modal_content);
        return contentContainer.findViewById(id);
    }

    public void show() {
        View view = mViewHistory.get(viewIndex);

        ViewGroup overlayContainer = Util.showOverlay(mActivity);
        overlayContainer.removeAllViews();
        overlayContainer.addView(mContainer);
    }

    public void hide() {
        Util.hideOverlay(mActivity);
    }

    void onBack() {

    }
}
