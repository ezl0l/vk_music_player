package com.ezlol.vkmusicplayer;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.*;

class Requests {
    public static final MediaType MEDIA_TYPE = MediaType.parse("multipart/form-data; charset=utf-8");

    public static class Session {
        OkHttpClient client;

        public Session() {
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            client = new OkHttpClient.Builder()
                    .cookieJar(new JavaNetCookieJar(cookieManager))
                    .build();
        }

        public Response get(String url, Map<String, String> headers) {
            try {
                Headers headersBuild = Headers.of(headers);
                Request request = new Request.Builder().url(url).headers(headersBuild).build();
                okhttp3.Response response = client.newCall(request).execute();
                return new Response(response.body().string(), response.code(), response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Response post(String url, Map<String, String> data, Map<String, String> headers){
            try {
                FormBody.Builder body = new FormBody.Builder();
                for (Map.Entry<String, String> e : data.entrySet()) {
                    body.add(e.getKey(), e.getValue());
                }
                Headers headersBuild = Headers.of(headers);
                Request request = new Request.Builder().url(url).headers(headersBuild).post(body.build()).build();
                okhttp3.Response response = client.newCall(request).execute();
                Log.e("EZLOL", (response.headers().values("Set-Cookie") .get(0).split(";"))[0]);
                return new Response(response.body().string(), response.code(), response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Response post(String url, Map<String, String> data){
            return post(url, data, new HashMap<String, String>());
        }

        public Response get(String url){
            return post(url, new HashMap<String, String>());
        }
    }

    public static Response get(String url, Map<String, String> headers) {
        try {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Headers headersBuild = Headers.of(headers);
            Request request = new Request.Builder().url(url).headers(headersBuild).build();
            okhttp3.Response response = client.newCall(request).execute();
            return new Response(response.body().string(), response.code(), response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Response post(String url, Map<String, String> data, Map<String, String> headers){
        try {
            OkHttpClient client = new OkHttpClient();
            FormBody.Builder body = new FormBody.Builder();
            for (Map.Entry<String, String> e : data.entrySet()) {
                body.add(e.getKey(), e.getValue());
            }
            Headers headersBuild = Headers.of(headers);
            Request request = new Request.Builder().url(url).headers(headersBuild).post(body.build()).build();
            okhttp3.Response response = client.newCall(request).execute();
            return new Response(response.body().string(), response.code(), response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Response post(String url, Map<String, String> data){
        return post(url, data, new HashMap<String, String>());
    }

    public static Response get(String url){
        return post(url, new HashMap<String, String>());
    }
}

class Response {
    private final String string;
    private final int statusCode;
    private final okhttp3.Response responseBody;

    public Response(String string, int statusCode, okhttp3.Response responseBody) {
        this.string = string;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public okhttp3.Response getResponseBody() {
        return responseBody;
    }

    public JSONObject json(){
        return JSON.decode(string);
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return string;
    }
}