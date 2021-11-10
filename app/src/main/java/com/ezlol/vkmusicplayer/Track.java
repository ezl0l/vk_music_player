/* Класс трека
 */

package com.ezlol.vkmusicplayer;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class Track extends MusicObject {
    private LinearLayout layout;
    private JSONObject data;
    private ImageView image, cachedTrackImage;
    private LinearLayout trackNamesLayout, trackDurationLayout;
    private RelativeLayout trackDurationRelativeLayout;
    private TextView trackName;
    private TextView trackAuthor;
    private TextView trackDuration;

    public Track(JSONObject data){
        this.data = data;
    }

    public LinearLayout getLayout() {
        return layout;
    }

    public ImageView getImage() {
        return image;
    }

    public LinearLayout getTrackNamesLayout() {
        return trackNamesLayout;
    }

    public TextView getTrackName() {
        return trackName;
    }

    public TextView getTrackAuthor() {
        return trackAuthor;
    }

    public TextView getTrackDuration() {
        return trackDuration;
    }

    public JSONObject getData() {
        return data;
    }

    public ImageView getCachedTrackImage() {
        return cachedTrackImage;
    }

    public LinearLayout getTrackDurationLayout() {
        return trackDurationLayout;
    }

    public RelativeLayout getTrackDurationRelativeLayout() {
        return trackDurationRelativeLayout;
    }

    @Override
    public void createView(Context c) {
        try {
            layout = new LinearLayout(c);
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding((int) c.getResources().getDimension(R.dimen.trackLayoutLeftPadding),
                    (int) c.getResources().getDimension(R.dimen.trackLayoutTopPadding),
                    (int) c.getResources().getDimension(R.dimen.trackLayoutRightPadding),
                    (int) c.getResources().getDimension(R.dimen.trackLayoutBottomPadding));

            image = new ImageView(c);
            image.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            image.getLayoutParams().height = (int) c.getResources().getDimension(R.dimen.trackImageHeight);
            image.getLayoutParams().width = (int) c.getResources().getDimension(R.dimen.trackImageWidth);

            trackNamesLayout = new LinearLayout(c);
            trackNamesLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            trackNamesLayout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams trackNameAndAuthorParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            trackNameAndAuthorParams.setMargins((int) c.getResources().getDimension(R.dimen.trackNameAndAuthorLeftMargin),
                    (int) c.getResources().getDimension(R.dimen.trackNameAndAuthorTopMargin),
                    (int) c.getResources().getDimension(R.dimen.trackNameAndAuthorRightMargin),
                    (int) c.getResources().getDimension(R.dimen.trackNameAndAuthorBottomMargin));

            trackName = new TextView(c);
            trackName.setLayoutParams(trackNameAndAuthorParams);
            trackName.setTypeface(Typeface.DEFAULT_BOLD);
            trackName.setText(data.getString("title"));

            trackAuthor = new TextView(c);
            trackAuthor.setLayoutParams(trackNameAndAuthorParams);
            trackAuthor.setText(data.getString("artist"));

            trackNamesLayout.addView(trackName);
            trackNamesLayout.addView(trackAuthor);

            trackDurationLayout = new LinearLayout(c);
            trackDurationLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            trackDurationLayout.setOrientation(LinearLayout.VERTICAL);

            trackDurationRelativeLayout = new RelativeLayout(c);
            trackDurationRelativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));

            trackDuration = new TextView(c);
            LinearLayout.LayoutParams trackDurationLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            trackDurationLayoutParams.setMargins(0, 0, (int) c.getResources().getDimension(R.dimen.trackDurationRightMargin), 0);
            trackDuration.setLayoutParams(trackDurationLayoutParams);
            trackDuration.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            trackDuration.setText(AppAPI.beautifySeconds(data.getInt("duration")));

            cachedTrackImage = new ImageView(c);
            RelativeLayout.LayoutParams cachedTrackImageLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            cachedTrackImageLayoutParams.width = (int) c.getResources().getDimension(R.dimen.cachedTrackImageWidth);
            cachedTrackImageLayoutParams.height = (int) c.getResources().getDimension(R.dimen.cachedTrackImageHeight);
            cachedTrackImageLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            cachedTrackImage.setLayoutParams(cachedTrackImageLayoutParams);
            //cachedTrackImage.setImageResource(R.drawable.ic_save_icon);

            trackDurationRelativeLayout.addView(trackDuration);
            trackDurationRelativeLayout.addView(cachedTrackImage);

            trackDurationLayout.addView(trackDurationRelativeLayout);

            layout.addView(image);
            layout.addView(trackNamesLayout);
            layout.addView(trackDurationLayout);
        }catch (JSONException e){
            Log.e("Track.createView", e.toString());
        }
    }
}