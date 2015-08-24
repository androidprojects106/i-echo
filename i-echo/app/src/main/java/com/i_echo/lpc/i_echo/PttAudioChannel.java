package com.i_echo.lpc.i_echo;

import android.content.res.Resources;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by LPC-Home1 on 7/17/2015.
 */
public class PttAudioChannel {
    private ActivityMain mActivity;
    private AppProtDataQueue mAppDataQue;
    private AudioRecord mAudioRecorder =null;
    private AudioTrack mAudioPlayer=null;
    private short mDataSeqNum;
    private AppState mAppState;
    private int mBufferSize;

    public PttAudioChannel(ActivityMain activity) {
        mActivity =activity;
        mAppState =mActivity.getAppState();
        mDataSeqNum =0;

        mBufferSize = getAppState().getAudioParamFirst().getBufferSize(); // *BytesPerElement;
        // minBuferSize for this combination of audio parameters

        if (initUdpDataQue()) {
            initAudioRecorder(mBufferSize);
            initAudioPlayer(mBufferSize);
            TTSHelpers.getInstance().init(mActivity);
        }
    }

    public void close() {
        if (null != mAudioRecorder) {
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
        if (null != mAudioPlayer) {
            mAudioPlayer.release();
            mAudioPlayer = null;
        }
        if (null != mAppDataQue)
            mAppDataQue.close();
    }

    protected short getDataSeqNum() {
        return mDataSeqNum;
    }

    protected void setDataSeqNum(short seqNum) {
        mDataSeqNum = seqNum;
    }

    protected AppProtDataQueue getAppDataCh() {
        return mAppDataQue;
    }

    protected AudioRecord getAudioRecorder() {
        return mAudioRecorder;
    }

    protected AudioTrack getAudioPlayer() {
        return mAudioPlayer;
    }

    protected int getBuffersize() {
        return mBufferSize;
    }

    protected AppState getAppState() {
        return mAppState;
    }

    protected Resources getResources() {
        return mActivity.getResources();
    }


    private boolean initUdpDataQue() {
        if (null == mActivity) {
            mAppDataQue = null;
            return false;
        }
        else {
            final int serverPort = mActivity.getServerUdpPort();
            final String serverIpAddress = mActivity.getAppServerIpAddress();

            // UDP data channel started here in separate thread
            // since this is a sending only UDP stream (for voice msgs)
            // no need to start a thread for listening to the UDP port
            mAppDataQue = new AppProtDataQueue(serverIpAddress, serverPort);
            if (!mAppDataQue.waitForUdpDataChannel()) {
                if (Constants.DEBUGGING)
                    Log.i("ProtDataTag", "AsyncPttChatTask: return RESULT_UDPDATACHANNELFAILURE");
                mAppDataQue = null;
                return false;
            }
            else {
                mAppDataQue.startReceiving();
            }
            return true;
        }
    }

    private boolean initAudioRecorder(int bufferSize) {
        int sampleRate = getAppState().getAudioParamFirst().getSampleRate(); // RECORDER_SAMPLERATE
        int channelInConfig = getAppState().getAudioParamFirst().getChannelInConfig(); // RECORDER_CHANNELS
        int audioFormat = getAppState().getAudioParamFirst().getAudioFormat(); // RECORDER_AUDIO_ENCODING

        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelInConfig, audioFormat, bufferSize);

        return (AudioRecord.STATE_INITIALIZED == mAudioRecorder.getState());
    }

    private boolean initAudioPlayer(int bufferSize) {
        int sampleRate = getAppState().getAudioParamFirst().getSampleRate();
        int channelOutConfig = getAppState().getAudioParamFirst().getChannelOutConfig();
        int audioFormat = getAppState().getAudioParamFirst().getAudioFormat();

        mAudioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelOutConfig,
                audioFormat, bufferSize, AudioTrack.MODE_STREAM);
        mAudioPlayer.setPlaybackRate(sampleRate);

        return (AudioTrack.STATE_INITIALIZED == mAudioPlayer.getState());
    }
}
