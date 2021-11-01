package com.ezlol.vkmusicplayer;

import android.annotation.SuppressLint;
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
    public static class AuthException extends java.lang.Exception {
        public AuthException(String message) {super(message);}
    }

    public static class NeedValidationException extends java.lang.Exception {
        private String type;
        public NeedValidationException(String message) {
            super(message);
        }
        public NeedValidationException(String message, String type) {
            super(message);
            this.type = type;
        }
        public String getType() {
            return type;
        }
    }

    public static class NeedCaptchaException extends java.lang.Exception {
        private String type;
        public NeedCaptchaException(String message) {
            super(message);
        }
        public NeedCaptchaException(String message, String type) {
            super(message);
            this.type = type;
        }
        public String getType() {
            return type;
        }
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
        Log.e("STRING URL", url);
        Pattern p = Pattern.compile("/[a-zA-Z\\d]{6,}(/.*?[a-zA-Z\\d]+?)/index\\.m3u8()");
        Matcher m = p.matcher(url);
        m.find();
        Log.e("MATCHES", m.groupCount() + "-");
        if(m.groupCount() == 2) {
            Log.e("GROUP 0", m.group(0) + " - 0");
            Log.e("GROUP 1", m.group(1) + " - 1");
            return m.replaceFirst("$1$2.mp3");
        }
        return null;
    }

    public static class Auth {
        private static Map<String, String> HEADERS = new HashMap<>();

        private String token, secret, deviceID;
        private JSONObject userData;
        private boolean isNeedValidation = false;
        private boolean isNeedCaptcha = false;
        private boolean isSuccess = false;

        private final Requests.Session session = new Requests.Session();;

        double v = 5.95;

        public Auth(String token, String secret, boolean isUseToken) {
            HEADERS.put("User-Agent", "VKAndroidApp/4.13.1-1206 (Android 4.4.3; SDK 19; armeabi; ; ru)");
            HEADERS.put("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, */*");
            this.secret = secret;
            this.token = token;
            this.deviceID = "nwsrzy1efk4cewiq";//genDeviceID();
        }

        public Auth(String username, String password) throws AuthException, NeedValidationException, NeedCaptchaException {
            HEADERS.put("User-Agent", "VKAndroidApp/4.13.1-1206 (Android 4.4.3; SDK 19; armeabi; ; ru)");
            HEADERS.put("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, */*");
            this.deviceID = "nwsrzy1efk4cewiq";

            Response r = session.get(String.format("https://oauth.vk.com/token?grant_type=password&scope=nohttps,audio&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&username=%s&password=%s&v=5.131&2fa_supported=1", username, password), HEADERS);
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
                        throw new AuthException("Invalid username or password");
                    }
                }else{
                    try {
                        String errorType = response.getString("error");
                        switch (errorType){
                            case "invalid_client":{
                                throw new AuthException("Invalid username or password");
                            }
                            case "need_validation":{
                                this.isNeedValidation = true;
                                //throw new NeedValidationException("Need validation.", response.getString("validation_type"));
                            }
                            case "need_captcha":{
                                this.isNeedCaptcha = true;
                                //throw new NeedCaptchaException("Need captcha.");
                            }
                        }
                    }catch (JSONException ignored){}
                }
            }
        }

        public boolean isNeedCaptcha() {
            return isNeedCaptcha;
        }

        public boolean isNeedValidation() {
            return isNeedValidation;
        }

        public boolean validate(String code, String username, String password, boolean forceSMS) throws AuthException {
            Response r = session.get(String.format("https://oauth.vk.com/token?grant_type=password&scope=nohttps,audio&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&username=%s&password=%s&v=5.131&2fa_supported=1&code=%s&force_sms=", username, password, code, forceSMS ? "1" : "0"), HEADERS);
            if(r != null){
                JSONObject response = r.json();
                if(!response.has("error")){
                    try {
                        this.secret = response.getString("secret");
                        this.token = response.getString("access_token");

                        Map<String, String> p = new HashMap<>();
                        p.put("func_v", "9");
                        this.send(this.formRequest("execute.getUserInfo", p), null);
                        this.send(String.format("/method/auth.refreshToken?access_token=%s&v=%s&device_id=%s&lang=ru", this.token, this.v, this.deviceID), null);
                        return true;
                    } catch (JSONException ignored) {
                        throw new AuthException("Invalid username or password");
                    }
                }else{
                    Log.e("AppAPI", "Validation error: " + r);
                }
            }
            return false;
        }

        public boolean validate(String code, String username, String password) throws AuthException {
            return validate(code, username, password, false);
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
            return url;
        }

        public JSONObject send(String url, Map<String, String> params){
            String hash;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String toHash = (url + this.secret);
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
            Response r = session.get(allURL, HEADERS);
            if(r != null){
                return r.json();
            }
            return null;
        }

        public JSONObject getAudios(int count){
            Map<String, String> params = new HashMap<>();
            params.put("code", "return [API.audio.get({count:" + count + "})];");//,API.audio.getPlaylists({count:1,owner_id:API.users.get()[0].id})
            return this.send(formRequest("execute", params), null);
        }

        public JSONObject getAudios(){
            return getAudios(100);
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

    @SuppressLint("DefaultLocale")
    public static String beautifySeconds(int seconds){
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
