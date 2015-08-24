package com.i_echo.lpc.i_echo;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.i_echo.lpc.i_echo.Utils.UtilsHelpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by LPC-Home1 on 7/4/2015.
 */

/*
Background asynchronous audio recording thread and send the audio directly to the server
for update user status to the server
**/
public class ProcessorVoiceMsgAsyncTask extends AsyncTask<Void, Integer, Integer> {
    private static final int MAX_AUDIOMSGTIME = 30;            // Max recorded audio clip (30sec)
    private static final int BytesPerElement = 2;              // 2 bytes in PCM-16bit format

    private static final int RESULT_MEDIASUCCESS = 1;
    private static final int RESULT_MEDIAEMPTY = 2;
    private static final int RESULT_UDPDATACHANNELFAILURE = 3;
    private static final int RESULT_AUDIORECORDFAILURE = 4;
    private static final int RESULT_AUDIOTRACKFAILURE = 5;
    private static final int RESULT_HDFAILURE = 9;

    private ActivityMain mActivity;
    private boolean mIsFinalCleaned;
    private AudioRecord mRecorder;

    private int bufferSize;
    private long startTime;
    private AppProtDataQueue appDataQue;

    public ProcessorVoiceMsgAsyncTask(ActivityMain activity) {
        mActivity =activity;
    }


    @Override
    protected void onPreExecute() {
        int sampleRate = mActivity.getAppState().getAudioParamFirst().getSampleRate();
        // RECORDER_SAMPLERATE
        int channelInConfig = mActivity.getAppState().getAudioParamFirst().getChannelInConfig();
        // RECORDER_CHANNELS
        int audioFormat = mActivity.getAppState().getAudioParamFirst().getAudioFormat();
        // RECORDER_AUDIO_ENCODING

        bufferSize = mActivity.getAppState().getAudioParamFirst().getBufferSize(); // *BytesPerElement;
        // minBuferSize for this combination of audio parameters
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelInConfig, audioFormat, bufferSize);
        // mBarProgress.setVisibility(View.VISIBLE);
        mActivity.getUiHolder().mBarProgress.setProgress(0);
        publishProgress(1);
        startTime = System.currentTimeMillis();
        mIsFinalCleaned = false;

        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncAudioRecordingTask: onPreExecute");
    }

    @Override
    protected Integer doInBackground(Void... arg0)  // Running in separate thread
    {
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncAudioRecordingTask: doInBackground");

        final int serverPort = mActivity.getServerUdpPort();
        final String serverIpAddress = mActivity.getAppServerIpAddress();

        if (null == mRecorder) {
            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "AsyncAudioRecordingTask: return RESULT_AUDIORECORDFAILURE");
            return RESULT_AUDIORECORDFAILURE;
        }
        // UDP data channel started here in separate thread
        // since this is a sending only UDP stream (for voice msgs)
        // no need to start a thread for listening to the UDP port
        appDataQue = new AppProtDataQueue(serverIpAddress, serverPort);
        if (!appDataQue.waitForUdpDataChannel()) {
            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "AsyncAudioRecordingTask: return RESULT_UDPDATACHANNELFAILURE");
            return RESULT_UDPDATACHANNELFAILURE;
        }

        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncAudioRecordingTask: enter recording");
        long timePassed = 0;
        long timeMax = MAX_AUDIOMSGTIME * 1000; // max 30 seconds
        int bytesRecorded = 0;
        int progressPercent;
        short dataSeqNum = 0;
        int appSessionId = mActivity.getAppState().getMediaSessionId();
        String targetId = mActivity.getAppTarget();
            /*
            PCM-8bit and PCM-16bit
            8000    |	samples/second
            160     |	samples/20 millisecond
            320     |   bytes/ 20 millisecond/PCM16bit
            960     |   bytes/ each 3x 20 millisecond packet
             */
        byte[] bData = new byte[bufferSize];
        byte dataType = AppProtData.UDPTYPE_AUDIOMSG_PCM16;
        mRecorder.startRecording();       // not supported in the emulator

        File root = android.os.Environment.getExternalStorageDirectory();
        String fileNameSessionId = UtilsHelpers.fileMediaFormat(targetId, appSessionId);
        String fileFullPath = root.getAbsolutePath() + "/echoData/" + fileNameSessionId;
        BufferedOutputStream bufO =null;
        try {
            bufO= new BufferedOutputStream(new FileOutputStream(fileFullPath, true));
        } catch (IOException e) {
            e.printStackTrace();
            mActivity.getUiHolder().showUIToastMsg(mActivity.getResources().getString(R.string.string_notsavesdfile));
        }
        while (true) {
            bytesRecorded = mRecorder.read(bData, 0, bufferSize);
            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "AsyncAudioRecordingTask: bytes recorded: "
                        + bytesRecorded + " seqnum " + dataSeqNum);
            // not supported in the emulator
            // bytesRecorded = simulate_recorder_read(sData, 0, BufferElements2Rec);
            if (-3 != bytesRecorded    // (0xfffffffd) ERROR_INVALID_OPERATION
                    && -2 != bytesRecorded) {      //(0xfffffffe) ERROR_BAD_VALUE
                dataSeqNum = appDataQue.sendRecordToServer(dataSeqNum, dataType,
                        bData, bytesRecorded, appSessionId);
                if (bufO!=null)
                    UtilsHelpers.writeVoiceMsgDataToStream(bData,bufO);
                // writes the audio data channel (UDP) to server
                // stores in the byte buffer
            }
            timePassed = System.currentTimeMillis() - startTime;
            progressPercent = (int) (100 * timePassed / timeMax);
            publishProgress(progressPercent);
            if (timePassed >= timeMax)
                break;
            if (isCancelled())
                break;
        }
        if (null != bufO) {
            try {
                bufO.flush();
                bufO.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return (timePassed > 1000) ? RESULT_MEDIASUCCESS : RESULT_MEDIAEMPTY;
        // less than one second regarded nothing recorded
    }


    protected void onProgressUpdate(Integer... progressPercent) {
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "setProgress: "+progressPercent[0]);
        mActivity.getUiHolder().mBarProgress.setProgress(progressPercent[0]);
    }

    @Override
    protected void onPostExecute(Integer result) {
        String resultMessage;
        switch (result) {
            case RESULT_MEDIASUCCESS: {
                resultMessage = mActivity.getResources().getString(R.string.string_messagesent);
                break;
            }
            case RESULT_UDPDATACHANNELFAILURE: {
                resultMessage = mActivity.getResources().getString(R.string.string_cannotconnectudp);
                break;
            }
            case RESULT_AUDIORECORDFAILURE: {
                resultMessage = mActivity.getResources().getString(R.string.string_audiorecordererror);
                break;
            }
            case RESULT_HDFAILURE: {
                resultMessage = mActivity.getResources().getString(R.string.string_noaccesssdfile);
                break;
            }
            case RESULT_MEDIAEMPTY:
            default: {
                resultMessage = mActivity.getResources().getString(R.string.string_noaudiorecorded);
                break;
            }
        }

        if (!mIsFinalCleaned) {
            finalCleanup();
            mIsFinalCleaned = true;
        }
        mActivity.getUiHolder().showUIToastMsg(resultMessage);
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncAudioRecordingTask: onPostExecute");

        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncAudioRecordingTask: onPostExecute - onCancelled()");
        if (!mIsFinalCleaned) {
            finalCleanup();
            mIsFinalCleaned = true;
        }

        super.onCancelled();
    }

    @Override
    protected void onCancelled(Integer result) {
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncAudioRecordingTask: onPostExecute - onCancelled(i)");

        switch (result) {
            case RESULT_MEDIAEMPTY: {
                mActivity.getUiHolder().showUIToastMsg(mActivity.getResources().getString(R.string.string_noaudiorecorded));
                break;
            }
            default:
                break;
        }
        if (!mIsFinalCleaned) {
            finalCleanup();
            mIsFinalCleaned = true;
        }

        super.onCancelled(result);
    }

    private void finalCleanup() {
        mActivity.getUiHolder().mBarProgress.setProgress(0);
        mActivity.getUiHolder().showUIDefaultEcho();

        if (null != mRecorder) {
            mRecorder.stop();     // never started, emulator not supported
            mRecorder.release();
            mRecorder = null;
        }
        if (null != appDataQue) {
            appDataQue.close();
            appDataQue =null;
        }
    }
}