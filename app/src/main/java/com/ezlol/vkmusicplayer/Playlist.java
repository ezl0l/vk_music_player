package com.ezlol.vkmusicplayer;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playlist extends MusicObject {
    private final List<Track> tracks;
    private final boolean canAdd;
    protected boolean isUntouchable = false;

    private LinearLayout layout;
    private ImageView image;
    private TextView textView;
    private String name;

    private JSONObject data;

    public Playlist(List<Track> tracks, boolean canAdd, boolean isUntouchable){
        this(tracks, canAdd);
        this.isUntouchable = isUntouchable;
    }

    public Playlist(List<Track> tracks, boolean canAdd){
        this.tracks = tracks;
        this.canAdd = canAdd;
    }

    public Playlist(List<Track> tracks) {
        this(tracks, true);
    }

    public Playlist(boolean isUntouchable){
        this();
        this.isUntouchable = isUntouchable;
    }

    public Playlist(){
        this(new ArrayList<>());
    }

    public boolean addTrack(Track track){
        if(!this.canAdd) return false;
        return this.tracks.add(track);
    }

    public boolean addTracks(List<Track> tracks){
        if(!this.canAdd) return false;
        return this.tracks.addAll(tracks);
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Track> getShuffled() {
        List<Track> t = this.tracks;
        Collections.shuffle(t);
        return t;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public JSONObject getData() {
        return data;
    }

    @Override
    public void createView(Context c){
        if(layout == null){
            layout = new LinearLayout(c);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins((int) c.getResources().getDimension(R.dimen.playlistLayoutLeftMargin), 0, 0, 0);

            layout.setLayoutParams(layoutParams);
            layout.setOrientation(LinearLayout.VERTICAL);

            image = new ImageView(c);
            image.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            image.getLayoutParams().height = (int) c.getResources().getDimension(R.dimen.playlistImageHeight);
            image.getLayoutParams().width = (int) c.getResources().getDimension(R.dimen.playlistImageWidth);
            image.setImageResource(R.drawable.ic_default_track_album);

            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLayoutParams.setMargins(0, (int) c.getResources().getDimension(R.dimen.playlistTextTopMargin), 0, 0);

            textView = new TextView(c);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(textLayoutParams);
            textView.setMaxLines(1);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setText(name);
            Log.i("Playlist title", name + "");

            layout.addView(image);
            layout.addView(textView);
        }
    }

    public LinearLayout getLayout() {
        return layout;
    }

    public ImageView getImage() {
        return image;
    }

    public TextView getTextView() {
        return textView;
    }

    public Track getTrack(int i){
        return this.tracks.get(i);
    }

    protected void clear(){
        this.tracks.clear();
    }

    public boolean isCanAdd() {
        return canAdd;
    }

    public boolean isUntouchable() {
        return isUntouchable;
    }
}
