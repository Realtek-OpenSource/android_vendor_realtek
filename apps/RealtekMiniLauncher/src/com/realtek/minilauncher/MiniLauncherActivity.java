package com.realtek.minilauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.realtek.minilauncher.adapter.AppAdapter;
import com.realtek.minilauncher.adapter.ViewHolder;
import com.realtek.minilauncher.widget.RtkFileBrowserGridView;


public class MiniLauncherActivity extends Activity {
    private String TAG = "MiniLauncherActivity";
    private RtkFileBrowserGridView mGridView = null;
    private AppAdapter mAdapter;
    private final BroadcastReceiver mPackageChangeReceiver = new PackageReceiver();
    private Context mContext;
    private static int initpos = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGridView = findViewById(R.id.gridview);
        mAdapter = new AppAdapter(this);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemSelectedListener(new AbstractViewItemSelectedListener());//Handle of item is selected
        mGridView.setOnItemClickListener(new AbstractViewItemClickListener());//Handle of item is clicked

    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        mContext = getApplicationContext();
        mGridView.setFocusable(true);
        mGridView.requestFocus();
        mGridView.setSelected(true);
        mGridView.setSelection(initpos);
        mGridView.setClickable(true);
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pkgFilter.addDataScheme("package");
        mContext.registerReceiver(mPackageChangeReceiver, pkgFilter);
        refreshGridView();
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        mContext.unregisterReceiver(mPackageChangeReceiver);
    }
    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }
    private class AbstractViewItemSelectedListener implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position , long id) {
            //Log.d(TAG,"onItemSelected position:"+position+" id:"+id);
            initpos = position;
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //Log.d(TAG,"onNothingSelected:");
        }
    }

    private class AbstractViewItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id)
        {
            ViewHolder holder = (ViewHolder) v.getTag();
            ApplicationInfo app = holder.app;
            String activityname = holder.activityName;
            launchInstalledActivity(app.packageName, activityname, mContext);

        }
    }
    protected void launchInstalledActivity(String packageName, String activityName, Context context){
        final PackageManager packageManager = context.getPackageManager();
        Intent intent=packageManager.getLaunchIntentForPackage(packageName);
        Log.d(TAG,"intent is null packageName="+packageName+" activityName="+activityName);
        if (intent == null) {
            Log.d(TAG,"intent is null");
            Intent launchTvSettingsIntent = new Intent(); 
            launchTvSettingsIntent.setComponent(new ComponentName(packageName, activityName));
            launchTvSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchTvSettingsIntent);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    public class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    || Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                Log.d(TAG,"receive intent action = "+action);
                refreshGridView();
            }
        }
    }
    public void refreshGridView() {
        Log.d(TAG,"refreshGridView");
        mAdapter.loadAllApps(mContext);
        mGridView.setSelected(true);
        mAdapter.notifyDataSetChanged();
        mGridView.requestFocus();
        mGridView.setSelection(initpos);
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //Log.e("Launcher", "get dispatchKeyEvent");
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
