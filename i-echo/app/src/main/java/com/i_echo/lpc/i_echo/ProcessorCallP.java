package com.i_echo.lpc.i_echo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.i_echo.lpc.i_echo.Audio.Audio;
import com.i_echo.lpc.i_echo.Utils.UtilsHelpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 7/4/2015.
 */

public class ProcessorCallP implements Runnable {
    private ActivityMain mActivity;
    private AppState mAppState;
    private AppProtMsgQueue mAppMsgQue;
    private ArrayList<ContactInfoUser> mAppUsers;
    private ActivityMainUiManager mUiHolder;
    ICallProcListener mCallback;

    private long t1_timePolling;
    private AppProtMsg appServMsg;

    public ProcessorCallP(final ActivityMain activity) {
        mActivity = activity;
        mAppState =mActivity.getAppState();
        mAppMsgQue =mActivity.getAppMsgQue();
        mAppUsers = mActivity.getAppUserAndTarget();
        mUiHolder = mActivity.getUiHolder();

        mCallback = (ICallProcListener)activity;

        t1_timePolling = System.currentTimeMillis();
        appServMsg = null;
    }

    // implement this interface for the Activity to communicate with the fragment
    //       for information related to the count of the (ordering selected) items
    public interface ICallProcListener {
        public void onCallProcFinish(int returnCode);
        public void onCmAvailable(ArrayList<CmMatch> cmItems);
        public void notifyDisplayListUpdate(String userId);
    }

    private void showCmSelection(ArrayList<CmMatch> cmItems) {
        mCallback.onCmAvailable(cmItems);
    }

    private void showDisplayListUpdate(String userId) {
        mCallback.notifyDisplayListUpdate(userId);
    }

    protected AppState getAppState() {
        return mAppState;
    }

    protected ActivityMainUiManager getUiHolder() {
        return mUiHolder;
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

    private void resetPolling() {
        t1_timePolling = System.currentTimeMillis();
    }

    private boolean isPollingTime() {
        return ((System.currentTimeMillis() - t1_timePolling)
                >= TimerControl.MAX_TIMEPOLLING);
    }

    @Override
    public void run() {
        resetPolling();

        int result =  doAppCallP();
        mCallback.onCallProcFinish(result);     // doAppFinish();
    }

    private int doAppCallP() {
        int returnCode =Constants.APP_FINISH_NORMAL;

        int numTryRegister = 3;
        int numTryDeregister = 3;

        while (getAppState().isSyncRunning(AppState.APP_STATE_LOCK)
                && getAppMsgQue().isConnected()) {
            switch (getAppState().getSyncSmState(AppState.APP_STATE_LOCK)) {
                case AppState.SM_STATE_WAITINGFORUI_PROBEMATCH:
                case AppState.SM_STATE_WAITINGFORUI_CMSELECTION:
                case AppState.SM_STATE_WAITINGFORUI_TEXTMSGTRANSMIT:
                case AppState.SM_STATE_WAITINGFORUI_VOICEMSGTRANSMIT: {
                    getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                    break;
                }
                case AppState.SM_STATE_READY: {
                    if (!isPollingTime())
                        getAppState().isSyncRunning(AppState.APP_STATE_UNLOCK);
                    else if ((appServMsg = getAppMsgQue().readMsgFromServPendedQue()) != null) {
                        getAppState().setSmState(handleMsgFromServer(appServMsg.getMsgType()));
                        resetPolling();
                    } else if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        getAppState().setSmState(handleMsgFromServer(appServMsg.getMsgType()));
                        resetPolling();
                    } else {
                        getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                        // turn to UI actions that may be pending
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        resetPolling();
                    }
                    if (getAppState().isSendKeepAlive()) {
                        short seqNum = getAppState().getAppSeqNum();
                        String userId = getAppUserId(); // getAppUsers().get(0).getAppUserId();
                        String serverId = getAppState().getAppServerId();
                        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_KEEPALIVE, userId, serverId);
                        getAppState().updateKeepAliveSent();
                    }
                    break;
                }
                case AppState.SM_STATE_REGISTRATION: {
                    int stateResult;
                    numTryRegister--;
                    stateResult = handleClientRegistration();
                    if (stateResult == AppState.SM_STATE_REGISTERFAIL) {
                        if (numTryRegister <= 0) {
                            getAppState().setRunning(false);
                            returnCode =Constants.APP_REGISTRATION_FAILED;
                        }
                    }
                    break;
                }
                case AppState.SM_STATE_DEREGISTER: {
                    int stateResult;
                    numTryDeregister--;
                    stateResult = handleClientDeregister();
                    if (stateResult == AppState.SM_STATE_DEREGISTERSUCCESS) {
                        getAppState().setRunning(false);
                        returnCode = Constants.APP_DEREGISTRATION_SUCCESS;
                    }
                    else if (numTryDeregister <= 0) {
                        returnCode = Constants.APP_DEREGISTRATION_FAILED;
                        getAppState().setRunning(false);
                    }
                    break;
                }
                case AppState.SM_STATE_PROBEREQUESTTRYING: {
                    int stateResult = handleProbeTrying();
                    if (stateResult == AppState.SM_STATE_PROBEREQUESTSUCCESS)
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_CMSELECTION);
                    else getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRECEIVED: {
                    int stateResult = handleProbeReqReceived(appServMsg, getAppState().getUserState(),
                            getAppTarget());       // appUsers.get(1));       // the other party info
                    if (stateResult == AppState.SM_STATE_PROBEMATCHSUCCESS)
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_PROBEMATCH);
                    else
                        getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRESP_PHONEIN: {
                    handleProbeReceivedResponseManual(appServMsg, CmIdxItems.CM_TYPE_PHONE, CmIdxItems.DIRECTION_CALLIN);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRESP_VOIPIN: {
                    handleProbeReceivedResponseManual(appServMsg, CmIdxItems.CM_TYPE_VOIP, CmIdxItems.DIRECTION_CALLIN);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRESP_PTTIN: {
                    handleProbeReceivedResponseManual(appServMsg, CmIdxItems.CM_TYPE_PTT, CmIdxItems.DIRECTION_CALLIN);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PTTCALLANNOUNCE: {
                    getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                    handlePttReceiveSession(appServMsg);
                    getAppState().getSyncSmState(AppState.APP_STATE_LOCK);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PTTCALLREQUEST: {
                    getAppState().getSyncSmState(AppState.APP_STATE_UNLOCK);
                    handlePttRequestSession();
                    getAppState().getSyncSmState(AppState.APP_STATE_LOCK);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRESP_VOICEMSG_ACTIVEIN: {
                    handleProbeReceivedResponseManual(appServMsg, CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE, CmIdxItems.DIRECTION_CALLIN);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRESP_VOICEMSG_SILENTIN: {
                    handleProbeReceivedResponseManual(appServMsg, CmIdxItems.CM_TYPE_VOICEMSG_SILENT, CmIdxItems.DIRECTION_CALLIN);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PROBEREQRESP_TEXTMSGIN: {
                    handleProbeReceivedResponseManual(appServMsg, CmIdxItems.CM_TYPE_TEXTMSG, CmIdxItems.DIRECTION_CALLIN);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_TEXTMSGSELECTED: {
                    int stateResult = handleTextMsgTransmitPrep();
                    if (stateResult == AppState.SM_STATE_TEXTMSGTRANSMIT_PREPARED)
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_TEXTMSGTRANSMIT);
                    else getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_TEXTMSGTRANSMIT_COMPLETE: {
                    int stateResult = handleTextMsgTransmitComplete();
                    if (stateResult == AppState.SM_STATE_TEXTMSGTRANSMIT_PREPARED)
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_TEXTMSGTRANSMIT);
                    else getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_VOIPSELECTED: {
                    handleVoipCall();
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_PHONESELECTED: {
                    handlePhoneCall();
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_EMAILSELECTED: {
                    handleEmail();
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_TEXTMSGRECEIVED: {
                    int stateResult = handleTextMsgReceive(appServMsg);
                    if (stateResult == AppState.SM_STATE_TEXTMSGRECEIVE_SUCCESS)
                        getAppState().setSmState(AppState.SM_STATE_READY);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_VOICEMSG_ACTIVESELECTED: {
                    int stateResult = handleVoiceMsgActiveTransmitPrep();
                    if (stateResult == AppState.SM_STATE_VOICEMSGTRANSMIT_PREPARED)
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_VOICEMSGTRANSMIT);
                    else getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_VOICEMSG_SILENTSELECTED: {
                    int stateResult = handleVoiceMsgSilentTransmitPrep();
                    if (stateResult == AppState.SM_STATE_VOICEMSGTRANSMIT_PREPARED)
                        getAppState().setSmState(AppState.SM_STATE_WAITINGFORUI_VOICEMSGTRANSMIT);
                    else getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_COMPLETE: {
                    handleVoiceMsgTransmitComplete();
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_VOICEMSGACTIVERECEIVED: {
                    handleVoiceMsgActiveReceive(appServMsg);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
                case AppState.SM_STATE_VOICEMSGSILENTRECEIVED: {
                    handleVoiceMsgSilentReceive(appServMsg);
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    break;
                }
            }
            if (getAppState() != null && !getAppState().isServerAlive()) {
                returnCode =Constants.APP_KEEPALIVE_TIMEOUT;
                break;
            }
        }
        return returnCode;
    }

    private int handlePttRequestSession() {

        new ProcessorPttChat(mActivity).doPttCallP();
        getUiHolder().showUIToastMsg(getResources().getString(R.string.string_chatcallcompleted));
        showDisplayListUpdate(getAppTargetId());
        getUiHolder().showUIDefaultEcho();

        return AppState.SM_STATE_PTTTRANSMIT_SUCCESS;
    }

    private int handlePttReceiveSession(AppProtMsg appMsg) {

        new ProcessorPttChat(mActivity, appMsg).doPttCallP();
        getUiHolder().showUIToastMsg(getResources().getString(R.string.string_chatcallcompleted));
        showDisplayListUpdate(getAppTargetId());
        getUiHolder().showUIDefaultEcho();

        return AppState.SM_STATE_PTTTRANSMIT_SUCCESS;
    }

    private int handleMsgFromServer(byte msgType) {

        switch (msgType) {
            case AppProtMsg.MSG_KEEPALIVE:
                return AppState.SM_STATE_READY;
            case AppProtMsg.MSG_PROBEREQUEST:
                return AppState.SM_STATE_PROBEREQRECEIVED;
            case AppProtMsg.MSG_PTTCALLANNOUNCE:
                return AppState.SM_STATE_PTTCALLANNOUNCE;
            case AppProtMsg.MSG_TEXTMSGRECEIVE:
                return AppState.SM_STATE_TEXTMSGRECEIVED;
            case AppProtMsg.MSG_VOICEMSGACTIVERECEIVEANNOUNCE:
                return AppState.SM_STATE_VOICEMSGACTIVERECEIVED;
            case AppProtMsg.MSG_VOICEMSGSILENTRECEIVEANNOUNCE:
                return AppState.SM_STATE_VOICEMSGSILENTRECEIVED;
            default:
                return AppState.SM_STATE_READY;
        }
    }

    private int handleClientDeregister() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        AppProtMsg appServMsg;
        short seqNum = getAppState().getAppSeqNum();
        String serverId = getAppState().getAppServerId();
        String userIdSrc = getAppUserId();     // mAppUsers.get(0).getAppUserId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_DEREGISTER;
        int stateResult = AppState.SM_STATE_DEREGISTERFAIL;

        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_DEREGISTER: {
                    getAppMsgQue().sendMsgToServer(seqNum,
                            AppProtMsg.MSG_DEREGISTER, userIdSrc, serverId, getAppState());
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_DEREGISTERING;
                    break;
                }
                case AppState.SM_STATE_DEREGISTERING: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_ACK)) {
                            substate = AppState.SM_STATE_DEREGISTERSUCCESS;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_DEREGISTERFAIL;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_DEREGISTERFAIL;
                            reasonCode = Constants.ILLEGAL_MSG;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_DEREGISTERFAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;      // continue to wait with timer
                }
                case AppState.SM_STATE_DEREGISTERSUCCESS: {
                    stateResult = AppState.SM_STATE_DEREGISTERSUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_DEREGISTERFAIL: {
                    // no need to send a NAK to the server in this case
                    stateResult = AppState.SM_STATE_DEREGISTERFAIL;
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "DEREG: reason code#" + reasonCode);
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }

    private int handleClientRegistration() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        AppProtMsg appServMsg;
        short seqNum = getAppState().getAppSeqNum();
        String serverId = getAppState().getAppServerId();
        String userIdSrc = getAppUserId();     // mAppUsers.get(0).getAppUserId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_REGISTRATION;
        int stateResult = AppState.SM_STATE_REGISTERFAIL;

        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_REGISTRATION: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_REGISTER,
                            userIdSrc, serverId, getAppState());
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_REGISTERING;
                    break;
                }
                case AppState.SM_STATE_REGISTERING: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_ACK)) {
                            substate = AppState.SM_STATE_REGISTERSUCCESS;
                        } else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_REGISTERFAIL;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            byte msgType = appServMsg.getMsgType();
                            // msg from the server not handled in current SM state
                            substate = AppState.SM_STATE_REGISTERFAIL;
                            reasonCode = Constants.ILLEGAL_MSG;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_REGISTERFAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;      // continue to wait with timer
                }
                case AppState.SM_STATE_REGISTERSUCCESS: {
                    getUiHolder().showUIDefaultEcho();
                    stateResult = AppState.SM_STATE_REGISTERSUCCESS;
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_REGISTERFAIL: {
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_notregistered));
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "REG: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_REGISTERFAIL;
                    getAppState().setSmState(AppState.SM_STATE_READY);
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }

    private int handleProbeTrying() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNumUser = getAppState().getAppSeqNum();
        boolean[] userState = getAppState().getUserState();
        String userIdSrc = getAppUserId();      // mAppUsers.get(0).getAppUserId();
        String targetId = getAppTargetId();     // mAppUsers.get(1).getAppUserId();   // String serverId = getAppState().getAppServerId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PROBEREQUESTTRYING;
        int stateResult = AppState.SM_STATE_PROBEREQUESTFAIL;
        AppProtMsg appServMsg = null;

        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PROBEREQUESTTRYING: {
                    int[] cmList = IntelliMethods.getProposedCmListAtSrc(userState);
                    getAppMsgQue().sendMsgToServer(seqNumUser, AppProtMsg.MSG_TRYPROBE,
                            userIdSrc, targetId, cmList, null);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_PROBERESPONSE);
                    substate = AppState.SM_STATE_PROBEREQUESTRESPONSE;
                    break;
                }
                case AppState.SM_STATE_PROBEREQUESTRESPONSE: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNumUser, AppProtMsg.MSG_TRYRESPONSE))
                            substate = AppState.SM_STATE_PROBEREQUESTCONFIRM;
                        else if (appServMsg.equalTo(seqNumUser, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_PROBEREQUESTABANDONED;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_PROBEREQUESTABANDONED;
                            reasonCode = Constants.ILLEGAL_MSG;
                        }           // pend msg from the server not handled in current SM state
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PROBEREQUESTABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;          // else - just continue the loop until timeout
                }
                case AppState.SM_STATE_PROBEREQUESTCONFIRM: {
                    getAppMsgQue().sendMsgToServer(seqNumUser, AppProtMsg.MSG_ACK,
                            userIdSrc, targetId, getAppState());
                    getAppState().updateKeepAliveSent();
                    substate = AppState.SM_STATE_CMACTION;
                    break;
                }
                case AppState.SM_STATE_PROBEREQUESTABANDONED: {
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_nocalledparty));
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PROBE: reason code#" + reasonCode);
                    substate = AppState.SM_STATE_PROBEREQUESTFAIL;
                    break;
                }
                case AppState.SM_STATE_CMACTION: {
                    // setCmSelection2(appServMsg);
                    showCmSelection(appServMsg.getCmMatches());
                    UiHelpers.playNotificationSound(mActivity);
                    getUiHolder().showUICmReadyList();
                    substate = AppState.SM_STATE_PROBEREQUESTSUCCESS;
                    break;
                }
                case AppState.SM_STATE_PROBEREQUESTSUCCESS: {
                    stateResult = AppState.SM_STATE_PROBEREQUESTSUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PROBEREQUESTFAIL: {
                    stateResult = AppState.SM_STATE_PROBEREQUESTFAIL;
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }


    private int handleProbeReqReceived(AppProtMsg appServMsg, boolean[] userState,
                                       ContactInfoUser appUser) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        int cmNum = appServMsg.getNumCM();
        int[] cmProposals = appServMsg.getCmList();
        String userId = appServMsg.getMsgSrc();
        ArrayList<CmMatch> cms =
                IntelliMethods.getMatchedCmListAtDst(cmProposals, cmNum, userState, appUser);

        if (!cms.isEmpty()) {
            if (userState[AppState.APP_USERSTATE_AUTO]) {
                handleProbeReceivedResponseAuto(appServMsg, cms);
                return AppState.SM_STATE_PROBEMATCHAUTO;
            } else {
                // setMatchedCMList2(cms);
                showCmSelection(cms);
                UiHelpers.vibrateNotification(mActivity);
                UiHelpers.playNotificationAndSound(mActivity, getResources().getString(R.string.string_title),
                        getResources().getString(R.string.string_cmannounce) + "\"" + userId + "\"");
                getUiHolder().showUICmSelectionList(userId);
                return AppState.SM_STATE_PROBEMATCHSUCCESS;
            }
        } else {
            reasonCode = Constants.EMPTYLIST_RESULT;
            if (Constants.DEBUGGING)
                Log.i(LOGTAG, "PROBEREC: reason code#" + reasonCode);
            return AppState.SM_STATE_PROBEREQMATCH_FAIL;
        }
    }

    private int handleProbeReceivedResponseAuto(AppProtMsg appServMsg,
                                                ArrayList<CmMatch> cms) {
        int[] cmList = IntelliMethods.getCmListFromCmArray(cms);
        String[] cmInfoStrings = IntelliMethods.getCmInfoFromCmArray(cms);

        if (cmList != null && cmInfoStrings != null
                && cmList.length == cmInfoStrings.length) {
            return handleProbeReceivedResponseBase(appServMsg, cmList, cmInfoStrings);
        } else
            return AppState.SM_STATE_PROBERESPFAIL;
    }


    private int handleProbeReceivedResponseManual(AppProtMsg appServMsg, int cmMethod,
                                                  int direction) {
        getUiHolder().showUIDefaultEcho();
        if (direction == CmIdxItems.DIRECTION_CALLIN) {
            return handleProbeReceivedResponseIN(appServMsg, cmMethod);
        } else if (direction == CmIdxItems.DIRECTION_CALLOUT) {
            // return handleProbeReceivedResponseOUT(appServMsg, cmMethod);
            return 0;
        } else return 0;
    }

    private int handleProbeReceivedResponseIN(AppProtMsg appServMsg, int cmMethod) {
        int[] cmList = IntelliMethods.getCmListResponseAtDst(cmMethod);
        String[] cmInfoStrings = IntelliMethods.getCmInfoStrings(cmList, getAppUser());     // mAppUsers.get(0));

        if (cmList != null && cmInfoStrings != null
                && cmList.length == cmInfoStrings.length) {
            return handleProbeReceivedResponseBase(appServMsg, cmList, cmInfoStrings);
        } else
            return AppState.SM_STATE_PROBERESPFAIL;
    }

    private int handleProbeReceivedResponseBase(AppProtMsg appServMsg,
                                                int[] cmList, String[] cmInfoStrings) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = appServMsg.getSeqNum();
        String serverId = getAppState().getAppServerId();
        String userIdSrc = getAppUserId();      // mAppUsers.get(0).getAppUserId();
        String targetId = appServMsg.getMsgSrc();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_PROBERECEIVERESPINIT;
        int stateResult = AppState.SM_STATE_PROBERESPFAIL;
        AppProtMsg appMsg = null;

        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_PROBERECEIVERESPINIT: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_PROBERESPWITHCM,
                            userIdSrc, targetId, cmList, cmInfoStrings);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_PROBERESPCONFIRM;
                    break;
                }
                case AppState.SM_STATE_PROBERESPCONFIRM: {
                    if ((appMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appMsg.equalTo(seqNum, AppProtMsg.MSG_ACK))
                            substate = AppState.SM_STATE_PROBERESPSUCCESS;
                        else if (appMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_PROBERESPFAIL;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appMsg)) {
                            substate = AppState.SM_STATE_PROBERESPFAIL;
                            reasonCode = Constants.ILLEGAL_MSG;
                        }           // pend msg from the server not handled in current SM state
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_PROBERESPFAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;          // else - just continue the loop until timeout
                }
                case AppState.SM_STATE_PROBERESPSUCCESS: {
                    stateResult = AppState.SM_STATE_PROBERESPSUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_PROBERESPFAIL: {
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "PRECRESP: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_PROBERESPFAIL;
                    continuing = false;
                    break;
                }
            }
        }

        return stateResult;
    }

    private int handleProbeReceivedResponseOUT(AppProtMsg appServerMsg, int cmMethod) {
        switch (cmMethod) {
            case AppState.SM_STATE_PROBEREQRESP_PHONEOUT:
            case AppState.SM_STATE_PROBEREQRESP_VOIPOUT:
            case AppState.SM_STATE_PROBEREQRESP_VOICEMSG_ACTIVEOUT:
            case AppState.SM_STATE_PROBEREQRESP_VOICEMSG_SILENTOUT:
            case AppState.SM_STATE_PROBEREQRESP_TEXTMSGOUT:
            default:
                break;
        }
        return 1;
    }

    private int handleVoipCall() {
        boolean result = false;

        if (getAppState().getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT]) {
            String skypeurl = getAppTargetId();     // mAppUsers.get(1).getAppUserVoipUrl();
            int index = skypeurl.indexOf('@');
            String skypeid = (index == -1) ? skypeurl
                    : skypeurl.substring(0, index).replaceAll("[-()]", "").trim();

            Intent skypeIntent = new Intent(Intent.ACTION_VIEW);
            skypeIntent.setData(Uri.parse("skype:" + skypeid + "?call"));
            skypeIntent.setComponent(new ComponentName(getResources().getString(R.string.string_skypeurl),
                    getResources().getString(R.string.string_skypemain)));
            skypeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(skypeIntent);
            result = true;
        } else {
            getUiHolder().showUIToastMsg(getResources().getString(R.string.string_voipnotsupported));
        }
        String targetId = getAppTargetId();     // mAppUsers.get(1).getAppUserId();
        getAppMsgQue().saveUserMsg(targetId, result, getResources().getString(R.string.string_skype_call), AppProtUserMsg.USERMSG_VOIP_TO);
        getUiHolder().showUIDefaultEcho();
        showDisplayListUpdate(targetId);

        return AppState.SM_STATE_READY;
    }


    private int handlePhoneCall() {
        boolean result = false;

        if (getAppState().getUserState()[AppState.APP_USERSTATE_PHONESUPPORT]) {
            String telNumber = getAppTarget().getAppUserMsdn().replaceAll("[-()]", "").trim();
            Intent callIntent = new Intent(Intent.ACTION_DIAL);

            callIntent.setData(Uri.parse("tel:" + Uri.encode(telNumber)));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(callIntent);
            result = true;
        } else {
            getUiHolder().showUIToastMsg(getResources().getString(R.string.string_phonenotsupported));
        }
        String targetId = getAppTargetId();
        getAppMsgQue().saveUserMsg(targetId, result, getResources().getString(R.string.string_phone_call),
                AppProtUserMsg.USERMSG_PHONE_TO);
        getUiHolder().showUIDefaultEcho();
        showDisplayListUpdate(targetId);

        return AppState.SM_STATE_READY;
    }

    private int handleEmail() {
        boolean result = false;

        if (getAppState().getUserState()[AppState.APP_USERSTATE_EMAILSUPPORT]) {
            String mailToAddr = getAppTarget().getAppUserEmailUrl();
            final Intent emailIntent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
            String subjectString = getResources().getString(R.string.string_messagefrom)
                    + getAppUser().getNameFirst() + " " + getAppUser().getNameLast();
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.setType("plain/text");  // emailIntent.setType("message/rfc822");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{mailToAddr});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subjectString);
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.string_myemail));

                /*
                Formatted for HTML
                emailIntent.putExtra(
                     Intent.EXTRA_TEXT,
                     Html.fromHtml(new StringBuilder()
                         .append("<p><b>Some Content</b></p>")
                         .append("<a>http://www.google.com</a>")
                         .append("<small><p>More content</p></small>")
                         .toString())
                     );*/
            mActivity.startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.string_sendemail)));
            result = true;
        } else
            getUiHolder().showUIToastMsg(getResources().getString(R.string.string_emailnotsupported));
        String targetId = getAppTargetId();
        getAppMsgQue().saveUserMsg(getAppTargetId(), result, getResources().getString(R.string.string_email_message),
                AppProtUserMsg.USERMSG_EMAIL_TO);
        getUiHolder().showUIDefaultEcho();
        showDisplayListUpdate(targetId);
        return AppState.SM_STATE_READY;
    }

    private int handleTextMsgTransmitPrep() {
        getUiHolder().showUITextMsgSend();
        return AppState.SM_STATE_TEXTMSGTRANSMIT_PREPARED;
    }

    private int handleTextMsgTransmitComplete() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userIdSrc = getAppUserId();
        String targetId = getAppTargetId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_TEXTMSGTRANSMIT;
        int stateResult = AppState.SM_STATE_TEXTMSGTRANSMIT_FAIL;
        AppProtMsg appServMsg = null;


        String msgBody = getUiHolder().mEditMsgView.getText().toString().trim();
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_TEXTMSGTRANSMIT: {
                    if (msgBody.length() > AppProtMsg.MAX_MSGSTRINGLENGTH)
                        msgBody = msgBody.substring(0, AppProtMsg.MAX_MSGSTRINGLENGTH);
                    if (msgBody.length() == 0) {
                        getUiHolder().showUIToastMsg(getResources().getString(R.string.string_typemessage));
                        return handleTextMsgTransmitPrep();         // make sure non-zero byte msg
                    } else {
                        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_TEXTMSG,
                                userIdSrc, targetId, msgBody);
                        getAppState().updateKeepAliveSent();
                        substate = AppState.SM_STATE_TEXTMSGTRANSMIT_CONFIRM;
                        timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    }
                    break;
                }
                case AppState.SM_STATE_TEXTMSGTRANSMIT_CONFIRM: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(getAppState().getAppSeqNum(), AppProtMsg.MSG_ACK))
                            substate = AppState.SM_STATE_TEXTMSGTRANSMIT_SUCCESS;
                        else if (appServMsg.equalTo(getAppState().getAppSeqNum(), AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG;
                        } // msg ignored if not handled in this tate
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;          // else - just continue the loop until timeout
                }
                case AppState.SM_STATE_TEXTMSGTRANSMIT_SUCCESS: {
                    // TODO: log the message content in memory for user record
                    // showUIDefaultEcho();
                    getAppMsgQue().saveUserMsg(targetId, true, msgBody, AppProtUserMsg.USERMSG_TEXT_TO);
                    showDisplayListUpdate(targetId);
                    getUiHolder().showUIDefaultEcho();
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_messagesent));
                    stateResult = AppState.SM_STATE_TEXTMSGTRANSMIT_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_TEXTMSGTRANSMIT_FAIL: {
                    getAppMsgQue().saveUserMsg(targetId, false, msgBody, AppProtUserMsg.USERMSG_TEXT_TO);
                    showDisplayListUpdate(targetId);
                    getUiHolder().showUIDefaultEcho();
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_messagenotsent));
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "TTRANS: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }


    private int handleTextMsgReceive(AppProtMsg appServMsg) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = appServMsg.getSeqNum();
        String sourceId = appServMsg.getMsgSrc();
        String userIdSrc = appServMsg.getMsgDst();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_TEXTMSGRECEIVED;
        int stateResult = AppState.SM_STATE_TEXTMSGRECEIVE_FAIL;
        AppProtMsg appMsg = null;

        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_TEXTMSGRECEIVED: {
                    String msgString = appServMsg.getMsgText();
                    getAppMsgQue().saveUserMsg(sourceId, true, msgString, AppProtUserMsg.USERMSG_TEXT_FROM);
                    showDisplayListUpdate(sourceId);
                    substate = AppState.SM_STATE_TEXTMSGRECEIVE_RESPONSE;
                    break;
                }
                case AppState.SM_STATE_TEXTMSGRECEIVE_RESPONSE: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_TEXTMSGCONFIRM,
                            userIdSrc, sourceId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_TEXTMSGRECEIVE_CONFIRM;
                    break;
                }
                case AppState.SM_STATE_TEXTMSGRECEIVE_CONFIRM: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_ACK))
                            substate = AppState.SM_STATE_TEXTMSGRECEIVE_SUCCESS;
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_TEXTMSGRECEIVE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_TEXTMSGRECEIVE_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_TEXTMSGRECEIVE_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;
                }
                case AppState.SM_STATE_TEXTMSGRECEIVE_SUCCESS: {
                    getUiHolder().showUIDefaultEcho();
                    stateResult = AppState.SM_STATE_TEXTMSGRECEIVE_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_TEXTMSGRECEIVE_FAIL: {
                    getUiHolder().showUIDefaultEcho();
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "TREC: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_TEXTMSGRECEIVE_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;
    }

    private int handleVoiceMsgActiveTransmitPrep() {

        return handleVoiceMsgTransmitPrepBase(CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE);
    }

    private int handleVoiceMsgSilentTransmitPrep() {

        return handleVoiceMsgTransmitPrepBase(CmIdxItems.CM_TYPE_VOICEMSG_SILENT);
    }

    private int handleVoiceMsgTransmitPrepBase(int voiceMsgMethod) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userIdSrc = getAppUserId();
        String tagetId = getAppTargetId();
        int sessionId = getAppState().getMediaSessionId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_VOICEMSG_ACTIVESELECTED;
        int stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
        AppProtMsg appServMsg = null;

        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_VOICEMSG_ACTIVESELECTED: {
                    byte msgType;
                    switch (voiceMsgMethod) {
                        case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE: {
                            msgType = AppProtMsg.MSG_VOICEMSGACTIVETRANSMITREQUEST;
                            break;
                        }
                        case CmIdxItems.CM_TYPE_VOICEMSG_SILENT:
                        default: {
                            msgType = AppProtMsg.MSG_VOICEMSGSILENTTRANSMITREQUEST;
                            break;
                        }
                    }
                    getAppMsgQue().sendMsgToServer(seqNum, msgType, userIdSrc, tagetId, sessionId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_VOICEMSGACTIVETRANSMIT_START;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGACTIVETRANSMIT_START: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_VOICEMSGTRANSMITCONFIRM))
                            substate = AppState.SM_STATE_VOICEMSGTRANSMIT_STARTED;
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            reasonCode = Constants.ILLEGAL_MSG;
                            substate = AppState.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_STARTED: {
                    sessionId = getAppState().newMediaSession(appServMsg.getSessionId());
                    // media session Id is aligned between the app and server
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_ACK, userIdSrc, tagetId, sessionId);
                    getAppState().updateKeepAliveSent();
                    getUiHolder().showUIVoiceRecording();
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_recordandsend));
                    substate = AppState.SM_STATE_VOICEMSGTRANSMIT_PREPARED;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_ABANDONED: {
                    // getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_NAK,userId, tagetId, sessionId);
                    // getAppState().updateKeepAliveSent();
                    getUiHolder().showUIToastMsg(getResources().getString(R.string.string_calledpartynotreceive));
                    stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_PREPARED: {
                    stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_PREPARED;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL: {
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "VTRANSP: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();

        return stateResult;
    }

    private int handleVoiceMsgTransmitComplete() {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = getAppState().getAppSeqNum();
        String userIdSrc =getAppUserId();
        String targetId =getAppTargetId();
        int sessionId = getAppState().getMediaSessionId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_VOICEMSGTRANSMIT_STOPPING;
        int stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
        AppProtMsg appServMsg = null;


        String fileNameSessionId = UtilsHelpers.fileMediaFormat(targetId, sessionId);
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_VOICEMSGTRANSMIT_STOPPING: {
                    getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_VOICEMSGTRANSMITEND,
                            userIdSrc, targetId);
                    getAppState().updateKeepAliveSent();
                    timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                    substate = AppState.SM_STATE_VOICEMSGTRANSMIT_STOP;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_STOP: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(getAppState().getAppSeqNum(), AppProtMsg.MSG_ACK)) {
                            substate = AppState.SM_STATE_VOICEMSGTRANSMIT_SUCCESS;
                            getUiHolder().showUIDefaultEcho();
                            getUiHolder().showUIToastMsg(getResources().getString(R.string.string_voicemessagesent));
                        } else if (appServMsg.equalTo(getAppState().getAppSeqNum(), AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                            reasonCode = Constants.NAK_RECEIVED;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG;
                        }       // continue to wait with timer
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG;
                    }
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_SUCCESS: {
                    getAppMsgQue().saveUserMsg(targetId, true, getResources().getString(R.string.string_voice_message),
                            AppProtUserMsg.USERMSG_VOICE_TO, fileNameSessionId);
                    stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_SUCCESS;
                    showDisplayListUpdate(targetId);
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL: {
                    getAppMsgQue().saveUserMsg(targetId, false, getResources().getString(R.string.string_voice_message),
                            AppProtUserMsg.USERMSG_VOICE_TO, fileNameSessionId);
                    showDisplayListUpdate(targetId);
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "VTRANSC: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        getAppState().incAppSeqNum();
        return stateResult;
    }

    private int handleVoiceMsgActiveReceive(AppProtMsg appServMsg) {
        int flag = CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE;
        return (handleVoiceMsgReceiveBase(appServMsg, flag));
    }

    private int handleVoiceMsgSilentReceive(AppProtMsg appServMsg) {
        int flag = CmIdxItems.CM_TYPE_VOICEMSG_SILENT;
        return (handleVoiceMsgReceiveBase(appServMsg, flag));
    }

    private int handleVoiceMsgReceiveBase(AppProtMsg appServMsg, int flag) {
        final String LOGTAG = getResources().getString(R.string.string_protomsg);
        int reasonCode = Constants.INIT;

        short seqNum = appServMsg.getSeqNum();
        String sourceId = appServMsg.getMsgSrc();
        String userIdDst = appServMsg.getMsgDst();
        int sessionId = appServMsg.getSessionId();
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppState.SM_STATE_VOICEMSGACTIVERECEIVED;
        int stateResult = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
        AppProtDataQueue appDataQue =
                new AppProtDataQueue(mActivity.getAppServerIpAddress(), mActivity.getServerUdpPort());

        String fileNameSessionId = UtilsHelpers.fileMediaFormat(sourceId, sessionId);
        getAppState().setMediaSessionId(sessionId);
        while (continuing) {
            switch (substate) {
                case AppState.SM_STATE_VOICEMSGACTIVERECEIVED: {
                    if (appDataQue.sendUdpTriggerToServer(AppProtData.UDPTYPE_TRIGGER, sessionId)) {
                        appDataQue.startReceiving();
                        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_VOICEMSGRECEIVEREADY,
                                userIdDst, sourceId, sessionId);
                        getAppState().updateKeepAliveSent();
                        timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                        substate = AppState.SM_STATE_VOICEMSGRECEIVE_CONFIRM;
                    } else {
                        getAppMsgQue().sendMsgToServer(seqNum, AppProtMsg.MSG_VOICEMSGRECEIVEFAIL,
                                userIdDst, sourceId);
                        getAppState().updateKeepAliveSent();
                        substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                        reasonCode = Constants.NOTFOUND_UDP;
                    }
                    break;
                }
                case AppState.SM_STATE_VOICEMSGRECEIVE_CONFIRM: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_ACK))
                            substate = AppState.SM_STATE_VOICEMSGRECEIVE_STARTED;
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED1;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG1;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG1;
                    }
                    break;
                }
                case AppState.SM_STATE_VOICEMSGRECEIVE_STARTED: {
                    if (appDataQue.isConnected()) {
                        getUiHolder().showUIVoiceReceiving(sourceId, flag == CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE);
                        try {
                            sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        File root = android.os.Environment.getExternalStorageDirectory();
                        String fileFullPath = root.getAbsolutePath() + "/echoData/" + fileNameSessionId;
                        BufferedOutputStream bufO = null;
                        try {
                            bufO = new BufferedOutputStream(new FileOutputStream(fileFullPath, true));
                        } catch (IOException e) {
                            getUiHolder().showUIToastMsg(getResources().getString(R.string.string_notsavesdfile));
                            e.printStackTrace();
                        }
                        Audio.readAndPlayVoiceMsgFromServer(getAppState(), bufO, appDataQue,
                                sessionId, flag == CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE);
                        appDataQue.stopReceiving();
                        appDataQue.close();
                        appDataQue = null;
                        if (bufO != null) {
                            try {
                                bufO.flush();
                                bufO.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        getUiHolder().showUIVoiceReceivingDone(flag == CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE);
                        getUiHolder().showUIDefaultEcho();
                        timerControl.setTimer(TimerControl.TIMER_SERVERRESPONSE);
                        substate = AppState.SM_STATE_VOICEMSGRECEIVE_STOP;
                    } else {
                        getAppState().setSmState(AppState.SM_STATE_VOICEMSGRECEIVE_FAIL);
                        reasonCode = Constants.NOTFOUND_UDP;
                    }
                    break;
                }
                case AppState.SM_STATE_VOICEMSGRECEIVE_STOP: {
                    if ((appServMsg = getAppMsgQue().readMsgFromServActiveQue()) != null) {
                        getAppState().updateKeepAliveReceived();
                        if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_VOICEMSGRECEIVEFINISH))
                            substate = AppState.SM_STATE_VOICEMSGRECEIVE_SUCCESS;
                        else if (appServMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                            substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                            reasonCode = Constants.NAK_RECEIVED2;
                        } else if (!getAppMsgQue().pendMsgIncomingFromServ(appServMsg)) {
                            substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                            reasonCode = Constants.ILLEGAL_MSG2;
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG2;
                    }
                    break;      // continue to wait with timer
                }
                case AppState.SM_STATE_VOICEMSGRECEIVE_SUCCESS: {
                    getAppMsgQue().saveUserMsg(sourceId, true, getResources().getString(R.string.string_voice_message),
                            AppProtUserMsg.USERMSG_VOICE_FROM, fileNameSessionId);
                    showDisplayListUpdate(sourceId);
                    stateResult = AppState.SM_STATE_VOICEMSGRECEIVE_SUCCESS;
                    continuing = false;
                    break;
                }
                case AppState.SM_STATE_VOICEMSGRECEIVE_FAIL: {
                    getAppMsgQue().saveUserMsg(sourceId, false, getResources().getString(R.string.string_voice_message),
                            AppProtUserMsg.USERMSG_VOICE_FROM, fileNameSessionId);
                    getUiHolder().showUIDefaultEcho();
                    showDisplayListUpdate(sourceId);
                    if (Constants.DEBUGGING)
                        Log.i(LOGTAG, "VREC: reason code#" + reasonCode);
                    stateResult = AppState.SM_STATE_VOICEMSGRECEIVE_FAIL;
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;
    }
}