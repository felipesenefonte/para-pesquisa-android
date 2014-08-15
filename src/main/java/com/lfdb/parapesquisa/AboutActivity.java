package com.lfdb.parapesquisa;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.UPPSText;
import com.lfdb.parapesquisa.storage.UPPSStorage;

/**
 * Created by Igor on 9/5/13.
 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Util.initActivity(this);
        Util.setMenuBarVisibleIcons(this, 0);

        GuidedTour.init(this);

        if(Build.VERSION.SDK_INT >= 11)
            this.getActionBar().hide();

        TextView txtMenuName = (TextView)findViewById(R.id.menu_name);
        TextView txtMenuServer = (TextView)findViewById(R.id.menu_server);

        txtMenuName.setText(UPPSCache.currentUser.name);
        txtMenuServer.setText(UPPSServer.getActiveServer().getHost());

        fillTexts();
    }

    void fillTexts() {
        ViewGroup table = (ViewGroup)findViewById(R.id.about_table);

        for(int i = 0; i < UPPSCache.getTextCount(); i++) {
            UPPSText text = UPPSCache.getTextAt(i);

            RelativeLayout row = new RelativeLayout(this);
            getLayoutInflater().inflate(R.layout.about_row, row, true);

            TextView txtTitle = (TextView)row.findViewById(R.id.about_row_title);
            TextView txtDesc = (TextView)row.findViewById(R.id.about_row_description);

            row.findViewById(R.id.text_root).setTag(i);
            txtTitle.setText(text.title);
            txtDesc.setText(text.description);

            table.addView(row);
        }
    }

    public void showText(View view) {
        UPPSCache.currentText = UPPSCache.getTextAt((Integer)view.getTag());

        startActivity(new Intent(this, AboutInActivity.class));
    }

    public void showMenu(View view) {
        View menu = findViewById(R.id.forms_menu);
        menu.setVisibility(menu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

        View overlay = findViewById(R.id.forms_menu_overlay);
        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0.5F, 0.5F);
        alpha.setDuration(1); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        overlay.startAnimation(alpha);
    }

    public void hideMenu(View view) {
        View menu = findViewById(R.id.forms_menu);
        menu.setVisibility(View.GONE);
    }

    public void menuClick(View view) {
        int activeColor = 0xff67b7d3;
        View menu = findViewById(R.id.forms_menu);
        View btnForms = menu.findViewById(R.id.menu_forms);
        View btnAbout = menu.findViewById(R.id.menu_about);
        View btnLogout = menu.findViewById(R.id.menu_logout);
        View btnHelp = menu.findViewById(R.id.menu_help);

        btnForms.setBackgroundColor(0);
        btnAbout.setBackgroundColor(0);
        btnLogout.setBackgroundColor(0);
        btnHelp.setBackgroundColor(0);

        if(view.getId() == R.id.menu_forms) {
            btnForms.setBackgroundColor(activeColor);
            startActivity(new Intent(this, FormsActivity.class));
        } else if(view.getId() == R.id.menu_about) {
            btnAbout.setBackgroundColor(activeColor);
            hideMenu(null);
        } else if(view.getId() == R.id.menu_help) {
            hideMenu(null);
            GuidedTour.start(this);
        } else if(view.getId() == R.id.menu_logout) {
            this.stopService(new Intent(this, com.lfdb.parapesquisa.api.UPPSService.class));
            UPPSStorage.clear();
            UPPSCache.init();
            //finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}
