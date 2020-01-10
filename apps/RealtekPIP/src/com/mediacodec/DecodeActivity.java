package com.mediacodec;

import java.util.ArrayList;
import java.io.IOException;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.View.OnTouchListener;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.content.Context;
import java.io.File;
import com.mediacodec.HDMIRxPlayer;
import android.os.SystemProperties;
import android.widget.Toast;
import decode.AudioDecodeThread;
import decode.VideoDecodeThread;

import android.widget.SeekBar;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import java.util.TimerTask;
import java.util.Timer;
import android.os.Message;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class DecodeActivity extends Activity implements SurfaceHolder.Callback, View.OnTouchListener {
    public static final String TAG = "DecodeActivity";
    public static final String TAG2 = "[RTKRRS]";
    private int gridNum = 2;
    private int surfaceCount = 0;
    private boolean initFinish = false;
    private static final int MAX_GRID_NUM = 16;
    private static int MAX_VIDEO_OUT = 8;
    private static int MAX_AUDIO_OUT = 1;
    private static int REPEAT_OPTION = 1;
    private static int AUDIO_CLICK_OPTION = 1;
    private static int FREE_RUN_OPTION = 1;
    private static int TUNNEL_MODE_OPTION = 1;
    private static int PERFORMANCE_MODE = 1;
    private static int RENDER = 1;
    private static int TIMBAR = 0;
    private static final int HDMIRXID = 1;
    private static final int USBCAMERAID = 0;
    private final Handler mHandler = new Handler();
    private String SAMPLE[] = new String[MAX_GRID_NUM];
    private VideoDecodeThread mPlayer[] = new VideoDecodeThread[MAX_GRID_NUM];
    private int surfaceViewArray[] = new int[MAX_GRID_NUM];
    private AudioDecodeThread mAudioPlayer[] = new AudioDecodeThread[MAX_GRID_NUM];
    private repeatThread mRepeatThread = new repeatThread();
    private SurfaceView mSurfaceView[] = new SurfaceView[MAX_GRID_NUM];
    private SurfaceHolder mSurfaceHolder[] = new SurfaceHolder[MAX_GRID_NUM];
    private AudioManager am = null;
    private int mAudioSessionID[] = new int[MAX_GRID_NUM];
    private ArrayList<String> playList = null;
    private int startIdx = 0;
    private int lastVideoInstance = 0;
    private int lastAudioInstance = 0;
    //For CAM output
    private Camera mCamera = null;
    private boolean CameraInited = false;
    //For HDMI SRC output
    private HDMIRxPlayer m_HDMIRxPlayer = null;
    private boolean HDMIRxInited = false;
    private boolean mSkipControl = false;
    private long mLastVideoTimeStamp = 0;
    //For timeBar
    private TextView PlayCurPlace = null;
    private SeekBar TimBar = null;
    private Timer mTimer = null;
    private SurfaceView surfaceView = null;
    private RelativeLayout videoControl = null;
    private boolean dismiss = true;
    private TimerTask timTaskBar = null;
    private long mediaDuration = 0;
    private int timeBarMax = 0;
    private boolean mTimeBarIsChanging = false;
    private static final int MSG_TIME_BAR_DISMISS = 0;
    private static final int MSG_TIME_BAR_SET = 1;
    private static final int MSG_TIME_BAR_SEEK = 2;
    private long mSeekToPosition = 0;
    private int pipDemo = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        //SystemProperties.set("rtk.hwc.support.v1.flip","0");
        //SystemProperties.set("persist.rtk.cameraMode","1");
        //SystemProperties.set("persist.media.USE_RTMediaPlayer","false");
        //SystemProperties.set("persist.media.USE_RtkExtractor","false");
        Intent it = getIntent();
        Bundle bundle = it.getExtras();
        gridNum = bundle.getInt("DATA_INT");
        MAX_VIDEO_OUT = bundle.getInt("LIMIT_INT");
        MAX_AUDIO_OUT = bundle.getInt("LIMIT_AUD_INT");
        REPEAT_OPTION = bundle.getInt("REPEAT");
        AUDIO_CLICK_OPTION = bundle.getInt("AUDIOCLICK");
        FREE_RUN_OPTION = bundle.getInt("FREERUN");
        TUNNEL_MODE_OPTION = bundle.getInt("TUNNELMODE");
        PERFORMANCE_MODE = bundle.getInt("PERFORMANCE");
        RENDER = bundle.getInt("RENDER");
        TIMBAR = bundle.getInt("TIMBAR");
        playList = bundle.getStringArrayList("LIST");
        for(int i=0; i< playList.size(); i++)
        {
            SAMPLE[i] = playList.get(i);
            Log.d(TAG, "Source " + i + " = " + SAMPLE[i]);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        switch (gridNum) {
        case 1:
            setContentView(R.layout.recgrid);
            String path = SAMPLE[0];
            if(path.equalsIgnoreCase("CAM") == false && path.equalsIgnoreCase("SRC") == false)
            {
                MediaExtractor extractor = new MediaExtractor();
                try {
                    extractor.setDataSource(path);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    Log.d(TAG2, "ErrCode 1 setDataSource IO error " + path);
                    extractor.release();
                    DecodeActivity.this.finish();
                    return;
                }
                int hasAudio = 0;
                int hasVideo = 0;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);

                    Log.d(TAG, "mime type " + mime);
                    if (mime.startsWith("video/"))
                        hasVideo = 1;
                    else if(mime.startsWith("audio/"))
                        hasAudio = 1;
                }
                if(MAX_VIDEO_OUT == 1 && hasVideo == 0)
                {
                    Log.d(TAG, "Parser cannot get video format");
                    MAX_VIDEO_OUT = 0;
                }
                if(MAX_AUDIO_OUT == 1 && hasAudio == 0)
                {
                    Log.d(TAG, "Parser cannot get audio format");
                    MAX_AUDIO_OUT = 0;
                }
                extractor.release();
            }
            break;
        case 2:
            setContentView(R.layout.recgrid2);
            break;
        case 4:
            setContentView(R.layout.recgrid4);
            break;
        case 9:
            setContentView(R.layout.recgrid9);
            break;
        case 16:
            setContentView(R.layout.recgrid16);
            break;
        case 11:
            gridNum = 2;
            pipDemo = 1;
            setContentView(R.layout.recdemo);
            break;
        default:
            setContentView(R.layout.recgrid2);
            break;
        }
//        SAMPLE = getResources().getStringArray(R.array.playList);
        setupViewArray();
        setupViewComponent();

        //For timeBar
        if ((gridNum == 1) && (TIMBAR==1)) {
            mTimer = new Timer();
            timTaskBar = new TimerTask() {
                @Override
                public void run(){
                    if (mTimeBarIsChanging){
                        return;
                    }
                    else{
                        handleProgress.sendEmptyMessage(MSG_TIME_BAR_SET);
                    }
                }
            };
            mTimer.schedule(timTaskBar,0,1000);

            TimBar=(SeekBar)findViewById(R.id.video_controlprogress);
            PlayCurPlace=(TextView)findViewById(R.id.video_curplace);
            videoControl=(RelativeLayout)findViewById(R.id.video_play_control_layout);
            mSurfaceView[0].setOnClickListener(onClickListener);
            videoControl.setVisibility(View.VISIBLE);

            TimBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mTimeBarIsChanging = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int process = seekBar.getProgress();
                    mSeekToPosition = process*mediaDuration/timeBarMax;
                    handleProgress.sendEmptyMessage(MSG_TIME_BAR_SEEK);
                    mTimeBarIsChanging = false;
               }
            });
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume ... ");
        super.onResume();
        if(mRepeatThread == null)
        {
            Log.d(TAG, "Donot support resume playback from home, finished self ");
            DecodeActivity.this.finish();
        }
    }

    private void setupViewArray() {

        if (gridNum >= 1) {
            surfaceViewArray[0] = R.id.surfaceView1;
        }

        if (gridNum >= 2) {
            surfaceViewArray[1] = R.id.surfaceView2;
        }

        if (gridNum >= 4) {
            surfaceViewArray[2] = R.id.surfaceView3;
            surfaceViewArray[3] = R.id.surfaceView4;
        }

        if (gridNum >= 9) {
            surfaceViewArray[4] = R.id.surfaceView5;
            surfaceViewArray[5] = R.id.surfaceView6;
            surfaceViewArray[6] = R.id.surfaceView7;
            surfaceViewArray[7] = R.id.surfaceView8;
            surfaceViewArray[8] = R.id.surfaceView9;
        }

        if (gridNum == 16) {
            surfaceViewArray[9] = R.id.surfaceView10;
            surfaceViewArray[10] = R.id.surfaceView11;
            surfaceViewArray[11] = R.id.surfaceView12;
            surfaceViewArray[12] = R.id.surfaceView13;
            surfaceViewArray[13] = R.id.surfaceView14;
            surfaceViewArray[14] = R.id.surfaceView15;
            surfaceViewArray[15] = R.id.surfaceView16;
        }
    }

    private void setupViewComponent() {
        Log.d(TAG, "gridNum " + gridNum);
        for (int i = 0; i < gridNum; i++) {
            mSurfaceView[i] = (SurfaceView) findViewById(surfaceViewArray[i]);
            mSurfaceView[i].setOnTouchListener(this);
            mSurfaceHolder[i] = mSurfaceView[i].getHolder();
            mSurfaceHolder[i].setFixedSize(1920, 1080);
            mSurfaceHolder[i].addCallback(this);

            if(i == 1)
            {
                mSurfaceView[i].setZOrderOnTop(true);
                mSurfaceView[i].setZOrderMediaOverlay(true);
            }

            if(i == 0)
                mSurfaceHolder[i].setFormat(PixelFormat.TRANSPARENT);
            if(am == null)
            {
                am = (AudioManager)mSurfaceView[i].getContext().getSystemService(Context.AUDIO_SERVICE);
                if(TUNNEL_MODE_OPTION == 1)
                {
                    if(am.isWiredHeadsetOn() || am.isBluetoothA2dpOn())
                    {
                        Log.d(TAG, "Disable tunnel mode since audio devices not support");
                        TUNNEL_MODE_OPTION = 0;
                    }
                }
            }
        }
    }
    
    /**
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)
            {
                Log.d(TAG, "RIGHT KEY ...");
                if(initFinish && event.getRepeatCount()==0)
                {
                    initFinish = false;
                    ForwardSurfaceContent();
                }
                return true;
            }
            else if(event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT)
            {
                Log.d(TAG, "LEFT KEY ...");
                if(initFinish && event.getRepeatCount()==0)
                {
                    initFinish = false;
                    BackwardSurfaceContent();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    **/

    @Override
    public boolean onTouch(View v, MotionEvent event){
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if(AUDIO_CLICK_OPTION == 0)
                return false;
            for (int i = 0; i < gridNum; i++) {
                if(v == mSurfaceView[i])
                {
                    if(mAudioPlayer[i] != null && mAudioPlayer[i].isAlive())
                        mAudioPlayer[i].switchMute();
                }
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ......... !!!");
        mHandler.removeCallbacksAndMessages(null);
        //Intent intent = new Intent();
        //intent.setClass(DecodeActivity.this, MainActivity.class);
        //startActivity(intent);
        ForceStopRepeatThread();
        ForceStopAllDecodeThread();

        if(HDMIRxInited && m_HDMIRxPlayer != null)
        {
            if(m_HDMIRxPlayer.isPlaying() == true)
            {
                Log.d(TAG, "SRC IN Stop ...");
                m_HDMIRxPlayer.stop();
            }
        }

        if(CameraInited && mCamera != null)
        {
            Log.d(TAG, "CAM Stop ...");
            mCamera.stopPreview();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop ......... !!!");
        super.onStop();

        if(HDMIRxInited && m_HDMIRxPlayer != null) {
            m_HDMIRxPlayer.release();
            m_HDMIRxPlayer = null;
        }

        if(CameraInited && mCamera != null)
        {
            mCamera.release();
            mCamera = null;
        }

        if(timTaskBar != null)
            timTaskBar.cancel();
        if(mTimer != null)
            mTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy ......... !!!");
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(surfaceCount == gridNum)
            return;
        
        Log.d(TAG, "surfaceChanged surfaceCount " + surfaceCount);
        if(surfaceCount == 0)
        {
            ShowTitle();
        }
        
        surfaceCount = surfaceCount + 1;
        if(surfaceCount == gridNum)
        {
            mHandler.post(decodeThread);
            mRepeatThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {   
        Log.d(TAG, "SufaceDestroyed ......... !!!");        
    }

    private void ForceStopRepeatThread(){
        if(mRepeatThread != null)
        {
            if(mRepeatThread.isAlive())
            {
                Log.d(TAG, "Force stop ForceStopRepeatThread");
                mRepeatThread.stopThread();
                while(mRepeatThread.isAlive())
                {
                    try {
                        Thread.sleep(500);
                        Log.d(TAG, "ForceStopRepeatThread WAIT ...");
                    } catch(InterruptedException  ex) {
                    }
                }
            }
            mRepeatThread = null;
        }
    }
    
    private void ForceStopAllDecodeThread(){
        int timeout = 5;
        for (int i = 0; i < gridNum; i++) {
            if (mPlayer[i] != null) {
                if(mPlayer[i].isAlive())
                {
                    Log.d(TAG, "Force stop VideoDecodeThread instance " + i + " !!!");
                    mPlayer[i].stopThread();
                }
            }

            if (mAudioPlayer[i] != null) {
                Log.d(TAG, "Force stop AudioDecodeThread instance " + i
                        + " !!!");
                if(mAudioPlayer[i].isAlive())
                {
                    mAudioPlayer[i].stopThread();
                }
            }
        }

        for (int i = 0; i < gridNum; i++) {
            timeout = 5;
            if (mPlayer[i] != null) {
                while(mPlayer[i].isAlive())
                {
                    try {
                        Thread.sleep(1000);
                        timeout--;
                        Log.d(TAG, "ForceStopAllDecodeThread Video WAIT ...");
                    } catch(InterruptedException  ex) {
                    }

                    if(timeout<0)
                    {
                        mPlayer[i].releaseResource();
                        break;
                    }
                }
                mPlayer[i] = null;
            }
            timeout = 5;
            if (mAudioPlayer[i] != null) {
                while(mAudioPlayer[i].isAlive())
                {
                    try {
                        Thread.sleep(1000);
                        timeout--;
                        Log.d(TAG, "ForceStopAllDecodeThread Audio WAIT ...");
                    } catch(InterruptedException  ex) {
                    }

                    if(timeout<0)
                    {
                        mAudioPlayer[i].releaseResource();
                        break;
                    }
                }
                mAudioPlayer[i] = null;
            }
        }
    }

    private final boolean CheckPathAvailable(int idx)
    {
        for (int i = 0; i < gridNum; i++) {
            String path = SAMPLE[startIdx+i];
            if(path.equalsIgnoreCase("CAM") ||
                path.equalsIgnoreCase("SRC"))
                continue;
            else
            {
                File file = new File(path);
                if(!file.exists())
                    return false;
            }
        }
        return true;
    }

    private final void ForwardSurfaceContent(){
        Log.d(TAG, "Enter ForwardSurfaceContent() ");
        startIdx = startIdx + gridNum;
        if(startIdx >= MAX_GRID_NUM)
            startIdx = MAX_GRID_NUM - gridNum;

        if(CheckPathAvailable(startIdx))
        {
            ShowTitle();

            if(HDMIRxInited && m_HDMIRxPlayer != null)
            {
                if(m_HDMIRxPlayer.isPlaying() == true)
                {
                    Log.d(TAG, "SRC IN Stop ...");
                    m_HDMIRxPlayer.stop();
                }
                m_HDMIRxPlayer.release();
                m_HDMIRxPlayer = null;
                HDMIRxInited = false;
            }

            if(CameraInited && mCamera != null)
            {
                Log.d(TAG, "CAM Stop ...");
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                CameraInited = false;
            }

            ForceStopRepeatThread();
            ForceStopAllDecodeThread();
            
            mHandler.post(decodeThread);
            mRepeatThread = new repeatThread();
            mRepeatThread.start();
        }
    }
    
    private final void BackwardSurfaceContent(){
        Log.d(TAG, "Enter BackwardSurfaceContent() ");

        startIdx = startIdx - gridNum;
        if(startIdx < 0)
            startIdx = 0;

        if(CheckPathAvailable(startIdx))
        {
            ShowTitle();

            if(HDMIRxInited && m_HDMIRxPlayer != null)
            {
                if(m_HDMIRxPlayer.isPlaying() == true)
                {
                    Log.d(TAG, "SRC IN Stop ...");
                    m_HDMIRxPlayer.stop();
                }
                m_HDMIRxPlayer.release();
                m_HDMIRxPlayer = null;
                HDMIRxInited = false;
            }

            if(CameraInited && mCamera != null)
            {
                Log.d(TAG, "CAM Stop ...");
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                CameraInited = false;
            }

            ForceStopRepeatThread();
            ForceStopAllDecodeThread();
            
            mHandler.post(decodeThread);
            mRepeatThread = new repeatThread();
            mRepeatThread.start();
        }
    }
    
    private void ShowTitle()
    {        
        String s = "Multi-Video: Start content from " + (startIdx+1) + " to " + (startIdx+gridNum);
        //setTitle(s);
        //Toast.makeText(DecodeActivity.this, s, Toast.LENGTH_SHORT).show();
    }
    
    private final Runnable decodeThread = new Runnable(){
        public void run (){
            for (int i = 0; i < gridNum; i++) {

                if(i >= MAX_VIDEO_OUT && i >= MAX_AUDIO_OUT)
                {
                    break;
                }          
                else {
                    if(i < MAX_VIDEO_OUT)
                    {
                        if(CameraInited == false && SAMPLE[startIdx+i].equalsIgnoreCase("CAM"))
                        {
                            CameraInited = true;
                            InitCameraParameter(mSurfaceHolder[i]);
                        }
                        else if(HDMIRxInited == false && SAMPLE[startIdx+i].equalsIgnoreCase("SRC"))
                        {
                            HDMIRxInited = true;
                            InitHDMISrcIn(mSurfaceView[i], mSurfaceHolder[i], i);
                        }
                        else if ((mPlayer[i] == null || !mPlayer[i].isAlive()) && !SAMPLE[startIdx+i].equalsIgnoreCase("CAM") && !SAMPLE[startIdx+i].equalsIgnoreCase("SRC")) 
                        {
                            if(i < MAX_AUDIO_OUT && mAudioPlayer[i] != null && mAudioPlayer[i].isAlive())
                            {
                                mAudioPlayer[i].stopThread();
                                if(mAudioPlayer[i].isAlive())
                                {
                                    Log.d(TAG, "Video WAIT Audio Stop ...");
                                    break;
                                }
                            }
                            if(TUNNEL_MODE_OPTION == 1)
                            {
                                if(i < MAX_AUDIO_OUT)
                                    mAudioSessionID[i] = am.generateAudioSessionId();
                                else
                                    mAudioSessionID[i] = 0;
                                Log.d(TAG, "Create AudioSessionID " + mAudioSessionID[i]);
                                mPlayer[i] = new VideoDecodeThread(mSurfaceHolder[i].getSurface(),
                                    SAMPLE[startIdx+i], FREE_RUN_OPTION, mAudioSessionID[i], PERFORMANCE_MODE, RENDER, i==1?pipDemo:0);
                            }
                            else
                                mPlayer[i] = new VideoDecodeThread(mSurfaceHolder[i].getSurface(),
                                    SAMPLE[startIdx+i], FREE_RUN_OPTION, -1, PERFORMANCE_MODE, RENDER, i==1?pipDemo:0);
                            Log.d(TAG, "Start VideoDecodeThread instance " + i);
                            if(initFinish == false && lastVideoInstance < i)
                                lastVideoInstance = i;
                            mPlayer[i].start();
                        }
                    }
                        
                    if (i < MAX_AUDIO_OUT && !SAMPLE[startIdx+i].equalsIgnoreCase("CAM") && !SAMPLE[startIdx+i].equalsIgnoreCase("SRC"))
                    {
                        if(mAudioPlayer[i] == null || !mAudioPlayer[i].isAlive())
                        {
                            if(i < MAX_VIDEO_OUT && mPlayer[i].startMs == 0 && TUNNEL_MODE_OPTION != 1)
                            {
                                if(REPEAT_OPTION == 1) {
                                    Log.d(TAG, "Wait Video startTime ready ...");
                                    break;
                                } else {
                                    try {
                                        int timeout = 10;
                                        while(mPlayer[i].startMs == 0 && timeout > 0) {
                                            timeout--;
                                            Thread.sleep(500);
                                            Log.d(TAG, "Wait Video startTime ready ...");
                                        }
                                    } catch(InterruptedException  ex) {
                                    }
                                }
                            }
                            if(mPlayer[i] != null)
                            {
                                if(mPlayer[i].isAlive() && mPlayer[i].curExtractorTimeStamp > 6000000) {
                                    Log.d(TAG, "Audio was ready but video still playback ...");
                                    if(TUNNEL_MODE_OPTION == 1)
                                    {
                                        Log.d(TAG, "Tunnel mode stop video immediately");
                                         mPlayer[i].stopThread();
                                    }
                                    break;
                                }
                                if(mPlayer[i].startMs == 0)
                                    mPlayer[i].startMs = System.currentTimeMillis();
                                if(TUNNEL_MODE_OPTION == 1)
                                    mAudioPlayer[i] = new AudioDecodeThread(SAMPLE[startIdx+i], mPlayer[i].startMs, FREE_RUN_OPTION, mAudioSessionID[i]);
                                else
                                    mAudioPlayer[i] = new AudioDecodeThread(SAMPLE[startIdx+i], mPlayer[i].startMs, FREE_RUN_OPTION, -1);
                            }
                            else
                            {
                                if(TUNNEL_MODE_OPTION == 1)
                                {
                                    mAudioSessionID[i] = am.generateAudioSessionId();
                                    Log.d(TAG, "Create AudioSessionID " + mAudioSessionID[i]);
                                    mAudioPlayer[i] = new AudioDecodeThread(SAMPLE[startIdx+i], System.currentTimeMillis(), FREE_RUN_OPTION, mAudioSessionID[i]);
                                }
                                else
                                    mAudioPlayer[i] = new AudioDecodeThread(SAMPLE[startIdx+i], System.currentTimeMillis(), FREE_RUN_OPTION, -1);
                            }

                            Log.d(TAG, "Start AudioDecodeThread instance " + i);
                            if(initFinish == false && lastAudioInstance < i)
                                lastAudioInstance = i;
                            mAudioPlayer[i].start();
                        }
                    }

                    if(i == (MAX_VIDEO_OUT >= MAX_AUDIO_OUT ? MAX_VIDEO_OUT-1 : MAX_AUDIO_OUT-1))
                    {
                        initFinish = true;
                    }
                }
            }
        }
    };

    public class repeatThread extends Thread {
        
        private boolean stopThread = false;
        private int error_count = 0;
        private int i = 0;
        @Override
        public void run() {
            if(MAX_VIDEO_OUT >= MAX_AUDIO_OUT)
            while(mPlayer[lastVideoInstance] == null && !stopThread)
            {
                try {
                    Thread.sleep(1000);
                    Log.d(TAG, "repeatThread last video ready WAIT ...");
                } catch(InterruptedException  ex) {
                }
            }
            else
            while(mAudioPlayer[lastAudioInstance] == null && !stopThread)
            {
                try {
                    Thread.sleep(1000);
                    Log.d(TAG, "repeatThread last audio ready WAIT ...");
                } catch(InterruptedException  ex) {
                }
            }
            Log.d(TAG, "Prepare to execute decodeRepeatThread !!!");
            while (!stopThread) {
                if(REPEAT_OPTION == 1)
                mHandler.post(decodeThread);
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException  ex) {
                }
                error_count++;
//                Log.d(TAG, "repeatThread normally WAIT ...");
                for (int i = 0; i < gridNum; i++) {
                    if(i < MAX_VIDEO_OUT && mPlayer[i] != null && REPEAT_OPTION == 0)
                    {
                        if(mPlayer[i].curTimeStamp == 0 && error_count > 10)
                        {
                            Log.e(TAG2, "ErrCode 7 idle for a while, terminate it ...");
                            DecodeActivity.this.finish();
                        }

                        if(MAX_VIDEO_OUT == 1 && mPlayer[i].curTimeStamp != 0)
                        {
                            if(mPlayer[i].curTimeStamp != mLastVideoTimeStamp)
                            {
                                mLastVideoTimeStamp = mPlayer[i].curTimeStamp;
                                error_count = 0;
                            }
                            else if(error_count > 3)
                            {
                                Log.e(TAG2, "ErrCode 8 idle for a while, terminate it ...");
                                DecodeActivity.this.finish();
                            }
                        }
                    }
                    else if(i < MAX_AUDIO_OUT && mAudioPlayer[i] != null && REPEAT_OPTION == 0 && TUNNEL_MODE_OPTION == 0)
                    {
                        if(mAudioPlayer[i].curTimeStamp == 0 && error_count > 10)
                        {
                            Log.d(TAG, "mAudioPlayer[" + i + "] " + SAMPLE[i] + " was idle for a while, terminate it ... ");
                            DecodeActivity.this.finish();
                        }
                    }

                    if(i < MAX_VIDEO_OUT && i < MAX_AUDIO_OUT && mPlayer[i] != null && mAudioPlayer[i] != null && TUNNEL_MODE_OPTION == 0) {
                        if(mPlayer[i].curTimeStamp + 50000/*50ms*/ < mAudioPlayer[i].curTimeStamp) {
                            if(mSkipControl == false) {
                                mSkipControl = true;
                                mPlayer[i].skipRefFrameControl(1);
                            }
                        } else {
                            if(mSkipControl == true) {
                                mSkipControl = false;
                                mPlayer[i].skipRefFrameControl(0);
                            }
                        }
                    }

                    if(MAX_VIDEO_OUT == 1 && !mPlayer[0].isAlive() && REPEAT_OPTION == 0)
                        DecodeActivity.this.finish();


                    if(gridNum == 2 && mPlayer[i] != null && mPlayer[i].ErrPTS == true)
                        DecodeActivity.this.finish();
/**
                    if(i < MAX_VIDEO_OUT && mPlayer[i] != null) {
                        Log.v(TAG, "mPlayer[" + i + "] " + SAMPLE[i] + " State " + mPlayer[i].flag);
                        Log.v(TAG, "mPlayer[" + i + "] frameRate " + mPlayer[i].mDecodeFPS);
                    }
                    if(i < MAX_AUDIO_OUT && mAudioPlayer[i] != null)
                        Log.v(TAG, "mAudioPlayer["+ i +"] " + SAMPLE[i] + " State " + mAudioPlayer[i].flag);
**/
                }
            }
            
        }
        public void stopThread() {
            Log.d(TAG, "repeatThread being terminated");
            stopThread = true;
        }
    }

    private void InitCameraParameter(SurfaceHolder holder)
    {
        mCamera = Camera.open(USBCAMERAID);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException ioe) {
          // Something bad happened
        }
    }

    private void InitHDMISrcIn(SurfaceView view, SurfaceHolder holder, int idx)
    {
        m_HDMIRxPlayer = new HDMIRxPlayer(this, view, holder, 1920, 1080);
        if(m_HDMIRxPlayer.isPlaying() == true)
            m_HDMIRxPlayer.stop();
        m_HDMIRxPlayer.play();
        if(idx != 0)
            m_HDMIRxPlayer.togglePreview(true, false);
    }

    //for timebar
    Handler handleProgress= new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TIME_BAR_DISMISS:
                    if (dismiss) {
                        dismiss = false;
                        videoControl.setVisibility(View.GONE);
                    }
                    break;
                case MSG_TIME_BAR_SET:

                    long currentTim = 0;
                    long pos = 0;
                    if (MAX_AUDIO_OUT == 0 && mPlayer[0] != null && mPlayer[0].isAlive()) {
                        mediaDuration = mPlayer[0].getDuration();
                        currentTim = mPlayer[0].curTimeStamp;
                    }else {
                        if (mAudioPlayer[0] != null && mAudioPlayer[0].isAlive()&& mPlayer[0] != null && mPlayer[0].isAlive()) {
                            mediaDuration = mAudioPlayer[0].getDuration();
                            currentTim = mAudioPlayer[0].curTimeStamp;
                        }
                    }
                    if (mediaDuration != 0) {
                        timeBarMax=TimBar.getMax();
                        pos=timeBarMax*currentTim / mediaDuration;
                        TimBar.setProgress((int)pos);
                        PlayCurPlace.setText(changeToTimeFormat(currentTim)+"/"+changeToTimeFormat(mediaDuration));
                    }
                    break;
                case MSG_TIME_BAR_SEEK:

                    if(TUNNEL_MODE_OPTION == 1) {
                        Toast.makeText(DecodeActivity.this, "Tunnel mode not support seek !!!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long actualSeekToPos = 0;
                    if(mPlayer[0]!=null)
                        actualSeekToPos = mPlayer[0].seekTo(mSeekToPosition);
                    if(mAudioPlayer[0]!=null) {
                        mAudioPlayer[0].seekTo(actualSeekToPos); // avoid sound rendering delays
                        mAudioPlayer[0].startMs = mPlayer[0].startMs - 300;
                    }
                    PlayCurPlace.setText(changeToTimeFormat(actualSeekToPos)+"/"+changeToTimeFormat(mediaDuration));
                default:
                    break;
            }
        };
    };

    //for timBar occur and dispare
    OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.surfaceView1:
                    showControlView();
                    break;
                default:
                    break;
            }
        }
    };
    protected void showControlView() {
        if (dismiss) {
            dismiss=false;
            videoControl.setVisibility(View.GONE);
        }
        else {
            dismiss=true;
            videoControl.setVisibility(View.VISIBLE);
            handleProgress.sendEmptyMessageDelayed(0, 5000);
        }
    }
    public static String changeToTimeFormat(long microseconds) {
        long seconds=TimeUnit.MICROSECONDS.toSeconds(microseconds);
        long minus=seconds/60;
        long second=seconds%60;
        if (second < 10 && minus < 10)
            return "0" + minus + ":0" + second;
        else {
            if (second >= 10 && minus < 10)
                return "0"+ minus + ":" + second;
            else {
                if(second<10 && minus >=10)
                   return minus + ":0" + second;
                else
                   return minus + ":"+second;
            }
        }
    }
}
