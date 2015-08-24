package com.i_echo.lpc.i_echo.Audio;

/**
 * Created by LPC-Home1 on 7/18/2015.
 */
public class AudioParams {
    int mSampleRate;            // { 8000, 11025, 22050, 44100, 48000 }
    int mChannelInConfig;       // AudioRecord { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }
    int mChannelOutConfig;      // AudioTrack { AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO }
    int mAudioFormat;         // { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }
    int mBufferSize;

    public AudioParams(int sampleRate, int channelInConfig, int channelOutConfig, short audioFormat, int bufSize) {
        mSampleRate = sampleRate;
        mChannelInConfig =channelInConfig;
        mChannelOutConfig =channelOutConfig;
        mAudioFormat =audioFormat;
        mBufferSize =bufSize;
    }

    /*
     Utilities
     */
    public int getSampleRate() { return mSampleRate; }
    public void setSampleRate(int rate) {mSampleRate = rate; }
    public int getAudioFormat() { return mAudioFormat; }
    public void setAudioFormat(short format) {mAudioFormat = format; }
    public int getChannelInConfig() { return mChannelInConfig; }
    public void setChannelInConfig(short config) {mChannelInConfig = config; }
    public int getChannelOutConfig() { return mChannelOutConfig; }
    public void setChannelOutConfig(short config) {mChannelOutConfig = config; }
    public int getBufferSize() { return mBufferSize; }
    public void setBufferSize(int size) {mBufferSize = size; }
}
