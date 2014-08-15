package com.lfdb.parapesquisa.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfdb.parapesquisa.ParaPesquisa;
import com.lfdb.parapesquisa.R;
import com.lfdb.parapesquisa.api.UPPSServer;
import com.lfdb.parapesquisa.api.callbacks.AccessDeniedCallback;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;

/**
 * Created by Igor on 8/1/13.
 */
public class RestClient {
    String mBasePath;

    public class Result {
        public int resultCode;
        public String result;
        public String etag;
    }

    public RestClient(String basePath) {
        mBasePath = basePath;
    }

    public Result send(String method, String action, JSONObject parameters, String sessionId, String etag) throws IOException {
        return send(method, action, (parameters != null ? parameters.toString() : null), sessionId, etag, null);
    }

    public Result send(String method, String action, String data, String sessionId, String etag, ArrayList<?> timestamps) throws IOException {
        org.apache.http.params.BasicHttpParams params = new BasicHttpParams();
        org.apache.http.params.HttpConnectionParams.setConnectionTimeout(params, 10000);

        HttpClient client = new DefaultHttpClient(params);
        HttpResponse response = null;

        if(method.equals("GET")) {
            HttpGet req = new HttpGet(this.mBasePath + action);
            req.setHeader("Accept-Encoding", "gzip");
            if(sessionId != null) {
                req.setHeader("X-Session-ID", sessionId);
            }
            if(etag != null) {
                req.setHeader("If-None-Match", etag);
            }
            if(timestamps != null) {
                StringWriter writer = new StringWriter();
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(writer, timestamps);

                String json = writer.toString();
                writer.close();

                req.setHeader("X-Timestamps", json);
            }
            if(ParaPesquisa.getContext() != null)
                req.setHeader("X-App-Version", ParaPesquisa.getContext().getString(R.string.version));

            response = client.execute(req);
        } else if(method.equals("POST")) {
            byte[] json = data.getBytes("utf-8");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(json);
            gzipOutputStream.close();

            byte[] compressed = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            HttpPost req = new HttpPost(this.mBasePath + action);
            req.setHeader("Accept-Encoding", "gzip");
            if(sessionId != null) {
                req.setHeader("X-Session-ID", sessionId);
            }
            if(timestamps != null) {
                StringWriter writer = new StringWriter();
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(writer, timestamps);

                String jsontimestamps = writer.toString();
                writer.close();

                req.setHeader("X-Timestamps", jsontimestamps);
            }
            if(ParaPesquisa.getContext() != null)
                req.setHeader("X-App-Version", ParaPesquisa.getContext().getString(R.string.version));

            req.setHeader("Content-Type", "application/json; charset=utf-8");
            req.setHeader("Content-Encoding", "gzip");
            req.setEntity(new ByteArrayEntity(compressed));
            response = client.execute(req);
        } else if(method.equals("PUT")) {
            byte[] json = data.getBytes("utf-8");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(json);
            gzipOutputStream.close();

            byte[] compressed = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            HttpPut req = new HttpPut(this.mBasePath + action);
            req.setHeader("Accept-Encoding", "gzip");
            if(sessionId != null) {
                req.setHeader("X-Session-ID", sessionId);
            }
            if(ParaPesquisa.getContext() != null)
                req.setHeader("X-App-Version", ParaPesquisa.getContext().getString(R.string.version));

            req.setHeader("Content-Type", "application/json; charset=utf-8");
            req.setHeader("Content-Encoding", "gzip");
            req.setEntity(new ByteArrayEntity(compressed));
            response = client.execute(req);
        }

        Header encoding = response.getFirstHeader("Content-Encoding");
        HttpEntity ent = response.getEntity();

        String responseStr = null;
        if(ent != null) {
            ByteArrayOutputStream str = new ByteArrayOutputStream(ent.getContentLength() > 0 ? (int)ent.getContentLength() : 1024);

            InputStream contentStr = ent.getContent();
            if(encoding != null && encoding.getValue().equalsIgnoreCase("gzip"))
                contentStr = new GZIPInputStream(contentStr);

            byte buffer[] = new byte[1024];
            int readed = 0;
            while((readed = contentStr.read(buffer, 0, buffer.length)) > 0) {
                str.write(buffer, 0, readed);
            }
            responseStr = str.toString("utf-8");
        }

        Result res = new Result();
        res.result = responseStr;
        res.resultCode = response.getStatusLine().getStatusCode();

        if(response.getFirstHeader("ETag") != null) {
            res.etag = response.getFirstHeader("ETag").getValue();
        }

        if(res.resultCode == 403) {
            UPPSServer.getActiveServer().postCallback(AccessDeniedCallback.k_iCallback, new AccessDeniedCallback());
        }

        return res;
    }
}
