package com.ezlol.vkmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//vkAudioLink = new AppAPI.Auth("330155561f0f86218afb51b1fae0ac79b1f87a648deeebb6632b19dcc8b4f83726b426cd141805c399ae4", "a8a41f4d2920819806", true);
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    AppAPI.Auth authLink;

    Button exitBtn;
    LinearLayout tracksLayout, bottomCurrentTrackLayout;
    ImageView BCTImage, BCTPlayBtn, BCTPauseBtn, BCTNextBtn;
    TextView BCTName, BCTAuthor;
    ProgressBar tracksProgressBar;

    MediaPlayer mediaPlayer = new MediaPlayer();

    private int currentTrack = 0;
    ArrayList<JSONObject> tracksData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tracksLayout = findViewById(R.id.tracks);

        bottomCurrentTrackLayout = findViewById(R.id.bottomCurrentTrack);
        BCTImage = findViewById(R.id.BCTImage);
        BCTPlayBtn = findViewById(R.id.BCTPlayBtn);
        BCTPauseBtn = findViewById(R.id.BCTPauseBtn);
        BCTNextBtn = findViewById(R.id.BCTNextBtn);
        BCTName = findViewById(R.id.BCTName);
        BCTAuthor = findViewById(R.id.BCTAuthor);
        exitBtn = findViewById(R.id.exitBtn);
        tracksProgressBar = findViewById(R.id.tracksProgressBar);

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

        exitBtn.setOnClickListener(this);
        BCTPlayBtn.setOnClickListener(this);
        BCTPauseBtn.setOnClickListener(this);
        BCTNextBtn.setOnClickListener(this);
        bottomCurrentTrackLayout.setOnClickListener(this);

        LoadingAudio loadingAudioTask = new LoadingAudio();
        loadingAudioTask.execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.exitBtn: {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("access_token", null)
                        .putString("secret", null)
                        .apply();
                Toast.makeText(this, R.string.exit, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return; // ХЗ, надо ли раз есть finish(), но на всякий случай
            }

            case R.id.BCTPauseBtn:{
                BCTPauseBtn.setVisibility(View.GONE);
                BCTPlayBtn.setVisibility(View.VISIBLE);
                mediaPlayer.pause();
                break;
            }

            case R.id.BCTPlayBtn:{
                BCTPlayBtn.setVisibility(View.GONE);
                BCTPauseBtn.setVisibility(View.VISIBLE);
                mediaPlayer.start();
                break;
            }

            case R.id.BCTNextBtn:{
                Snackbar.make(BCTNextBtn, "I can't ;/", Snackbar.LENGTH_SHORT).show();
                break;
            }
        }
    }

    class LoadingAudio extends AsyncTask<Void, Void, Map<LinearLayout, JSONObject>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tracksProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Map<LinearLayout, JSONObject> doInBackground(Void... voids) {
            JSONObject jsonObject = authLink.getAudios();
            if(jsonObject != null){
                if(jsonObject.has("response")) {
                    try {
                        JSONArray response = jsonObject.getJSONArray("response");
                        if (response.length() > 0) {
                            JSONArray tracks = response.getJSONObject(0).getJSONArray("items");

                            Map<LinearLayout, JSONObject> tracksLayouts = new HashMap<>();

                            ImageView trackImage;
                            LinearLayout trackNamesLayout;
                            TextView trackName, trackAuthor, trackDuration;
                            for (int i = 0; i < tracks.length(); i++) {
                                JSONObject track = tracks.getJSONObject(i);
                                tracksData.add(track);

                                LinearLayout trackLayout = new LinearLayout(getApplicationContext());
                                trackLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                trackLayout.setOrientation(LinearLayout.HORIZONTAL);
                                trackLayout.setPadding((int) getResources().getDimension(R.dimen.trackLayoutLeftPadding),
                                        (int) getResources().getDimension(R.dimen.trackLayoutTopPadding),
                                        (int) getResources().getDimension(R.dimen.trackLayoutRightPadding),
                                        (int) getResources().getDimension(R.dimen.trackLayoutBottomPadding));

                                trackImage = new ImageView(getApplicationContext());
                                trackImage.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                trackImage.getLayoutParams().height = (int) getResources().getDimension(R.dimen.trackImageHeight);
                                trackImage.getLayoutParams().width = (int) getResources().getDimension(R.dimen.trackImageWidth);
                                try {
                                    URL urlConnection = new URL(track.getJSONObject("album").getJSONObject("thumb").getString("photo_135"));
                                    HttpURLConnection connection = (HttpURLConnection) urlConnection
                                            .openConnection();
                                    connection.setDoInput(true);
                                    connection.connect();
                                    InputStream input = connection.getInputStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                                    trackImage.setImageBitmap(bitmap);
                                } catch (IOException ignored){}

                                trackNamesLayout = new LinearLayout(getApplicationContext());
                                trackNamesLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                trackNamesLayout.setOrientation(LinearLayout.VERTICAL);

                                LinearLayout.LayoutParams trackNameAndAuthorParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                trackNameAndAuthorParams.setMargins((int) getResources().getDimension(R.dimen.trackNameAndAuthorLeftMargin),
                                        (int) getResources().getDimension(R.dimen.trackNameAndAuthorTopMargin),
                                        (int) getResources().getDimension(R.dimen.trackNameAndAuthorRightMargin),
                                        (int) getResources().getDimension(R.dimen.trackNameAndAuthorBottomMargin));

                                trackName = new TextView(getApplicationContext());
                                trackName.setLayoutParams(trackNameAndAuthorParams);
                                trackName.setTypeface(Typeface.DEFAULT_BOLD);
                                trackName.setText(track.getString("title"));

                                trackAuthor = new TextView(getApplicationContext());
                                trackAuthor.setLayoutParams(trackNameAndAuthorParams);
                                trackAuthor.setText(track.getString("artist"));

                                trackNamesLayout.addView(trackName);
                                trackNamesLayout.addView(trackAuthor);

                                trackDuration = new TextView(getApplicationContext());
                                trackDuration.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                                trackDuration.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                                trackDuration.setText(AppAPI.beautifySeconds(track.getInt("duration")));

                                trackLayout.addView(trackImage);
                                trackLayout.addView(trackNamesLayout);
                                trackLayout.addView(trackDuration);

                                tracksLayouts.put(trackLayout, track);
                            }
                            return tracksLayouts;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                Log.e("ERROR", "!23");
                Snackbar.make(tracksLayout, R.string.failed_audio_loading, Snackbar.LENGTH_LONG).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Map<LinearLayout, JSONObject> tracksLayouts) {
            super.onPostExecute(tracksLayouts);
            tracksProgressBar.setVisibility(View.GONE);
            for (Map.Entry<LinearLayout, JSONObject> e: tracksLayouts.entrySet()) {
                LinearLayout trackLayout = e.getKey();
                JSONObject trackData = e.getValue();
                trackLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            int trackID = trackData.getInt("id");
                            if(currentTrack != trackID) {
                                for(LinearLayout t : tracksLayouts.keySet()) {
                                    t.setBackgroundColor(getResources().getColor(R.color.white));
                                }
                                CreateMediaPlayerFromURI createMediaPlayerFromURITask = new CreateMediaPlayerFromURI();
                                createMediaPlayerFromURITask.execute(AppAPI.toMP3(trackData.getString("url")));
                                trackLayout.setBackgroundColor(getResources().getColor(R.color.track_selected));

                                // BCT
                                BCTAuthor.setText(trackData.getString("artist"));
                                BCTName.setText(trackData.getString("title"));
                                BCTPlayBtn.setVisibility(View.GONE);
                                BCTPauseBtn.setVisibility(View.VISIBLE);
                                new SetBCTInfo().execute(trackData);

                                currentTrack = trackID;
                            }
                        } catch (JSONException ignored) {}
                    }
                });
                tracksLayout.addView(trackLayout);
            }
        }

        class CreateMediaPlayerFromURI extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... urls) {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(urls[0]);

                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mediaPlayer.stop();
                            mediaPlayer.reset();
                        }
                    });
                    mediaPlayer.prepare();
                }catch (IOException ignored){}
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                mediaPlayer.start();
            }
        }
    }

    class SetBCTInfo extends AsyncTask<JSONObject, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(JSONObject... jsonObjects) {
            JSONObject trackData = jsonObjects[0];
            try {
                URL urlConnection = new URL(trackData.getJSONObject("album").getJSONObject("thumb").getString("photo_135"));
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException | JSONException ignored){}
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if(bitmap != null){
                BCTImage.setImageBitmap(bitmap);
            }
        }
    }
}