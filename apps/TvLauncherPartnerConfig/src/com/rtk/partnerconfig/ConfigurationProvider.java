package com.rtk.partnerconfig;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.util.Log;
import android.database.MatrixCursor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConfigurationProvider extends ContentProvider {
    private static final boolean DEBUG = true;
    private static final boolean DISABLE_WIDGET = true;
    private static final String TAG = "ConfigurationProvider";
    private static final String CONFIG_AUTHORITY = "tvlauncher.config";
    private static final String APPRECS_AUTHORITY = "tvlauncher.apprecs";
    private static final String WIDGET_AUTHORITY = "tvlauncher.widget";
    private static final String MIC_STATUS_AUTHORITY = "tvlauncher.mic";

    private static final String CONFIGURATION_DATA = "configuration";
    private static final String APP_RECOMMENDATION_DATA ="app_recommendations";
    private static final String MIC_STATUS_DATA ="farfield_mic_status";
    private static final String WIDGET_DATA = "widget";

    private static final int MATCH_CONFIGURATION = 1;
    private static final int MATCH_APP_RECOMMENDATIONS = 2;
    private static final int MATCH_WIDGET = 3;
    private static final int MATCH_MIC_STATUS = 4;
    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(
                CONFIG_AUTHORITY, CONFIGURATION_DATA, MATCH_CONFIGURATION);
        sUriMatcher.addURI(
                APPRECS_AUTHORITY, APP_RECOMMENDATION_DATA, MATCH_APP_RECOMMENDATIONS);
        if(!DISABLE_WIDGET) {
            sUriMatcher.addURI(
                    WIDGET_AUTHORITY, WIDGET_DATA, MATCH_WIDGET);
        }
        sUriMatcher.addURI(
                MIC_STATUS_AUTHORITY, MIC_STATUS_DATA, MATCH_MIC_STATUS);

    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws
            FileNotFoundException {
        log("openFile");
        final InputStream stream;
        PipeDataWriter writer;
        switch (sUriMatcher.match(uri)) {
            case MATCH_CONFIGURATION:
                log("MATCH_CONFIGURATION");
                // In this example, the configuration file is static and
                // resides in
                // the res/raw folder of the app.
                stream = getContext().getResources().openRawResource(
                        R.raw.configuration);
                break;
            case MATCH_APP_RECOMMENDATIONS:
                log("MATCH_APP_RECOMMENDATIONS");
                stream = getContext().getResources().openRawResource(
                        R.raw.app_recommendations);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        writer = new PipeDataWriter() {
            @Override
            public void writeDataToPipe(@NonNull ParcelFileDescriptor output,
                                        @NonNull Uri uri, @NonNull String mimeType,
                                        @Nullable Bundle opts, @Nullable Object args) {
                try (FileOutputStream out = new FileOutputStream(
                        output.getFileDescriptor())) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send file " + uri, e);
                }
            }
        };
        return openPipeHelper(uri, "text/xml", null, null, writer);
    }

    private void log(String s) {
        if(DEBUG)
            Log.d(TAG, s);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch (sUriMatcher.match(uri)) {
            case MATCH_WIDGET:
                log("query MATCH_WIDGET");
                if(DISABLE_WIDGET) {
                   return null;
                }
//                // In this example , the action uri for the widget points to an
//                // activity in
//                // the customization app but the uri can point to separate app too.
                Intent intent = new Intent(getContext(), MainActivity.class);
                String action = intent.toUri(Intent.URI_INTENT_SCHEME);
                String title = getContext().getString(R.string.widget_name);
//                // Uri to icon can point to an image on a web server or to an image
//                // resource
//                // in the customization app.
                Uri iconPath = Uri.parse("android.resource://com.rtk.partnerconfig/" + R.mipmap.ic_launcher);
                String icon = iconPath.toString();//"http://url_to_image";
                MatrixCursor cursor = new MatrixCursor(new String[]{"icon",
                        "title",
                        "action"});
                cursor.addRow(new String[]{icon, title, action});
                return cursor;
            case MATCH_MIC_STATUS:
                log("query MATCH_MIC_STATUS");
                MatrixCursor cursor2 = new MatrixCursor(new String[]{"mic_status"});
                /*
                    NOT_SUPPORTED (1): No farfield mic exists.
                    MIC_OFF (2): Farfield mic(s) exist, but none are on.
                    MIC_ON (3): At least one mic exists and is currently listening.
                 */
                int ret = SystemProperties.getInt("persist.rtk.hotword", 1);
                log("return "+ret);
                cursor2.addRow(new Object[]{ret});
                return cursor2;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }
}