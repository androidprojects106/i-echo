package com.i_echo.lpc.i_echo.Audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.i_echo.lpc.i_echo.AppProtData;
import com.i_echo.lpc.i_echo.AppProtDataQueue;
import com.i_echo.lpc.i_echo.AppState;
import com.i_echo.lpc.i_echo.Constants;
import com.i_echo.lpc.i_echo.Utils.UtilsHelpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 6/24/2015.
 */
public class Audio {

    public static int[] SM_AUDIO_SAMPLERATE = new int[] {16000, 22050, 44100, 48000, 8000, 11025 };
    // sample rate - preference in decreasing order, adjust to set preferences
    public static short[] SM_AUDIO_CHANNELCONFIG = new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT };
    // channelConfig
    public static short[] SM_AUDIO_AUDIO_IN_FORMAT = new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
    // audioFormat {mono or stereo} for the audio recrod
    public static short[] SM_AUDIO_AUDIO_OUT_FORMAT = new short[] { AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO };
    // audioFormat {mono or stereo} for the audio track

    private Audio() {}

    /*
    Find the lowest sample-rate of the audio codec and use that for the current demo
    solution (may need to have the full list and do codec negotiation in actual product)
    */
    public static LinkedList<AudioParams> findAudioParams() {
        AudioRecord recorder =null;
        LinkedList<AudioParams> audioParamsList =null;

        for (int rate : SM_AUDIO_SAMPLERATE) {
            for (short audioFormat : SM_AUDIO_CHANNELCONFIG) {
                for (int i=0; i< SM_AUDIO_AUDIO_IN_FORMAT.length; i++) {
                    try {
                        int channelInConfig = SM_AUDIO_AUDIO_IN_FORMAT[i];
                        int channelOutConfig = SM_AUDIO_AUDIO_OUT_FORMAT[i];

                        if (Constants.DEBUGGING)
                            Log.d("findAudioRecord", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                    + channelInConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelInConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            // AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, 4*bufferSize);
                            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelInConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                recorder.release();

                                if (audioParamsList==null)
                                    audioParamsList = new LinkedList<AudioParams>();
                                audioParamsList.addLast(new AudioParams(rate, channelInConfig, channelOutConfig, audioFormat, bufferSize));
                            }
                        }
                    } catch (Exception e) {
                        if (Constants.DEBUGGING)
                            Log.e("findAudioRecord", rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        if (recorder!=null) recorder.release();

        return audioParamsList;
    }

    /*
     Read from Udp data from file and play them in the designated audio track
     instance.
    */
    public static void readAndPlayVoiceMsgFromFile(final AppState appState,
                                              final BufferedInputStream bufI) {
        final int sampleRate = appState.getAudioParamFirst().getSampleRate();
        // RECORDER_SAMPLERATE
        final int channelOutConfig = appState.getAudioParamFirst().getChannelOutConfig();
        // RECORDER_CHANNELS
        final int audioFormat = appState.getAudioParamFirst().getAudioFormat();
        // RECORDER_AUDIO_ENCODING
        final int bufferSize = appState.getAudioParamFirst().getBufferSize();
        // minBuferSize for this combination of audio parameters


        Thread completePlay = new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                if (null == bufI)
                    return;

                final AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRate, channelOutConfig, audioFormat, bufferSize,
                        AudioTrack.MODE_STREAM);
                if (player.getState() == AudioTrack.STATE_INITIALIZED) {
                    player.setPlaybackRate(sampleRate);
                    player.play();
                    doWritePlayerBufContinuous(bufI, player, bufferSize);
                    try { sleep(1000); }     // let the audio to drain
                    catch (InterruptedException e) { e.printStackTrace(); }
                    player.stop();
                }
                player.release();

                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            }
        });
        completePlay.start();
        try { completePlay.join(); } catch (InterruptedException e) {
            e.printStackTrace();
        }       // make sure audio is completed before returning
    }


    /*
     Play from Udp media packets in the designated audio track
     instance.
    */
    private static void doWritePlayerBufContinuous(final BufferedInputStream bufI,
                                            final AudioTrack player,final int bufferSize) {
        byte[] audioBuf = new byte[bufferSize];
        boolean workDone = false;
        int bytesRead =0;
        do {
            try {
                bytesRead = bufI.read(audioBuf, 0, audioBuf.length);
                if (bytesRead >0) {
                    player.write(audioBuf, 0, bytesRead);      // play the audio
                    workDone =false;
                }
                else workDone =true;
            } catch (IOException e) {
                workDone =true;
                e.printStackTrace();
            }
        } while (!workDone);
    }


    public static void readAndPlayVoiceMsgFromServer(final AppState appState, final BufferedOutputStream bufO,
                                                     final AppProtDataQueue appDataQue, final int sessionId,
                                                     final boolean flag) {
        if (null == appState)
            return;

        final int sampleRate = appState.getAudioParamFirst().getSampleRate();
        // RECORDER_SAMPLERATE
        final int channelOutConfig = appState.getAudioParamFirst().getChannelOutConfig();
        // RECORDER_CHANNELS
        final int audioFormat = appState.getAudioParamFirst().getAudioFormat();
        // RECORDER_AUDIO_ENCODING
        final int bufferSize = appState.getAudioParamFirst().getBufferSize();
        // minBuferSize for this combination of audio parameters
        final AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, channelOutConfig, audioFormat, bufferSize,
                AudioTrack.MODE_STREAM);

        Thread completePlay = new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                if (AudioTrack.STATE_INITIALIZED == player.getState()) {
                    player.setPlaybackRate(sampleRate);
                    player.play();
                    doWritePlayerBufContinuous(bufO, player, appDataQue, sessionId, bufferSize, flag);
                    try { sleep(1000); }     // let the audio to drain
                    catch (InterruptedException e) { e.printStackTrace(); }
                    player.stop();
                }
                player.release();
            }
        });
        completePlay.start();
        try { completePlay.join(); } catch (InterruptedException e) {
            e.printStackTrace();
        }       // make sure audio is completed before returning
    }


    /*
     Read from Udp data channel media packets and play them in the designated audio track
     instance. Session ID is enclosed to identify the particular media session (though
     media packet reordering is not currently taken care of
    */
    private static void doWritePlayerBufContinuous(final BufferedOutputStream bufO,
                                                   final AudioTrack player, final AppProtDataQueue appDataQue,
                                                   int sessionId, final int bufferSize, final boolean flag) {
        byte[] audioBuf = new byte[bufferSize];
        int bufPos, bufToFill, bufFillSize;
        int offsetData, dataSize;
        boolean workDone;
        AppProtData appData;

        appData = null;
        offsetData = 0;
        bufPos = 0;
        workDone = false;
        while (!workDone) {
            bufToFill = bufferSize;
            while (bufToFill > 0) {
                if (null == appData) {
                    appData = appDataQue.readDataFromServQue(sessionId);
                    if (null != appData && appData.isValid())
                        offsetData = 0;
                    else {
                        workDone = true;        // no more audio data to play
                        break;
                    }
                }
                dataSize = appData.getDataSize()-offsetData;
                bufFillSize = (bufToFill > dataSize) ? dataSize : bufToFill;
                System.arraycopy(appData.getData(), offsetData, audioBuf, bufPos, bufFillSize);
                bufPos += bufFillSize;
                offsetData += bufFillSize;
                bufToFill -= bufFillSize;
                if (offsetData == appData.getDataSize()) {
                    appData = null;             // reached the end of this data packet
                    offsetData = 0;
                }
            }
            if (bufPos > 0) {
                if (flag)
                    player.write(audioBuf, 0, bufPos);      // play the audio
                if (bufO!=null)
                    UtilsHelpers.writeVoiceMsgDataToStream(audioBuf, bufO);
                bufPos = 0;
            }
        }
    }

}
