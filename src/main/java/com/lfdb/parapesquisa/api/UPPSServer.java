package com.lfdb.parapesquisa.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.lfdb.parapesquisa.NotificationCenter;
import com.lfdb.parapesquisa.api.descriptors.*;
import com.lfdb.parapesquisa.api.rest.RestClient;

import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import com.lfdb.parapesquisa.api.callbacks.*;

import android.app.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Created by Igor on 7/29/13.
 */
public class UPPSServer {
    public static final int UserCallbacks = 100;
    public static final int FormsCallbacks = 200;
    public static final int SubmissionsCallbacks = 300;
    public static final int NotificationsCallbacks = 400;
    public static final int UtilCallbacks = 600;

    class CallbackRegister_t {
        public int iCallback;
        public UPPSCallback pListener;
        public boolean isAsync;
    }

    abstract class Job {
        public abstract void complete(int resultCode, org.json.JSONObject result);
    }

    abstract class CJob<T> {
        public abstract void complete(int resultCode, T result);
    }

    static UPPSServer sActiveServer;
    UPPSServer mNextServer;
    String mHost;
    String mGivenHost;
    Vector<CallbackRegister_t> mCallbacks;
    RestClient mClient;
    String mToken;
    ObjectMapper mObjectMapper;

    public static UPPSServer getActiveServer() {
        return sActiveServer;
    }

    public UPPSServer getNextServer() {
        return mNextServer;
    }

    public String getHost() {
        return mGivenHost;
    }

    public UPPSServer() {
        if(sActiveServer != null)
            this.mNextServer = sActiveServer;

        sActiveServer = this;
        mCallbacks = new Vector<CallbackRegister_t>();

        mObjectMapper = new ObjectMapper();
    }

    public void connect(String host) {
        this.mHost = "http://" + host + "/1/";
        mGivenHost = host;
        mClient = new RestClient(mHost);
    }

    /*
     * Restaura uma sessão criada anteriormente
     */
    public void connect(String host, String token) {
        this.mHost = "http://" + host + "/1/";
        mGivenHost = host;
        this.mToken = token;
        mClient = new RestClient(mHost);
    }

    public void registerCallback(int iCallback, UPPSCallback pListener) {
        CallbackRegister_t p = new CallbackRegister_t();
        p.iCallback = iCallback;
        p.pListener = pListener;

        this.mCallbacks.add(p);
    }

    public void registerAsyncCallback(int iCallback, UPPSCallback pListener) {
        CallbackRegister_t p = new CallbackRegister_t();
        p.iCallback = iCallback;
        p.pListener = pListener;
        p.isAsync = true;

        this.mCallbacks.add(p);
    }

    public void unregisterCallback(UPPSCallback pListener) {
        ArrayList<CallbackRegister_t> toRemove = new ArrayList<CallbackRegister_t>();
        for(CallbackRegister_t p : mCallbacks) {
            if(p.pListener == pListener) {
                toRemove.add(p);
            }
        }

        for(CallbackRegister_t q : toRemove)
            mCallbacks.remove(q);
    }

    public void unregisterCallback(int iCallback, UPPSCallback pListener) {
        ArrayList<CallbackRegister_t> toRemove = new ArrayList<CallbackRegister_t>();
        for(CallbackRegister_t p : mCallbacks) {
            if(p.pListener == pListener && p.iCallback == iCallback) {
                toRemove.add(p);
            }
        }

        for(CallbackRegister_t q : toRemove)
            mCallbacks.remove(q);
    }

    public void postCallback(final int iCallback, final Object param) {
        ArrayList<CallbackRegister_t> runCallbacks = new ArrayList<CallbackRegister_t>();

        synchronized (this.mCallbacks) {
            for(final CallbackRegister_t p : this.mCallbacks) {
                if(p.iCallback == iCallback) {
                    runCallbacks.add(p);
                }
            }
        }

        for(final CallbackRegister_t p : runCallbacks) {
            if(!p.isAsync && p.pListener instanceof Activity)
                ((Activity)p.pListener).runOnUiThread(new Runnable() {
                    public void run() {
                        p.pListener.onCallback(iCallback, param);
                    }
                });
            else
                p.pListener.onCallback(iCallback, param);
        }
    }

    void sendRequest(final String method, final String path, final Class<?> resultType, final Object params, final String etag, final CJob callback) {
        sendRequest(method, path, resultType, params, etag, null, callback);
    }

    void sendRequest(final String method, final String path, final Class<?> resultType, final Object params, final String etag, final ArrayList<?> timestamps, final CJob callback) {
        final RestClient restClient = this.mClient;
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                RestClient.Result res = null;
                try {
                    StringWriter writer = new StringWriter();
                    if(params != null) {
                        if(params instanceof String)
                            writer.append((String)params);
                        else
                            mObjectMapper.writeValue(writer, params);
                    }
                    String json = writer.toString();
                    writer.close();

                    res = restClient.send(method, path, json, mToken, etag, timestamps);

                    Log.i("API", "API Request:\r\n" +
                            method + " /1/" + path + "\r\n" +
                            "Request: " + (params != null ? params.toString() : "") + "\r\n" +
                            "Response: (" + res.resultCode + ") " + (res.result != null ? res.result : ""));

                    Object resultObj = null;
                    if(resultType != null) {
                        ObjectReader reader = mObjectMapper.reader(resultType);
                        resultObj = res.result.length() > 0 ? reader.readValue(res.result) : null;
                    }

                    callback.complete(res.resultCode, resultObj);
                } catch (Exception ex){
                    //this.stop();
                    Log.e("API", "Error", ex);
                    Log.i("API", "api: " + ex.getMessage());
                    ex.printStackTrace();
                    callback.complete(res != null ? res.resultCode : 0, null);
                    Log.e("Request Failed", "Request failed: " + method + " " + path + "(" + (res != null ? res.resultCode : "-") + ")");
                }
            }
        }, method + " " + path);

        worker.start();
    }

    /*
     * Uma vez logado, a instância da classe poderá fazer referência apenas a uma sessão.
     * Por exemplo: se o login for efetuado com sucesso para o usuário Fulano, apenas os formulários
     * de Fulano poderão ser recuperados.
     */
    public void tryLogin(String username, String password) {
        LoginRequest obj = new LoginRequest();
        obj.username = username;
        obj.password = password;


        sendRequest("POST", "session", LoginResponse.class, obj, null, new CJob<LoginResponse>() {
            public void complete(int resultCode, LoginResponse result) {
                LoginResultCallback callback = new LoginResultCallback();
                try {
                    callback.result = (resultCode == 201 || resultCode == 200 ? EResult.OK : EResult.InvalidPassword);
                    if(resultCode == 201 || resultCode == 200) {
                        callback.response = result.response;
                        mToken = result.response.session_id;
                        UPPSCache.currentToken = mToken;
                    } else if(resultCode == 0) {
                        callback.result = EResult.NoConnection;
                    }

                    NotificationCenter.init();
                } catch(Exception ex) {
                    callback.result = EResult.Fail;
                }
                postCallback(LoginResultCallback.k_iCallback, callback);
            }
        });
    }

    public void requestUserInfo(int userId) {
        requestUserInfo(userId, null);
    }

    public void requestUserInfo(int userId, final String etag) {
        sendRequest("GET", "users/" + userId, UserInfoResponse.class, null, etag, new CJob<UserInfoResponse>() {
            public void complete(int resultCode, UserInfoResponse result) {
                UserInfoCallback callback = new UserInfoCallback();
                try {
                    EResult res = EResult.OK;
                    switch(resultCode) {
                        case 403:
                            res = EResult.Deactivated;
                            break;
                        case 404:
                            res = EResult.NotFound;
                            break;
                        case 200:
                            res = EResult.OK;
                            break;
                        case 304:
                            res = EResult.NotModified;
                    }

                    callback.result = res;
                    if(res == EResult.OK) {
                        callback.response = result.response;
                    }
                } catch(Exception ex) {
                    callback.result = EResult.Fail;
                }
                postCallback(UserInfoCallback.k_iCallback, callback);
            }
        });
    }

    public void requestUsers(int user_id) {
        sendRequest("GET", "users/" + user_id + "/users", UsersResponse.class, null, null, new CJob<UsersResponse>() {
            @Override
            public void complete(int resultCode, UsersResponse result) {
                UsersCallback callback = new UsersCallback();
                callback.result = resultCode == 200 ? EResult.OK : EResult.Fail;

                try {
                    if(callback.result == EResult.OK) {
                        callback.assignments = result.response;
                    }
                }
                catch (Exception ex) {
                    callback.result = EResult.Fail;
                }
                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void swapSubmissions(int from, int to, Date date) {
        SwapSubmissionsRequest request = new SwapSubmissionsRequest();
        request.user_id_from = from;
        request.user_id_to = to;
        if(date != null)
            request.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(date);

        sendRequest("POST", "submissions/swap", null, request, null, new CJob() {
            @Override
            public void complete(int resultCode, Object result) {
                SwapSubmissionsCallback callback = new SwapSubmissionsCallback();
                callback.result = resultCode == 200 ? EResult.OK : EResult.Fail;

                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void transferSubmissions(int dest_id, ArrayList<Integer> submission_ids, Date date) {
        TransferSubmissionsRequest request = new TransferSubmissionsRequest();
        request.user_id = dest_id;
        request.submissions_ids = submission_ids;
        if(date != null)
            request.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(date);

        sendRequest("POST", "submissions/transfer", null, request, null, new CJob() {
            @Override
            public void complete(int resultCode, Object result) {
                TransferSubmissionsCallback callback = new TransferSubmissionsCallback();
                callback.result = resultCode == 200 || resultCode == 204 ? EResult.OK : EResult.Fail;

                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void requestSubmissions(int user_id) {
        ArrayList<GenericItemsRequest.Item> items = new ArrayList<GenericItemsRequest.Item>();
        for(int i = 0; i < UPPSCache.getFormCount(); i++) {
            UPPSForm form = UPPSCache.getFormAt(i);
            if(form == null)
                continue;

            for(int j = 0; j < form.submissions.size(); j++) {
                UPPSSubmission submission = form.submissions.get(j);
                if(submission != null && submission.updated_at != null && (submission.id & 0x80000000) != 0x80000000) // submission isn't local
                {
                    GenericItemsRequest.Item item = new GenericItemsRequest.Item();
                    item.id = submission.id;
                    item.timestamp = submission.updated_at.getTime() / 1000L;

                    items.add(item);
                }
            }
        }

        sendRequest("GET", "users/" + user_id + "/submissions", NewSubmissionsResponse.class, null, mToken, items, new CJob<NewSubmissionsResponse>() {
            @Override
            public void complete(int resultCode, NewSubmissionsResponse result) {
                NewSubmissionsCallback callback = new NewSubmissionsCallback();
                callback.result = resultCode == 200 ? EResult.OK : EResult.Fail;

                try {
                    if(callback.result == EResult.OK) {
                        callback.items = result.response;
                    }
                } catch (Exception ex) {
                    callback.result = EResult.Fail;
                    ex.printStackTrace();
                }

                postCallback(NewSubmissionsCallback.k_iCallback, callback);
            }
        });
    }

    public void legacy_requestSubmissions(int user_id) {
        sendRequest("GET", "users/" + user_id + "/submissions", SubmissionsResponse.class, null, null, new CJob<SubmissionsResponse>() {
            @Override
            public void complete(int resultCode, SubmissionsResponse result) {
                SubmissionsCallback callback = new SubmissionsCallback();
                callback.result = resultCode == 200 ? EResult.OK : EResult.Fail;

                if(callback.result == EResult.OK) {
                    callback.submissions = result.response;
                }

                postCallback(SubmissionsCallback.k_iCallback, callback);
            }
        });
    }

    public void requestTexts() {
        sendRequest("GET", "texts", TextsResponse.class, null, null, new CJob<TextsResponse>() {
            @Override
            public void complete(int resultCode, TextsResponse result) {
                TextsCallback callback = new TextsCallback();

                EResult res = EResult.Fail;
                switch(resultCode) {
                    case 403:
                        res = EResult.Deactivated;
                        break;
                    case 200:
                        res = EResult.OK;
                        break;
                }

                callback.result = res;
                callback.texts = result.response;
                postCallback(TextsCallback.k_iCallback, callback);
            }
        });
    }

    public void requestForms(final int userId) {
        ArrayList<GenericItemsRequest.Item> items = new ArrayList<GenericItemsRequest.Item>();
        for(int i = 0; i < UPPSCache.getFormCount(); i++) {
            UPPSForm form = UPPSCache.getFormAt(i);

            if(form != null && form.updated_at != null) {
                GenericItemsRequest.Item item = new GenericItemsRequest.Item();
                item.id = form.id;
                item.timestamp = form.updated_at.getTime() / 1000L;

                items.add(item);
            }
        }

        sendRequest("GET", "users/" + userId + "/forms", NewFormsResponse.class, null, mToken, items, new CJob<NewFormsResponse>() {
            public void complete(int resultCode, NewFormsResponse result) {
                NewFormsCallback callback = new NewFormsCallback();
                try {
                    EResult res = EResult.Fail;
                    switch(resultCode) {
                        case 403:
                            res = EResult.Deactivated;
                            break;
                        case 404:
                            res = EResult.NotFound;
                            break;
                        case 200:
                            res = EResult.OK;
                            break;
                    }

                    callback.result = res;
                    if(res == EResult.OK) {
                        callback.userId = userId;
                        callback.forms = result.response;
                    }
                } catch(Exception ex) {
                    callback.result = EResult.Fail;
                    ex.printStackTrace();
                }
                postCallback(NewFormsCallback.k_iCallback, callback);
            }
        });
    }

    public void publishSubmission(final UPPSSubmission submission) {
        PublishSubmissionRequest request = new PublishSubmissionRequest();
        request.started_at = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(submission.started_at);

        for(String key : submission.formdata.keySet()) {
            UPPSForm.Input input = submission.form.getInput(key);
            Object value = submission.serializeField(key);

            if(value == null)
                continue;

            request.addAnswer(Integer.parseInt(input.name), value);
        }

        sendRequest("POST", "forms/" + submission.form.id + "/submissions", PublishSubmissionResponse.class, request, null, new CJob<PublishSubmissionResponse>() {
            @Override
            public void complete(int resultCode, PublishSubmissionResponse result) {
            PublishSubmissionCallback callback = new PublishSubmissionCallback();
            callback.result = resultCode == 201 || resultCode == 200 ? EResult.OK : EResult.Fail;
            callback.local_id = submission.id;

            if(resultCode == 201 || resultCode == 200) {
                callback.submission_id = result.response.submission_id;
                submission.id = callback.submission_id;
            }

            postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void updateSubmission(final UPPSSubmission submission, UPPSSubmission.State newState, Date date) {
        UpdateSubmissionRequest request = new UpdateSubmissionRequest();
        if(submission.started_at != null)
            request.started_at = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(submission.started_at);
        if(date != null)
            request.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(date);

        String state = "waiting_approval";
        switch (newState) {
            case WaitingForApproval:
                state = "waiting_approval";
                break;
        }

        request.status = state;

        for(String key : submission.formdata.keySet()) {
            UPPSForm.Input input = submission.form.getInput(key);
            Object value = submission.serializeField(key);

            if(value == null)
                continue;

            request.addAnswer(Integer.parseInt(input.name), value);
        }

        sendRequest("PUT", "submissions/" + submission.id, null, request, null, new CJob() {
            @Override
            public void complete(int resultCode, Object result) {
                UpdateSubmissionCallback callback = new UpdateSubmissionCallback();
                callback.result = resultCode == 204 ? EResult.OK : EResult.Fail;
                callback.local_id = submission.id;
                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void correctSubmission(final UPPSSubmission submission, Date date) {
        CorrectSubmissionRequest request = new CorrectSubmissionRequest();
        if(date != null)
            request.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(date);

        for(UPPSSubmission.ReviewData reviewdata : submission.reviewdata) {
            request.addCorrection(Integer.parseInt(reviewdata.fieldName), reviewdata.message);
        }

        sendRequest("PUT", "forms/" + submission.form.id + "/submissions/" + submission.id + "", null, request, null, new CJob() {
            @Override
            public void complete(int resultCode, Object result) {
                CorrectSubmissionCallback callback = new CorrectSubmissionCallback();
                callback.result = resultCode == 204 ? EResult.OK : EResult.Fail;
                callback.local_id = submission.id;
                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void rescheduleSubmission(final UPPSSubmission submission) {
        if((submission.id & 0x80000000) == 0x80000000) {
            PublishSubmissionRequest request = new PublishSubmissionRequest();
            if(submission.started_at != null)
                request.started_at = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(submission.started_at);

            for(String key : submission.formdata.keySet()) {
                UPPSForm.Input input = submission.form.getInput(key);
                Object value = submission.serializeField(key);

                if(value == null)
                    continue;

                request.addAnswer(Integer.parseInt(input.name), value);
            }

            if(submission.rescheduleDate != null)
                request.status = "rescheduled";
            else
                request.status = "cancelled";

            sendRequest("POST", "forms/" + submission.form.id + "/submissions", PublishSubmissionResponse.class, request, null, new CJob<PublishSubmissionResponse>() {
                @Override
                public void complete(int resultCode, PublishSubmissionResponse result) {
                    if(resultCode == 201 || resultCode == 200) {
                        submission.id = result.response.submission_id;

                        RescheduleSubmissionRequest reschedule = new RescheduleSubmissionRequest();
                        reschedule.reason_id = submission.stopReason;
                        if(submission.rescheduleDate != null)
                            reschedule.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(submission.rescheduleDate);

                        sendRequest("POST", "forms/" + submission.form.id + "/submissions/" + submission.id + "/reschedule", null, reschedule, null, new CJob() {
                            @Override
                            public void complete(int resultCode, Object result) {
                                RescheduleSubmissionCallback callback = new RescheduleSubmissionCallback();
                                callback.result = resultCode == 204 ? EResult.OK : EResult.Fail;
                                callback.local_id = submission.id;
                                postCallback(callback.k_iCallback, callback);
                            }
                        });
                    } else {
                        RescheduleSubmissionCallback callback = new RescheduleSubmissionCallback();
                        callback.result = EResult.Fail;
                        callback.local_id = submission.id;
                        postCallback(callback.k_iCallback, callback);
                    }
                }
            });
        } else {
            RescheduleSubmissionRequest reschedule = new RescheduleSubmissionRequest();
            reschedule.reason_id = submission.stopReason;
            if(submission.rescheduleDate != null)
                reschedule.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(submission.rescheduleDate);


            sendRequest("POST", "forms/" + submission.form.id + "/submissions/" + submission.id + "/reschedule", null, reschedule, null, new CJob() {
                @Override
                public void complete(int resultCode, Object result) {
                    RescheduleSubmissionCallback callback = new RescheduleSubmissionCallback();
                    callback.result = resultCode == 204 ? EResult.OK : EResult.Fail;
                    postCallback(callback.k_iCallback, callback);
                }
            });
        }
    }

    public void moderateSubmission(final UPPSSubmission submission, String action, Date date) {
        ModerateSubmissionRequest request = new ModerateSubmissionRequest();
        request.submission_id = submission.id;
        request.submission_action = action;
        if(date != null)
            request.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(date);

        sendRequest("POST", "forms/" + submission.form.id + "/submissions/" + submission.id + "/moderate", null, request, null, new CJob() {
            @Override
            public void complete(int resultCode, Object result) {
                ModerateSubmissionCallback callback = new ModerateSubmissionCallback();
                callback.result = resultCode == 204 ? EResult.OK : EResult.Fail;
                callback.local_id = submission.id;
                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void requestAppInfo() {
        sendRequest("GET", "application", AppInfoResponse.class, null, null, new CJob<AppInfoResponse>() {
            @Override
            public void complete(int resultCode, AppInfoResponse result) {
                AppInfoCallback callback = new AppInfoCallback();
                callback.result = resultCode == 200 ? EResult.OK : EResult.Fail;

                if(callback.result == EResult.OK && result != null && result.response != null) {
                    callback.title_line1 = result.response.title_line_1;
                    callback.title_line2 = result.response.title_line_2;
                    callback.header_url = result.response.header_url;
                }

                postCallback(callback.k_iCallback, callback);
            }
        });
    }

    public void loadBitmap(final String url, final Object tag, final int density, final int targetDensity) {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL connection = new URL(new URL("http://api.atipp.com.br/"), url);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inDensity = density;
                    options.inTargetDensity = targetDensity;
                    Bitmap bitmap = BitmapFactory.decodeStream(connection.openConnection().getInputStream(), null, options);

                    BitmapLoadedCallback callback = new BitmapLoadedCallback();
                    callback.bitmap = bitmap;
                    callback.tag = tag;
                    postCallback(callback.k_iCallback, callback);
                } catch (Exception ex) {

                } catch (OutOfMemoryError ex) {

                }
            }
        }, "Load bitmap: " + url);

        worker.start();
    }
}
