package com.i_echo.lpc.i_echo;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;

import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 7/17/2015.
 */

public class ProcessorPttAsyncTask extends AsyncTask<Void, Integer, Integer> {
    private final static int PTTCHAT_STATE_READY =10;
    private final static int PTTCHAT_STATE_SPEAKING =11;
    private final static int PTTCHAT_STATE_LISTENING =12;
    private final static int PTTCHAT_STATE_STOPPING =14;


    private static final int RESULT_AUDIORECORDSUCCESS = 1;
    private static final int RESULT_AUDIORECORDFAILURE = 4;

    private ActivityMain mActivity;
    private PttAudioChannel mPttChannel;
    private int mSessionId;

    long startTime;
//    ProgressDialog progress;

    public ProcessorPttAsyncTask(ActivityMain activity, PttAudioChannel pttChannel, int sessionId) {
        mActivity =activity;
        mPttChannel =pttChannel;
        mPttChannel = new PttAudioChannel(mActivity);
        mSessionId =sessionId;
    }

    @Override
    protected void onPreExecute() {
        startTime = System.currentTimeMillis();
//        progress = ProgressDialog.show(mActivity, "", "Chat in progress", true);
//        progress.getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncPttChatTask: onPreExecute");
    }

    @Override
    protected Integer doInBackground(Void... arg0)  // Running in separate thread
    {
        final long timeMax = 30 * 1000; // max 30 seconds
        final byte dataType = AppProtData.UDPTYPE_AUDIOMSG_PCM16;

//        int progressPercent;
        int buffersize =mPttChannel.getBuffersize();
        byte[] bData = new byte[buffersize];
        int bytesRecorded = 0;
        long timePassed = 0;

        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncPttChatTask: doInBackground");

        if (null == mPttChannel.getAudioRecorder() || null == mPttChannel.getAppDataCh()) {
            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "AsyncPttChatTask: return RESULT_AUDIORECORDFAILURE");
            return RESULT_AUDIORECORDFAILURE;
        }

        mPttChannel.getAudioRecorder().startRecording();        // not supported in the emulator
        while (true) {
            bytesRecorded = mPttChannel.getAudioRecorder().read(bData, 0, buffersize);
            if (-3 != bytesRecorded             // (0xfffffffd) ERROR_INVALID_OPERATION
                    && -2 != bytesRecorded) {      //(0xfffffffe) ERROR_BAD_VALUE
                short seqNum =mPttChannel.getAppDataCh().
                        sendRecordToServer(mPttChannel.getDataSeqNum(), dataType, bData, bytesRecorded, mSessionId);
                mPttChannel.setDataSeqNum(seqNum);
            }
            timePassed = System.currentTimeMillis() - startTime;
//            progressPercent = (int) (100 * timePassed/timeMax);
//            publishProgress(progressPercent);
            if (timePassed >= timeMax)
                break;
            if (isCancelled())
                break;
        }

        return RESULT_AUDIORECORDSUCCESS;
    }

    protected void onProgressUpdate(Integer... progressUpdate) {
//        progress.setProgress(progressUpdate[0]);
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (Constants.DEBUGGING)
            Log.i("ProtDataTag", "AsyncPttChatTask: onPostExecute");
        mPttChannel.getAudioRecorder().stop();
//        progress.dismiss();

        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled(Integer result) {
/*
After invoking .cancell() method, onCancelled(Object), instead of onPostExecute(Object)
will be invoked after doInBackground(Object[]) returns
 */
        if (Build.VERSION.SDK_INT >= 11) {
            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "AsyncPttChatTask: onCancelled(int)");
            mPttChannel.getAudioRecorder().stop();
//            progress.dismiss();
        }

        super.onCancelled(result);
    }

    @Override
    protected void onCancelled() {
/*
After invoking .cancell() method, onCancelled(Object), instead of onPostExecute(Object)
will be invoked after doInBackground(Object[]) returns
 */
        if (Build.VERSION.SDK_INT < 11) {
            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "AsyncPttChatTask: onCancelled()");
            mPttChannel.getAudioRecorder().stop();
//            progress.dismiss();
        }

        super.onCancelled();
    }
}
