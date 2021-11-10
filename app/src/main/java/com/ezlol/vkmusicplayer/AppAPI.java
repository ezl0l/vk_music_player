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
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
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
        Log.e("STRING URL", newToMP3(url));
        try {
            Pattern p = Pattern.compile("/[a-zA-Z\\d]{6,}(/.*?[a-zA-Z\\d]+?)/index\\.m3u8()");
            Matcher m = p.matcher(url);
            if (m.find() && m.groupCount() == 2) {
                return m.replaceFirst("$1$2.mp3");
            }
        }catch (Exception ignored){}
        return url;
    }

    public static String newToMP3(String url){
        // Not a m3u8 url
        if (!url.contains("index.m3u8"))
            return url;
        if (url.contains("/audios/"))
            return url.replaceFirst("^(.+?)/[^/]+?/audios/([^/]+)/.+$", "$1/audios/$2.mp3");
        else
            return url.replaceFirst("^(.+?)/(p[0-9]+)/[^/]+?/([^/]+)/.+$", "$1/$2/$3.mp3");
    }

    public static class Auth {
        private static final Map<String, String> HEADERS = new HashMap<>();

        private String token, secret, deviceID;
        private JSONObject userData;
        private boolean isNeedValidation = false;
        private boolean isNeedCaptcha = false;
        private boolean isSuccess = false;

        private String userID = null;

        private final Requests.Session session = new Requests.Session();

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
            this.deviceID = "nwsrzy1efk4cewiq";//genDeviceID();

            Response r = session.get(String.format("https://oauth.vk.com/token?grant_type=password&scope=nohttps,audio&client_id=2274003&client_secret=hHbZxrka2uZ6jB1inYsH&username=%s&password=%s&v=5.131&2fa_supported=1", username, password), HEADERS);
            if (r != null) {
                Log.e("RESPONSE 0", r.toString());
                JSONObject response = r.json();
                if (!response.has("error")) {
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
                } else {
                    try {
                        String errorType = response.getString("error");
                        switch (errorType) {
                            case "invalid_client": {
                                throw new AuthException("Invalid username or password");
                            }
                            case "need_validation": {
                                this.isNeedValidation = true;
                                //throw new NeedValidationException("Need validation.", response.getString("validation_type"));
                            }
                            case "need_captcha": {
                                this.isNeedCaptcha = true;
                                //throw new NeedCaptchaException("Need captcha.");
                            }
                        }
                    } catch (JSONException ignored) {
                    }
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
            if (r != null) {
                JSONObject response = r.json();
                if (!response.has("error")) {
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
                } else {
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

        public String formRequest(String method, Map<String, String> params) {
            String url = String.format("/method/%s?v=%s&access_token=%s&device_id=%s", method, this.v, this.token, this.deviceID);
            if(params != null) {
                for (Map.Entry<String, String> e : params.entrySet()) {
                    url += "&" + e.getKey() + "=" + e.getValue();
                }
            }
            return url;
        }

        public void refreshUserID(){
            Map<String, String> p = new HashMap<>();
            p.put("func_v", "9");
            JSONObject r = this.send(this.formRequest("execute.getUserInfo", p), null);
            try {
                Log.i("r", r.toString());
                this.userID = r.getJSONObject("response").getJSONObject("profile").getString("id");
            }catch (JSONException ignored){}
        }

        public JSONObject send(String url, Map<String, String> params) {
            String hash;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String toHash = (url + this.secret);
                md.update(toHash.getBytes());
                hash = bytesToHex(md.digest());
            } catch (NoSuchAlgorithmException ignored) {
                return null;
            }
            if (params != null) {
                for (Map.Entry<String, String> e : params.entrySet()) {
                    url += "&" + e.getKey() + "=" + e.getValue();
                }
            }

            String allURL = "https://api.vk.com" + url + "&sig=" + hash;
            Response r = session.get(allURL, HEADERS);
            if (r != null) {
                return r.json();
            }
            return null;
        }

        public JSONObject getAudios(int count) {
            Map<String, String> params = new HashMap<>();
            params.put("code", "return [API.audio.get({count:" + count + "})];");//,API.audio.getPlaylists({count:1,owner_id:API.users.get()[0].id})
            return send(formRequest("execute", params), null);
        }

        public JSONObject getAudios() {
            return getAudios(100);
        }

        public JSONObject getAudios(int count, int albumID){
            Map<String, String> params = new HashMap<>();
            params.put("code", "return [API.audio.get({count:" + count + ",album_id:" + albumID + "})];");//,API.audio.getPlaylists({count:1,owner_id:API.users.get()[0].id})
            return send(formRequest("execute", params), null);
        }

        public JSONObject getAudioById(String ids){
            Map<String, String> params = new HashMap<>();
            //params.put("code", "return [API.audio.getById({audios:" + ids + "})];");
            //return send(formRequest("execute", params), null);
            params.put("audios", ids);
            return send(formRequest("audio.getById", params), null);
        }

        public JSONObject getAudioById(List ids){
            return getAudioById(join(ids));
        }

        public JSONObject getPlaylists(int ownerID, int count, int offset, boolean extended){
            Map<String, String> params = new HashMap<>();
            params.put("owner_id", String.valueOf(ownerID));
            params.put("count", String.valueOf(count));
            params.put("offset", String.valueOf(offset));
            params.put("extended", extended ? "1" : "0");
            return send(formRequest("audio.getPlaylists", params), null);
        }

        public JSONObject getPlaylists(int ownerID) {
            return getPlaylists(ownerID, 20, 0, false);
        }

        public JSONObject getPlaylists() {
            if(this.userID == null)
                refreshUserID();
            return getPlaylists(Integer.parseInt(this.userID));
        }

        private static String bytesToHex(byte[] bytes) {
            BigInteger bigInt = new BigInteger(1, bytes);
            String sb = bigInt.toString(16);

            while( sb.length() < 32 ){
                sb = "0" + sb;
            }
            return sb;
        }

        public String addOwnerId2Id(String id){
            if(this.userID == null)
                refreshUserID();
            return this.userID + "_" + id;
        }

        public String getUserID() {
            return userID;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String beautifySeconds(int seconds){
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static String join(List list){
        String s = "";
        for(int i = 0; i < list.size(); i++)
            s += list.get(i);
        return s;
    }
}
