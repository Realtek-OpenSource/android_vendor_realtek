package com.realtek.minilauncher.adapter;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.realtek.minilauncher.widget.MarqueeTextView;
import com.realtek.minilauncher.widget.RtkFileBrowserGridView;
import com.realtek.minilauncher.provider.InstalledAppInfoProvider;
import com.realtek.minilauncher.R;

public class AppAdapter extends BaseAdapter {

    private static final String TAG = "AppAdapter";

    private Context mContext;
    LayoutInflater mInflater;
    List<ResolveInfo> mData;
    private PackageManager mPackageManager;

    public AppAdapter(Context c) {
        mContext = c;
        mPackageManager = mContext.getPackageManager();
        loadAllApps(c);
        mInflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void loadAllApps(Context c) {
        Log.d(TAG,"loadAllApps");
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mData = mPackageManager.queryIntentActivities(mainIntent, 0);
        /*if(mData!=null) {
            Log.d(TAG,"app num = "+mData.size());
            for(int i=0;i<mData.size();i++){
                Log.d(TAG, "App:"+mData.get(i).activityInfo.name);
            }
        }*/
        final Intent mainTvIntent = new Intent(Intent.ACTION_MAIN, null);
        mainTvIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> mTvData = mPackageManager.queryIntentActivities(mainTvIntent, 0);
        if(mTvData!=null) {
            //Log.d(TAG,"tv app num = "+mTvData.size());
            for(int i=0;i<mTvData.size();i++){
                boolean isfound = false;
                //Log.d(TAG, "tv App:"+mTvData.get(i).activityInfo.name);
                for(int j=0;j<mData.size();j++) {
                    if (mTvData.get(i).activityInfo.name.equals(mData.get(j).activityInfo.name)) {
                        isfound = true;
                        break;
                    }
                }
                if (!isfound) {
                    mData.add(mTvData.get(i));
                }
            }
        }
        final Intent TvSettingsIntent = new Intent(Intent.ACTION_MAIN, null);
        TvSettingsIntent.addCategory("android.intent.category.LEANBACK_SETTINGS");
        List<ResolveInfo> TvSettingsData = mPackageManager.queryIntentActivities(TvSettingsIntent, 0);
        if(TvSettingsData!=null) {
            //Log.d(TAG,"settings app num = "+TvSettingsData.size());
            for(int i=0;i<TvSettingsData.size();i++){
                //Log.d(TAG, "settings App:"+TvSettingsData.get(i).activityInfo.name);
                if(TvSettingsData.get(i).activityInfo.name
                    .equals("com.android.tv.settings.MainSettings")) {
                    mData.add(TvSettingsData.get(i));
                }
            }
        }
        Collections.sort(mData,new ResolveInfo.DisplayNameComparator(mPackageManager));
        /*if(mData!=null) {
            Log.d(TAG,"end app num = "+mData.size());
            for(int i=0;i<mData.size();i++){
                Log.d(TAG, "App:"+mData.get(i).activityInfo.name);
            }
        }*/
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int arg0) {
        return mData.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        //Log.d(TAG,"getView position="+position);
        if (convertView == null) {
            //Log.d(TAG,"getView convertView == null");
            // if it's not recycled, initialize some attributes
            convertView = mInflater.inflate(R.layout.item_of_gridview, null);
            holder = new ViewHolder();
            holder.imageView = (ImageView)(convertView.findViewById(R.id.grid_img));
            holder.FocusView = (ImageView)(convertView.findViewById(R.id.focus_img));
            holder.tv = (MarqueeTextView)(convertView.findViewById(R.id.grid_text));
            holder.tv.setSelectedChangeStyle(R.style.THUSelectedText,R.style.THUUnselectedText);
            holder.FocusView.setImageResource(R.drawable.selector_grid_focused_bar);

            convertView.setTag(holder);
        } else {
            //Log.d(TAG,"getView convertView != null");
            holder = (ViewHolder)convertView.getTag();
        }
        if(((RtkFileBrowserGridView)parent).isOnMeasure) {
            return convertView;
        }
        RefreshGridView(position, holder);
        return convertView;
    }
    public void RefreshGridView(int position, ViewHolder holder) {
        //Log.d(TAG,"refreshGridView position="+position);
        ApplicationInfo app = mData.get(position).activityInfo.applicationInfo;
        String label= (String)mPackageManager.getApplicationLabel(app);
        holder.tv.setText(label);
        // Icon
        Drawable icon=InstalledAppInfoProvider.getFullResIcon(app.packageName, app.icon, mContext);
        holder.imageView.setImageDrawable(icon);

        holder.app = app;
        String activityname = mData.get(position).activityInfo.name;
        holder.activityName = activityname;
    }
}