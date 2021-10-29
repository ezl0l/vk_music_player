package com.ezlol.vkmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//vkAudioLink = new AppAPI.Auth("330155561f0f86218afb51b1fae0ac79b1f87a648deeebb6632b19dcc8b4f83726b426cd141805c399ae4", "a8a41f4d2920819806", true);
public class MainActivity extends AppCompatActivity {
    AppAPI.Auth authLink;

    LinearLayout tracksLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tracksLayout = findViewById(R.id.tracks);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = preferences.getString("access_token", "");
        String secret = preferences.getString("secret", "");
        if(!(accessToken.length() > 0 && secret.length() > 0)){
            Toast.makeText(this, R.string.exit, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return; // ХЗ, надо ли раз есть finish(), но на всякий случай
        }
        authLink = new AppAPI.Auth(accessToken, secret, true);

        LoadingAudio loadingAudioTask = new LoadingAudio();
        loadingAudioTask.execute();
    }

    class LoadingAudio extends AsyncTask<Void, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Void... voids) {
            return authLink.getAudios();
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            Log.e("RESP", jsonObject.toString());
            if(jsonObject != null){
                if(jsonObject.has("response")) {
                    try {
                        JSONArray response = jsonObject.getJSONArray("response");
                        if (response.length() > 0) {
                            JSONArray tracks = response.getJSONObject(0).getJSONArray("items");
                            JSONObject track;
                            for (int i = 0; i < tracks.length(); i++) {
                                track = tracks.getJSONObject(i);
                                Log.e("TRACK " + track.getString("title"), AppAPI.toMP3(track.getString("url")));
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                Log.e("ERROR", "!23");
                Snackbar.make(tracksLayout, R.string.failed_audio_loading, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}