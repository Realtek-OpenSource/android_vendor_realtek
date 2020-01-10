package com.realtek.Utils;

import com.realtek.rtksmb.SmbUtils;

import java.io.File;

public class FileInfo {
	public String mSmbPath;
	public int mFileType;
	File mFile;
	public String fileName;
	String time ="";
	public String path ="";
    long   Size = 0;
	public FileInfo(int mFileType, File mFile) {
		this.mFileType = mFileType;
		this.mFile = mFile;
	}

	public FileInfo(int fileType, String smbPath){
		this.mFileType = fileType;
		this.mSmbPath = smbPath;
		int lastIndex = smbPath.lastIndexOf('/');
		if(lastIndex<0)
			this.fileName = smbPath;
		else if(lastIndex==smbPath.length()-1) {
			//smb://172.21.174.216/DATA_smb/audio/
			int slashIndex = SmbUtils.nthLastIndexOf(smbPath, '/', 2);
			this.fileName = smbPath.substring(slashIndex+1);
		} else
			this.fileName = smbPath.substring(smbPath.lastIndexOf('/')+1);
	}
	public FileInfo(int mFileType, File mFile, String fileName) {
		this.mFileType = mFileType;
		this.mFile = mFile;
		this.fileName = fileName;
	}
	public FileInfo(int mFileType, File mFile, String fileName, String path) {
		this.mFileType = mFileType;
		this.mFile = mFile;
		this.fileName = fileName;
		this.path = path;
	}

	@Override
	public String toString() {
		return String.format("%s,%s,%s,%s,%s,%s", mFileType, mFile.getName(), fileName, time, path, Size);
	}

	public int getmFileType() {
		return mFileType;
	}
    public long getSize()
    {
        return Size;
    }
    public void setSize(long Size)
    {
        this.Size = Size;
    }
	public void setmFileType(int mFileType) {
		this.mFileType = mFileType;
	}
	public File getmFile() {
		return mFile;
	}
	public void setmFile(File mFile) {
		this.mFile = mFile;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
}
