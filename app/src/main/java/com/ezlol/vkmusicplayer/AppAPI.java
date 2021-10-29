package com.ezlol.vkmusicplayer;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppAPI {
    public static class Exception extends java.lang.Exception {
        public Exception(String message) {super(message);}
    }

    public static String genDeviceID(int length, char[] symbols){
        String s = "";
        for(int i = 0; i < length; i++){
            s += symbols[(int) (Math.random() * symbols.length)];
        }
        return s;
    }

    public static String genDeviceID(){
        return genDeviceID(16, new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'});
    }

    public static String toMP3(String url){
        Pattern p = Pattern.compile("[a-zA-Z\\d]{6,}(/.*?[a-zA-Z\\d]+?)/index.m3u8()");
        Matcher m = p.matcher(url);
        if(m.matches() && m.groupCount() == 2) {
            Log.e("GROUP 0", m.group(0) + " - 0");
            Log.e("GROUP 1", m.group(1) + " - 1");
            return url.replaceFirst(m.group(0), "").replaceFirst(m.group(1), m.group(1));
        }
        return null;
    }

    public static class Auth {
        private static Map<String, String> HEADERS = new HashMap<>();

        private String token, secret, deviceID;
        private JSONObject userData;

        private Requests.Session session = new Requests.Session();;

        double v = 5.95;

        public Auth(String token, String secret, boolean isUseToken) {
            HEADERS.put("User-Agent", "VKAndroidApp/4.13.1-1206 (Android 4.4.3; SDK 19; armeabi; ; ru)");
            HEADERS.put("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, */*");
            this.secret = secret;
            this.token = token;
            this.deviceID = "nwsrzy1efk4cewiq";//genDeviceID();
        }

        public Auth(String username, String password) throws Exception {
            HEADERS.put("User-Agent", "VKAndroidApp/4.13.1-1206 (Android 4.4.3; SDK 19; armeabi; ; ru)");
            HEADERS.put("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, */*");
            this.deviceID = "nwsrzy1efk4cewiq";

            Response r = session.get(String.format("https://oauth.vk.com/token?grant_type=password&scope=nohttps,audio&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&username=%s&password=%s", username, password), HEADERS);
            Log.e("RESPONSE 0", r.toString());
            if(r != null){
                Log.e("RESPONSE 0", r.toString());
                JSONObject response = r.json();
                if(!response.has("error")){
                    try {
                        this.secret = response.getString("secret");
                        this.token = response.getString("access_token");

                        Map<String, String> p = new HashMap<>();
                        p.put("func_v", "9");
                        this.send(this.formRequest("execute.getUserInfo", p), null);
                        this.send(String.format("/method/auth.refreshToken?access_token=%s&v=%s&device_id=%s&lang=ru", this.token, this.v, this.deviceID), null);
                    } catch (JSONException ignored) {
                        throw new Exception("Invalid username or password");
                    }
                }
            }
        }

        public String getSecret() {
            return secret;
        }

        public String getToken() {
            return token;
        }

        public String getDeviceID() {
            return deviceID;
        }

        public String formRequest(String method, Map<String, String> params){
            String url = String.format("/method/%s?v=%s&access_token=%s&device_id=%s", method, this.v, this.token, this.deviceID);
            for (Map.Entry<String, String> e : params.entrySet()) {
                url += "&" + e.getKey() + "=" + e.getValue();
            }
            Log.e("URL in formRequest", url);
            return url;
        }

        public JSONObject send(String url, Map<String, String> params){
            Log.e("URL in send", url);
            String hash;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String toHash = (url + this.secret);
                Log.e("TOHASH", toHash);
                md.update(toHash.getBytes());
                hash = bytesToHex(md.digest());
            } catch (NoSuchAlgorithmException ignored) {
                return null;
            }
            if(params != null) {
                for (Map.Entry<String, String> e : params.entrySet()) {
                    url += "&" + e.getKey() + "=" + e.getValue();
                }
            }

            String allURL = "https://api.vk.com" + url + "&sig=" + hash;
            Log.e("allURL in send 2", allURL);
            Response r = session.get(allURL, HEADERS);
            if(r != null){
                return r.json();
            }
            return null;
        }

        public JSONObject getAudios(){
            Map<String, String> params = new HashMap<>();
            params.put("code", "return [API.audio.get({count:2}),API.audio.getPlaylists({count:1,owner_id:API.users.get()[0].id})];");
            return this.send(formRequest("execute", params), null);
        }

        private static String bytesToHex(byte[] bytes) {
            BigInteger bigInt = new BigInteger(1, bytes);
            String sb = bigInt.toString(16);

            while( sb.length() < 32 ){
                sb = "0" + sb;
            }
            return sb;
        }
    }
}
