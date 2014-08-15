package com.lfdb.parapesquisa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.ui.CurrencyControl;
import com.lfdb.parapesquisa.ui.DragGroup;
import com.lfdb.parapesquisa.ui.RoundIndicator;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.lfdb.parapesquisa.api.callbacks.*;
import com.lfdb.parapesquisa.api.*;
import com.lfdb.parapesquisa.storage.*;
import com.lfdb.parapesquisa.ui.Window;

/**
 * Created by Igor on 7/29/13.
 */
public class FormsActivity extends Activity implements UPPSCallback, View.OnClickListener {
    Window mLogsWindow;
    String title1;
    String title2;
    boolean presentingLogoutWarning;

    public void onCreate(Bundle savedInstanceState) {
        try {


        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_forms);

        Util.initActivity(this);

        int iconFlags = Util.ICON_REFRESH;
        Util.setMenuBarVisibleIcons(this, iconFlags);

        if(Build.VERSION.SDK_INT >= 11)
            this.getActionBar().hide();

        //UPPSServer.getActiveServer().registerCallback(FormsCallback.k_iCallback, this);
        //UPPSServer.getActiveServer().registerCallback(FormInfoCallback.k_iCallback, this);

        View logs = findViewById(R.id.forms_logs);
        logs.setVisibility(UPPSCache.currentUser.isCoordinator() ? View.VISIBLE : View.INVISIBLE);

        int activeColor = 0xff67b7d3;
        View menu = findViewById(R.id.forms_menu);
        View btnForms = menu.findViewById(R.id.menu_forms);
        btnForms.setBackgroundColor(activeColor);

        TextView txtMenuName = (TextView)findViewById(R.id.menu_name);
        TextView txtMenuServer = (TextView)findViewById(R.id.menu_server);

        txtMenuName.setText(UPPSCache.currentUser.name);
        txtMenuServer.setText(UPPSServer.getActiveServer().getHost());

        fillForms();

        if(!UPPSService.isRunning)
            startService(new Intent(this, UPPSService.class));

        GuidedTour.init(this);

        if(getIntent().getBooleanExtra("sync", false)) {
            Util.beginSync(findViewById(R.id.menubar_refresh));
            Util.showSyncModal(this);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        UPPSServer.getActiveServer().registerCallback(BitmapLoadedCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(AppInfoCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(AccessDeniedCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(SyncStartedCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(SyncResultCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(NotificationCallback.k_iCallback, this);
        UPPSServer.getActiveServer().registerCallback(NewFormsCallback.k_iCallback, this);
        UPPSServer.getActiveServer().loadBitmap(UPPSCache.currentUser.avatar_url, UPPSCache.currentUser, DisplayMetrics.DENSITY_MEDIUM, getResources().getDisplayMetrics().densityDpi);
        UPPSServer.getActiveServer().requestAppInfo();

        try {
            String[] titles = UPPSStorage.getAppTitles();
            if(titles != null) {
                TextView txtLine1 = (TextView)findViewById(R.id.title_line1);
                TextView txtLine2 = (TextView)findViewById(R.id.title_line2);

                byte[] imageBlob = UPPSStorage.getAppLogo();
                if(imageBlob != null && imageBlob.length > 0) {
                    Bitmap b = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                    float originalWide = (float)b.getWidth() / 3.0f; // mdpi
                    float originalTall = (float)b.getHeight() / 3.0f;

                    float dipWide = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, originalWide, getResources().getDisplayMetrics());
                    float dipTall = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, originalTall, getResources().getDisplayMetrics());

                    ImageView logoView = (ImageView)findViewById(R.id.forms_applogo);
                    logoView.setImageDrawable(new BitmapDrawable(getResources(), b));

                    txtLine1.setVisibility(View.INVISIBLE);
                    txtLine2.setVisibility(View.INVISIBLE);
                } else if(titles[0] != null && titles[1] != null) {
                    txtLine1.setText(titles[0].toUpperCase());
                    txtLine2.setText(titles[1].toUpperCase());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        } catch (Exception ex) {
            UPPSStorage.init(this);
            //UPPSStorage.clear();
            UPPSCache.init();
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    public void onClick(View view) {
        if(view.getId() == R.id.logs_myforms) {
            //LinearLayout layout = new LinearLayout()
        } else if(view.getId() == R.id.modal_cancel && presentingLogoutWarning) {
            presentingLogoutWarning = false;
            Util.hideOverlay(this);
        } else if(view.getId() == R.id.modal_buttoncontainer && presentingLogoutWarning) {
            logout();
        }
    }

    void fillForms() {
        ScrollView scroll = (ScrollView)this.findViewById(R.id.forms_scroll);
        ViewGroup table = (ViewGroup)this.findViewById(R.id.forms_table);
        table.removeAllViews();


        for(int i = 0; i < UPPSCache.getFormCount(); i++) {
            if(UPPSCache.getFormAt(i).endTime != null && Calendar.getInstance().getTime().getTime() >= UPPSCache.getFormAt(i).endTime.getTime())
                continue;

            FrameLayout tblRow = new FrameLayout(this);
            getLayoutInflater().inflate(R.layout.forms_row, tblRow);
            Util.hookTapEvent(tblRow.getChildAt(0));

            UPPSForm form = UPPSCache.getFormAt(i);

            RoundIndicator indicator = (RoundIndicator)tblRow.findViewById(R.id.forms_row_indicator);
            indicator.setFillColor(255, 51, 51, 51);
            indicator.setCount(form.quota > 0 ? form.getRemainingCount() : 0);
            indicator.setVisibility(form.quota > 0 ? View.VISIBLE : View.GONE);

            View remaining = tblRow.findViewById(R.id.forms_row_remaining);
            remaining.setVisibility(form.quota > 0 ? View.VISIBLE : View.GONE);

            TextView txtRemaining = (TextView)tblRow.findViewById(R.id.forms_row_remaining);
            txtRemaining.setText(getResources().getString(R.string.remaining_submissions) + " " + (form.getQuota() > 0 ? getString(R.string.of) + " " + form.getQuota() : ""));

            TextView txtTitle = (TextView)tblRow.findViewById(R.id.forms_row_title);
            txtTitle.setText(form.title);

            TextView txtDesc = (TextView)tblRow.findViewById(R.id.forms_row_description);
            txtDesc.setText(form.description);

            TextView txtId = (TextView)tblRow.findViewById(R.id.id);
            txtId.setText(((Integer)form.id).toString());

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");

            TextView txtDeadline = (TextView)tblRow.findViewById(R.id.forms_row_deadline);
            if(form.startTime != null && form.endTime != null)
                if(form.getRemainingDays() > 1)
                    txtDeadline.setText(getResources().getString(R.string.form_deadline).replace("{from}", dateFormat.format(form.startTime)).replace("{to}", dateFormat.format(form.endTime)).replace("{remaining}", Integer.toString(form.getRemainingDays())));
                else
                    txtDeadline.setText(getResources().getString(R.string.form_deadline_singular).replace("{from}", dateFormat.format(form.startTime)).replace("{to}", dateFormat.format(form.endTime)).replace("{remaining}", Integer.toString(form.getRemainingDays())));
            else
                txtDeadline.setVisibility(View.GONE);

            table.addView(tblRow);
            Util.hookTapEvent(tblRow);
        }
    }

    @Override
    public void onCallback(int iCallback, Object obj) {
        if(iCallback == NewFormsCallback.k_iCallback || iCallback == FormInfoCallback.k_iCallback) {
            fillForms();
        } else if(iCallback == BitmapLoadedCallback.k_iCallback) {
            BitmapLoadedCallback result = (BitmapLoadedCallback)obj;
            TextView txtLine1 = (TextView)findViewById(R.id.title_line1);
            TextView txtLine2 = (TextView)findViewById(R.id.title_line2);

            if(result.tag != null && result.tag.toString().equals("logo")) {
                Bitmap b = result.bitmap;

                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    b.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    byte[] blob = stream.toByteArray();
                    stream.close();

                    UPPSStorage.setAppData(title1, title2, blob);

                    float originalWide = (float)b.getWidth() / 3.0f; // mdpi
                    float originalTall = (float)b.getHeight() / 3.0f;

                    float dipWide = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, originalWide, getResources().getDisplayMetrics());
                    float dipTall = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, originalTall, getResources().getDisplayMetrics());

                    ImageView logoView = (ImageView)findViewById(R.id.forms_applogo);
                    logoView.setImageDrawable(new BitmapDrawable(getResources(), b));

                    txtLine1.setVisibility(View.INVISIBLE);
                    txtLine2.setVisibility(View.INVISIBLE);
                } catch (Exception ex) {
                    ex.printStackTrace();

                    txtLine1.setVisibility(View.VISIBLE);
                    txtLine2.setVisibility(View.VISIBLE);
                }
                catch (OutOfMemoryError ex) {
                    ex.printStackTrace();

                    txtLine1.setVisibility(View.VISIBLE);
                    txtLine2.setVisibility(View.VISIBLE);
                }
            } else {
                ImageView avatar = (ImageView)findViewById(R.id.menu_avatar);
                avatar.setImageBitmap(result.bitmap);
            }
        } else if(iCallback == AppInfoCallback.k_iCallback) {
            AppInfoCallback result = (AppInfoCallback)obj;
            if(result.result == EResult.OK) {
                title1 = result.title_line1;
                title2 = result.title_line2;

                TextView txtLine1 = (TextView)findViewById(R.id.title_line1);
                TextView txtLine2 = (TextView)findViewById(R.id.title_line2);

                if(result.header_url != null && result.header_url.length() > 0) {
                    UPPSServer.getActiveServer().loadBitmap(result.header_url, "logo", DisplayMetrics.DENSITY_XXHIGH, getResources().getDisplayMetrics().densityDpi);
                }
                if(title1 != null && title2 != null) {
                    txtLine1.setText(title1.toUpperCase());
                    txtLine2.setText(title2.toUpperCase());
                }
            }
        } else if(iCallback == AccessDeniedCallback.k_iCallback) {
            Util.showSessionExpiredModal(this);
        } else if(iCallback == SyncStartedCallback.k_iCallback) {
            Util.animateSyncButton(this);
            Util.showSyncModal(this);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if(iCallback == SyncResultCallback.k_iCallback) {
            fillForms();

            Util.stopAnimatingSyncButton(this);
            Util.hideOverlay(this);

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if(iCallback == NotificationCallback.k_iCallback) {
            NotificationCenter.refreshBadge(this);
        }
    }

    public void showMenu(View view) {
        final View menu = findViewById(R.id.forms_menu);
        final int newVisibility = menu.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
        final int listWidth = findViewById(R.id.forms_menu_list).getWidth();

        if(newVisibility == View.VISIBLE) {

            TranslateAnimation translate = new TranslateAnimation(-listWidth, 0, 0, 0);
            translate.setDuration(300);
            findViewById(R.id.forms_menu_list).startAnimation(translate);

            translate = new TranslateAnimation(0, listWidth, 0, 0);
            translate.setDuration(300);
            translate.setFillAfter(true);
            findViewById(R.id.forms_window).startAnimation(translate);

            menu.setVisibility(newVisibility);
        } else {
            TranslateAnimation translate = new TranslateAnimation(0, -listWidth, 0, 0);
            translate.setDuration(300);
            findViewById(R.id.forms_menu_list).startAnimation(translate);

            translate = new TranslateAnimation(listWidth, 0, 0, 0);
            translate.setDuration(300);
            translate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    menu.setVisibility(newVisibility);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            findViewById(R.id.forms_window).startAnimation(translate);
        }

        View overlay = findViewById(R.id.forms_menu_overlay);
        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0.5F, 0.5F);
        alpha.setDuration(1); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        overlay.startAnimation(alpha);
    }

    public void hideMenu(View view) {
        showMenu(view);
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
            hideMenu(null);
        } else if(view.getId() == R.id.menu_about) {
            btnAbout.setBackgroundColor(activeColor);
            startActivity(new Intent(this, AboutActivity.class));
        } else if(view.getId() == R.id.menu_help) {
            hideMenu(null);
            GuidedTour.start(this);
        } else if(view.getId() == R.id.menu_logout) {
            tryLogout();
        }
    }

    void tryLogout() {
        TextView txt = Util.showDarkOkCancelModal(this, "Aviso", "Sair", this);
        txt.setText(getString(R.string.logout_warning));
        presentingLogoutWarning = true;
    }

    void logout() {
        this.stopService(new Intent(this, com.lfdb.parapesquisa.api.UPPSService.class));
        UPPSStorage.clear();
        UPPSCache.init();
        UPPSServer.getActiveServer().unregisterCallback(this);
        startActivity(new Intent(this, LoginActivity.class));
    }

    public void showLogs(View view) {
        Util.showLogsWindow(this);
    }

    public void showForm(View view)
    {
        TextView txtId = (TextView)view.findViewById(R.id.id);
        String txt = txtId.getText().toString();
        int id = Integer.parseInt(txt);

        UPPSCache.currentForm = UPPSCache.getForm(id);

        Intent intent = new Intent(this, FormActivity.class);
        intent.putExtra("id", id);
        this.startActivity(intent);
    }

    @Override
    public void onBackPressed() {

    }
}