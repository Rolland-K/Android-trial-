package com.music.monir;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;
import com.smp.soundtouchandroid.OnProgressChangedListener;
import com.smp.soundtouchandroid.SoundStreamAudioPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private FloatingActionButton btnPlay;
    private SoundStreamAudioPlayer mPlayer;
    private SoundStreamAudioPlayer mTanpuraPlayer;
    private File mFile;
    private File mTanpuraFile;
    private TextView tvTitle;
    private String mCurrentMusicName = "santoor_Tintaal_Bhupali_240bpm_D#_LR.mp3";
    private RangeSeekBar sbBpm;
    private RangeSeekBar sbPitch;
    private RangeSeekBar sbTanpra;
    private RangeSeekBar sbMasterVolumn;
    private RangeSeekBar sbVolumnLeft;
    private RangeSeekBar sbVolumnRight;
    private float pitch = 1.0f;
    private float bpm = 1.0f;
    private float volumn_left = 1.0f;
    private float volumn_right = 1.0f;
    private float tanpura = 1.0f;
    private boolean isLREnabled = false;
    private boolean isPlaying = false;
    private float currentBpm = 0;
    private float currentPitch = 0;
    private float tanpura_pitch = 0;
    private ImageView mIvHelp, mIvInfo;
    private String mTanpuraFileName;
    private TextView tvLeftVol, tvRightVol, tvMasterVol;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent.hasExtra("filename")) {
            mCurrentMusicName = intent.getStringExtra("filename");
            mTanpuraFileName = intent.getStringExtra("tanpura");
        }

        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setSelected(true);

        tvLeftVol = findViewById(R.id.tvLeftVol);
        tvRightVol = findViewById(R.id.tvRightVol);
        tvMasterVol = findViewById(R.id.tvMasterVol);

        sbBpm = findViewById(R.id.sb_bpm);
        sbPitch = findViewById(R.id.sb_pitch);
        sbTanpra = findViewById(R.id.sb_volumn);
        sbMasterVolumn = findViewById(R.id.sb_master_volumn);
        sbVolumnLeft = findViewById(R.id.sb_volumn_l);
        sbVolumnRight = findViewById(R.id.sb_volumn_r);
        mIvHelp = findViewById(R.id.iv_help);
        mIvHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Intent intent = new Intent (MainActivity.this, HelpActivity.class);
              startActivity(intent);
            }
        });

        mIvInfo = findViewById(R.id.iv_info);
        mIvInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });

        mCurrentMusicName = mCurrentMusicName.substring(0, mCurrentMusicName.length()-4);
       // mCurrentMusicName.split("_");
        String instrument = mCurrentMusicName.split("_")[0];
        if(instrument.substring(0, 5).compareTo("Vocal") == 0)
        {
            sbPitch.setEnabled(false);
            sbBpm.setEnabled(false);
        }

        String strBPM =  mCurrentMusicName.split("_")[3];
        strBPM = strBPM.substring(0, strBPM.length()-3);
        String strPitch = mCurrentMusicName.split("_")[4];
        Log.e("fef", strPitch);
        Log.e("fef", String.valueOf(mCurrentMusicName.split("_").length));
        if(mCurrentMusicName.split("_").length == 6)
            isLREnabled = true;
        sbPitch.setRange(0, 11, 1);
        currentPitch = getPitchValueFromText(strPitch);
        tanpura_pitch = getPitchValueFromText(mTanpuraFileName.split("_")[1]);
        sbPitch.setProgress(currentPitch);
        int bpm = Integer.parseInt(strBPM);
        sbBpm.setRange(bpm/2, bpm*2, 0);
        sbBpm.setIndicatorTextDecimalFormat("0 bpm");
        sbBpm.setProgress(Integer.parseInt(strBPM));
        currentBpm = Integer.parseInt(strBPM);
        sbTanpra.setRange(0, 100, 1);
        sbTanpra.setIndicatorTextDecimalFormat("0");
        sbTanpra.setProgress(0);

        sbVolumnLeft.setRange(0, 100, 1);
        sbVolumnLeft.setIndicatorTextDecimalFormat("0");

        sbVolumnRight.setRange(0, 100, 1);
        sbVolumnRight.setIndicatorTextDecimalFormat("0");

        sbMasterVolumn.setRange(0, 100, 1);
        sbMasterVolumn.setIndicatorTextDecimalFormat("0");

        if(!isLREnabled)
        {
            sbVolumnRight.setEnabled(false);
            sbVolumnRight.setProgressDefaultColor(Color.DKGRAY);
            sbVolumnRight.setProgressColor(Color.DKGRAY);
            sbVolumnLeft.setEnabled(false);
            sbVolumnLeft.setProgressDefaultColor(Color.DKGRAY);
            sbVolumnLeft.setProgressColor(Color.DKGRAY);
            tvLeftVol.setTextColor(Color.DKGRAY);
            tvRightVol.setTextColor(Color.DKGRAY);


        }
        else {

            sbMasterVolumn.setEnabled(false);
            sbMasterVolumn.setProgressDefaultColor(Color.DKGRAY);
            sbMasterVolumn.setProgressColor(Color.DKGRAY);
            tvMasterVol.setTextColor(Color.DKGRAY);
        }
        sbBpm.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {

                float tempo = mPlayer.getTempo();
                tempo = leftValue/currentBpm;
                //rate = 2.0f * leftValue/500.0f + 0.1f ;
                mPlayer.setTempo(tempo);

            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });
        sbTanpra.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                mTanpuraPlayer.setVolume(leftValue/100.0f, leftValue/100.0f);
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });

        sbPitch.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                Log.e("pitch", String.valueOf(mPlayer.getPitchSemi()));

                mPlayer.setPitchSemi(leftValue - currentPitch);
                mTanpuraPlayer.setPitchSemi(leftValue - tanpura_pitch);
                pitch = leftValue;
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });

        sbMasterVolumn.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                volumn_left = leftValue/100.0f;
                volumn_right = leftValue/100.0f;
                Log.e("master", String.valueOf(volumn_left));
                mPlayer.setVolume(volumn_left, volumn_right);
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });

        sbVolumnLeft.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
              volumn_left = leftValue/100.0f;
              Log.e("left", String.valueOf(volumn_left));
              mPlayer.setVolume(volumn_left, volumn_right);
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });

        sbVolumnRight.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                volumn_right = leftValue/100.0f;
                Log.e("master", String.valueOf(volumn_right));
                mPlayer.setVolume(volumn_left, volumn_right);
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });

      //  copyAllFilesToStorage();
        btnPlay = findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(this);
        initPlayer(getPitchValueFromText(strPitch));
        initTanpuraPlayer(currentPitch - tanpura_pitch);
        sbMasterVolumn.setProgress(50);
        sbVolumnLeft.setProgress(50);
        sbVolumnRight.setProgress(50);
        mPlayer.setVolume(volumn_left, volumn_right);
        mTanpuraPlayer.setVolume(0, 0);
        String[] res = mCurrentMusicName.split("_");
        if(res.length > 3) {
            mCurrentMusicName =  res[2].equals("")? (res[0] + "_" + res[1]): (res[0] + "_" + res[1] + "_" + res[2]);

        }
        tvTitle.setText(mCurrentMusicName);

    }

    private int getPitchValueFromText(String text){
        switch (text){
            case "BSharp":
                return 3;

            case "C":
                return 3;

            case "CSharp":
                return 4;

            case "D":
                return 5;

            case "DSharp":
                return 6;

            case "E":
                return 7;

            case "ESharp":
                return 8;

            case "F":
                return 8;

            case "FSharp":
                return 9;

            case "G":
                return 10;

            case "GSharp":
                return 11;

            case "A":
                return 0;

            case "ASharp":
                return 1;

            case "B":
                return 2;
            default: return 0;

        }

    }


    private void initPlayer(float pitch) {
        Log.e("fef", String.valueOf(pitch));
        mFile = new File(getExternalFilesDir(null), "temp.mp3");

        try {
            mPlayer = new SoundStreamAudioPlayer(0, mFile.getPath(), 1.0f, 0);
           // mPlayer.setLoopStart(0);
           // mPlayer.setLoopEnd(mPlayer.getDuration());
            mPlayer.setOnProgressChangedListener(new OnProgressChangedListener() {
                @Override
                public void onProgressChanged(int track, double currentPercentage, long position) {
                 //   status_seek.setProgress((int)(currentPercentage*100));
                }

                @Override
                public void onTrackEnd(int track) {

                    //mPlayer.stop();
                    mPlayer.seekTo(0);
                    mPlayer.start();

                }

                @Override
                public void onExceptionThrown(String string) {

                }
            });
        } catch (IOException e) {
        }
    }

    private void initTanpuraPlayer(float pitch) {
        Log.e("fef", String.valueOf(pitch));
        mTanpuraFile = new File(getExternalFilesDir(null), "tanpura.mp3");

        try {
            mTanpuraPlayer = new SoundStreamAudioPlayer(1, mTanpuraFile.getPath(), 1.0f, pitch);
            // mPlayer.setLoopStart(0);
            // mPlayer.setLoopEnd(mPlayer.getDuration());
            mTanpuraPlayer.setOnProgressChangedListener(new OnProgressChangedListener() {
                @Override
                public void onProgressChanged(int track, double currentPercentage, long position) {
                    //   status_seek.setProgress((int)(currentPercentage*100));
                }

                @Override
                public void onTrackEnd(int track) {

                    //mPlayer.stop();
                    mTanpuraPlayer.seekTo(0);
                    mTanpuraPlayer.start();

                }

                @Override
                public void onExceptionThrown(String string) {

                }
            });
        } catch (IOException e) {
        }
    }



    private void copyAllFilesToStorage(){
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);

                File outFile = new File(getExternalFilesDir(null), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    @Override
    public void onPause() {

        super.onPause();
//        if(isPlaying) {
//            mPlayer.pause();
//            mTanpuraPlayer.pause();
//            btnPlay.setImageResource(R.drawable.btn_play);
//        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        if(isPlaying) {
            mPlayer.stop();
            mTanpuraPlayer.stop();

        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_play){
            if(!isPlaying)
            {
                new Thread(mPlayer).start();
                mPlayer.start();
                new Thread(mTanpuraPlayer).start();
                mTanpuraPlayer.start();
                btnPlay.setImageResource(R.drawable.btn_pause);
                isPlaying = true;
            }
            else {
                if(mPlayer.isPaused()){
                    btnPlay.setImageResource(R.drawable.btn_pause);
                    mPlayer.start();
                    mTanpuraPlayer.start();
                }
                else {
                    mPlayer.pause();
                    mTanpuraPlayer.pause();
                    btnPlay.setImageResource(R.drawable.btn_play);
                }
            }
        }
    }
}
