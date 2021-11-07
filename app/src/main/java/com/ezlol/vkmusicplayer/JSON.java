package com.ezlol.vkmusicplayer;

import org.json.JSONException;
import org.json.JSONObject;

public class JSON {
    public static JSONObject decode(String json){
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
    public static String encode(JSONObject json){
        return json.toString();
    }

    public static boolean isSuccess(JSONObject jsonObject){
        try {
            return jsonObject != null
                    && jsonObject.has("success")
                    && jsonObject.getBoolean("success");
        } catch (JSONException ignored) {
            return false;
        }
    }
}
