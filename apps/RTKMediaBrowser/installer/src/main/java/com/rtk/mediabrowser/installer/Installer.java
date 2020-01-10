package com.rtk.mediabrowser.installer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;

public class Installer extends Activity{

    private static final String TAG = "MediaBrowserInstaller";
    private static final int REQUEST_READ_STORAGE = 0;

    private static Uri mUri;

    private void requestPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "READ_EXTERNAL_STORAGE permission not granted, asking for it...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_STORAGE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if(intent == null) return;
        String filePath = intent.getStringExtra("EXTRA_FILE_PATH");
        mUri = Uri.fromFile(new File(filePath));
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions();
        else {
            testInstallPackage(mUri);
        }
    }

    private void testInstallPackage(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        File f = new File(uri.getPath());
        Uri apkUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", f);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                |Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean isGranted = false;
        for (int i = 0; i < grantResults.length; i++)
            if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                isGranted = true;
        if (isGranted) {
            testInstallPackage(mUri);
        }
        else
            Log.w(TAG, "READ_EXTERNAL_STORAGE permission not granted.");
    }
}
