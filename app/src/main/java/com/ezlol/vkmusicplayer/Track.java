package com.ezlol.vkmusicplayer;

import android.media.MediaPlayer;
import android.widget.LinearLayout;

import org.json.JSONObject;

public class Track {
    private final LinearLayout layout;
    private final JSONObject data;
    private MediaPlayer mediaPlayer;

    public Track(LinearLayout layout, JSONObject data) {
        this.layout = layout;
        this.data = data;
    }

    public MediaPlayer createMediaPlayer(){
        return mediaPlayer = new MediaPlayer();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public JSONObject getData() {
        return data;
    }

    public LinearLayout getLayout() {
        return layout;
    }
}
