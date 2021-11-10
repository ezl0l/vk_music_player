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
import android.util.Pair;
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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,
        NavigationView.OnNavigationItemSelectedListener {
    private static final String TRACK_ALBUM_PHOTO_QUALITY = "photo_135";
    private static final String PLAYLIST_ALBUM_PHOTO_QUALITY = "photo_300";

    AppAPI.Auth authLink;

    ScrollView scrollView;
    Button exitBtn;
    LinearLayout tracksLayout, bottomCurrentTrackLayout, currentTrackLayout, playlistsLayout;
    ImageView BCTImage, BCTPlayBtn, BCTPauseBtn, BCTNextBtn, playlistAllTracks, playlistCachedTracks;
    TextView BCTName, BCTAuthor;
    ProgressBar tracksProgressBar;

    ImageView currentTrackImage, currentTrackBackBtn, currentTrackPauseBtn, currentTrackPlayBtn, currentTrackNextBtn;
    TextView currentTrackCurrentTime, currentTrackDuration, currentTrackName, currentTrackAuthor;
    SeekBar currentTrackSeekBar;

    BottomNavigationView navView;

    MediaPlayer mediaPlayer = new MediaPlayer();

    private int currentTrack = 0, currentTrackNumber = 0;
    private final Map<Integer, Bitmap> tracksAlbums = new HashMap<>();

    private final List<Playlist> playlistsList = new ArrayList<>();
    private final Map<Integer, Bitmap> playlistsAlbums = new HashMap<>();
    private final CachePlaylist cachePlaylist = new CachePlaylist();
    private final Playlist mainPlaylist = new Playlist(true);
    private Playlist currentPlaylist = mainPlaylist;

    private boolean isCurrentTrackLayoutShow = false;
    private boolean isAudioHandlerTaskWork = true;
    private boolean isPlaylistTracksLoaderWork = false;

    private final String CHANNEL_ID = "VK Music Player";
    private final int NOTIFICATION_ID = 101;

    private File USER_CACHE_DIR;

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

        playlistsLayout = findViewById(R.id.playlists);
        playlistAllTracks = findViewById(R.id.playlist_all_tracks);
        playlistCachedTracks = findViewById(R.id.playlist_cached_tracks);

        currentTrackBackBtn.setOnClickListener(this);
        currentTrackPauseBtn.setOnClickListener(this);
        currentTrackPlayBtn.setOnClickListener(this);
        currentTrackNextBtn.setOnClickListener(this);

        playlistAllTracks.setOnClickListener(this);
        playlistCachedTracks.setOnClickListener(this);

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
                        .setMessage("You really want delete ALL cache with albums, cached tracks, etc?")
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
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    "VK Music Player",
                    NotificationManager.IMPORTANCE_DEFAULT);

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

        Objects.requireNonNull(getSupportActionBar()).hide();

        navView = findViewById(R.id.nav_view);
        navView.setOnItemSelectedListener(item -> {
            switch(item.getItemId()){
                case R.id.navigation_settings:{
                    getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new SettingsActivity()).commit();
                    break;
                }

                case -1: break; // это чтобы студия не доставала с заменой на иф
            }
            return true;
        });

        //Load albums photos
        new InitializeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // set main playlists names
        cachePlaylist.setName(getString(R.string.cache_playlist_title));
        mainPlaylist.setName(getString(R.string.main_playlist_title));

        cachePlaylist.refreshTracks(this);
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

    private void saveTrack(int trackID){
        new TrackCachingMachine().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, String.valueOf(trackID));
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

            case R.id.bottomCurrentTrack: {
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
            case R.id.currentTrackPlayBtn: {
                resumeTrack();
                break;
            }

            case R.id.BCTPauseBtn:
            case R.id.currentTrackPauseBtn: {
                pauseTrack();
                break;
            }

            case R.id.BCTNextBtn:
            case R.id.currentTrackNextBtn: {
                setTrack(++currentTrackNumber);
                break;
            }

            case R.id.currentTrackBackBtn: {
                setTrack(--currentTrackNumber);
                break;
            }

            case R.id.playlist_all_tracks: {
                setPlaylist(mainPlaylist);
                break;
            }

            case R.id.playlist_cached_tracks: {
                cachePlaylist.refreshTracks(this);
                setPlaylist(cachePlaylist);
                /*
                Map<LinearLayout, JSONObject> cachedTracks = getCachedTracks();
                if(cachedTracks != null) {
                    drawTracks(new ArrayList<>(cachedTracks.keySet()));
                } else {
                    Toast.makeText(MainActivity.this, R.string.not_found_cached_tracks, Toast.LENGTH_SHORT).show();
                    drawTracks(new ArrayList<>());
                }*/
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

    private void unselectAllTracks(){
        List<Track> tracks = currentPlaylist.getTracks();
        for(int i = 0; i < tracks.size(); i++){
            tracks.get(i).getLayout().setBackgroundResource(R.color.track_unselected);
        }
    }

    private void setPlaylist(Playlist playlist){
        currentPlaylist = playlist;
        if(currentPlaylist.getTracks().size() > 0)
            drawAllTracks();
        else if(!isPlaylistTracksLoaderWork) {
            isPlaylistTracksLoaderWork = true;
            new PlaylistTracksLoader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, currentPlaylist);
        }
    }

    private void drawAllTracks(){
        drawTracks(currentPlaylist.getTracks());
    }

    private void drawTracks(List<Track> tracksList){
        if(tracksLayout == null)
            return;
        for(int i = 0; i < tracksLayout.getChildCount(); i++) { // работает - и ладно!
            tracksLayout.removeViewAt(i); // работает - и ладно!
        } // работает - и ладно!
        tracksLayout.removeAllViews(); // работает - и ладно!
        Toast.makeText(this, tracksLayout.getChildCount() + "!", Toast.LENGTH_SHORT).show();
        for (int i = 0; i < tracksList.size(); i++) {
            Track track = tracksList.get(i);
            track.createView(MainActivity.this);
            try {
                File trackFile = new File(getCacheDir(), track.getData().getString("id") + ".music");
                if (trackFile.exists()) {
                    track.getCachedTrackImage().setImageResource(R.drawable.ic_save_icon);
                }
            }catch (JSONException ignored){}
            LinearLayout trackLayout = track.getLayout();
            //JSONObject trackData = tracksLayouts.get(trackLayout);
            trackLayout.setOnClickListener(view -> {
                try {
                    setTrack(tracksList.indexOf(track));
                } catch (Exception e) {
                    Log.e("track onClick error", e.toString());
                }
            });
            if(trackLayout.getParent() != null) // работает - и ладно!
                ((ViewGroup) trackLayout.getParent()).removeView(trackLayout); // работает - и ладно!
            tracksLayout.addView(trackLayout);
        }
        reloadAlbums();
        tracksLayout.requestLayout(); // работает - и ладно!
    }

    private void reloadAlbums(){
        for(Track track : currentPlaylist.getTracks()){
            JSONObject trackData = track.getData();
            try {
                int trackID = trackData.getInt("id");
                if(tracksAlbums.containsKey(trackID)){
                    track.getImage().setImageBitmap(tracksAlbums.get(trackID));
                }
            }catch (JSONException ignored){}
        }
    }

    private void drawPlaylists(List<Playlist> playlists){
        if(playlistsLayout == null)
            return;
        for(int i = 0; i < playlistsLayout.getChildCount(); i++) { // работает - и ладно!
            playlistsLayout.removeViewAt(i); // работает - и ладно!
        } // работает - и ладно!
        playlistsLayout.removeAllViews(); // работает - и ладно!
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            playlist.createView(MainActivity.this);
            LinearLayout playlistLayout = playlist.getLayout();
            //JSONObject trackData = tracksLayouts.get(trackLayout);
            playlistLayout.setOnClickListener(view -> {
                try {
                    setPlaylist(playlist);
                } catch (Exception e) {
                    Log.e("playlist onClick error", e.toString());
                }
            });
            if(playlistLayout.getParent() != null) // работает - и ладно!
                ((ViewGroup) playlistLayout.getParent()).removeView(playlistLayout); // работает - и ладно!
            playlistsLayout.addView(playlistLayout);
        }
        playlistsLayout.requestLayout(); // работает - и ладно!
    }

    private void setTrack(int trackNum){
        //if(trackNum == currentTrack) return;
        try {
            Log.i("setTrackNumber", trackNum + "");
            List<Track> tracksList = currentPlaylist.getTracks();
            Track track = tracksList.get(trackNum);
            LinearLayout trackLayout = track.getLayout();
            JSONObject trackData = track.getData();
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
                } else {
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
            Log.e("setTrack", "any error:" + e.toString());
        }
    }

    class InitializeTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            authLink.refreshUserID();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            File cacheDir = getCacheDir();
            if(cacheDir != null) {
                String userID = authLink.getUserID();
                USER_CACHE_DIR = new File(getCacheDir(), userID);
                if(USER_CACHE_DIR.exists() || USER_CACHE_DIR.mkdir()){
                    for (File file : Objects.requireNonNull(USER_CACHE_DIR.listFiles())) {
                        String fileName = file.getName();
                        Log.i("fileName", fileName);
                        if (fileName.endsWith(".jpg")) {
                            fileName = fileName.replaceFirst("\\.jpg", "");
                            if (fileName.endsWith("_track")) {
                                try {
                                    tracksAlbums.put(
                                            Integer.parseInt(fileName.replaceFirst("_track", "")),
                                            BitmapFactory.decodeFile(file.getAbsolutePath()));
                                } catch (Exception ignored) {
                                }
                            } else if (fileName.endsWith("_playlist")) {
                                try {
                                    playlistsAlbums.put(
                                            Integer.parseInt(fileName.replaceFirst("_playlist", "")),
                                            BitmapFactory.decodeFile(file.getAbsolutePath()));
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
            new LoadingAudio().execute();
            new LoadingPlaylists().execute();
        }
    }

    class LoadingAudio extends AsyncTask<Void, Void, List<Track>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tracksProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Track> doInBackground(Void... voids) {
            JSONObject jsonObject = authLink.getAudios();
            List<Track> trackArrayList = new ArrayList<>();
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
                            } catch (Exception ignored){}
                            JSONArray tracks = response.getJSONObject(0).getJSONArray("items");
                            for (int i = 0; i < tracks.length(); i++) {
                                JSONObject trackData = tracks.getJSONObject(i);
                                savedTracksData.put(trackData.getString("id"), trackData);

                                Track track = new Track(trackData);
                                //track.createView(MainActivity.this);

                                @SuppressLint("DefaultLocale")
                                File trackFile = new File(getCacheDir(), String.format("%d.music", trackData.getInt("id")));
                                Log.i("trackFile", trackFile.getAbsolutePath() + " " + trackFile.exists());

                                //track.getImage().setImageResource(R.drawable.ic_default_track_album);

                                mainPlaylist.addTrack(track);
                                trackArrayList.add(track);
                            }
                            PreferenceManager
                                    .getDefaultSharedPreferences(MainActivity.this)
                                    .edit()
                                    .putString("tracksData", savedTracksData.toString())
                                    .apply();
                            return trackArrayList;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                //loading cache
                //Toast.makeText(MainActivity.this, R.string.loading_cache, Toast.LENGTH_SHORT).show();
                return cachePlaylist.getTracks();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Track> tracks) {
            super.onPostExecute(tracks);
            tracksProgressBar.setVisibility(View.GONE);
            drawTracks(tracks);
            new TrackAlbumLoading().execute();
            // Snackbar.make(tracksLayout, R.string.failed_audio_loading, Snackbar.LENGTH_LONG).setAction(R.string.try_again, view -> new LoadingAudio().execute()).show();
        }
    }

    class LoadingPlaylists extends AsyncTask<Void, Void, List<Playlist>> {
        @Override
        protected List<Playlist> doInBackground(Void... voids) {
            JSONObject d = authLink.getPlaylists();
            List<Playlist> playlistList = new ArrayList<>();
            if(d != null){
                if(d.has("response")) {
                    try {
                        JSONObject response = d.getJSONObject("response");
                        if(response.has("items")){
                            JSONArray playlistsData = response.getJSONArray("items");
                            Log.i("playlistsData", playlistsData.toString());
                            for(int i = 0; i < playlistsData.length(); i++){
                                JSONObject playlistData = playlistsData.getJSONObject(i);
                                Playlist playlist = new Playlist();
                                playlist.setName(playlistData.getString("title"));
                                playlist.setData(playlistData);
                                /*JSONObject playlistTracksData = authLink.getAudios(100, playlistData.getInt("id"));
                                if(playlistTracksData != null) {
                                    if (playlistTracksData.has("response")) {
                                        try {
                                            JSONObject playlistTracksResponse = playlistTracksData.getJSONObject("response");
                                            if (playlistTracksResponse.has("items")) {
                                                JSONArray playlistTracks = playlistTracksResponse.getJSONArray("items");
                                                for(int j = 0; j < playlistTracks.length(); j++) {
                                                    playlist.addTrack(new Track(playlistTracks.getJSONObject(j)));
                                                }
                                            }
                                        } catch (JSONException ignored) {}
                                    }
                                }*/
                                playlistList.add(playlist);
                            }
                        }
                    } catch (JSONException ignored) {}
                    Log.i("p", playlistList.size() + "");
                    return playlistList;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Playlist> playlists) {
            super.onPostExecute(playlists);
            if(playlists == null){
                playlists = new ArrayList<>();
            }

            playlistsList.clear();
            for (int i = 0; i < playlists.size(); i++) {
                Playlist playlist = playlists.get(i);
                Log.i("playlist", "playlist");
                playlist.createView(MainActivity.this);
            }

            new PlaylistAlbumLoading().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            playlists.add(0, cachePlaylist);
            playlists.add(0, mainPlaylist);

            playlistsList.addAll(playlists);

            drawPlaylists(playlists);
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

                        mediaPlayer.setOnCompletionListener(mp -> {
                            //mediaPlayer.stop();
                            Log.i("CreateMediaPlayer", "CMP onCompletion");
                            //mediaPlayer.reset();
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
                URL urlConnection = new URL(trackData.getJSONObject("album").getJSONObject("thumb").getString(TRACK_ALBUM_PHOTO_QUALITY));
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
            for(Track track : currentPlaylist.getTracks()){
                JSONObject trackData = track.getData();
                try {
                    int trackID = trackData.getInt("id");
                    if(!tracksAlbums.containsKey(trackID)) {
                        URL urlConnection = new URL(trackData.getJSONObject("album").getJSONObject("thumb").getString(TRACK_ALBUM_PHOTO_QUALITY));
                        HttpURLConnection connection = (HttpURLConnection) urlConnection
                                .openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap b = BitmapFactory.decodeStream(input);

                        tracksAlbums.put(trackID, b);

                        File file = new File(USER_CACHE_DIR, trackID + "_track.jpg");
                        FileOutputStream fOut = new FileOutputStream(file);

                        b.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        fOut.flush();
                        fOut.close();

                        Log.i("TrackAlbumLoader", "Loaded track album with ID " + trackID);
                    }
                }catch (IOException | JSONException ignored){}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            for(Track track : currentPlaylist.getTracks()) {
                JSONObject trackData = track.getData();
                try {
                    int trackID = trackData.getInt("id");
                    if(tracksAlbums.containsKey(trackID)){
                        track.getImage().setImageBitmap(tracksAlbums.get(trackID));
                    }
                }catch (JSONException ignored){}
            }
            Log.i("TrackAlbumLoader", "All albums have been loaded.");
        }
    }

    class PlaylistAlbumLoading extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            for (Playlist playlist : playlistsList) {
                JSONObject playlistData = playlist.getData();
                if (playlistData == null)
                    continue;
                try {
                    int playlistID = playlistData.getInt("id");
                    if (!playlistsAlbums.containsKey(playlistID)) {
                        URL urlConnection = new URL(playlistData.getJSONObject("photo").getString(PLAYLIST_ALBUM_PHOTO_QUALITY));
                        HttpURLConnection connection = (HttpURLConnection) urlConnection
                                .openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap b = BitmapFactory.decodeStream(input);

                        playlistsAlbums.put(playlistID, b);

                        File file = new File(USER_CACHE_DIR, playlistID + "_playlist.jpg");
                        FileOutputStream fOut = new FileOutputStream(file);

                        b.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        fOut.flush();
                        fOut.close();
                    }
                } catch (IOException | JSONException ignored) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            for(Playlist playlist : playlistsList) {
                JSONObject playlistData = playlist.getData();
                if(playlistData == null)
                    continue;
                try {
                    int playlistID = playlistData.getInt("id");
                    if(playlistsAlbums.containsKey(playlistID)){
                        playlist.getImage().setImageBitmap(playlistsAlbums.get(playlistID));
                    }
                }catch (JSONException ignored){}
            }
        }
    }

    class TrackCachingMachine extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            String trackURL = null;
            try {
                JSONObject audioResponse = authLink.getAudioById(authLink.addOwnerId2Id(strings[0]));
                Log.i("audioResponse", strings[0] + " - " + audioResponse.toString());
                if(audioResponse.has("response")){
                    JSONArray audioData = audioResponse.getJSONArray("response");
                    trackURL = AppAPI.newToMP3(audioData.getJSONObject(0).getString("url"));
                }
            }catch (JSONException e){
                Log.e("JSONException", e.toString());
                return false;
            }
            if(trackURL == null)
                return false;

            Log.i("TrackCachingMachine", "Start track saving");
            try {
                URL website = new URL(trackURL);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(new File(getCacheDir(), strings[0] + ".music")); // USER_CACHE_DIR
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

    class PlaylistTracksLoader extends AsyncTask<Playlist, Void, Playlist> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tracksProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Playlist doInBackground(Playlist... playlists) {
            for(int i = 0; i < playlists.length; i++){
                JSONObject playlistData = playlists[i].getData();
                if(playlistData != null){
                    try {
                        int playlistID = playlistData.getInt("id");
                        JSONObject playlistTracksData = authLink.getAudios(100, playlistID);
                        if(playlistTracksData != null && playlistTracksData.has("response")){
                            JSONObject response = playlistTracksData.getJSONArray("response").getJSONObject(0);
                            if(response.has("items")){
                                JSONArray tracks = response.getJSONArray("items");
                                for(int j = 0; j < tracks.length(); j++) {
                                    playlists[i].addTrack(new Track(tracks.getJSONObject(j)));
                                }
                            }
                        }
                    }catch (JSONException ignored){}
                }
            }
            return currentPlaylist;
        }

        @Override
        protected void onPostExecute(Playlist playlist) {
            super.onPostExecute(playlist);
            tracksProgressBar.setVisibility(View.GONE);
            setPlaylist(playlist);
            new TrackAlbumLoading().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            isPlaylistTracksLoaderWork = false;
        }
    }
}
