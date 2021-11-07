package com.ezlol.vkmusicplayer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
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
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        MediaPlayer.OnCompletionListener,
        NavigationView.OnNavigationItemSelectedListener {
    private static final String TRACK_ALBUM_PHOTO_QUALITY = "photo_135";

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

    BottomNavigationView navView;

    MediaPlayer mediaPlayer = new MediaPlayer();

    private int currentTrack = 0, currentTrackNumber = 0;
    static List<LinearLayout> tracksList = new ArrayList<>();
    static Map<LinearLayout, JSONObject> tracksLayouts = new HashMap<>();
    static Map<Integer, Bitmap> tracksAlbums = new HashMap<>();

    private boolean isCurrentTrackLayoutShow = false;
    private boolean isAudioHandlerTaskWork = true;

    private final String CHANNEL_ID = "VK Music Player";
    private final int NOTIFICATION_ID = 101;

    @SuppressLint("NonConstantResourceId")
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

        findViewById(R.id.deleteCache).setOnClickListener(new View.OnClickListener() {
            private boolean deleteDir(File dir) {
                if (dir != null && dir.isDirectory()) {
                    String[] children = dir.list();
                    for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                        boolean success = deleteDir(new File(dir, children[i]));
                        if (!success) {
                            return false;
                        }
                    }
                    return dir.delete();
                } else if(dir!= null && dir.isFile()) {
                    return dir.delete();
                } else {
                    return false;
                }
            }

            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Are u sure?")
                        .setMessage("You really want delete ALL cache?")
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                            File cacheDir = getCacheDir();
                            if(cacheDir != null) {
                                if(deleteDir(cacheDir))
                                    Toast.makeText(MainActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(MainActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(MainActivity.this, "Cache dir == null!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

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

        Objects.requireNonNull(getSupportActionBar()).hide();

        navView = findViewById(R.id.nav_view);
        navView.setOnItemSelectedListener(item -> {
            switch(item.getItemId()){
                case R.id.navigation_settings:{
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    break;
                }

                case -1: break; // это чтобы студия не доставала с заменой на иф
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        navView.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_save:{
                saveTrack(currentTrack);
                break;
            }

            case R.id.action_delete:{
                if(deleteTrack(currentTrack))
                    Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTrack(int trackID, String trackURL){
        Log.i("saveTrack", "trackID: " + trackID + " trackURL: " + trackURL);
        String trackRealURL = AppAPI.newToMP3(trackURL);
        if(trackRealURL.equals(trackURL)){
            Toast.makeText(this, R.string.error_download_track, Toast.LENGTH_SHORT).show();
            return;
        }
        new TrackCachingMachine().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(trackID), trackRealURL);
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

    private boolean deleteTrack(int trackID){
        @SuppressLint("DefaultLocale")
        File trackFile = new File(getCacheDir(), String.format("%d.music", trackID));
        if(trackFile.exists())
            return trackFile.delete();
        return false;
    }

    @SuppressLint("NonConstantResourceId")
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
                scrollView.setVisibility(View.GONE);
                bottomCurrentTrackLayout.setVisibility(View.GONE);
                exitBtn.setVisibility(View.GONE);
                currentTrackLayout.setVisibility(View.VISIBLE);
                navView.setVisibility(View.GONE);
                Objects.requireNonNull(getSupportActionBar()).show();
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
            Objects.requireNonNull(getSupportActionBar()).hide();
            navView.setVisibility(View.VISIBLE);
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
    protected void onStop() {
        super.onStop();
        isCurrentTrackLayoutShow = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isCurrentTrackLayoutShow = true;
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
        //if(trackNum == currentTrack) return;
        try {
            Log.i("setTrackNumber", trackNum + "");
            LinearLayout trackLayout = tracksList.get(trackNum);
            JSONObject trackData = tracksLayouts.get(trackLayout);
            if(trackData == null) return;

            //Change notification
            NotificationCompat.Builder n = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_default_track_album)
                    .setContentTitle(trackData.getString("title"))
                    .setContentText(trackData.getString("artist"))
                    .setSubText(CHANNEL_ID)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setSound(null)
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(NOTIFICATION_ID, n.build());
            currentTrack = trackData.getInt("id");
            unselectAllTracks();

            currentTrackNumber = trackNum;
            BCTImage.setImageResource(R.drawable.ic_default_track_album);
            synchronized (this) {
                if(tracksAlbums.containsKey(trackData.getInt("id"))){
                    BCTImage.setImageBitmap(tracksAlbums.get(trackData.getInt("id")));
                }

                @SuppressLint("DefaultLocale")
                File trackFile = new File(getCacheDir(), String.format("%d.music", trackData.getInt("id")));
                if(trackFile.exists()){
                    Log.i("setTrack", "Take a track from cache.");
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
            if(tracksLayouts != null)
                tracksLayouts.clear();
            else
                tracksLayouts = new HashMap<>();
            if(tracksList != null)
                tracksList.clear();
            else
                tracksList = new ArrayList<>();
        }

        @Override
        protected Map<LinearLayout, JSONObject> doInBackground(Void... voids) {
            JSONObject jsonObject = authLink.getAudios();
            if(jsonObject != null){
                if(jsonObject.has("response")) {
                    try {
                        JSONArray response = jsonObject.getJSONArray("response");
                        if (response.length() > 0) {
                            JSONObject savedTracksData = new JSONObject();
                            try {
                                savedTracksData = JSON.decode(
                                        PreferenceManager
                                                .getDefaultSharedPreferences(MainActivity.this)
                                                .getString("tracksData", ""));
                            }catch (Exception ignored){}
                            JSONArray tracks = response.getJSONObject(0).getJSONArray("items");
                            for (int i = 0; i < tracks.length(); i++) {
                                JSONObject trackData = tracks.getJSONObject(i);
                                savedTracksData.put(trackData.getString("id"), trackData);

                                Track track = new Track(MainActivity.this, trackData);

                                track.getImage().setImageResource(R.drawable.ic_default_track_album);

                                Bitmap bitmap = null;
                                if(tracksAlbums.containsKey(trackData.getInt("id"))){
                                    bitmap = tracksAlbums.get(trackData.getInt("id"));
                                }else {
                                    try {
                                        URL urlConnection = new URL(trackData
                                                .getJSONObject("album")
                                                .getJSONObject("thumb")
                                                .getString(TRACK_ALBUM_PHOTO_QUALITY));
                                        HttpURLConnection connection = (HttpURLConnection) urlConnection
                                                .openConnection();
                                        connection.setDoInput(true);
                                        connection.connect();
                                        InputStream input = connection.getInputStream();
                                        bitmap = BitmapFactory.decodeStream(input);
                                        tracksAlbums.put(trackData.getInt("id"), bitmap);
                                    } catch (IOException | JSONException ignored) {
                                        Log.e("Album loading error", track.toString());
                                    }
                                }
                                if(bitmap != null)
                                    track.getImage().setImageBitmap(bitmap);

                                tracksLayouts.put(track.getLayout(), trackData);
                                tracksList.add(track.getLayout());
                            }
                            PreferenceManager
                                    .getDefaultSharedPreferences(MainActivity.this)
                                    .edit()
                                    .putString("tracksData", savedTracksData.toString())
                                    .apply();
                            return tracksLayouts;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                //loading cache
                //Toast.makeText(MainActivity.this, R.string.loading_cache, Toast.LENGTH_SHORT).show();
                File cacheDir = getCacheDir();
                if(cacheDir != null) {
                    JSONObject tracksData = JSON.decode(
                            PreferenceManager
                                    .getDefaultSharedPreferences(MainActivity.this)
                                    .getString("tracksData", "")
                    ); // типо этого {<stringifyTrackID>:{"url":"https://..."...}...}

                    //Map<Track, String> tracksPaths = new HashMap<>();
                    String trackName, trackID;
                    for (File trackFile : Objects.requireNonNull(cacheDir.listFiles())) {
                        trackName = trackFile.getName();
                        if (trackName.endsWith(".music")){
                            trackID = trackName.replaceFirst("\\.music", "");
                            Log.i("CacheLoader", trackID);

                            JSONObject trackData;
                            Track track;
                            try {
                                trackData = tracksData.getJSONObject(trackID);
                            }catch (JSONException e) {
                                trackData = new JSONObject();
                                try {
                                    trackData.put("artist", "Unknown artist");
                                    trackData.put("title", "Unknown track");
                                    trackData.put("duration", 0);
                                } catch (JSONException ignored) {}
                            }

                            track = new Track(MainActivity.this, trackData);
                            tracksLayouts.put(track.getLayout(), trackData);
                            tracksList.add(track.getLayout());
                            //tracksPaths.put(track, trackFile.getAbsolutePath());
                        }
                    }
                    return tracksLayouts;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Map<LinearLayout, JSONObject> jsonObjectMap) {
            super.onPostExecute(tracksLayouts);
            MainActivity.tracksLayouts = jsonObjectMap;
            tracksProgressBar.setVisibility(View.GONE);
            if(tracksLayouts != null) {
                if(tracksLayout != null) {
                    tracksLayout.removeAllViews();
                }
                for (int i = 0; i < tracksList.size(); i++) {
                    LinearLayout trackLayout = tracksList.get(i);
                    //JSONObject trackData = tracksLayouts.get(trackLayout);
                    trackLayout.setOnClickListener(view -> {
                        try {
                            setTrack(tracksList.indexOf(trackLayout));
                        } catch (Exception e) {
                            Log.e("track onClick error", e.toString());
                        }
                    });
                    if(trackLayout.getParent() != null)
                        ((ViewGroup) trackLayout.getParent()).removeView(trackLayout);
                    tracksLayout.addView(trackLayout);
                }
                new TrackAlbumLoading().execute();
            }else{
                Snackbar.make(tracksLayout, R.string.failed_audio_loading, Snackbar.LENGTH_LONG).setAction(R.string.try_again, view -> new LoadingAudio().execute()).show();
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
        protected void onPreExecute() {
            super.onPreExecute();
            mediaPlayer.setOnBufferingUpdateListener(null);
        }

        @Override
        protected Void doInBackground(String... strings) {
            synchronized (mediaPlayer) {
                try {
                    mediaPlayer.setDataSource(strings[0]);
                    mediaPlayer.prepare();
                } catch ( IOException | RuntimeException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            currentTrackSeekBar.setSecondaryProgress(currentTrackSeekBar.getMax());
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
