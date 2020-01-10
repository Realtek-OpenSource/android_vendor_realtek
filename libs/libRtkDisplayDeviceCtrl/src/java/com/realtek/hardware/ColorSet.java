package com.realtek.hardware;

import com.realtek.hardware.RtkHDMIManager2;

public class ColorSet {
    int mColorMode;
    int mColorDepth;

    public ColorSet(){
        mColorMode = RtkHDMIManager2.ColorNone;
        mColorDepth = RtkHDMIManager2.DepthNone;
    }

    public ColorSet(int colorMode,
        int colorDepth) {

        mColorMode = colorMode;
        mColorDepth = colorDepth;
    }

    public boolean isValidate() {
        if(mColorMode == RtkHDMIManager2.ColorNone)
            return false;
        if(mColorDepth == RtkHDMIManager2.DepthNone)
            return false;

        return true;
    }
}
