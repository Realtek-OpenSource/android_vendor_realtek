package com.realtek.hardware;

public class OutputFormat {
    int mVIC;
    int mFreqShift;
    int mColor;
    int mColorDepth;
    int m3DFormat;
    int mHDR;

    public OutputFormat() {
    }

    public OutputFormat(
        int vic,
        int freqShift,
        int color,
        int colorDepth,
        int _3DFormat,
        int hdr) {

        mVIC        = vic;
        mFreqShift  = freqShift;
        mColor      = color;
        mColorDepth = colorDepth;
        m3DFormat   = _3DFormat;
        mHDR        = hdr;
    }
}
