package com.zmb.filebrowser.mediaplayer;

import android.animation.Animator;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zmb.filebrowser.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnVideoSizeChangedListener, SurfaceHolder.Callback {
    private static final String VIDEO_PATH = "/sdcard/TimeScapes.mp4";
    private int player_state = 0;
    private static final int ERROR = -1;
    private static final int INIT = 0;
    private static final int PREPARE = 1;
    private static final int START = 1 << 1;
    private static final int PAUSE = 1 << 2;
    private static final int STOP = 1 << 3;
    private static final int COMPLETE = 1 << 4;
    private static final int END = 1 << 5;
    private static final int MAX_SHOW_TIME = 5000;

    private boolean needHideStatusBar = true;
    private static boolean LOOP = false;
    private int videolen = 0;
    private int screenHeight = 0;
    private int screenWidth = 0;
    private int playerHeight = 0;
    private int playerWidth = 0;

    private static final String TAG = "PlayerActivity";
    private MediaPlayer player = null;
    private SurfaceView surfaceView = null;
    private SurfaceHolder surfaceHolder = null;
    private TextView title = null;
    private Button start, pause, stop, loop;
    private ImageButton statusBar,playerbot;
    private SeekBar seekBar  = null;
    private AssetFileDescriptor descriptor = null;
    private AssetManager assetManager = null;
    private Timer timer = null;
    private FrameLayout playerlayout = null;
    private RelativeLayout statusbarbotlayout = null;
    private int videowidth = 0;
    private int videoheight = 0;
    private static boolean STATUS_STOP = false;
    private boolean isTouch = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_player);
        Log.i(TAG, "onCreate: ");
        init();
//        playVedio();

    }

    private void init() {
        Log.i(TAG, "init: ");
        player = new MediaPlayer();
        player.setLooping(LOOP);
        surfaceView = (SurfaceView) findViewById(R.id.videoview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        title = (TextView) findViewById(R.id.title);
        start = (Button) findViewById(R.id.play);
        pause = (Button) findViewById(R.id.pause);
        stop = (Button) findViewById(R.id.stop);
        loop = (Button) findViewById(R.id.loop);
        statusBar = (ImageButton) findViewById(R.id.statusbar);
        playerbot = (ImageButton) findViewById(R.id.playerbot);
        seekBar = (SeekBar) findViewById(R.id.seek);
        playerlayout = (FrameLayout) findViewById(R.id.playerlayout);
        getScreenSize();
        playerlayout.setLayoutParams(new LinearLayout.LayoutParams(screenWidth,getPlayerHeight()));
        playerlayout.setMinimumHeight(getPlayerHeight());
        statusbarbotlayout = (RelativeLayout) findViewById(R.id.statusbarbot);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i(TAG, "onProgressChanged: "+progress);
                if(isTouch) {
                    float progr = ((float) progress) / 100;
                    int pos = (int) (videolen * progr);
                    player.seekTo(pos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTouch = true;
                timer.cancel();
                timer = null;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTouch = false;
                timer = new Timer();
                timer.schedule(creatTimerTask(),0,100);
            }
        });
        start.setOnClickListener(onClickListener);
        pause.setOnClickListener(onClickListener);
        stop.setOnClickListener(onClickListener);
        loop.setOnClickListener(onClickListener);
        surfaceView.setOnClickListener(onClickListener);
        statusBar.setOnClickListener(onClickListener);
        playerbot.setOnClickListener(onClickListener);
        assetManager = getAssets();
        timer = new Timer();
        timer.schedule(creatTimerTask(),0,100);
        manageColorofLoopBtn();
        manageTextofLoopBtn();

    }
    private void getScreenSize()
    {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
    }
    private int getPlayerHeight()
    {
        playerWidth = screenWidth;
        playerHeight = playerWidth*3/4;
        return playerHeight;
    }
    private TimerTask creatTimerTask()
    {
        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "run: videolen"+videolen);
                if (videolen != 0) {
                    PlayerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateSeekBarProgressfromPlayer();
                        }
                    });
                }
            }
        };
        return timertask;
    }

    Runnable changeStatusBarStatethread = new Runnable() {
        long startTime;
        long showTime;
        long currentTime;

        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            while (needHideStatusBar) {
                currentTime = System.currentTimeMillis();
                showTime = currentTime - startTime;
                if (showTime >= MAX_SHOW_TIME) {
                    PlayerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideStatusBar();
                        }
                    });
                }
            }

        }
    };

    private void hideStatusBar() {
        statusBar.setVisibility(View.GONE);
        needHideStatusBar = false;
    }
    private void updateSeekBarProgressfromPlayer()
    {
        int currentpos = player.getCurrentPosition();
        int progress = (currentpos*100)/videolen;
        Log.i(TAG, "updateSeekBarProgressfromPlayer:pos "+videolen+":"+currentpos);
        seekBar.setProgress(progress);
        seekBar.postInvalidate();
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void changeSrcofStatusBar(int id) {
        statusBar.setBackgroundResource(id);
    }
    private void changebkofStatusBar(int currentPlayerStatus) {
        if(currentPlayerStatus == PAUSE)
        {
            statusBar.setBackgroundResource(R.drawable.pause);
            playerbot.setBackgroundResource(R.drawable.pausebom);
        }
        else
            if(currentPlayerStatus == START){
                statusBar.setBackgroundResource(R.drawable.play);
                playerbot.setBackgroundResource(R.drawable.playbot);
            }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void showStatusBar() {
        statusBar.setVisibility(View.VISIBLE);
        needHideStatusBar = true;
    }

    private void hideStatusBarBot()
    {
        statusbarbotlayout.animate().alpha(0f).setDuration(50).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                surfaceView.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                surfaceView.setClickable(true);
                statusbarbotlayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                surfaceView.setClickable(true);
                statusbarbotlayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void showStatusBarBot()
    {
        statusbarbotlayout.animate().alpha(1f).setDuration(50).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                statusbarbotlayout.setVisibility(View.VISIBLE);
                surfaceView.setClickable(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                surfaceView.setClickable(true);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                surfaceView.setClickable(true);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
    private void prepare() {
        try {
            player.prepare();
            player_state = PREPARE;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareAsync() {
        player.prepareAsync();
        player_state = PREPARE;
    }

    private void start() {
        player.start();
        player_state = START;
    }

    private void pause() {
        player.pause();
        player_state = PAUSE;
    }

    private void stop() {
        player.stop();
        player_state = STOP;
    }

    private void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void setListener() {
        player.setOnBufferingUpdateListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnPreparedListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnVideoSizeChangedListener(this);
    }

    private void setDataSource() {
        try {
            descriptor = assetManager.openFd("vid.mp4");
            if (descriptor != null) {
                player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    private void setDataSource() {
//        try {
//            descriptor = assetManager.openFd("vid.mp4");
//            if (descriptor != null) {
//                player.setDataSource(VIDEO_PATH);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    private void setDataSource() {
//        try {
//            Uri uri = Uri.parse("http://flv.bn.netease.com/tvmrepo/2012/7/C/7/E868IGRC7-mobile.mp4");
//            player.setDataSource(this,uri);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    private void setPlayerFixedSize(MediaPlayer mp) {
        this.videowidth = mp.getVideoWidth();
        this.videoheight = mp.getVideoHeight();
        Log.i(TAG, "setPlayerFixedSize: videowidth, videoheight,playerHeight,playerWidth" + videowidth + ":" + videoheight+":"+playerHeight+":"+playerWidth);
        if (this.videoheight * this.videowidth != 0) {
            if(this.videoheight*4 >= this.videowidth*3) {
                float scale = (float) playerHeight / (float) videoheight;
                this.videoheight = playerHeight;
                this.videowidth  = (int)(this.videowidth * scale);
            }else {
                float scale = (float) playerWidth / (float) videowidth;
                this.videowidth = playerWidth;
                this.videoheight  = (int)(this.videoheight * scale);
            }
            Log.i(TAG, "setPlayerFixedSize: videowidth, videoheight:" + videowidth + ":" + videoheight);
            surfaceHolder.setFixedSize(videowidth, videoheight);
        }
    }

    private void setLooping() {
        LOOP = !LOOP;
        player.setLooping(LOOP);
    }

    private void manageColorofLoopBtn() {
        if (LOOP) {
            loop.getBackground().setAlpha(0);
        } else {
            loop.getBackground().setAlpha(100);
        }
    }

    private void manageTextofLoopBtn() {
        if (LOOP) {
            loop.setText("不循环");
        } else {
            loop.setText("循环");
        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play:
                    if (STATUS_STOP) {
                        player.prepareAsync();
                    }
                    if (!player.isPlaying()) {
                        player.start();
                    }
                    break;
                case R.id.pause:
                    if (player.isPlaying()) {
                        player.pause();
                    }
                    break;
                case R.id.stop:
                    if (player.isPlaying()) {
                        player.pause();
                        player.seekTo(player.getDuration());
                        player.stop();
                        STATUS_STOP = true;
                    }
                    break;
                case R.id.loop:
                    setLooping();
                    manageColorofLoopBtn();
                    manageTextofLoopBtn();
                    break;
                case R.id.videoview:
                    if (needHideStatusBar) {
                        hideStatusBar();
                        hideStatusBarBot();
                    } else {
                        showStatusBar();
                        showStatusBarBot();
                        Log.i(TAG, "onClick: showStatusBar()");
                        new Thread(changeStatusBarStatethread).start();
                        Log.i(TAG, "onClick: showStatusBar() end");
                    }
                    break;
                case R.id.statusbar:
                    if (player_state == PAUSE) {
                        start();
                        changebkofStatusBar(PAUSE);
                        hideStatusBar();
                        playerbot.setBackgroundResource(R.drawable.pausebom);
                    } else {
                        if (player_state == START) {
                            pause();
                            changebkofStatusBar(START);
                            new Thread(changeStatusBarStatethread).start();
                        }
                    }
                    break;
                case R.id.playerbot:
                    if (player_state == PAUSE) {
                        start();
                        changebkofStatusBar(PAUSE);
                        hideStatusBar();
                    } else {
                        if (player_state == START) {
                            pause();
                            changebkofStatusBar(START);
                            hideStatusBar();
                        }
                    }
                    break;
            }
        }
    };


    @Override
    public void onPrepared(MediaPlayer mp) {

        setPlayerFixedSize(mp);
        videolen = mp.getDuration();
        start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "onCompletion: ");
        changebkofStatusBar(START);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.i(TAG, "onSeekComplete: ");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.i(TAG, "onBufferingUpdate");
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.i(TAG, "onVideoSizeChanged");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.i(TAG, "surfaceCreated:"    );
        player.setDisplay(surfaceHolder);
        Log.i(TAG, "surfaceCreated: setDataSource");
        setDataSource();
        setListener();
        prepareAsync();
        Log.i(TAG, "surfaceCreated: prepareAsync");
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        release();
    }

}
