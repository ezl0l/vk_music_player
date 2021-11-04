package com.ezlol.vkmusicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener {
    static String TRACK_ALBUM_PHOTO_QUALITY = "photo_135";

    AppAPI.Auth authLink;

    ScrollView scrollView;
    Button exitBtn;
    LinearLayout tracksLayout, bottomCurrentTrackLayout, currentTrackLayout;
    ImageView BCTImage, BCTPlayBtn, BCTPauseBtn, BCTNextBtn;
    TextView BCTName, BCTAuthor;
    ProgressBar tracksProgressBar;

    ImageView currentTrackImage, currentTrackBackBtn, currentTrackPauseBtn, currentTrackPlayBtn, currentTrackNextBtn;
    TextView currentTrackCurrentTime, currentTrackDuration, currentTrackName, currentTrackAuthor;
    SeekBar currentTrackSeekBar;

    MediaPlayer mediaPlayer = new MediaPlayer();
    NotificationManager notificationManager;

    private int currentTrack = 0, currentTrackNumber = 0;
    static List<LinearLayout> tracksList = new ArrayList<>();
    static Map<LinearLayout, JSONObject> tracksLayouts = new HashMap<>();
    static Map<Integer, Bitmap> tracksAlbums = new HashMap<>();

    private boolean isCurrentTrackLayoutShow = false;
    private boolean isAudioHandlerTaskWork = true;

    private final String CHANNEL_ID = "VK Music Player";
    private final int NOTIFICATION_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = findViewById(R.id.scrollView);
        tracksLayout = findViewById(R.id.tracks);
        currentTrackLayout = findViewById(R.id.currentTrackLayout);

        bottomCurrentTrackLayout = findViewById(R.id.bottomCurrentTrack);
        BCTImage = findViewById(R.id.BCTImage);
        BCTPlayBtn = findViewById(R.id.BCTPlayBtn);
        BCTPauseBtn = findViewById(R.id.BCTPauseBtn);
        BCTNextBtn = findViewById(R.id.BCTNextBtn);
        BCTName = findViewById(R.id.BCTName);
        BCTAuthor = findViewById(R.id.BCTAuthor);
        exitBtn = findViewById(R.id.exitBtn);
        tracksProgressBar = findViewById(R.id.tracksProgressBar);

        currentTrackImage = findViewById(R.id.currentTrackImage);
        currentTrackBackBtn = findViewById(R.id.currentTrackBackBtn);
        currentTrackPauseBtn = findViewById(R.id.currentTrackPauseBtn);
        currentTrackPlayBtn = findViewById(R.id.currentTrackPlayBtn);
        currentTrackNextBtn = findViewById(R.id.currentTrackNextBtn);
        currentTrackCurrentTime = findViewById(R.id.currentTrackCurrentTime);
        currentTrackDuration = findViewById(R.id.currentTrackDuration);
        currentTrackName = findViewById(R.id.currentTrackName);
        currentTrackAuthor = findViewById(R.id.currentTrackAuthor);
        currentTrackSeekBar = findViewById(R.id.currentTrackSeekBar);

        currentTrackBackBtn.setOnClickListener(this);
        currentTrackPauseBtn.setOnClickListener(this);
        currentTrackPlayBtn.setOnClickListener(this);
        currentTrackNextBtn.setOnClickListener(this);

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
        currentTrackSeekBar.setOnSeekBarChangeListener(this);

        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                Log.i(getClass().getSimpleName(), "i: " + i);
                currentTrackSeekBar.setSecondaryProgress(i);
            }
        });

        //Creating notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "VK Music Player", NotificationManager.IMPORTANCE_DEFAULT);

            //notificationChannel.setDescription(CHANNEL_ID);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        //Creating notification
        NotificationCompat.Builder n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_default_track_album)
                .setContentTitle(CHANNEL_ID)
                .setContentText("MediaPlayer")
                .setSubText("No track selected")
                .setAutoCancel(false);
        NotificationManagerCompat a = NotificationManagerCompat.from(this);
        a.notify(NOTIFICATION_ID, n.build());

        LoadingAudio loadingAudioTask = new LoadingAudio();
        loadingAudioTask.execute();

        getSupportActionBar().hide();

        /*Toast.makeText(this, getCacheDir().toString(), Toast.LENGTH_LONG).show();
        File dir = getCacheDir();
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                Toast.makeText(this, f.toString(), Toast.LENGTH_LONG).show();
            }
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_save:{
                Log.i("TrackCachingMachine", "" + currentTrack);
                saveTrack(currentTrack);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTrack(int trackID, String trackURL){
        Log.i("saveTrack", "trackID: " + trackID + " trackURL: " + trackURL);
        new TrackCachingMachine().execute(String.valueOf(trackID), trackURL);
    }

    private void saveTrack(int trackID){
        Log.i("saveTrack", "success: processing");
        JSONObject currentTrackData = null;
        for(JSONObject trackData : tracksLayouts.values()){
            try {
                if (trackData.getInt("id") == trackID) {
                    currentTrackData = trackData;
                    break;
                }
            }catch (JSONException ignored){}
        }
        if(currentTrackData == null || !currentTrackData.has("url")){
            Log.i("saveTrack", "Not found track or it doesn't have url.");
            return;
        }
        try {
            Log.i("saveTrack", "success: true");
            saveTrack(trackID, currentTrackData.getString("url"));
            return;
        }catch (JSONException ignored){}
        Log.i("saveTrack", "success: false");
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
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

            case R.id.bottomCurrentTrack:{
                getSupportActionBar().show();
                scrollView.setVisibility(View.GONE);
                bottomCurrentTrackLayout.setVisibility(View.GONE);
                exitBtn.setVisibility(View.GONE);
                currentTrackLayout.setVisibility(View.VISIBLE);
                isCurrentTrackLayoutShow = true;
                new AudioHandlerTask().execute();
                break;
            }

            case R.id.BCTPlayBtn:
            case R.id.currentTrackPlayBtn:{
                resumeTrack();
                break;
            }

            case R.id.BCTPauseBtn:
            case R.id.currentTrackPauseBtn:{
                pauseTrack();
                break;
            }

            case R.id.BCTNextBtn:
            case R.id.currentTrackNextBtn:{
                setTrack(++currentTrackNumber);
                break;
            }

            case R.id.currentTrackBackBtn:{
                setTrack(--currentTrackNumber);
                break;
            }
        }
    }

    private void pauseTrack() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            //unselectAllTracks();
            currentTrackPauseBtn.setVisibility(View.GONE);
            currentTrackPlayBtn.setVisibility(View.VISIBLE);
            BCTPauseBtn.setVisibility(View.GONE);
            BCTPlayBtn.setVisibility(View.VISIBLE);
        }
    }

    private void resumeTrack() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            currentTrackPlayBtn.setVisibility(View.GONE);
            currentTrackPauseBtn.setVisibility(View.VISIBLE);
            BCTPlayBtn.setVisibility(View.GONE);
            BCTPauseBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if(isCurrentTrackLayoutShow) {
            getSupportActionBar().hide();
            closeOptionsMenu();
            currentTrackLayout.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
            bottomCurrentTrackLayout.setVisibility(View.VISIBLE);
            exitBtn.setVisibility(View.VISIBLE);
            isCurrentTrackLayoutShow = false;
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if(b){
            mediaPlayer.seekTo(i * 1000);
        }
        Log.e("SEEKBAR", i + " - " + b);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mediaPlayer.pause();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    private void unselectAllTracks(){
        for(int i = 0; i < tracksList.size(); i++){
            tracksList.get(i).setBackgroundResource(R.color.track_unselected);
        }
    }

    public void setTrack(int trackNum){
        try {
            Log.i("setTrackNumber", trackNum + "");
            LinearLayout trackLayout = tracksList.get(trackNum);
            JSONObject trackData = tracksLayouts.get(trackLayout);

            //Change notification
            if (trackData != null) {
                NotificationCompat.Builder n = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_default_track_album)
                        .setContentTitle(CHANNEL_ID)
                        .setContentText(trackData.getString("title"))
                        .setSubText(trackData.getString("artist"))
                        .setAutoCancel(false);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(NOTIFICATION_ID, n.build());
                currentTrack = trackData.getInt("id");
            }
            unselectAllTracks();

            currentTrackNumber = trackNum;
            BCTImage.setImageResource(R.drawable.ic_default_track_album);
            synchronized (this) {
                if(tracksAlbums.containsKey(trackData.getInt("id"))){
                    BCTImage.setImageBitmap(tracksAlbums.get(trackData.getInt("id")));
                }

                File trackFile = new File(getCacheDir(), String.format("%d.music", trackData.getInt("id")));
                if(trackFile.exists()){
                    new CreateMediaPlayerFromFile().execute(trackFile.getAbsolutePath());
                }else {
                    CreateMediaPlayerFromURI createMediaPlayerFromURITask = new CreateMediaPlayerFromURI();
                    String url = AppAPI.toMP3(trackData.getString("url"));
                    if (url != null) {
                        if(url.contains("m3u8")){
                            Toast.makeText(this, R.string.cant_convert_m3u8_to_mp3, Toast.LENGTH_SHORT).show();
                        }
                        createMediaPlayerFromURITask.execute(url);
                    } else {
                        Toast.makeText(this, R.string.error_download_track, Toast.LENGTH_SHORT).show();
                    }
                }
                new SetBCTInfo().execute(trackData);
            }

            trackLayout.setBackgroundResource(R.color.track_selected);
            // BCT
            bottomCurrentTrackLayout.setVisibility(View.VISIBLE);
            BCTAuthor.setText(trackData.getString("artist"));
            BCTName.setText(trackData.getString("title"));
            currentTrackAuthor.setText(trackData.getString("artist"));
            currentTrackName.setText(trackData.getString("title"));
            currentTrackDuration.setText(AppAPI.beautifySeconds(trackData.getInt("duration")));
            currentTrackSeekBar.setMax(trackData.getInt("duration"));

            BCTPlayBtn.setVisibility(View.GONE);
            BCTPauseBtn.setVisibility(View.VISIBLE);
            currentTrackPlayBtn.setVisibility(View.GONE);
            currentTrackPauseBtn.setVisibility(View.VISIBLE);
            currentTrack = trackData.getInt("id");
        }catch (Exception e){
            int s = Log.e("setTrack", "any error:" + e.toString());
        }
    }

    class LoadingAudio extends AsyncTask<Void, Void, Map<LinearLayout, JSONObject>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tracksProgressBar.setVisibility(View.VISIBLE);
            tracksLayouts.clear();
            tracksList.clear();
        }

        @Override
        protected Map<LinearLayout, JSONObject> doInBackground(Void... voids) {
            JSONObject jsonObject = authLink.getAudios();
            Log.i("AUDIO:", jsonObject.toString());
            if(jsonObject != null){
                if(jsonObject.has("response")) {
                    try {
                        JSONArray response = jsonObject.getJSONArray("response");
                        if (response.length() > 0) {
                            JSONArray tracks = response.getJSONObject(0).getJSONArray("items");

                            ImageView trackImage;
                            LinearLayout trackNamesLayout;
                            TextView trackName, trackAuthor, trackDuration;
                            for (int i = 0; i < tracks.length(); i++) {
                                JSONObject track = tracks.getJSONObject(i);

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

                                trackImage.setImageResource(R.drawable.ic_default_track_album);

                                Bitmap bitmap = null;
                                if(tracksAlbums.containsKey(track.getInt("id"))){
                                    bitmap = tracksAlbums.get(track.getInt("id"));
                                }else {
                                    try {
                                        URL urlConnection = new URL(track.getJSONObject("album").getJSONObject("thumb").getString(TRACK_ALBUM_PHOTO_QUALITY));
                                        HttpURLConnection connection = (HttpURLConnection) urlConnection
                                                .openConnection();
                                        connection.setDoInput(true);
                                        connection.connect();
                                        InputStream input = connection.getInputStream();
                                        bitmap = BitmapFactory.decodeStream(input);
                                        tracksAlbums.put(track.getInt("id"), bitmap);
                                    } catch (IOException | JSONException ignored) {
                                        Log.e("Album loading error", track.toString());
                                    }
                                }
                                if(bitmap != null)
                                    trackImage.setImageBitmap(bitmap);

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
                                tracksList.add(trackLayout);
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
        protected void onPostExecute(Map<LinearLayout, JSONObject> jsonObjectMap) {
            super.onPostExecute(tracksLayouts);
            MainActivity.tracksLayouts = jsonObjectMap;
            Log.e("tracksList", tracksList.toString());
            Log.e("tracksLayouts", tracksLayouts.toString());
            tracksProgressBar.setVisibility(View.GONE);
            if(tracksLayouts != null) {
                synchronized (tracksLayout) {
                    tracksLayout.removeAllViews();
                }
                for (int i = 0; i < tracksList.size(); i++) {
                    LinearLayout trackLayout = tracksList.get(i);
                    //JSONObject trackData = tracksLayouts.get(trackLayout);
                    trackLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                setTrack(tracksList.indexOf(trackLayout));
                            } catch (Exception e) {
                                Log.e("track onClick error", e.toString());
                            }
                        }
                    });
                    if(trackLayout.getParent() != null)
                        ((ViewGroup) trackLayout.getParent()).removeView(trackLayout);
                    tracksLayout.addView(trackLayout);
                }
                new TrackAlbumLoading().execute();
            }else{
                Snackbar.make(tracksLayout, R.string.failed_audio_loading, Snackbar.LENGTH_LONG).setAction(R.string.try_again, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new LoadingAudio().execute();
                    }
                }).show();
            }
        }
    }

    class CreateMediaPlayerFromURI extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isAudioHandlerTaskWork = false;
            if(mediaPlayer != null) {
                Log.i("CreateMediaPlayer", "STOP AND RELease");
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            Log.i("CreateMediaPlayer", "Created new");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                    Log.i(getClass().getSimpleName(), "i: " + currentTrackSeekBar.getMax() * i / 100);
                    currentTrackSeekBar.setSecondaryProgress(currentTrackSeekBar.getMax() * i / 100);
                }
            });
        }

        @Override
        protected Void doInBackground(String... urls) {
            try {
                synchronized (mediaPlayer) {
                    try {
                        mediaPlayer.setDataSource(urls[0]);

                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                //mediaPlayer.stop();
                                Log.i("CreateMediaPlayer", "CMP onCompletion");
                                //mediaPlayer.reset();
                            }
                        });

                        mediaPlayer.prepare();
                    } catch (IllegalStateException e) {
                        Log.e("CreateMediaPlayer", "Error while setDataSource or prepare audio: " + e.toString());
                        Log.i("CreateMediaPlayer", "currentTrack: " + currentTrack);
                    }
                }
            }catch (IOException e){
                Log.e("CreateMediaPlayer", e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            mediaPlayer.start();

            isAudioHandlerTaskWork = true;
            new AudioHandlerTask().execute();

            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                currentTrackSeekBar.setProgress(0);
                Log.e("tracksList", tracksList.toString() + currentTrackNumber);
                Log.e("tracksLayouts", tracksLayouts.toString());
                Log.e("SETTRACK", currentTrackNumber + "");
                setTrack(++currentTrackNumber);
            });
        }
    }

    class CreateMediaPlayerFromFile extends CreateMediaPlayerFromURI {
        @Override
        protected Void doInBackground(String... strings) {
            synchronized (mediaPlayer) {
                try {
                    mediaPlayer.setDataSource(strings[0]);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                }
            }
            return null;
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
                currentTrackImage.setImageBitmap(bitmap);
            }
        }
    }

    class AudioHandlerTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            while(isAudioHandlerTaskWork){
                if(mediaPlayer != null
                        && mediaPlayer.isPlaying()
                        && isCurrentTrackLayoutShow){
                    currentTrackCurrentTime.setText(AppAPI.beautifySeconds(mediaPlayer.getCurrentPosition() / 1000));
                    currentTrackSeekBar.setProgress(mediaPlayer.getCurrentPosition() / 1000);
                }
            }
            return null;
        }
    }

    class TrackAlbumLoading extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            for(JSONObject track : tracksLayouts.values()){
                try {
                    int trackID = track.getInt("id");
                    if(!tracksAlbums.containsKey(trackID)) {
                        URL urlConnection = new URL(track.getJSONObject("album").getJSONObject("thumb").getString(TRACK_ALBUM_PHOTO_QUALITY));
                        HttpURLConnection connection = (HttpURLConnection) urlConnection
                                .openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap b = BitmapFactory.decodeStream(input);
                        tracksAlbums.put(trackID, b);
                        Log.i("TrackAlbumLoader", "Loaded track album with ID " + trackID);
                    }
                }catch (IOException | JSONException ignored){}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            Log.i("TrackAlbumLoader", "All albums have been loaded.");
        }
    }

    class TrackCachingMachine extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            Log.i("TrackCachingMachine", "Start track saving");
            try {
                URL website = new URL(strings[1]);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(new File(getCacheDir(), strings[0] + ".music"));
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                Log.i("TrackCachingMachine", e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            Log.i("TrackCachingMachine", "End track saving");
            if(!b){
                Toast.makeText(getApplicationContext(), R.string.error_download_track, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), R.string.track_saved, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
