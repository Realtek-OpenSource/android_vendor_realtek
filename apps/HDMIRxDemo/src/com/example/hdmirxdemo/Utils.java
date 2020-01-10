package com.example.hdmirxdemo;

public class Utils {

    private static Utils sInstance = null;

    /*
    static {
        System.loadLibrary("hdmidemoutils");
    }
    */

    public void init() {
        // dummy function, do nothing
    }

    public void prepare() {
        // dummy function, do nothing
    }

    public static Utils getInstance(){
        if(sInstance == null){
            sInstance = new Utils();
        }
        return sInstance;
    }

}
