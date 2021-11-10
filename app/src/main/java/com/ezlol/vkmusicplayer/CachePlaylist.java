package com.ezlol.vkmusicplayer;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class CachePlaylist extends Playlist {
    public CachePlaylist(List<Track> tracks) {
        super(tracks, false);
    }

    public CachePlaylist(){
        super();
        super.isUntouchable = true;
    }

    public void refreshTracks(Context c){
        File cacheDir = c.getCacheDir();
        if(cacheDir != null) {
            JSONObject tracksData = JSON.decode(
                    PreferenceManager
                            .getDefaultSharedPreferences(c)
                            .getString("tracksData", "")
            ); // типо этого {<stringifyTrackID>:{"url":"https://..."...}...}

            //Map<Track, String> tracksPaths = new HashMap<>();
            super.clear();
            String trackName, trackID;
            for (File trackFile : Objects.requireNonNull(cacheDir.listFiles())) {
                trackName = trackFile.getName();
                if (trackName.endsWith(".music")) {
                    trackID = trackName.replaceFirst("\\.music", "");
                    Log.i("CacheLoader", trackID);

                    JSONObject trackData;
                    try {
                        trackData = tracksData.getJSONObject(trackID);
                    } catch (JSONException e) {
                        trackData = new JSONObject();
                        try {
                            trackData.put("artist", "Unknown artist");
                            trackData.put("title", "Unknown track");
                            trackData.put("duration", 0);
                        } catch (JSONException ignored) {}
                    }
                    super.addTrack(new Track(trackData));
                }
            }
        }
    }
}
