package com.i_echo.lpc.i_echo;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 5/25/2015.
 */
public class ProcessorPttChat {
    final static int AVERAGES_BY_AMP = 1;
    final static int AVERAGES_BY_POWER = 2;

    final static int AUDIO_AMP_THRESHOLD = 350;

    private ActivityMain mActivity;
    private AppState mAppState;
    private AppProtMsgQueue mAppMsgQue;
    private ArrayList<ContactInfoUser> mAppUsers;
    private ActivityMainUiManager mUiHolder;
    private AppProtMsg mAppServMsg;
    private PttAudioChannel mPttChannel;
    private boolean mRunningAudioTask;
    private static ProcessorPttAsyncTask mPttRecordingTask;
    int mSessionId = 0;
    short mDataSeqNum = 0;

    IPttProcListener mCallback;

    public interface IPttProcListener {
        public void onPttChatSessionBegin();
        public PttAudioChannel onPttCallProc();
        public void onPttChatSessionFinish();
    }


    public ProcessorPttChat(final ActivityMain activity) {
        mActivity = activity;
        mCallback = (IPttProcListener) activity;
        mAppState = mActivity.getAppState();
        mAppMsgQue = mActivity.getAppMsgQue();
        mAppUsers = mActivity.getAppUserAndTarget();
        mAppServMsg = null;
        mUiHolder = mActivity.getUiHolder();
        mRunningAudioTask =false;
    }

    public ProcessorPttChat(final ActivityMain activity, AppProtMsg appMsg) {
        mActivity =activity;
        mCallback = (IPttProcListener)activity;
        mAppState =mActivity.getAppState();
        mAppMsgQue =mActivity.getAppMsgQue();
        mAppUsers =mActivity.getAppUserAndTarget();
        mAppServMsg = appMsg;
        mUiHolder =mActivity.getUiHolder();
        mRunningAudioTask =false;
    }


    public void doPttCallP() {
        int startState;

        mCallback.onPttChatSessionBegin();
        if (null == mAppServMsg) {
            startState = AppState.SM_STATE_PTTCALLREQUEST;
        }
        else {
            startState = AppState.SM_STATE_PTTCALLANNOUNCE;
        }
        doPttLoop(startState);
        mCallback.onPttChatSessionFinish();
    }

    protected ActivityMainUiManager getUiHolder() {
        return mUiHolder;
    }

    protected AppState getAppState() {
        return mAppState;
    }

    protected ArrayList<ContactInfoUser> getAppUsers() {
        return mAppUsers;
    }

    protected AppProtMsgQueue getAppMsgQue() {
        return mAppMsgQue;
    }

    protected Resources getResources() {
        return mActivity.getResources();
    }

    protected ContactInfoUser getAppUser() {
        return mAppUsers.get(0);
    }

    protected ContactInfoUser getAppTarget() {
        return mAppUsers.get(1);
    }

    protected String getAppUserId() {
        return mAppUsers.get(0).getAppUserId();
    }

    protected String getAppTargetId() {
        return mAppUsers.get(1).getAppUserId();
    }

    private int doPttLoop(int state) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        int returnCode = Constants.APP_FINISH_NORMAL;
        TimerControl timerHoldOff = new TimerControl();
        TimerControl timerBarcall = new TimerControl();
        boolean continuing = true;

        mPttChannel =mCallback.onPttCallProc();
        timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF); // holdoff timer
        timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL); // interval for call attempts
        while (continuing
                && getAppState().isSyncRunning(AppState.APP_STATE_LOCK)
                && getAppMsgQue().isConnected()) {
            switch (getAppState().getSyncSmState(AppState.APP_STATE_LOCK)) {
                case AppState.SM_STATE_WAITINGFORUI_PTTTRANSMIT:
                case AppState.SM_STATE_WAITINGFORUI_PTTFLOOR:
                case AppState.SM_STATE_WAITINGFORUI_PTTYOUSAY: {
                    getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                    break;  // allow UI interrupt for user response
                }
                case AppState.SM_STATE_PTTHOLDOFF: {
                    // check that the ptt holdoff timer has expired and exit PTT mode
                    if (timerHoldOff.isTimeout()) {
                        getAppState().setSmState(AppState.SM_STATE_PTTCALLFINISH);
                        break;
                    }
                    else {
                        boolean isPendedInMainCallp = false;
                        mAppServMsg = getAppMsgQue().readMsgFromServActiveQue();
                        if (null == mAppServMsg) {
                            mAppServMsg = getAppMsgQue().readMsgFromServPendedQue();
                            isPendedInMainCallp = true;
                        } else {
                            getAppState().updateKeepAliveReceived();
                            if (getAppState().isSendKeepAlive()) {
                                String userId = getAppUserId(); // getAppUsers().get(0).getAppUserId();
                                String serverId = getAppState().getAppServerId();
                                getAppMsgQue().sendMsgToServer(mAppServMsg.getSeqNum(),
                                        AppProtMsg.MSG_KEEPALIVE, userId, serverId);
                                getAppState().updateKeepAliveSent();
                            }
                        }
                        // now handle the non-null message from the server
                        if (null == mAppServMsg) {
                            getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                            break;  // allow UI floor request
                        }
                        else {
                            short seqNum = mAppServMsg.getSeqNum();
                            int sessionId = mAppServMsg.getSessionId();

                            if (mAppServMsg.equalTo(seqNum, AppProtMsg.MSG_KEEPALIVE)) {
                                getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                                break; //do nothing   // allow UI floor request
                            } else if (mAppServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTCALLEND)) {
                                getAppState().setSmState(AppState.SM_STATE_PTTCALLSUCCESS);
                                break;
                            } else if (mAppServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTFLOORTAKE, sessionId)) {
                                getAppState().setSmState(AppState.SM_STATE_PTTFLOORANNOUNCE);
                                break;
                            } else if (mAppServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTYOUSAYANNOUNCE, sessionId)) {
                                getAppState().setSmState(AppState.SM_STATE_PTTYOUSAYANNOUNCE);
                                break;
                            }  else if (mAppServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                                reasonCode = Constants.NAK_RECEIVED;
                                getAppState().setSmState(AppState.SM_STATE_PTTCALLFAIL);
                                break;
                            }
                            else if (!isPendedInMainCallp &&
                                    !getAppMsgQue().pendMsgIncomingFromServ(mAppServMsg)) {
                                reasonCode = Constants.ILLEGAL_MSG;
                                getAppState().setSmState(AppState.SM_STATE_PTTCALLFAIL);
                                break;
                            }
                            else {
                                getAppMsgQue().pendMsgIncomingFromServ(mAppServMsg);
                                getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);  // allow UI floor request
                                break; // only pending messages in main callp loop remains
                            }
                        }
                    }
                }
                case AppState.SM_STATE_PTTCALLREQUEST: {
                    state = handlePttCallRequestPrep();
                    if (AppState.SM_STATE_PTTTRANSMIT_PREPARED == state) {
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_PTTTRANSMIT);
                    }
                    else
                        getAppState().setSmState(AppState.SM_STATE_PTTCALLFAIL);
                    break;
                }
                case AppState.SM_STATE_PTTTRANSMIT_COMPLETE: {
                    handlePttCallRequestComplete();
                    timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL);
                    getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    break;
                }
                case AppState.SM_STATE_PTTCALLANNOUNCE: {
                    state = handlePttCallReceive(mAppServMsg);
                    timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL);
                    mAppServMsg =null;
                    getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYREQUEST: {
                    if (timerBarcall.isTimeout()) {
                        state = handlePttYouSayRequestPrep();
                        if (AppState.SM_STATE_PTTYOUSAY_SUCCESS == state) {
                            getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_PTTYOUSAY);
                        } else
                            getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    }
                    else {
                        mUiHolder.showUIToastMsg("Hang on!");
                        getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    }
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYREQUEST_COMPLETE: {
                    state = handlePttYouSayRequestComplete();
                    timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL);
                    getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYANNOUNCE: {
                    state = handlePttYouSayReceive(mAppServMsg);
                    timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL);
                    mAppServMsg =null;
                    getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    break;
                }
                case AppState.SM_STATE_PTTFLOORREQUEST: {
                    if (timerBarcall.isTimeout()) {
                        state = handlePttFloorRequestPrep();
                        if (AppState.SM_STATE_PTTFLOOR_PREPARED == state) {
                            getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_PTTFLOOR);
                        } else
                            getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    }
                    else {
                        mUiHolder.showUIToastMsg("Hang on!");
                        getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    }
                    break;
                }
                case AppState.SM_STATE_PTTFLOOR_COMPLETE: {
                    state = handlePttFloorRequestComplete();
                    timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL);
                    getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    break;
                }
                case AppState.SM_STATE_PTTFLOORANNOUNCE: {
                    state = handlePttFloorReceive(mAppServMsg);
                    timerHoldOff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    timerBarcall.setTimer(TimerControl.TIMER_PTTBARCALL);
                    mAppServMsg =null;
                    getAppState().setSmState(AppState.SM_STATE_PTTHOLDOFF);
                    break;
                }
                case AppState.SM_STATE_PTTCALLFINISH: {
                    handlePttEnd();
                    getAppState().setSmState(AppState.SM_STATE_PTTCALLSUCCESS);
                    break;
                }
                case AppState.SM_STATE_PTTCALLSUCCESS: {
                    continuing =false;
                    returnCode =Constants.APP_FINISH_NORMAL;
                    getAppState().setSmState(AppState.SM_STATE_PTTCALLSUCCESS);
                    getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                    break;
                }
                case AppState.SM_STATE_PTTCALLFAIL: {
                    continuing =false;
                    returnCode =Constants.APP_FINISH_ABNORMAL;
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTLOOP: reason code#" + reasonCode);
                    getAppState().setSmState(AppState.SM_STATE_PTTCALLFAIL);
                    getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                    break;
                }
            }
        }

        return returnCode;
    }

    private int handlePttYouSayRequestPrep() {

        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId();
        String tagetId = getAppTargetId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PTTYOUSAYREQUEST;
        int stateResult = AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL;
        AppProtMsg appServMsg = null;

        mSessionId = getAppState().getMediaSessionId();
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PTTYOUSAYREQUEST: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTYOUSAY,
                            userId, tagetId, mSessionId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTYOUSAYCONFIRM;
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYCONFIRM: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTYOUSAYCONFIRM)) {
                            mSessionId = appServMsg.getSessionId();
                            getAppState().setMediaSessionId(mSessionId);
                            // media session Id is aligned between the app and server
                            substate = AppState.SM_STATE_PTTYOUSAY_STARTED;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTYOUSAYREJECT)) {
                            substate = AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED1;
                            substate = AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            reasonCode = Constants.ILLEGAL_MSG;
                            substate = AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAY_STARTED: {
                    readAndPlayPttFromServer(false);
                    // do not wait for the audio to complete
                    // - controlled by user button release
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTYOUSAY_SUCCESS;
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAY_SUCCESS: {
                    stateResult = AppState.SM_STATE_PTTYOUSAY_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL: {
                    stateResult = AppState.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTTRANS: reason code#" + reasonCode);
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }

    private int handlePttYouSayRequestComplete() {

        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId();
        String tagetId = getAppTargetId();

        mSessionId = getAppState().getMediaSessionId();

        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTFLOORRELEASE,
                userId, tagetId, mSessionId);
        getAppState().updateKeepAliveSent();
        getAppState().incAppSeqNum();

        return AppState.SM_STATE_PTTYOUSAY_SUCCESS;
    }

    private int handlePttYouSayReceive(AppProtMsg appServMsg) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = appServMsg.getSeqNum();
        String sourceId = appServMsg.getMsgSrc();
        String userIdDst = appServMsg.getMsgDst();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PTTYOUSAYANNOUNCE;
        int stateResult = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;

        mSessionId = appServMsg.getSessionId();
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PTTYOUSAYANNOUNCE: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_ACK,
                            userIdDst, sourceId, mSessionId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_PREPARE;
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYRECEIVE_PREPARE: {
                    UiHelpers.playNotificationSound(mActivity);
                    TTSHelpers.getInstance().speak("You can speak", TextToSpeech.QUEUE_FLUSH);
                    if (!mRunningAudioTask) {
                        mRunningAudioTask = true;
                        substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_STOPPING;
                        timerControl.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                                mPttRecordingTask = new ProcessorPttAsyncTask(mActivity,
                                        mPttChannel, getAppState().getMediaSessionId());
                                mPttRecordingTask.execute();
                            }
                        });
                    }
                    else {
                        substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;
                        reasonCode = Constants.AUDIOALREADYRUNNING;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYRECEIVE_STOPPING: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTFLOORFREE)) {
                            substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_STOP;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED2;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG2;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG2;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYRECEIVE_STOP: {
                    if (mRunningAudioTask) {
                        if (null != mPttRecordingTask && AsyncTask.Status.FINISHED != mPttRecordingTask.getStatus())
                            mPttRecordingTask.cancel(false);
                        mRunningAudioTask = false;
                        substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_SUCCESS;
                    }
                    else {
                        substate = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;
                        reasonCode = Constants.AUDIONOTRUNNING;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYRECEIVE_SUCCESS: {
                    stateResult = AppState.SM_STATE_PTTYOUSAYRECEIVE_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL: {
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTFREC: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_PTTYOUSAYRECEIVE_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;
    }

    private int handlePttCallRequestPrep() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId();
        String tagetId = getAppTargetId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PTTCALLREQUEST;
        int stateResult = AppState.SM_STATE_PTTTRANSMIT_FAIL;
        AppProtMsg appServMsg = null;

        mSessionId = getAppState().getMediaSessionId();
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PTTCALLREQUEST: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTCALLREQUEST,
                            userId, tagetId, mSessionId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTTRANSMIT_ACCEPTED;
                    break;
                }
                case AppState.SM_STATE_PTTTRANSMIT_ACCEPTED: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTCALLACCEPT)) {
                            mSessionId = appServMsg.getSessionId();
                            getAppState().setMediaSessionId(mSessionId);
                            // media session Id is aligned between the app and server
                            substate = AppState.SM_STATE_PTTTRANSMIT_STARTED;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTCALLREJECT)) {
                            substate = AppState.SM_STATE_PTTTRANSMIT_FAIL;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED;
                            substate = AppState.SM_STATE_PTTTRANSMIT_FAIL;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            reasonCode = Constants.ILLEGAL_MSG;
                            substate = AppState.SM_STATE_PTTTRANSMIT_ABANDONED;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTTRANSMIT_STARTED: {
                    UiHelpers.playNotificationSound(mActivity);
                    getUiHolder().showUIPttButton();
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_recordandsend));
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_ACK, userId, tagetId, mSessionId);
                    getAppState().updateKeepAliveSent();
                    substate = AppState.SM_STATE_PTTTRANSMIT_PREPARED;
                    break;
                }
                case AppState.SM_STATE_PTTTRANSMIT_PREPARED: {
                    stateResult = AppState.SM_STATE_PTTTRANSMIT_PREPARED;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PTTTRANSMIT_ABANDONED: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_NAK,
                            userId, tagetId);
                    substate = AppState.SM_STATE_PTTTRANSMIT_FAIL;
                    break;
                }
                case AppState.SM_STATE_PTTTRANSMIT_FAIL: {
                    stateResult = AppState.SM_STATE_PTTTRANSMIT_FAIL;
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTTRANS: reason code#" + reasonCode);
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }

    private int handlePttCallRequestComplete() {
        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId();
        String tagetId = getAppTargetId();

        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTFLOORRELEASE,
                userId, tagetId, mSessionId);
        getAppState().updateKeepAliveSent();
        getAppMsgQue().saveUserMsg(tagetId, true, getResources().getString(R.string.string_ptt_message),
                AppProtUserMsg.USERMSG_PTT_TO);
        getAppState().incAppSeqNum();

        return AppState.SM_STATE_PTTTRANSMIT_SUCCESS;
    }

    private int handlePttCallReceive(AppProtMsg appServMsg) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = appServMsg.getSeqNum();
        String sourceId = appServMsg.getMsgSrc();
        String userIdDst = appServMsg.getMsgDst();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PTTCALLANNOUNCE;
        int stateResult = AppState.SM_STATE_PTTRECEIVE_FAIL;

        mSessionId = appServMsg.getSessionId();
        getAppState().setMediaSessionId(mSessionId);
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PTTCALLANNOUNCE: {
                    if (mPttChannel.getAppDataCh().isConnected() &&
                            mPttChannel.getAppDataCh().sendUdpTriggerToServer(AppProtData.UDPTYPE_TRIGGER, mSessionId)) {
                        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTCALLACCEPT,
                                userIdDst, sourceId, mSessionId);
                        getAppState().updateKeepAliveSent();
                        timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                        substate = AppState.SM_STATE_PTTRECEIVE_CONFIRM;
                    } else {
                        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTCALLREJECT,
                                userIdDst, sourceId, mSessionId);
                        getAppState().updateKeepAliveSent();
                        substate = AppState.SM_STATE_PTTRECEIVE_FAIL;
                        reasonCode = Constants.NOTFOUND_UDP;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTRECEIVE_CONFIRM: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_ACK))
                            substate = AppState.SM_STATE_PTTRECEIVE_STARTED;
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_PTTRECEIVE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED1;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_PTTRECEIVE_ABANDONED;
                            reasonCode = Constants.ILLEGAL_MSG1;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTRECEIVE_ABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG1;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTRECEIVE_STARTED: {
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_chatcallreceived));
                    getUiHolder().showUIPttButton();
                    readAndPlayPttFromServer(true);
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTRECEIVE_STOP;
                    break;
                }
                case AppState.SM_STATE_PTTRECEIVE_STOP: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTFLOORFREE)) {
                            substate = AppState.SM_STATE_PTTRECEIVE_SUCCESS;
                        }
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_PTTRECEIVE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED2;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_PTTRECEIVE_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG2;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTRECEIVE_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG2;
                    }
                    break;      // continue to wait with timer
                }
                case AppState.SM_STATE_PTTRECEIVE_SUCCESS: {
                    getAppMsgQue().saveUserMsg(sourceId, true, getResources().getString(R.string.string_ptt_message),
                            AppProtUserMsg.USERMSG_PTT_FROM);
                    stateResult = AppState.SM_STATE_PTTRECEIVE_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PTTRECEIVE_FAIL: {
                    getAppMsgQue().saveUserMsg(sourceId, false, getResources().getString(R.string.string_ptt_message),
                            AppProtUserMsg.USERMSG_PTT_FROM);
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTCRREC: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_PTTRECEIVE_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;
    }

    private int handlePttFloorRequestPrep() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId();
        String tagetId = getAppTargetId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PTTFLOORREQUEST;
        int stateResult = AppState.SM_STATE_PTTFLOORREQUEST_FAIL;
        AppProtMsg appServMsg = null;

        mSessionId = getAppState().getMediaSessionId();
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PTTFLOORREQUEST: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTFLOORREQUEST,
                            userId, tagetId, mSessionId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTFLOORGRANTED;
                    break;
                }
                case AppState.SM_STATE_PTTFLOORGRANTED: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTFLOORGRANT)) {
                            mSessionId = appServMsg.getSessionId();
                            getAppState().setMediaSessionId(mSessionId);
                            // media session Id is aligned between the app and server
                            substate = AppState.SM_STATE_PTTFLOOR_PREPARED;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTFLOORREJECT)) {
                            substate = AppState.SM_STATE_PTTFLOORREQUEST_FAIL;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED;
                            substate = AppState.SM_STATE_PTTFLOORREQUEST_FAIL;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            reasonCode = Constants.ILLEGAL_MSG;
                            substate = AppState.SM_STATE_PTTFLOORREQUEST_FAIL;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTFLOORREQUEST_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;
                }
                case AppState.SM_STATE_PTTFLOOR_PREPARED: {
                    UiHelpers.playNotificationSound(mActivity);
                    stateResult = AppState.SM_STATE_PTTFLOOR_PREPARED;
                    if (!mRunningAudioTask) {
                        mRunningAudioTask = true;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                                mPttRecordingTask = new ProcessorPttAsyncTask(mActivity,
                                        mPttChannel, getAppState().getMediaSessionId());
                                mPttRecordingTask.execute();
                            }
                        });
                    }
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PTTFLOORREQUEST_FAIL: {
                    stateResult = AppState.SM_STATE_PTTFLOORREQUEST_FAIL;
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTTRANS: reason code#" + reasonCode);
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }

    private int handlePttFloorRequestComplete() {
        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId();
        String tagetId = getAppTargetId();

        if (mRunningAudioTask) {
            if (null != mPttRecordingTask && AsyncTask.Status.FINISHED != mPttRecordingTask.getStatus())
                mPttRecordingTask.cancel(false);
            mRunningAudioTask = false;
        }
        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTFLOORRELEASE,
                userId, tagetId, mSessionId);
        getAppState().updateKeepAliveSent();
        getAppState().incAppSeqNum();

        return AppState.SM_STATE_PTTTRANSMIT_SUCCESS;
    }

    private int handlePttFloorReceive(AppProtMsg appServMsg) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = appServMsg.getSeqNum();
        String sourceId = appServMsg.getMsgSrc();
        String userIdDst = appServMsg.getMsgDst();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PTTFLOORANNOUNCE;
        int stateResult = AppState.SM_STATE_PTTFLOORANNOUNCE_FAIL;

        mSessionId = appServMsg.getSessionId();
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PTTFLOORANNOUNCE: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_ACK,
                            userIdDst, sourceId, mSessionId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTFLOORRECEIVE_STARTED;
                    break;
                }
                case AppState.SM_STATE_PTTFLOORRECEIVE_STARTED: {
                    readAndPlayPttFromServer(true);
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PTTRECEIVE_STOP;
                    break;
                }
                case AppState.SM_STATE_PTTRECEIVE_STOP: {
                    if (null != (appServMsg = getAppMsgQue().readMsgFromServActiveQue())) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_PTTFLOORFREE))
                            substate = AppState.SM_STATE_PTTFLOORRECEIVE_SUCCESS;
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED2;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG2;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG2;
                    }
                    break;      // continue to wait with timer
                }
                case AppState.SM_STATE_PTTFLOORRECEIVE_SUCCESS: {
                    stateResult = AppState.SM_STATE_PTTRECEIVE_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PTTFLOORANNOUNCE_FAIL: {
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PTTFREC: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_PTTRECEIVE_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;
    }

    private int handlePttEnd() {
        short seqNum = getAppState().getAppSeqNum();
        String userId = getAppUserId(); // getAppUsers().get(0).getAppUserId();
        String targetId = getAppTargetId();
        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PTTCALLEND, userId, targetId, mSessionId);
        return 1;
    }



    /*
     This version acts on the receiving queue in real time with the PTT jitter wait time
     */
    public void readAndPlayPttFromServer(final boolean flagWait) {
        Thread completePlay = new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                mPttChannel.getAudioPlayer().play();
                doWritePlayerContinuous();
                mPttChannel.getAudioPlayer().stop();
            }
        });
        completePlay.start();
        try {
            if (flagWait)
                completePlay.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }       // make sure audio is completed before returning
    }

    private void doWritePlayerContinuous() {
        final int TIMER_ATTEMPTS_TIMES =3;

        int buffersize =mPttChannel.getBuffersize();
        byte[] audioBuf = new byte[buffersize];
        int bufPos, bufToFill, bufFillSize;
        int offsetData, dataSize;
        boolean workDone = false;
        boolean firsTime = true;
        AppProtData appData;
        TimerControl timerControl = new TimerControl();

        appData = null;
        offsetData = 0;
        bufPos = 0;
        while (!workDone) {
            bufToFill = buffersize;
            while (0 < bufToFill) {
                if (null == appData) {
                    timerControl.setTimer((firsTime) ? 10000
                            : TimerControl.TIMER_PTTJITTERWAIT);
                    appData = mPttChannel.getAppDataCh().readDataFromServQue(mSessionId);
                    while ((null == appData || !appData.isValid())
                            && !timerControl.isTimeout()) {
                        try { sleep(100); } catch (InterruptedException e) {
                            e.printStackTrace(); }
                        appData = mPttChannel.getAppDataCh().readDataFromServQue(mSessionId);
                    }
                    if (null != appData && appData.isValid()) {
                        firsTime = false;
                        offsetData = 0;
                        if (Constants.DEBUGGING) {
                            Log.i("ProtDataTag", "Play: at " + System.currentTimeMillis() + " <> "
                                    + "sessionId: " + appData.getSessionId()
                                    + " SeqNum: " + Short.toString(appData.getSeqNum())
                                    + " Data Len: " + appData.getDataSize()
                                    + " Raw Len: " + appData.getDataSize());
                        }
                    }
                    else {
                        workDone = true;        // no more audio data to play
                        break;
                    }
                }
                dataSize = appData.getDataSize() - offsetData;
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
            if (0< bufPos) {
                mPttChannel.getAudioPlayer().write(audioBuf, 0, bufPos);      // play the audio
                bufPos = 0;
            }
        }
    }


    public static float sampleAveragesByAmp(byte[] byteSamples, int numSamples) {
        return sampleAverages(byteSamples, numSamples, AVERAGES_BY_AMP);
    }

    public static float sampleAveragesByPower(byte[] byteSamples, int numSamples) {
        return sampleAverages(byteSamples, numSamples, AVERAGES_BY_POWER);
    }

    public static float sampleAverages(byte[] byteSamples, int numSamples, int method) {
        float totalAbsValue = 0.0f;
        short sample = 0;

        for (int i = 0; i < byteSamples.length; i += 2) {
            sample = (short) ((byteSamples[i]) | byteSamples[i + 1] << 8);
            switch (method) {
                case AVERAGES_BY_POWER: {
                    totalAbsValue += (float) Math.abs(sample * sample);
                    break;
                }
                case AVERAGES_BY_AMP:
                default: {
                    totalAbsValue += (float) Math.abs(sample);
                    break;
                }
            }
        }
        return totalAbsValue = totalAbsValue / (float) (numSamples / 2);
    }

    public static boolean isPttChatSpeaking(final float[] ampSamples) {
        float totalSamples = 0.0f;

        for (int i = 0; i < ampSamples.length; i++)
            totalSamples += ampSamples[i];
        totalSamples = totalSamples / (float) (ampSamples.length);

        if (totalSamples > AUDIO_AMP_THRESHOLD)
            return true;
        else
            return true;
    }
}
