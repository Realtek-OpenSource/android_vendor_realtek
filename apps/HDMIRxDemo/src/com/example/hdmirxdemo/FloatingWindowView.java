package com.example.hdmirxdemo;

import android.content.Context;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.util.Log;
import android.view.Gravity;
import android.graphics.PixelFormat;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.view.IWindowManager;
import android.view.Display;
import android.graphics.Point;

public class FloatingWindowView extends FrameLayout
    implements View.OnClickListener {

    private static final String TAG = "HDMIRxFloatingWindowView";

    public static final int FLOATING_WINDOW_FULL_SCREEN_FLAG = WindowManager.LayoutParams.FLAG_FULLSCREEN;

    private int mDragStartX;
    private int mDragStartY;

    private int mXPos;
    private int mYPos;
    private int mWidth;
    private int mHeight;

    private Context mContext;

    private int xBeforeHide;
    private int yBeforeHide;

    private int mDownX;
    private int mDownY;

    private int mLatestPosX = Keys.sServiceModeLocation[0];
    private int mLatestPosY = Keys.sServiceModeLocation[1];

    private boolean mFullScreen = false;

    public boolean mVisible = true;
    public int mAnimationResource = 0; //R.style.Animation_Window; //R.anim.no_anim; //android.R.style.Animation_Translucent;

    public static int[] sInvisibleLocation = {5000,5000,1920,1080};

    public FloatingWindowView(Context c){
        super(c);
        mContext = c;
        setOnClickListener(this);
    }

    public FloatingWindowView(Context c, AttributeSet attrs){
        super(c,attrs);
        mContext = c;
        setOnClickListener(this);
    }

    public FloatingWindowView(Context c, AttributeSet attrs, int defStyleAttr){
        super(c,attrs,defStyleAttr);
        mContext = c;
        setOnClickListener(this);
    }

    public FloatingWindowView(Context c, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        super(c,attrs,defStyleAttr,defStyleRes);
        mContext = c;
        setOnClickListener(this);
    }

    public void updateLocation(int[] position, boolean touchable) {
        Log.d(TAG, "updateLocation");
        //updateLocation_2(sInvisibleLocation,touchable);
        updateLocation_2(position,touchable);
    }

    public void updateLocation_2(int[] position, boolean touchable) {

        int flag;
        if(touchable) {
            flag = FLOATING_WINDOW_FULL_SCREEN_FLAG|
                    //WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }else{
            flag = FLOATING_WINDOW_FULL_SCREEN_FLAG|
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }


        Log.i(TAG, "updateLocation_2 :"+position[0]+","+position[1]+","+position[2]+","+position[3]);

        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                position[2],
                position[3],
                FloatingWindowService2.FLOATING_WINDOW_TYPE, //WindowManager.LayoutParams.TYPE_TOAST,
                flag,
                PixelFormat.TRANSLUCENT);

        wmParams.x = position[0];
        wmParams.y = position[1];

        wmParams.windowAnimations = mAnimationResource; //android.R.style.Animation_Toast;

        wmParams.gravity = Gravity.TOP|Gravity.LEFT;
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.updateViewLayout(this, wmParams);
    }

    public void updateSize(int w, int h){
        int flag = FLOATING_WINDOW_FULL_SCREEN_FLAG|
                //WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                //WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                w,
                h,
                FloatingWindowService2.FLOATING_WINDOW_TYPE, //WindowManager.LayoutParams.TYPE_TOAST,
                flag,
                PixelFormat.TRANSLUCENT);

        wmParams.x = 0;
        wmParams.y = 0;

        wmParams.gravity = Gravity.TOP|Gravity.LEFT;
        wmParams.windowAnimations = mAnimationResource; //android.R.style.Animation_Toast;
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.updateViewLayout(this, wmParams);
    }

    public void setVisibile2(boolean on){

        if(on == mVisible)
            return;

        int flag;

        WindowManager.LayoutParams lParams = (WindowManager.LayoutParams) getLayoutParams();

        int x = lParams.x;
        int y = lParams.y;
        int w = lParams.width;
        int h = lParams.height;

        if(on){
            flag = FLOATING_WINDOW_FULL_SCREEN_FLAG|
                //WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                //WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }else{
            flag = FLOATING_WINDOW_FULL_SCREEN_FLAG|
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }

        if(!on){
            xBeforeHide = lParams.x;
            yBeforeHide = lParams.y;
        }

        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                w,
                h,
                FloatingWindowService2.FLOATING_WINDOW_TYPE, //WindowManager.LayoutParams.TYPE_TOAST,
                flag,
                PixelFormat.TRANSLUCENT);

        if(on){
            //this.setAlpha(1);
            wmParams.x = xBeforeHide;
            wmParams.y = yBeforeHide;
        }else{
            //this.setAlpha(0);
            wmParams.x = 5000;
            wmParams.y = 5000;
        }

        //wmParams.x = x;
        //wmParams.y = y;

        wmParams.gravity = Gravity.TOP|Gravity.LEFT;
        wmParams.windowAnimations = mAnimationResource; //android.R.style.Animation_Toast;
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.updateViewLayout(this, wmParams);
        mVisible = on;
    }

    private Point resolveWmSize() {

        IWindowManager mWm = IWindowManager.Stub.asInterface(
                ServiceManager.checkService(Context.WINDOW_SERVICE));

        Point initialSize = new Point();
        Point baseSize = new Point();

        try {

            mWm.getInitialDisplaySize(Display.DEFAULT_DISPLAY, initialSize);
            mWm.getBaseDisplaySize(Display.DEFAULT_DISPLAY, baseSize);

            Log.d(TAG, "Physical size: " + initialSize.x + "x" + initialSize.y);
            Log.d(TAG, "Override size: " + baseSize.x + "x" + baseSize.y);

        } catch (RemoteException e) {
        }

        return baseSize;
    }

    @Override
    public void onClick(View v) {
        /* Do nothing now */
    }

    private void toggleFullScreen() {
        Point p = resolveWmSize();
        if(mFullScreen) {
            int[] pos = Keys.sServiceModeLocation;
            pos[0] = mLatestPosX;
            pos[1] = mLatestPosY;

            /* review underflow */
            if(pos[0] < 0) pos[0] = 0;
            if(pos[1] < 0) pos[1] = 0;

            /* review overflow */
            if((pos[0] + Keys.sServiceModeLocation[2]) >= p.x) {
                pos[0] = p.x - Keys.sServiceModeLocation[2];
            }

            if((pos[1] + Keys.sServiceModeLocation[3]) >= p.y) {
                pos[1] = p.y - Keys.sServiceModeLocation[3];
            }


            Log.d(TAG, "Switch to PIP mode:"+
                    " "+pos[0]+
                    " "+pos[1]+
                    " "+pos[2]+
                    " "+pos[3]);

            FloatingWindowService2.updateWindowLocation(pos, true);
            mFullScreen = false;
        } else {
            int[] dim = {0, 0, p.x, p.y};

            FloatingWindowService2.updateWindowLocation(
                    dim,
                    true);
            mFullScreen = true;
        }
    }

    public boolean onTouchEvent (MotionEvent event){
        final int x = (int) event.getRawX();
        final int y = (int) event.getRawY();

        //Log.d(TAG, "action:"+event.getAction()+" X:"+x+" y:"+y);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                {
                    if(x == mDownX && y == mDownY) {
                        Log.d(TAG, "click event detected");
                        if(MyReceiver.isServiceMode()) {
                            toggleFullScreen();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                {
                    WindowManager.LayoutParams lParams = (WindowManager.LayoutParams) getLayoutParams();
                    //Log.d(TAG, "MotionEvent.ACTION_DOWN "+lParams);
                    mXPos = lParams.x;
                    mYPos = lParams.y;
                    mWidth = lParams.width;
                    mHeight = lParams.height;

                    mDragStartX = x;
                    mDragStartY = y;

                    /* click handling */
                    mDownX = x;
                    mDownY = y;

                    //Log.d(TAG, "view pos "+mXPos+" "+mYPos+" - "+mDragStartX+" "+mDragStartY);
                }
            break;
            case MotionEvent.ACTION_MOVE:
                {

                    //ViewGroup.LayoutParams lParams = getLayoutParams();
                    //Log.d(TAG, "MotionEvent.ACTION_MOVE "+lParams);
                    int dragX = x - mDragStartX;
                    int dragY = y - mDragStartY;

                    int newPosX = mXPos+dragX;
                    int newPosY = mYPos+dragY;
                    int flag = FLOATING_WINDOW_FULL_SCREEN_FLAG|
                        //WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                        //WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

                    // update layout
                    WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
                            mWidth,//WindowManager.LayoutParams.MATCH_PARENT,
                            mHeight,//WindowManager.LayoutParams.MATCH_PARENT,
                            FloatingWindowService2.FLOATING_WINDOW_TYPE, //WindowManager.LayoutParams.TYPE_TOAST,
                            flag,
                            PixelFormat.TRANSLUCENT);

                    wmParams.x = newPosX;
                    wmParams.y = newPosY;
                    wmParams.gravity = Gravity.TOP|Gravity.LEFT;
                    wmParams.windowAnimations = mAnimationResource; //android.R.style.Animation_Toast;

                    WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
                    wm.updateViewLayout(this, wmParams);

                    mLatestPosX = newPosX;
                    mLatestPosY = newPosY;

                    Keys.serviceModeX = mLatestPosX;
                    Keys.serviceModeY = mLatestPosY;

                    /*
                    setLayoutParams(wmParams);
                    Log.d(TAG, "ACTION_MOVE: setlayout:"+wmParams);
                    ViewParent parent = getParent();
                    Log.d(TAG, "parent:"+parent);
                    if(parent instanceof View){
                        View p = (View) parent;
                        Log.d(TAG, "invalidate");
                        p.invalidate();
                    }
                    */
                }
            break;
            default:
            break;
        }
        return true;
    }
}
