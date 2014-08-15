package com.lfdb.parapesquisa;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.UPPSText;

/**
 * Created by igorlira on 22/10/13.
 */
public class AboutInActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_in);

        Util.initActivity(this);
        Util.setMenuBarVisibleIcons(this, 0);

        if(Build.VERSION.SDK_INT >= 11)
            this.getActionBar().hide();

        TextView txtTitle = (TextView)findViewById(R.id.about_in_title);
        TextView txtTitle2 = (TextView)findViewById(R.id.about_in_title2);
        TextView txtDesc = (TextView)findViewById(R.id.about_in_description);
        TextView txtText = (TextView)findViewById(R.id.about_content);

        UPPSText text = UPPSCache.currentText;
        txtTitle.setText(text.title);
        txtTitle2.setText(text.title);
        txtDesc.setText(text.description);
        txtText.setText(Html.fromHtml(text.content));
    }

    public void back(View view) {
        finish();
    }
}
