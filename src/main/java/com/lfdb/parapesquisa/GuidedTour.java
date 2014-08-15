package com.lfdb.parapesquisa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.UPPSCache;
import com.lfdb.parapesquisa.api.UPPSForm;
import com.lfdb.parapesquisa.api.UPPSSubmission;
import com.lfdb.parapesquisa.ui.FontTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;

/**
 * Created by Igor on 9/2/13.
 */
public class GuidedTour implements View.OnClickListener {
    static JSONObject data;

    static int currentClassIndex;
    static int currentStep;
    static int formId;

    static String startActivity;
    public static boolean isRunning;

    static boolean didLayout;

    static long lastStepTransition;

    public static void init(final Activity activity) {
        if(data == null) {
            try {
                InputStream str = activity.getAssets().open("guidedtour.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(str));
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int readed;
                while((readed = reader.read(buffer, 0, buffer.length)) > 0) {
                    sb.append(buffer, 0, readed);
                }

                data = new JSONObject(sb.toString());
            } catch (Exception ex) {
                // This should never be triggered
                //
                // Because it's hardcoded
                //
                // ...
                //
                // AMIRITE???
                ex.printStackTrace();
            }
        }

        ViewGroup root = (ViewGroup)activity.findViewById(R.id.activity_root);

        FrameLayout overlay = new FrameLayout(activity);
        RelativeLayout overlaycontainer = new RelativeLayout(activity);

        overlay.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setId(R.id.activity_touroverlay);
        overlay.setVisibility(View.GONE);
        overlay.setBackgroundColor(0xff000000);
        overlay.setClickable(true);
        overlay.setOnClickListener(new GuidedTour());

        overlaycontainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlaycontainer.setId(R.id.activity_touroverlaycontainer);
        overlaycontainer.setVisibility(View.GONE);

        root.addView(overlay);
        root.addView(overlaycontainer);

        LinearLayout layout = new LinearLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.guidedtour_hint, layout, true);
        layout.setId(R.id.guidedtour_hint);

        ImageView focus = new ImageView(activity);
        focus.setId(R.id.guidedtour_focus);

        LinearLayout buttonsContainer = new LinearLayout(activity);
        buttonsContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((RelativeLayout.LayoutParams)buttonsContainer.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        ((RelativeLayout.LayoutParams)buttonsContainer.getLayoutParams()).addRule(RelativeLayout.CENTER_HORIZONTAL);
        ((RelativeLayout.LayoutParams)buttonsContainer.getLayoutParams()).setMargins(0, 0, 0, 20);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);

        FontTextView touchToContinue = new FontTextView(activity);
        touchToContinue.setText(activity.getString(R.string.guidedtour_touchtocontinue));
        touchToContinue.setBackgroundColor(0x7f000000);
        touchToContinue.setTextSize(20);
        touchToContinue.setPadding(20, 20, 20, 20);
        touchToContinue.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams)touchToContinue.getLayoutParams()).setMargins(0, 0, 20, 0);

        FontTextView touchToExit = new FontTextView(activity);
        touchToExit.setText(activity.getString(R.string.guidedtour_touchtoexit));
        touchToExit.setBackgroundColor(0x7f000000);
        touchToExit.setTextSize(20);
        touchToExit.setPadding(20, 20, 20, 20);
        touchToExit.setClickable(true);
        touchToExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                end(activity);
            }
        });
        Util.hookTapEvent(touchToExit);

        buttonsContainer.addView(touchToContinue);
        buttonsContainer.addView(touchToExit);

        overlaycontainer.addView(focus);
        overlaycontainer.addView(layout);
        overlaycontainer.addView(buttonsContainer);
    }

    public void onClick(View view) {
        if(System.currentTimeMillis() - lastStepTransition < 1000) {
            // don't spam me, bro!
            return;
        }
        else {
            lastStepTransition = System.currentTimeMillis();
            next((Activity)view.getContext());
        }
    }

    static void end(Activity activity) {
        try {
            activity.startActivity(new Intent(activity, Class.forName("com.lfdb.parapesquisa." + startActivity)));
        } catch (ClassNotFoundException ex) {
            return;
        }
        hideOverlay(activity);
        isRunning = false;
    }

    static void next(Activity activity) {
        try {
            JSONArray currActivityData = data.getJSONArray("pages").getJSONObject(currentClassIndex).getJSONArray("data");

            currentStep++;

            if(currentStep >= currActivityData.length()) {
                currentStep = 0;
                currentClassIndex++;

                if(currentClassIndex >= data.getJSONArray("pages").length()) {
                    end(activity);
                    return;
                }

                try {
                    String newClassName = data.getJSONArray("pages").getJSONObject(currentClassIndex).getString("activity");
                    Class activityClass = Class.forName("com.lfdb.parapesquisa." + newClassName);
                    Intent intent = new Intent(activity, activityClass);
                    if(formId > 0) {
                        intent.putExtra("id", formId);
                        formId = 0;
                    }
                    activity.startActivity(intent);

                    hideOverlay(activity);
                } catch (ClassNotFoundException ex) {
                    hideOverlay(activity);
                    isRunning = false;
                    return;
                }
            }

            refresh(activity);
        } catch(JSONException ex) {
            hideOverlay(activity);
            isRunning = false;
            return;
        }
    }

    public static void restore(Activity activity) {
        showOverlay(activity);
        refresh(activity);
    }

    static void showOverlay(Activity activity) {
        FrameLayout overlay = (FrameLayout)activity.findViewById(R.id.activity_touroverlay);
        RelativeLayout overlaycontainer = (RelativeLayout)activity.findViewById(R.id.activity_touroverlaycontainer);

        overlay.setVisibility(View.VISIBLE);
        overlaycontainer.setVisibility(View.VISIBLE);

        AlphaAnimation anim = new AlphaAnimation(0, .5f);
        anim.setDuration(500);
        anim.setFillAfter(true);

        overlay.startAnimation(anim);
    }

    static void hideOverlay(Activity activity) {
        FrameLayout overlay = (FrameLayout)activity.findViewById(R.id.activity_touroverlay);
        RelativeLayout overlaycontainer = (RelativeLayout)activity.findViewById(R.id.activity_touroverlaycontainer);

        overlay.setVisibility(View.GONE);
        overlaycontainer.setVisibility(View.GONE);

        AlphaAnimation anim = new AlphaAnimation(.5f, 0);
        anim.setDuration(500);
        anim.setFillAfter(false);

        overlay.startAnimation(anim);
    }

    public static void start(Activity activity) {
        try {
            String className;
            if(activity != null)
                className = activity.getClass().getSimpleName();
            else
                className = (String)data.getJSONObject("pages").keys().next();

            startActivity = className;

            lastStepTransition = System.currentTimeMillis();
            currentClassIndex = 0;
            currentStep = 0;
            showOverlay(activity);
            refresh(activity);

            isRunning = true;
        } catch (JSONException ex) {
            // JAVA Y U NO STOP BITCHING ABOUT EXCEPTIONS THAT WILL NEVER BE THROWN
            // Irony: this was actually called once
            return;
        }
    }

    static int getId(String name) {
        try {
            return R.id.class.getField(name).getInt(null);
        } catch (NoSuchFieldException ex) {
            return -1;
        }
        catch (IllegalAccessException ex) {
            return -1;
        }
    }

    static void showMenu(Activity activity) {
        View menu = activity.findViewById(R.id.forms_menu);
        menu.setVisibility(menu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

        View overlay = activity.findViewById(R.id.forms_menu_overlay);
        android.view.animation.AlphaAnimation alpha = new android.view.animation.AlphaAnimation(0.5F, 0.5F);
        alpha.setDuration(1); // Make animation instant
        alpha.setFillAfter(true); // Tell it to persist after the animation ends
        overlay.startAnimation(alpha);
    }

    static void hideMenu(Activity activity) {
        View menu = activity.findViewById(R.id.forms_menu);
        menu.setVisibility(View.GONE);
    }

    static int getTop(View view) {
        int top = view.getTop();
        Log.d("lol", "top (" + view.toString() + ") = " + top);

        if(view.getParent() != null && view.getParent() instanceof View && ((View) view.getParent()).getId() != android.R.id.content)
            top += getTop((View)view.getParent());

        return top;
    }

    static int getRight(View view) {
        int loc[] = new int[2];
        view.getLocationOnScreen(loc);

        Display display = ((Activity)view.getContext()).getWindowManager().getDefaultDisplay();
        int width = display.getWidth();  // deprecated
        int height = display.getHeight();  // deprecated

        return width - loc[0] - view.getWidth();
    }

    static void postRefresh(Activity activity) {
        try {
            JSONArray currActivityData = data.getJSONArray("pages").getJSONObject(currentClassIndex).getJSONArray("data");
            JSONObject currHintData = (JSONObject)currActivityData.get(currentStep);

            if(currHintData.has("focus")) {
                View v = activity.findViewById(getId(currHintData.getString("focus")));
                if(v != null) {
                    Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    v.draw(canvas);

                    ImageView focusImage = (ImageView)activity.findViewById(R.id.guidedtour_focus);
                    focusImage.setImageBitmap(bitmap);

                    int[] pos = new int[2];
                    v.getLocationOnScreen(pos);

                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(pos[0], getTop(v), lp.rightMargin, lp.bottomMargin);
                    focusImage.setLayoutParams(lp);
                }
            } else {
                ImageView focusImage = (ImageView)activity.findViewById(R.id.guidedtour_focus);
                focusImage.setImageBitmap(null);
            }

            View layout = activity.findViewById(R.id.guidedtour_container);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);

            if(currHintData.has("alignLeftRight")) {
                View v = activity.findViewById(getId(currHintData.getString("alignLeftRight")));
                if(v != null) {
                    int loc[] = new int[2];
                    v.getLocationOnScreen(loc);

                    int sz[] = new int[] { v.getMeasuredWidth(), v.getMeasuredHeight() };

                    lp.setMargins(loc[0] + sz[0], lp.topMargin, lp.rightMargin, lp.bottomMargin);
                }
            }
            if(currHintData.has("alignLeftLeft")) {
                View v = activity.findViewById(getId(currHintData.getString("alignLeftLeft")));
                if(v != null) {
                    int loc[] = new int[2];
                    v.getLocationOnScreen(loc);

                    int sz[] = new int[] { v.getMeasuredWidth(), v.getMeasuredHeight() };

                    lp.setMargins(loc[0], lp.topMargin, lp.rightMargin, lp.bottomMargin);
                }
            }
            if(currHintData.has("alignRightRight")) {
                View v = activity.findViewById(getId(currHintData.getString("alignRightRight")));
                if(v != null) {
                    lp.setMargins(lp.leftMargin, lp.topMargin, getRight(v), lp.bottomMargin);
                }
            }
            if(currHintData.has("alignBottomTop")) {
                View v = activity.findViewById(getId(currHintData.getString("alignBottomTop")));
                if(v != null) {
                    int top = getTop(v);

                    int loc[] = new int[2];
                    v.getLocationInWindow(loc);

                    int sz[] = new int[] { v.getMeasuredWidth(), v.getMeasuredHeight() };

                    int height = layout.getMeasuredHeight();
                    lp.setMargins(lp.leftMargin, top - height, lp.rightMargin, lp.bottomMargin);
                }
            }
            if(currHintData.has("alignRightLeft")) {
                View v = activity.findViewById(getId(currHintData.getString("alignRightLeft")));
                if(v != null) {
                    int loc[] = new int[2];
                    v.getLocationInWindow(loc);

                    int sz[] = new int[] { v.getMeasuredWidth(), v.getMeasuredHeight() };

                    int w = v.getWidth();
                    int r = getRight(v);
                    lp.setMargins(lp.leftMargin, lp.topMargin, r + w, lp.bottomMargin);
                }
            }
            if(currHintData.has("alignTopTop")) {
                View v = activity.findViewById(getId(currHintData.getString("alignTopTop")));
                if(v != null) {
                    int top = getTop(v);
                    lp.setMargins(lp.leftMargin, top, lp.rightMargin, lp.bottomMargin);
                }
            }
            if(currHintData.has("alignTopBottom")) {
                View v = activity.findViewById(getId(currHintData.getString("alignTopBottom")));
                if(v != null) {
                    lp.setMargins(lp.leftMargin, getTop(v) + v.getHeight(), lp.rightMargin, lp.bottomMargin);
                }
            }
            if(currHintData.has("alignBottomTop")) {
                View v = activity.findViewById(getId(currHintData.getString("alignBottomTop")));
                if(v != null) {
                    lp.setMargins(lp.leftMargin, getTop(v) - layout.getHeight(), lp.rightMargin, lp.bottomMargin);
                }
            }

            if(currHintData.has("align")) {
                String align = currHintData.getString("align");

                DisplayMetrics displayMetrics = layout.getContext().getResources().getDisplayMetrics();
                Display display = ((Activity)layout.getContext()).getWindowManager().getDefaultDisplay();
                int width = display.getWidth();  // deprecated

                if(align.equals("right")) {
                    int newX = width - layout.getWidth() - (int)((float)lp.rightMargin * displayMetrics.density);
                    if(newX < 0)
                        newX = 0;

                    lp.setMargins(newX, lp.topMargin, lp.rightMargin, lp.bottomMargin);
                }
            }

            if(currHintData.has("center")) {
                String center = currHintData.getString("center");

                DisplayMetrics displayMetrics = layout.getContext().getResources().getDisplayMetrics();
                Display display = ((Activity)layout.getContext()).getWindowManager().getDefaultDisplay();
                int width = display.getWidth();  // deprecated
                int height = display.getHeight();

                if(center.equals("horizontal")) {
                    int newX = (width - layout.getWidth()) / 2;

                    lp.setMargins(newX, lp.topMargin, 0, lp.bottomMargin);
                } else if(center.equals("vertical")) {
                    int newY = (height - layout.getHeight()) / 2;

                    lp.setMargins(lp.leftMargin, newY, lp.rightMargin, 0);
                } else if(center.equals("both")) {
                    int newX = (width - layout.getWidth()) / 2;
                    int newY = (height - layout.getHeight()) / 2;
                    lp.setMargins(newX, newY, 0, 0);
                }
            }

            if(currHintData.has("margin")) {
                JSONArray margin = currHintData.getJSONArray("margin");

                int left = margin.getInt(0);
                int top = margin.getInt(1);
                int right = margin.getInt(2);
                int bottom = margin.getInt(3);

                DisplayMetrics displayMetrics = layout.getContext().getResources().getDisplayMetrics();

                lp.setMargins((int)((float)left * displayMetrics.density) + lp.leftMargin, (int)((float)top * displayMetrics.density) + lp.topMargin, (int)((float)right * displayMetrics.density) + lp.rightMargin, (int)((float)bottom * displayMetrics.density) + lp.bottomMargin);
            }

            layout.setLayoutParams(lp);
        } catch (JSONException ex) {
            hideOverlay(activity);
            isRunning = false;
            return;
        }
    }

    static boolean metCondition(Activity activity, String condition) {
        UPPSForm firstForm = UPPSCache.getFormCount() > 0 ? UPPSCache.getFormAt(0) : null;

        if(condition.equals("form.hasDeadline"))
            return UPPSCache.getFormCount() > 0 && firstForm.startTime != null && firstForm.endTime != null;
        if(condition.equals("form.hasQuota"))
            return UPPSCache.getFormCount() > 0 && firstForm.quota > 0;
        if(condition.equals("user.isCoordinator"))
            return UPPSCache.currentUser != null && UPPSCache.currentUser.isCoordinator();
        if(condition.equals("!user.isCoordinator"))
            return UPPSCache.currentUser != null && !UPPSCache.currentUser.isCoordinator();
        return false;
    }

    static void refresh(final Activity activity) {
        try {
            JSONArray currActivityData = data.getJSONArray("pages").getJSONObject(currentClassIndex).getJSONArray("data");
            JSONObject currHintData = (JSONObject)currActivityData.get(currentStep);

            final View layout = activity.findViewById(R.id.guidedtour_container);

            if(currHintData.has("action")) {
                String action = currHintData.getString("action");
                if(action.equals("showMenu")) {
                    showMenu(activity);
                } else if(action.equals("hideMenu")) {
                    hideMenu(activity);
                } else if(action.equals("showLogs")) {
                    Util.showLogsWindow(activity);
                } else if(action.equals("setFormId")) {
                    formId = UPPSCache.getFormAt(0).id;
                } else if(action.equals("showSearch") && activity instanceof FormActivity) {
                    ((FormActivity)activity).showSearch();
                } else if(action.equals("hideSearch") && activity instanceof FormActivity) {
                    ((FormActivity)activity).hideSearch();
                } else if(action.equals("setFakeSubmission")) {
                    UPPSCache.currentSubmission = UPPSCache.getFormAt(0).createFakeSubmission();
                } else if(action.equals("addFakeReviewData")) {
                    UPPSSubmission submission = UPPSCache.currentSubmission;

                    int index = 0;
                    UPPSForm.Input firstField = null;
                    while(submission.form.pages.get(0).inputs.get(index) != null) {
                        if(!submission.form.pages.get(0).inputs.get(index).readonly) {
                            firstField = submission.form.pages.get(0).inputs.get(index);
                            break;
                        }
                        index++;
                    }

                    if(firstField != null) {
                        submission.addReviewData(firstField.name, "Lorem Ipsum", UPPSCache.currentUserId, Calendar.getInstance().getTime());
                        ((SubmissionActivity)activity).refreshPage();
                    }
                }
            }

            if(currHintData.has("condition") && !metCondition(activity, currHintData.getString("condition"))) {
                next(activity);
                return;
            }

            TextView txtTitle = (TextView)activity.findViewById(R.id.guidedtour_title);
            TextView txtText = (TextView)activity.findViewById(R.id.guidedtour_desc);

            txtTitle.setVisibility(currHintData.has("title") ? View.VISIBLE : View.GONE);

            txtTitle.setText(currHintData.getString("title"));
            txtText.setText(currHintData.getString("text"));

            didLayout = false;
            layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if(!didLayout) {
                        postRefresh(activity);
                        didLayout = true;
                    }
                    /*try {
                        layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } catch (Exception ex) { }*/
                }
            });
        } catch (JSONException ex) {
            hideOverlay(activity);
            isRunning = false;
            return;
        }
    }
}
