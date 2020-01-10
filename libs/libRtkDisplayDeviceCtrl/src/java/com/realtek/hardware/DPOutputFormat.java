package com.realtek.hardware;

public class DPOutputFormat {
    int mVIC;
    int mDisplayMode;

    public DPOutputFormat() {
        mVIC            = 0;
        mDisplayMode    = 0;
    }

    public DPOutputFormat(
        int vic,
        int displayMode) {

        mVIC            = vic;
        mDisplayMode    = displayMode;
    }
}
