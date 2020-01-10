package com.mediacodec;

import java.io.File;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;

public class DemoActivity extends Activity implements OnClickListener {
    public static final String TAG = "DemoActivity";
    public static final int MAX = 6;
    static final public String sRoot = "/storage"; 
    static final public String sParent = "..";
    private Button btnDemo1 = null;
    private Button btnDemo2 = null;
    private Button btnDemo3 = null;
    private Button btnDemo4 = null;
    private Button btnDemo5 = null;
    private EditText detText = null;
    private EditText detText2 = null;
    private AutoCompleteTextView listText[] = new AutoCompleteTextView[MAX];
    private ArrayList<String> playList = null;
    private SharedPreferences playListRecord = null;
    Spinner mSpin = null;
    private ArrayList<File> fileList = new ArrayList<>();
    private ArrayList<String> mFileNameList = new ArrayList<>();
    private ArrayList<String> rootPath = new ArrayList<>();
    public String selectedfilepath = null;
    private final int PERMISSION_CONTACTS = 0;

    private ArrayAdapter<String> SourceList;
    private int sourceflag = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.demo);

        btnDemo1 = (Button) findViewById(R.id.btnDemo1);
        btnDemo2 = (Button) findViewById(R.id.btnDemo2);
        btnDemo3 = (Button) findViewById(R.id.btnDemo3);
        btnDemo4 = (Button) findViewById(R.id.btnDemo4);
        detText = (EditText) findViewById(R.id.edtText);
        detText2 = (EditText) findViewById(R.id.edtText2);
        
        listText[0] = (AutoCompleteTextView) findViewById(R.id.contText1);
        listText[1] = (AutoCompleteTextView) findViewById(R.id.contText2);
        listText[2] = (AutoCompleteTextView) findViewById(R.id.contText3);
        listText[3] = (AutoCompleteTextView) findViewById(R.id.contText4);
        listText[4] = (AutoCompleteTextView) findViewById(R.id.contText5);
        listText[5] = (AutoCompleteTextView) findViewById(R.id.contText6);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, PERMISSION_CONTACTS);
        }
        else {
            ArrayAdapter<String> dataList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mFileNameList);
            for(AutoCompleteTextView actv : listText){
                actv.setThreshold(0);
                actv.setAdapter(dataList);
                actv.setOnClickListener(this);
            }
        }
        
        playList = new ArrayList<String>();
        playListRecord = getSharedPreferences("RealtekPIP", 0);
        String temp = playListRecord.getString("PLAYLIST", "");
        if(temp != "")
        {
            String[] playlists = new String[MAX];
            playlists = temp.split(",");
            for(int i=0;i<MAX; i++)
            {
                if(playlists[i] != "")
                {
                    Log.d(TAG, "Set Source " + i + " Path " + playlists[i]);
                    listText[i].setText(playlists[i]);
                }
            }
        }
        

        btnDemo1.setOnClickListener(this);
        btnDemo2.setOnClickListener(this);
        btnDemo3.setOnClickListener(this);
        btnDemo4.setOnClickListener(this);
        getUSBPath(rootPath);
    }

    private void getAllVideoFileList(){
        getUSBPath(rootPath);
        fileList.clear();
	for (String path : rootPath) {
	    if (path == null) return;
            listAllFiles(path, fileList);
	}
        //Collections.sort(fileList, new FileTypeComparator());
        mFileNameList.clear();
        for(File f : fileList){
            //String mimeType = getMimeType(f.getPath());
//            log(String.format("fUri=%s, mime=%s", f.getPath(), mimeType));
            //if(mimeType!=null && mimeType.startsWith("video/")) 
            {
                log("f="+f.getPath());
                mFileNameList.add(f.getPath());
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CONTACTS:
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ArrayAdapter<String> dataList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mFileNameList);
                    for(AutoCompleteTextView actv : listText){
                        actv.setThreshold(0);
                        actv.setAdapter(dataList);
                        actv.setOnClickListener(this);
                    }
                } else {
                    DemoActivity.this.finish();
                }
                break;
            default:
                break;
        }
    }

    private void getUSBPath(ArrayList<String> rootPath) {
	rootPath.clear();
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList(this);
        for(StorageUtils.StorageInfo info : storageList){
	    Log.d("StorageInfo",String.format("removable=%s, path=%s", info.removable, info.path));
            if(info.removable && !info.isSdCard) {
                rootPath.add(info.path);
            }
        }
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = UrlToExtensionUtils.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public void listAllFiles(String directoryName, ArrayList<File> files) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listAllFiles(file.getAbsolutePath(), files);
            }
        }
    }

    private class FileTypeComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            String s1 = f1.getName();
            String s2 = f2.getName();
            int s1Idx = s1.indexOf(".");
            int s2Idx = s2.indexOf(".");
            if(s1Idx<0) return 1;
            if(s2Idx<0) return -1;
            return s1.substring(s1Idx).compareTo(s2.substring(s2Idx));
        }
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

//    @Override
//    protected void onResume() {
//        // TODO Auto-generated method stub
//        super.onResume();
//        Intent intent = new Intent();
//        intent.setClass(DemoActivity.this, DecodeActivity.class);
//        intent.putExtra("DATA_INT", 2);
//        intent.putExtra("LIMIT_INT", 1);
//        intent.putExtra("LIMIT_AUD_INT", 1);
//        intent.putExtra("REPEAT", 1);        
//        startActivity(intent);
//        DemoActivity.this.finish();
//    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        try{
            Thread.sleep(100);
        }catch(InterruptedException e){}      
    }
	
    	private void openCurDir(final AutoCompleteTextView auto,String curPath,final String root) {
		final List<Map<String,Object>> listItem = new ArrayList<Map<String,Object>> ();
		final List<Map<String,Object>> lfolders = new ArrayList<Map<String,Object>> ();
		final List<Map<String,Object>> lfiles = new ArrayList<Map<String,Object>> ();
	
	    	if (curPath.equals(root)) {
	        	listItem.clear();
			String[] arrrootPath = new String[rootPath.size()];
			for (int i=0; i<rootPath.size(); i++) {
				arrrootPath[i] = rootPath.get(i);
				String rootPathName = arrrootPath[i].substring(root.length()+1,arrrootPath[i].length());
				Map map = new HashMap();	
				map.put("name", rootPathName);
				map.put("image", R.drawable.filedialog_root);
				map.put("path", arrrootPath[i]);
				map.put("isDire", true);
				listItem.add(map);		
			}
	    	}else {
			File f = new File(curPath);
			File[] file = f.listFiles();
			Map map1 = new HashMap();
			map1.put("name", "..");
			map1.put("image", R.drawable.filedialog_folder_up);
			map1.put("path", f.getParent());
			map1.put("isDire", true);
			listItem.add(map1);	
			if(file != null) {
		    		for (int i = 0;i < file.length;i++){
					if (file[i].isDirectory()){
			    			Map map = new HashMap();
			    			map.put("name", file[i].getName());
			    			map.put("image", R.drawable.filedialog_folder);
			    			map.put("path", file[i].getPath());
			    			map.put("isDire", file[i].isDirectory());
			    			lfolders.add(map);
					}else if (file[i].isFile()){
			    			//String mimeType = getMimeType(file[i].getPath());
			    			//if(mimeType!=null && mimeType.startsWith("video/")) 
                            {
			        			Map map = new HashMap();
							map.put("name", file[i].getName());
							map.put("image", R.drawable.filedialog_video);
							map.put("path", file[i].getPath());
							map.put("isDire", file[i].isDirectory());
                					lfiles.add(map);		
			    			}
					}
		    		}
		    		listItem.addAll(lfolders);
		    		listItem.addAll(lfiles);
	    		}
		}
	    	SimpleAdapter adapter = new SimpleAdapter(this, listItem, R.layout.filedialog,new String[]{"name","image"},
							new int[]{R.id.filedialogitem_name,R.id.filedialogitem_img});
	    	final AlertDialog.Builder builder = new Builder(DemoActivity.this);
            	builder.setTitle("Add Video Path");
            	builder.setIcon(R.drawable.ic_launcher);
	    	builder.setAdapter(adapter, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
		    	// TODO Auto-generated method stub
		    		if ((Boolean) listItem.get(which).get("isDire")) {
	                		openCurDir(auto,(String)listItem.get(which).get("path"),root);
		    		}else {
					auto.setText((String)listItem.get(which).get("path"));
		    		}
		    	}
	        });
	    	builder.show();	
    	}
    
    @Override
    public void onClick(View v) {
        if(v instanceof AutoCompleteTextView){
            AutoCompleteTextView actv = (AutoCompleteTextView) v;
			switch(sourceflag) {
				case 0:
					if (rootPath.size() == 0) {
	    					Toast.makeText(DemoActivity.this, "No U-disk available!", Toast.LENGTH_SHORT).show();
					} else {
						openCurDir(actv, sRoot, sRoot);
					}
					break;
				case 1:
					break;
				case 2:
					actv.setText("SRC");
					break;
				case 3:
					actv.setText("CAM");
					break;
				default:
					actv.setText("/");
			}
            return;
        }
            // TODO Auto-generated method stub        
            Intent intent = new Intent();
            intent.setClass(DemoActivity.this, DecodeActivity.class);
            int intGridNum = 0;
            int intVideoGridNum = 0;
            int intAudioGridNum = 0;
            int startIndex = 0;
            switch (v.getId()) {
                case R.id.btnDemo1:
                    intGridNum = 1;
                    intVideoGridNum = 1;
                    intAudioGridNum = 1;
                    break;
                case R.id.btnDemo2:
                    intGridNum = 1;
                    intVideoGridNum = 1;
                    intAudioGridNum = 1;
                    startIndex = 1;
                    break;
                case R.id.btnDemo3:
                    intGridNum = 11;
                    intVideoGridNum = 2;
                    intAudioGridNum = 1;
                    startIndex = 2;
                    break;
                case R.id.btnDemo4:
                    intGridNum = 11;
                    intVideoGridNum = 2;
                    intAudioGridNum = 1;
                    startIndex = 4;
                    break;
            }

            intent.putExtra("DATA_INT", intGridNum);
            intent.putExtra("LIMIT_INT", intVideoGridNum);
            intent.putExtra("LIMIT_AUD_INT", intAudioGridNum);

            intent.putExtra("REPEAT", 1);
            intent.putExtra("AUDIOCLICK", 0);
            intent.putExtra("FREERUN", 0);
            intent.putExtra("TUNNELMODE", 1);
            intent.putExtra("PERFORMANCE", 0);
            intent.putExtra("RENDER", 1);
            StringBuilder sb = new StringBuilder();
            playList.clear();
            for(int i=0;i<MAX; i++)
            {
                String tmp = listText[(startIndex+i)%MAX].getText().toString();
                playList.add(tmp);
                String tmp2 = listText[i].getText().toString();
                sb.append(tmp2).append(",");
            }
            
            playListRecord.edit().putString("PLAYLIST", sb.toString()).commit();
            intent.putStringArrayListExtra("LIST", playList);
            
            Log.d(TAG, "onClick start DecodeActivity");
            startActivity(intent);
            //DemoActivity.this.finish();
    }


	public class SpinnerListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			
			switch(position) {
				case 0:	//U-Disk
					sourceflag = 0;
					getUSBPath(rootPath);
					break;
				case 1:	//Samba
					sourceflag = 1;
					break;
				case 2:	//HDMI-Rx
					sourceflag = 2;
					break;
				case 3:	//USB-Camera
					sourceflag = 3;
					break;
				default:
			
			}
		}
		public void onNothingSelected(AdapterView<?> parent) {
			
		}
	}
}
