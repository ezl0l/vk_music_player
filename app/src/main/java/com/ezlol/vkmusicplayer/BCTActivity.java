package com.ezlol.vkmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class BCTActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView currentTrackImage, currentTrackBackBtn, currentTrackPauseBtn, currentTrackPlayBtn, currentTrackNextBtn;
    TextView currentTrackCurrentTime, currentTrackDuration, currentTrackName, currentTrackAuthor;
    SeekBar currentTrackSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bctactivity);

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

        currentTrackBackBtn.setOnClickListener(new MainActivity());
        currentTrackPauseBtn.setOnClickListener(new MainActivity());
        currentTrackPlayBtn.setOnClickListener(new MainActivity());
        currentTrackNextBtn.setOnClickListener(new MainActivity());
        currentTrackBackBtn.setOnClickListener(this);
        currentTrackPauseBtn.setOnClickListener(this);
        currentTrackPlayBtn.setOnClickListener(this);
        currentTrackNextBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.currentTrackPlayBtn:{
                currentTrackPlayBtn.setVisibility(View.GONE);
                currentTrackPauseBtn.setVisibility(View.VISIBLE);
                break;
            }

            case R.id.currentTrackPauseBtn:{
                currentTrackPauseBtn.setVisibility(View.GONE);
                currentTrackPlayBtn.setVisibility(View.VISIBLE);
                break;
            }
        }
    }
}