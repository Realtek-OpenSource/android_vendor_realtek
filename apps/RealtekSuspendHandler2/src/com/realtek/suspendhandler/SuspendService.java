package com.realtek.suspendhandler;

import android.app.Service;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;

import android.os.IBinder;
import android.os.Binder;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
//import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UEventObserver;
import android.os.RemoteException;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;
import android.os.AsyncTask;
import android.widget.Toast;

import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import android.util.Log;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;

import com.realtek.hardware.RtkHDMIManager2;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SuspendService extends Service {

    private static final boolean DEBUG = false;
    private static final String TAG = "RealtekSuspendService";

    public static final String PROP_S3_HANDLE_READY = "vendor.rtk.s3.ready";

    private static final String KERNEL_SUSPEND_CONTEXT = "/sys/kernel/suspend/context";
    private static final String KERNEL_ANDROID_CTRL = "/sys/class/rtk_pm/android_control/pm_state";
    private static final String KERNEL_BLOCK_WAKELOCK = "/sys/class/rtk_pm/android_control/pm_block_wakelock";

    private final IBinder mBinder = new LocalBinder();

    private long mSuspendContext;

    private Handler mHandler = new Handler();

    public List<VolumeInfo> mUnmountedSDs;
    public List<VolumeInfo> mUnmountList;

    public boolean mIsSuspendMode = false;

    public RtkHDMIManager2 mHDMIManager = null;

    private WakeUpRunnable mWakeupRunnable = new WakeUpRunnable();

    private UnmountReceiver mUnmountReceiver = null;

    private ScreenOnOffReceiver mScreenStateReceiver = null;

    private boolean removeWakelockCountBlocker = false;

    private UEventObserver mObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            Log.d(TAG, "onUEvent:"+event);
            String line = readFile(KERNEL_ANDROID_CTRL);
            int intVal = Integer.parseInt(line);
            Log.d(TAG, "onUEvent value:"+line+ " intVal:"+intVal);

            if(intVal == 0) {

                long suspendContext = getKernelSuspendContext();
                Log.d(TAG, "suspendContext before Suspend :"+suspendContext);
                setSuspendContext(suspendContext);

                // writeFile(KERNEL_BLOCK_WAKELOCK,"1");
                // check S3 signal
                processS3PreHandling();
                notifyKernelComplete();

                postWakeUpTask();
            }
        }
    };

    class ScreenOnOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "receive: "+intent.getAction());
            /*
            if(Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if(removeWakelockCountBlocker) {
                    removeWakelockCountBlocker = false;
                    writeFile(KERNEL_BLOCK_WAKELOCK,"0");
                }
            }
            */
        }
    }

    class WakeUpRunnable implements Runnable{
        public void run() {
            long suspendContext =  getKernelSuspendContext();

            Log.d(TAG, "suspendContext:"+
                    suspendContext+
                    " mSuspendContext:"+
                    mSuspendContext);
            // clear pm_state
            writeFile(KERNEL_ANDROID_CTRL,"0");

            if(suspendContext > mSuspendContext){
                Log.d(TAG, "Execute WakeUp call PM");

                PowerManager pm =
                    (PowerManager)getSystemService(Context.POWER_SERVICE);
                long time = SystemClock.uptimeMillis();
                //long time = SystemClock.elapsedRealtime();
                pm.wakeUp(time);

                mIsSuspendMode = false;
                //SystemClock.sleep(1000);
                //Log.d(TAG, "write "+KERNEL_BLOCK_WAKELOCK);
                //writeFile(KERNEL_BLOCK_WAKELOCK,"0");
                removeWakelockCountBlocker = true;

                setHDMIEnable(true);
                setHDCPVersion(RtkHDMIManager2.HDCP_22);
                remountSDCards();
                Log.d(TAG, "finish WakeUpRunnable");
            }else{
                Log.d(TAG,"keep post WakeUpRunnable..");
                mHandler.postDelayed(mWakeupRunnable,500);
            }
        }
    }

    public class LocalBinder extends Binder {
        SuspendService getService() {
            return SuspendService.this;
        }
    }

    @Override
    public IBinder onBind (Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service onCreate");
        mHDMIManager = RtkHDMIManager2.getRtkHDMIManager(this);
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.d(TAG , "service onStartCommand");

        mObserver.startObserving("DEVPATH=/devices/virtual/rtk_pm");

        mUnmountedSDs = new ArrayList<VolumeInfo>();
        mUnmountList = new ArrayList<VolumeInfo>();

        IntentFilter filter;

        // register unmounted broadcast receiver
        mUnmountReceiver = new UnmountReceiver(this);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mUnmountReceiver, filter /*, null, mHandler */);

        /*
        mScreenStateReceiver = new ScreenOnOffReceiver();
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(mScreenStateReceiver,filter);
        */

        /*
        Toast toast = Toast.makeText(
                        this,
                        "RealtekSuspendService start",
                        Toast.LENGTH_LONG);
        toast.show();
        */

        SystemProperties.set(PROP_S3_HANDLE_READY,"1");

        // if Suspend service is killed, it would restart itself again
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service destroy");
        super.onDestroy();
    }

    public long getKernelSuspendContext(){

        try {
            String line = readFile(KERNEL_SUSPEND_CONTEXT);
            Log.d(TAG, "getSuspendContext line "+line);
            return Long.parseLong(line);
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException :" + KERNEL_SUSPEND_CONTEXT);
            return -1;
        }
    }

    public void setSuspendContext(long value){
        mSuspendContext = value;
    }

    public void postWakeUpTask(){
        mHandler.postDelayed(mWakeupRunnable,100);
    }

    public void cancelWakeUpTask(){
        mHandler.removeCallbacks(mWakeupRunnable);
    }

    private void processS3PreHandling() {
        Log.d(TAG, "exec processS3PreHandling");

        mIsSuspendMode = true;


        Log.d(TAG, "before setHDMIEnable");

        // TODO: turn off HDMI and DP
        setHDMIEnable(false);

        Log.d(TAG, "after setHDMIEnable");

        /*
        //backToHome();
        killHistoryActivities();

        // TODO review something maybe
        for(int i=0;i<20;i++){
            if(isAtHomeActivity() == true) { break; }
            SystemClock.sleep(100);
        }
        */

        unmountExtStorages();
        long t1 = SystemClock.elapsedRealtime();
        boolean breakLoop = false;
        while(mUnmountList.size() > 0 && !breakLoop) {
            SystemClock.sleep(500);
            long t2 = SystemClock.elapsedRealtime();
            long diff = t2 - t1;
            if(diff > 5000) {
                breakLoop = true;
            }
        }

        Log.d(TAG, "Exit processS3PreHandling");
        for(int i=0;i<mUnmountList.size();i++) {
            Log.d(TAG, "Failed to unmount:"+mUnmountList.get(i).path);
        }
    }

    private void notifyKernelComplete() {
        Log.d(TAG, "notifyKernelComplete write 1 to "+KERNEL_ANDROID_CTRL);
        writeFile(KERNEL_ANDROID_CTRL,"1");
    }

    private void writeFile(String path, String value) {
        Log.d(TAG, "write "+path+" "+value);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(value);
            bw.close();
        } catch (IOException e) {
            Log.w(TAG, "notifyKernelComplete write failed: " + path);
        }
    }

    private String readFile(String path) {
        try {
            BufferedReader br =
                new BufferedReader(new FileReader(path));
            String line = br.readLine().trim();
            br.close();
            return line;
        } catch (FileNotFoundException e){
            Log.e(TAG, "FileNotFoundException :" + path);
            return "";
        } catch (IOException e) {
            Log.e(TAG, "IOException :" + path);
            return "";
        }
    }

    private void backToHome(){
        Intent homeIntent =  new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        startActivityAsUser(homeIntent, UserHandle.CURRENT);
    }

    private boolean isCurrentHome(
            ComponentName component,
            ActivityInfo homeInfo) {
        if (homeInfo == null) {
            final PackageManager pm = getPackageManager();
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .resolveActivityInfo(pm, 0);
        }
        return homeInfo != null
            && homeInfo.packageName.equals(component.getPackageName())
            && homeInfo.name.equals(component.getClassName());
    }

    private void killHistoryActivities(){

        final int DISPLAY_TASKS = 20;
        final int MAX_TASKS = DISPLAY_TASKS + 1; // allow extra for non-apps
        // 1. get Activity list
        final PackageManager pm = getPackageManager();
        final ActivityManager am =
            (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RecentTaskInfo> recentTasks =
            am.getRecentTasks(MAX_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        int numTasks = recentTasks.size();

        ActivityInfo homeInfo =
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).
            resolveActivityInfo(pm, 0);

        ArrayList<TaskDescription> tasks = new ArrayList<TaskDescription>();
        final int first = 0;
        for (int i = first, index = 0; i < numTasks && (index < MAX_TASKS); ++i) {
            final ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);
            Intent intent = new Intent(recentInfo.baseIntent);
            if (recentInfo.origActivity != null) {
                intent.setComponent(recentInfo.origActivity);
            }

            // Don't load the current home activity.
            if (isCurrentHome(intent.getComponent(), homeInfo)) {
                continue;
            }

            boolean isMyself =
                intent.getComponent().getPackageName().equals(getPackageName());

            // Don't load ourselves
            if (isMyself) {
                continue;
            }

            TaskDescription item = createTaskDescription(recentInfo.id,
                            recentInfo.persistentId, recentInfo.baseIntent,
                            recentInfo.origActivity, recentInfo.description);

            if(item!=null){
                tasks.add(item);
                Log.d(TAG,"TaskItem:"+item);
                ++index;
            }
        }

        // loop tasks and kill them.
        for(int i=0;i<tasks.size();i++){
            TaskDescription ad = tasks.get(i);
            try{
                boolean rst =
                    ActivityManager.getService().removeTask(ad.persistentTaskId);
                Log.d(TAG, "remove task rst:"+rst);
            } catch(RemoteException e) {
                Log.w(TAG, "failed to remove task "+ad.persistentTaskId);
            }
        }
    }

    private TaskDescription createTaskDescription(
            int taskId,
            int persistentTaskId,
            Intent baseIntent,
            ComponentName origActivity,
            CharSequence description) {

        Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        final PackageManager pm = getPackageManager();
        intent.setFlags(
                (intent.getFlags()&~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            final ActivityInfo info = resolveInfo.activityInfo;
            final String title = info.loadLabel(pm).toString();

            if (title != null && title.length() > 0) {
                if (DEBUG) Log.v(TAG, "creating activity desc for id="
                        + persistentTaskId + ", label=" + title);

                TaskDescription item = new TaskDescription(taskId,
                        persistentTaskId, resolveInfo, baseIntent, info.packageName,
                        description);
                return item;
            } else {
                if (DEBUG) Log.v(TAG, "SKIPPING item " + persistentTaskId);
            }
        }
        return null;
    }

    public void remountSDCards(){

        //Log.d(TAG, "remountSDCards");
        StorageManager m =
            (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        for(VolumeInfo v:mUnmountedSDs){
            Log.d(TAG, "remount : "+v.path+" id :"+v.id);
            try {
                m.mount(v.id);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "SDCard "+v.path+" not exists");
            }
            Log.d(TAG, "remount complete");
        }

        // clear the list
        mUnmountedSDs.clear();

    }

    public void setHDCPVersion(int version) {
        if(mHDMIManager != null) {
            Log.d(TAG, "setHDCPVersion "+version);
            mHDMIManager.setHDCPVersion(version);
        }
    }

    public void setHDMIEnable(boolean enable) {
        if(mHDMIManager != null) {
            Log.d(TAG, "setHDMIEnable "+enable);
            mHDMIManager.setHDMIEnable(enable);
        }
    }

    public void getSinkCapability() {
        if(mHDMIManager != null) {
            Log.d(TAG, "getSinkCapability");
            mHDMIManager.getSinkCapability();
        }
    }

    public static long getCurrentThreadID(){
        long threadId = Thread.currentThread().getId();
        return threadId;
    }

    private boolean isAtHomeActivity(){

        final PackageManager pm = getPackageManager();
        ActivityInfo homeInfo =
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).
            resolveActivityInfo(pm, 0);

        String homePkgName = homeInfo.packageName;
        String homeClassName = homeInfo.name;

        ActivityManager am =
            (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentInfo = taskInfo.get(0).topActivity;
        Log.d(TAG, "homeInfo pkgName:"+homePkgName+ " ClassName:"+homeClassName);
        Log.d(TAG, "CURRENT Activity :: Package Name :"+
                componentInfo.getPackageName()+
                "ClassName:"+
                taskInfo.get(0).topActivity.getClassName());

        if( homePkgName.equals(componentInfo.getPackageName())
          && homeClassName.equals(taskInfo.get(0).topActivity.getClassName())){
            return true;
        }else{
            return true;
        }
    }

    private void unmountExtStorages(){

        Log.d(TAG, "unmountExtStorages");

        mUnmountList.clear();

        StorageManager manager =
            (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        // try to get VolumeInfo
        List<VolumeInfo> volumeInfoList = manager.getVolumes();

        for(VolumeInfo v : volumeInfoList){
            DiskInfo d = v.disk;
            String path = v.path;
            if(d!=null && path != null){
                if(d.isUsb() || d.isSd() ) {
                    Log.d(TAG, "Construct list - Path:"+v.path+
                            " SD:"+d.isSd()+
                            " USB:"+d.isUsb());

                    mUnmountList.add(v);

                }
            }
        }

        UnmountTaskParam param = new UnmountTaskParam();
        param.mList = mUnmountList;
        new UnmountTask().execute(param,param,param);
    }

    // helper class to keep task description
    class TaskDescription {
        final ResolveInfo resolveInfo;
        final int taskId; // application task id for curating apps
        final int persistentTaskId; // persistent id
        final Intent intent; // launch intent for application
        final String packageName; // used to override animations (see onClick())
        final CharSequence description;

        public TaskDescription(int _taskId, int _persistentTaskId,
            ResolveInfo _resolveInfo, Intent _intent,
            String _packageName, CharSequence _description) {

            resolveInfo = _resolveInfo;
            intent = _intent;
            taskId = _taskId;
            persistentTaskId = _persistentTaskId;

            description = _description;
            packageName = _packageName;
        }

        public String toString(){
            return "[TaskDescription]:"+packageName;
        }
    }

    public void handleUnmountedEvent(String path) {
        if(!mIsSuspendMode) {
            return;
        }

        int index = -1;
        for(int i=0;i<mUnmountList.size();i++) {
            VolumeInfo v = mUnmountList.get(i);
            if(v.path != null && v.path.equals(path) ) {
                index = i;
                break;
            }
        }

        if(index != -1) {
            Log.d(TAG, "Unmounted - PATH: "+path);
            mUnmountList.remove(index);
        }
    }

    class UnmountTaskParam {
        List<VolumeInfo> mList;
    }

    class UnmountTask extends AsyncTask<UnmountTaskParam, Void, Boolean> {
        @Override
        protected Boolean doInBackground(UnmountTaskParam... params){
            List<VolumeInfo> list = params[0].mList;

            if(mIsSuspendMode == false || list.size() <= 0){
                return true;
            }

            StorageManager manager =
                (StorageManager) getSystemService(Context.STORAGE_SERVICE);

            for(VolumeInfo v : list){
                DiskInfo d = v.disk;
                String path = v.path;
                if(d!=null && path != null){
                    if(d.isUsb() || d.isSd() ) {
                        Log.d(TAG, "Unmount Path:"+v.path+
                                " SD:"+d.isSd()+
                                " USB:"+d.isUsb());

                        manager.unmount(v.id);
                        handleUnmountedEvent(v.path);

                        if(d.isSd()) {
                            if(mIsSuspendMode) {
                                Log.d(TAG, "Save SD card in list "+path);
                                mUnmountedSDs.add(v);
                            }
                        }
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.d(TAG, "UnmountTask onPostExecute");
        }
    }
}
