package com.lfdb.parapesquisa;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lfdb.parapesquisa.api.*;
import com.lfdb.parapesquisa.api.callbacks.*;
import com.lfdb.parapesquisa.api.descriptors.FormInfo;
import com.lfdb.parapesquisa.api.descriptors.SubmissionInfo;
import com.lfdb.parapesquisa.api.descriptors.UserInfo;
import com.lfdb.parapesquisa.api.descriptors.UsersResponse;
import com.lfdb.parapesquisa.storage.UPPSStorage;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;

public class LoginActivity extends Activity implements UPPSCallback, ViewTreeObserver.OnGlobalLayoutListener, Runnable {
    int SERVER = 1;
    int USERNAME = 2;
    int PASSWORD = 4;
    boolean loginEnded = false;
    Thread sessionLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Util.init();
        UPPSStorage.init(this);

        setContentView(R.layout.activity_login);
        loginError(0);
        Util.initActivity(this);

        if(Build.VERSION.SDK_INT >= 11) this.getActionBar().hide();

        if(UPPSStorage.getToken() != null) {
            sessionLoader = new Thread(this);
            sessionLoader.start();

            findViewById(R.id.async_loading).setVisibility(View.VISIBLE);

            return;
        }

        EditText txtServer   = (EditText)this.findViewById(R.id.login_server);
        if(UPPSServer.getActiveServer() != null)
            txtServer.setText(UPPSServer.getActiveServer().getHost());

        TextView txtVersion = (TextView)findViewById(R.id.login_version);
        txtVersion.setText(getString(R.string.version_s) + " " + getString(R.string.version));

        View container = findViewById(R.id.login_container);
        ViewTreeObserver observer = container.getViewTreeObserver();
        if(observer.isAlive())
            observer.addOnGlobalLayoutListener(this);
    }

    public void run() {
        UPPSServer server = new UPPSServer();
        server.connect(UPPSStorage.getServer(), UPPSStorage.getToken());

        UPPSCache.init();
        NotificationCenter.init();

        UPPSServer.getActiveServer().registerAsyncCallback(NewFormsCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(FormInfoCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(UserInfoCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(NewSubmissionsCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(PublishSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(CorrectSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(RescheduleSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(UpdateSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(ModerateSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(UsersCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(TransferSubmissionsCallback.k_iCallback, UPPSCache.sInstance);

        final Activity activity = this;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(activity, FormsActivity.class);
                //intent.putExtra("sync", true);
                activity.startActivity(intent);
            }
        });
    }

    void loginError(int flags) {
        findViewById(R.id.login_error_server).setVisibility((flags & SERVER) == SERVER ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.login_error_username).setVisibility((flags & USERNAME) == USERNAME ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.login_error_password).setVisibility((flags & PASSWORD) == PASSWORD ? View.VISIBLE : View.INVISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onLogin(View view) {
        loginEnded = false;
        UPPSStorage.clear();
        UPPSCache.init();

        EditText txtServer   = (EditText)this.findViewById(R.id.login_server);
        EditText txtUsername = (EditText)this.findViewById(R.id.login_username);
        EditText txtPassword = (EditText)this.findViewById(R.id.login_password);
        View container = findViewById(R.id.login_buttoncontainer);
        View progress = findViewById(R.id.login_progress);

        String host     = txtServer.getText().toString();
        String username = txtUsername.getText().toString();
        String password = txtPassword.getText().toString();

        UPPSServer server = new UPPSServer();
        server.connect(host);
        server.registerCallback(LoginResultCallback.k_iCallback, this);
        server.registerCallback(UserInfoCallback.k_iCallback, this);
        server.registerCallback(NewFormsCallback.k_iCallback, this);
        server.registerCallback(NewSubmissionsCallback.k_iCallback, this);
        server.registerCallback(UsersCallback.k_iCallback, this);

        txtServer.setEnabled(false);
        txtUsername.setEnabled(false);
        txtPassword.setEnabled(false);
        container.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        server.tryLogin(username, password);
    }

    void enableFields() {
        EditText txtServer   = (EditText)this.findViewById(R.id.login_server);
        EditText txtUsername = (EditText)this.findViewById(R.id.login_username);
        EditText txtPassword = (EditText)this.findViewById(R.id.login_password);
        View container = findViewById(R.id.login_buttoncontainer);
        View progress = findViewById(R.id.login_progress);

        txtServer.setEnabled(true);
        txtUsername.setEnabled(true);
        txtPassword.setEnabled(true);
        container.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
    }

    void disableFields() {
        EditText txtServer   = (EditText)this.findViewById(R.id.login_server);
        EditText txtUsername = (EditText)this.findViewById(R.id.login_username);
        EditText txtPassword = (EditText)this.findViewById(R.id.login_password);
        View container = findViewById(R.id.login_buttoncontainer);
        View progress = findViewById(R.id.login_progress);

        txtServer.setEnabled(false);
        txtUsername.setEnabled(false);
        txtPassword.setEnabled(false);
        container.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    void networkError() {
        final Activity activity = this;
        TextView txt = Util.showDarkModal(this, getString(R.string.warning), getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.hideOverlay(activity);
            }
        });
        txt.setText(getString(R.string.login_error_network));
        txt.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    public void onCallback(int iCallback, Object param) {
        /*
            NTS:

            Cliente chama tryLogin
            LoginResultCallback é invocado
            Cliente requisita seus próprios dados
            UserInfoCallback é invocado
            Cliente requisita formulários
            FormsCallback é invocado

            se for coordenador:
                Cliente requisita informações sobre os usuários de cada formulário
                UserInfoCallback é invocado até que todos os usuários tenham sido carregados
            se não:
                continua
         */
        if(iCallback == LoginResultCallback.k_iCallback) {
            LoginResultCallback result = (LoginResultCallback)param;

            if(result.result == EResult.OK) {
                UPPSCache.currentUserId = result.response.user_id;
                UPPSCache.currentToken = result.response.session_id;
                UPPSServer.getActiveServer().requestUserInfo(result.response.user_id);
            } else if(result.result == EResult.Fail) { // Servidor indisponivel
                loginError(SERVER);
                enableFields();
            } else if(result.result == EResult.NoConnection) {
                networkError();
                enableFields();
            } else {
                loginError(USERNAME | PASSWORD);
                enableFields();
            }
        } else if(iCallback == UserInfoCallback.k_iCallback) {
            UserInfoCallback result = (UserInfoCallback)param;

            if(result.result == EResult.OK) {
                UPPSUser user = new UPPSUser(result.response);
                user.etag = result.etag;

                if(user.id == UPPSCache.currentUserId && UPPSCache.currentUser == null) {
                    UPPSCache.currentUser = user;
                    UPPSServer.getActiveServer().requestForms(UPPSCache.currentUserId);
                }

                UPPSCache.addUser(user);

                if(loginEnded && UPPSCache.areAllUsersLoaded()) {
                    loginOk();
                }
            } else {
                loginError(SERVER);
                enableFields();
            }
        } else if(iCallback == FormsCallback.k_iCallback) {
            FormsCallback result = (FormsCallback)param;

            if(result.result == EResult.OK) {
                if(result.userId == UPPSCache.currentUserId) {
                    for(int i = 0; i < result.forms.length; i++) {
                        FormInfo formdata = result.forms[i].form;

                        if(formdata == null)
                            continue;

                        UPPSForm form = new UPPSForm(formdata);
                        form.etag = result.etag;

                        if(result.forms[i].quota > 0)
                            form.quota = result.forms[i].quota;

                        UPPSCache.addForm(form);
                    }

                    if(!UPPSCache.currentUser.isCoordinator())
                        UPPSServer.getActiveServer().requestSubmissions(UPPSCache.currentUserId);
                    else
                        UPPSServer.getActiveServer().requestUsers(UPPSCache.currentUserId);
                }
            } else {
                loginError(SERVER);
                enableFields();
            }
        } else if(iCallback == UsersCallback.k_iCallback) {
            try {
                UsersCallback result = (UsersCallback)param;
                if(result.result == EResult.OK) {
                    for(int i = 0; i < UPPSCache.getFormCount(); i++) {
                        UPPSForm form = UPPSCache.getFormAt(i);
                        form.users.clear();
                        form.quotas.clear();
                    }
                    for(UsersResponse.Assignment assignment : result.assignments) {
                        UserInfo userdata = assignment.user;

                        UPPSUser user;
                        if(UPPSCache.getUser(userdata.id) != null) {
                            user = UPPSCache.getUser(userdata.id);
                            user.parse(userdata);
                        }
                        else {
                            user = new UPPSUser(userdata);
                            UPPSCache.addUser(user);
                        }

                        UPPSForm form = UPPSCache.getForm(assignment.form_id);
                        if(form != null) {
                            form.users.add(user.id);
                            form.quotas.put(user.id, assignment.quota);
                        }
                    }
                    UPPSServer.getActiveServer().requestSubmissions(UPPSCache.currentUserId);
                } else {
                    loginError(SERVER);
                    enableFields();
                }
            } catch (Exception ex) {
                loginError(SERVER);
                enableFields();
            }
        } else if(iCallback == SubmissionsCallback.k_iCallback) {
            SubmissionsCallback result = (SubmissionsCallback)param;
            if(result.result == EResult.OK) {
                for(SubmissionInfo submissiondata : result.submissions) {
                    if(UPPSCache.getForm(submissiondata.form_id) == null)
                        continue;

                    UPPSSubmission submission = new UPPSSubmission(UPPSCache.getForm(submissiondata.form_id));
                    submission.parse(submissiondata);
                    submission.form.submissions.add(submission);
                }

                loginEnded();
            } else {
                loginError(SERVER);
                enableFields();
            }
        } else if(iCallback == NewSubmissionsCallback.k_iCallback) {
            NewSubmissionsCallback result = (NewSubmissionsCallback)param;
            if(result.result == EResult.OK) {
                for(LinkedHashMap submissiondata : result.items) {
                    //Integer id = (Integer)submissiondata.get("id");
                    Integer form_id = (Integer)submissiondata.get("form_id");

                    if(UPPSCache.getForm(form_id) == null)
                        continue;

                    UPPSSubmission submission = new UPPSSubmission(UPPSCache.getForm(form_id));
                    submission.updateData(submissiondata);
                    submission.form.submissions.add(submission);
                }

                loginEnded();
            } else {
                loginError(SERVER);
                enableFields();
            }
        } else if(iCallback == NewFormsCallback.k_iCallback) {
            NewFormsCallback result = (NewFormsCallback)param;
            if(result.result == EResult.OK) {
                for(LinkedHashMap assignment : result.forms) {
                    Integer assignment_id = (Integer)assignment.get("id");
                    LinkedHashMap formdata = (LinkedHashMap)assignment.get("form");
                    if(formdata == null)
                        continue;

                    Integer quota = (Integer)assignment.get("quota");

                    UPPSForm localForm = new UPPSForm(formdata);
                    localForm.assignmentId = assignment_id;
                    UPPSCache.addForm(localForm);

                    if(quota != null && quota > 0)
                        localForm.quota = quota;
                }

                if(!UPPSCache.currentUser.isCoordinator())
                    UPPSServer.getActiveServer().requestSubmissions(UPPSCache.currentUserId);
                else
                    UPPSServer.getActiveServer().requestUsers(UPPSCache.currentUserId);

            } else if(result.result == EResult.Fail) {
                this.loginError(SERVER);
                this.enableFields();
            }
        }
    }

    void loginEnded() {
        loginEnded = true;
        if(UPPSCache.areAllUsersLoaded())
            loginOk();
        else
            UPPSCache.requestNotLoadedUsers();
    }

    void loginOk() {
        UPPSServer.getActiveServer().requestTexts();

        UPPSServer.getActiveServer().unregisterCallback(this);
        UPPSServer.getActiveServer().unregisterCallback(UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(NewFormsCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(FormInfoCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(UserInfoCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(TextsCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(NewSubmissionsCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(PublishSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(CorrectSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(RescheduleSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(UpdateSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(ModerateSubmissionCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(UsersCallback.k_iCallback, UPPSCache.sInstance);
        UPPSServer.getActiveServer().registerAsyncCallback(TransferSubmissionsCallback.k_iCallback, UPPSCache.sInstance);

        final Activity activity = this;
        Thread saverWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                UPPSCache.save();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(activity, FormsActivity.class);
                        activity.startActivity(intent);
                    }
                });
            }
        });
        saverWorker.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onGlobalLayout() {
        View container = findViewById(R.id.login_container);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        int normalHeight = container.getHeight();
        if(normalHeight < screenHeight)
            container.setLayoutParams(new android.widget.RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, screenHeight));
    }
}
