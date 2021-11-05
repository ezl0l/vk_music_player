/* Потом, когда-ни
 */

package com.ezlol.vkmusicplayer;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class Track {
    private LinearLayout layout;
    private JSONObject data;
    private ImageView image;
    private LinearLayout trackNamesLayout;
    private TextView trackName;
    private TextView trackAuthor;
    private TextView trackDuration;

    public Track(Context c, JSONObject data){
        try {
            this.data = data;

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

            trackDuration = new TextView(c);
            trackDuration.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            trackDuration.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            trackDuration.setText(AppAPI.beautifySeconds(data.getInt("duration")));

            layout.addView(image);
            layout.addView(trackNamesLayout);
            layout.addView(trackDuration);
        }catch (JSONException ignored){}
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
}
