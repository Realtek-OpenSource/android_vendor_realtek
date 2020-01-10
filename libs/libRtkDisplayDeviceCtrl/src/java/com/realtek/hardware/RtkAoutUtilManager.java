package com.realtek.hardware;


/*
 * The implementation of the RtkAoutUtilManager.
 */
public class RtkAoutUtilManager {
    static {
        System.loadLibrary("rtk-display_ctrl_jni");
    }

    private static final String TAG = "RtkAoutUtilManager";
    /**
     * Constructs a new RtkAoutUtilManager instance.
     *
     */
    public RtkAoutUtilManager() {

    }

    public boolean init() {
        return _init();
    }

    public boolean setAudioSpdifOutput(int mode) {
        return _setAudioSpdifOutput(mode);
    }

    public boolean setAudioSpdifOutputOffMode() {
        return _setAudioSpdifOutputOffMode();
    }

    public boolean setAudioHdmiOutput(int mode) {
        return _setAudioHdmiOutput(mode);
    }

    public boolean setAudioAGC(int mode) {
        return _setAudioAGC(mode);
    }

    public boolean setAudioForceChannelCtrl(int mode) {
        return _setAudioForceChannelCtrl(mode);
    }

    public boolean setAudioHdmiFreqMode() {
        return _setAudioHdmiFreqMode();
    }

    public void setAudioSurroundSoundMode(int mode) {
        _setAudioSurroundSoundMode(mode);
    }

    public boolean setAudioDACOn(int audio_dac_on) {
        return _setAudioDACOn(audio_dac_on);
    }

    public void setAudioDecVolume(int vol) {
        _setAudioDecVolume(vol);
    }
    /**
     * Below declaration are implemented by c native code
     * that implemented by frameworks/base/realtek/jni/com_realtek_hardware_RtkAoutUtilManager.cpp
     */

    private native boolean _init();
    private native boolean _setAudioSpdifOutput(int mode);
    private native boolean _setAudioSpdifOutputOffMode();
    private native boolean _setAudioHdmiOutput(int mode);
    private native boolean _setAudioAGC(int mode);
    private native boolean _setAudioForceChannelCtrl(int mode);
    private native boolean _setAudioHdmiFreqMode();
    private native void _setAudioSurroundSoundMode(int mode);
    private native boolean _setAudioDACOn(int audio_dac_on);
    private native void _setAudioDecVolume(int vol);

}
