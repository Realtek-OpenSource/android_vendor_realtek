package com.realtek.rtksmb;

import android.util.Log;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.rtk.mediabrowser.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class StreamServer extends NanoHTTPD
{
    private static final String TAG = "StreamServer";
    private static final boolean DEBUG = true;
    private static final String MIME_TYPE = "image/jpeg";
    private static final int PORT = 8088;
    private static StreamServer sInstance;
    private String mSmbPath;
    private String mDomain;
    private String mUser;
    private String mPassword;
    private DiskShare mShare;
    private SMBClient mClient;
    private Connection mConnection;
    private Session mSession;

    private void log(String s) {
        if(DEBUG)
            Log.d(TAG, s);
    }

    public static String getHttpPrefix(){
        return "http://127.0.0.1:"+PORT+"/";
    }

    public static StreamServer getInstance(String mSmbPath, String mDomain, String mUser, String mPassword) {
        if(sInstance==null){
            sInstance = new StreamServer();
        }
        sInstance.mSmbPath = mSmbPath;
        sInstance.mDomain = mDomain;
        sInstance.mUser = mUser;
        sInstance.mPassword = mPassword;
        return sInstance;
    }

	private StreamServer() {
		super(PORT);
        log("start StreamServer");
	}

    @Override
    public void start() throws IOException {
        if(wasStarted()) {
            stop();
        }
        super.start();
        openSmbFile(true);
    }

    @Override
    public void stop() {
        openSmbFile(false);
        super.stop();
    }

    private void openSmbFile(boolean open) {
        if(open) {
            mClient = new SMBClient(SmbConfig.createDefaultConfig());
            try {
                mConnection = mClient.connect(SmbUtils.getIpFromSmbPath(mSmbPath));
                mSession = mConnection.authenticate(new AuthenticationContext(mUser, mPassword.toCharArray(), mDomain));
                mShare = (DiskShare) mSession.connectShare(SmbUtils.getShareFromSmbPath(mSmbPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if(mShare!=null) {
                try {
                    mShare.close();
                    mShare = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(mSession!=null){
                try {
                    mSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(mConnection!=null){
                try {
                    mConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(mClient!=null){
                mClient.close();
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();
        if(uri.endsWith("favicon.ico")) return newFixedLengthResponse(Response.Status.FORBIDDEN,
                NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        uri = uri.substring(1);
        log("serve uri:"+uri);
        return serveStreamUri(uri, header, Util.GetMimeTypes(null).getMimeType(uri));
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    private Response serveStreamUri(String uri, Map<String, String> header,
                               String mime) {
        log("serveStreamUri, mime="+mime);
        com.hierynomus.smbj.share.File smbf = mShare.openFile(uri, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
        FileAllInformation fai = mShare.getFileInformation(uri);
        long fileLen = fai.getStandardInformation().getEndOfFile();
        // Calculate etag
        String etag = Integer.toHexString((uri
                + fai.getBasicInformation().getChangeTime()
                + "" + fileLen).hashCode());
        log("fileLen="+fileLen);
        Response res;
        try {

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range
                                    .substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is
            // requested
            if (range != null && startFrom >= 0) {
                log("serveStreamUri with range");
                if (startFrom >= fileLen) {
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                            NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    InputStream is = smbf.getInputStream();
//                    FileInputStream fis = new FileInputStream(file) {
//                        @Override
//                        public int available() {
//                            return (int) dataLen;
//                        }
//                    };
                    is.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime,
                            is, dataLen);
//                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-"
                            + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                log("serveFile without range");
                if (etag.equals(header.get("if-none-match")))
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = newFixedLengthResponse(Response.Status.OK, mime,
                            smbf.getInputStream(), fileLen);
//                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = newFixedLengthResponse(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    private Response serveFile(String uri, Map<String, String> header,
                               File file, String mime) {
        log("serveFile");
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath()
                    + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range
                                    .substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                log("serveFile with range");
                if (startFrom >= fileLen) {
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                            NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime,
                            fis, dataLen);
//                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-"
                            + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                log("serveFile without range");
                if (etag.equals(header.get("if-none-match")))
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = newFixedLengthResponse(Response.Status.OK, mime,
                            new FileInputStream(file), fileLen);
//                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = newFixedLengthResponse(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }
}
