package com.ezlol.vkmusicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playlist {
    private final List<Track> tracks;
    private final boolean canAdd;

    public Playlist(List<Track> tracks, boolean canAdd){
        this.tracks = tracks;
        this.canAdd = canAdd;
    }

    public Playlist(List<Track> tracks) {
        this(tracks, true);
    }

    public Playlist(){
        this(new ArrayList<>());
    }

    public boolean addTrack(Track track){
        if(!this.canAdd) return false;
        return this.tracks.add(track);
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Track> getShuffled() {
        List<Track> t = this.tracks;
        Collections.shuffle(t);
        return t;
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
}
