/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rtk.mediabrowser.recommend;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.app.recommendation.ContentRecommendation;
import android.support.media.tv.Channel;
import android.support.media.tv.ChannelLogoUtils;
import android.support.media.tv.PreviewProgram;
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import com.realtek.Utils.UseRtMediaPlayer;
import com.realtek.bitmapfun.util.ImageCache;
import com.realtek.bitmapfun.util.ImageFetcher;
import com.realtek.bitmapfun.util.ImageWorker;
import com.rtk.mediabrowser.MediaBrowser;
import com.rtk.mediabrowser.R;
import com.rtk.mediabrowser.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.realtek.Utils.Const.COVER_PATH;
import static com.rtk.mediabrowser.AbstractFBViewActivity.MEDIA_BROWSER_USE_RT_MEDIA_PLAYER;
import static com.rtk.mediabrowser.GenericContentViewFragment.IMAGE_CACHE_DIR;

/*
 * This class builds up to MAX_RECOMMENDATIONS of ContentRecommendations and defines what happens
 * when they're selected from Recommendations section on the Home screen by creating an Intent.
 */
public class UpdateRecommendService extends IntentService {
    private static final String TAG = "UpdateRecommendService";
    private static final boolean DEBUG = true;
    private static final int MAX_RECOMMENDATIONS = 3;

    private NotificationManager mNotifManager;
    private ImageWorker mImageWorker;

    public UpdateRecommendService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mNotifManager == null) {
            mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        mImageWorker = new ImageFetcher(getApplication(), null, 300, 200);
        mImageWorker.setImageCache(ImageCache.findOrCreateCache(getApplication(), IMAGE_CACHE_DIR));
        mImageWorker.setImageFadeIn(false);
    }

    private static final boolean DISABLE_RECOMMENDATION = true;
    @Override
    protected void onHandleIntent(Intent intent) {
        log("onHandleIntent");
        // Generate recommendations, but only if recommendations are enabled
        if (DISABLE_RECOMMENDATION) {
            log("Recommendations disabled");
            mNotifManager.cancelAll();
            return;
        }

        if(isLeanbackLauncherOnTop())
            notifyRecommendation();
    }

    private boolean isLeanbackLauncherOnTop() {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        Log.d(TAG, "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName());
        return taskInfo.get(0).topActivity.getClassName().equals("com.google.android.tvlauncher.MainActivity");
    }

    @android.annotation.TargetApi(26)
    private void notifyRecommendation() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifyRecommendationV26();
        } else {
            notifyRecommendationOld();
        }
    }

    private long createChannel(){
        Log.d(TAG, "Creating channel: " + getString(R.string.app_name));
        Uri logoUri = Uri.parse("android.resource://"+this.getPackageName()+"/"+R.drawable.ic_launcher);
        Channel.Builder builder = new Channel.Builder();
        // Every channel you create must have the type `TYPE_PREVIEW`
        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(getString(R.string.app_name))
                .setBrowsable(true)
                .setAppLinkIntentUri(Uri.parse(URI_VIEW));
        Channel channel = builder.build();
        Uri channelUri = this.getContentResolver().insert(
                TvContractCompat.Channels.CONTENT_URI, channel.toContentValues());
        long channelId = 0;
        channelId = ContentUris.parseId(channelUri);
        ChannelLogoUtils.storeChannelLogo(this, channelId, logoUri);
        return channelId;
    }

    private static final String PREF_NAME = "RECOMMENDATION";
    private static final String PREF_KEY = "CHANNEL_ID";
    private static final String SCHEMA_URI_PREFIX = "rtkrecommend://app/";
    public static final String BROWSE = "browse";
    private static final String URI_VIEW = SCHEMA_URI_PREFIX + BROWSE;
    @android.support.annotation.RequiresApi(api = 26)
    private void notifyRecommendationV26() {
        log("notifyRecommendationV26");
        //get channel id
        SharedPreferences pref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long channelId = pref.getLong(PREF_KEY, 0);

        Channel channel = null;
        if(channelId!=0) {
//            getContentResolver().delete(TvContractCompat.buildChannelUri(channelId), null, null);
            channel = getChannelById(channelId);
        }

        if(channel == null){
            channelId = createChannel();
            TvContractCompat.requestChannelBrowsable(this, channelId);
            pref.edit().putLong(PREF_KEY, channelId).apply();
            channel = getChannelById(channelId);
        }

        if (channel!=null && channel.isBrowsable()) {
            //delete old programs
            getContentResolver().delete(TvContractCompat.PreviewPrograms.CONTENT_URI, "channel_id=?", new String[]{String.valueOf(channelId)});

            //update channel's programs
            try(Cursor cursor = getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    null, // projection
                    null, // selection
                    null, // selection clause
                    "RANDOM() LIMIT " + MAX_RECOMMENDATIONS // sort order
            )) {
                if (cursor != null && cursor.moveToNext()) {
                    int index = 0;
                    do {
                        int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID));
                        String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
                        String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                        String coverPath = cursor.getString(cursor.getColumnIndex(COVER_PATH));
                        log("prepare program " + filePath);

                        if (filePath == null) continue;

                        Uri playVideoUri = Util.getUri(this, filePath);
                        grantUri(playVideoUri);
                        PreviewProgram.Builder programBuilder = new PreviewProgram.Builder();
                        programBuilder.setChannelId(channelId)
                                .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                                .setTitle(title)
//                                .setPreviewVideoUri(playVideoUri) //not worked now.
                                .setIntentUri(playVideoUri);
//                        Bitmap bitmap = mImageWorker.loadRecommendationImage(id, title, filePath, coverPath);
//                        Uri posterUri = getUriFromBitmap(bitmap, index);
//                        if (posterUri != null) {
//                            grantUri(Util.getUri(this, posterUri.toString()));
//                            programBuilder.setPosterArtUri(posterUri);
//                        } else {
                            Uri logoUri = Uri.parse("android.resource://" + this.getPackageName() + "/" + R.drawable.ic_launcher);
                            programBuilder.setPosterArtUri(logoUri);
//                        }
                        getContentResolver().insert(TvContractCompat.PreviewPrograms.CONTENT_URI,
                                programBuilder.build().toContentValues());
                        index++;
                    } while (cursor.moveToNext());
                } else {
                    log("no cursor data");
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        } else if(channel == null){
            log("channel is null");
        } else if(!channel.isBrowsable()){
            log("channel is not browsable");
        }
    }

    private void grantUri(Uri uri){
        log("grantUri"+uri);
        String[] grantPackages = new String[]{
                "com.google.android.tvlauncher"
        };
        for(String pkg : grantPackages) {
            grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private Uri getUriFromBitmap(Bitmap bitmap, int index) {
        if(bitmap==null) return null;
        File f = new File(getExternalCacheDir(), TAG+Integer.toString(index)+".png");
        try {
//            f.createNewFile();
            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            log("write bitmap "+f.toString());
            return Uri.parse(f.toString());
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private Channel getChannelById(long channelId) {
        Channel channel = null;
        try (Cursor cursor =
                     getContentResolver()
                             .query(
                                     TvContractCompat.buildChannelUri(channelId),
                                     null,
                                     null,
                                     null,
                                     null)) {
            if (cursor != null && cursor.moveToNext()) {
                channel = Channel.fromCursor(cursor);
            }
        }
        return channel;
    }

    private Intent buildMediaBrowserIntent() {
        Intent intent= new Intent(this,MediaBrowser.class);
        intent.setAction(Intent.ACTION_MAIN);
        return intent;
    }

    private void notifyRecommendationOld() {

        Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null, // projection
                null, // selection
                null, // selection clause
                "RANDOM() LIMIT " + MAX_RECOMMENDATIONS // sort order
        );

        if (cursor != null && cursor.moveToNext()) {
//            try {
                do {

                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.VideoColumns._ID));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
                    String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                    String coverPath = cursor.getString(cursor.getColumnIndex(COVER_PATH));

                    if (title != null) {
                        ContentRecommendation.Builder builder = new ContentRecommendation.Builder()
                            .setBadgeIcon(R.drawable.icon_allfile_v);
                        builder.setIdTag("Video" + id)
                                .setTitle(title)
                                .setText("Popular videos")
                                .setContentIntentData(ContentRecommendation.INTENT_TYPE_ACTIVITY,
                                        buildPendingIntent(filePath, id), 0, null);

//                    Bitmap bitmap = Glide.with(getApplication())
//                            .using(mModelLoader, Bitmap.class)
//                            .load(filePath)
//                            .load(Uri.fromFile( new File( filePath ) ))
//                            .asBitmap()
//                            .diskCacheStrategy(DiskCacheStrategy.RESULT)
//                            .into(300, 200) // Only use for synchronous .get()
//                            .get();
                        Bitmap bitmap = mImageWorker.loadRecommendationImage(id, title, filePath, coverPath);
                        if (bitmap != null) {
                            builder.setContentImage(bitmap);

                            // Create an object holding all the information used to recommend the content.
                            ContentRecommendation rec = builder.build();
                            Notification notification = rec.getNotificationObject(getApplicationContext());

                            log("Recommending video " + title);

                            // Recommend the content by publishing the notification.
                            mNotifManager.notify(id, notification);
                        }
                    }
                } while (cursor.moveToNext());
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            } finally {
                cursor.close();
//            }
        } else {
            log("no cursor data");
        }

    }

    private void log(String s) {
        if(DEBUG)
            Log.d(TAG, s);
    }

    private Intent buildPendingIntent(String videoPath, int id) {
        final Intent intentGallery = new Intent();
        Uri uri = Util.getUri(this, videoPath);
        ComponentName comp = new ComponentName("com.android.gallery3d", "com.android.gallery3d.app.MovieActivity");
        intentGallery.setComponent(comp);
        intentGallery.putExtra("SourceFrom","Local");
        intentGallery.putExtra(MEDIA_BROWSER_USE_RT_MEDIA_PLAYER, UseRtMediaPlayer.getInstance(this).getUseRtPlayer());
        intentGallery.setAction(Intent.ACTION_VIEW);
        intentGallery.setData(uri);
//        intentGallery.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK| Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intentGallery;
    }
}
