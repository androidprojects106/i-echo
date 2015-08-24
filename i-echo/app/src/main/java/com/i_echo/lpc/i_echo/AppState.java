package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import com.i_echo.lpc.i_echo.Audio.Audio;
import com.i_echo.lpc.i_echo.Audio.AudioParams;

import java.util.LinkedList;
import java.util.Random;

/**
 * Created by LPC-Home1 on 3/14/2015.
 */
public class AppState {

    public static final int APP_USERSTATE_NONE =-1;
    public static final int APP_USERSTATE_ROAMING =0;
    public static final int APP_USERSTATE_WIFI =1;
    public static final int APP_USERSTATE_WIFI_NO =100;
    public static final int APP_USERSTATE_BUSY =2;
    public static final int APP_USERSTATE_DRIVING =3;
    public static final int APP_USERSTATE_SLEEPING =4;
    public static final int APP_USERSTATE_AUTO =5;
    public static final int APP_USERSTATE_RESERVED =6;
    public static final int APP_USERSTATE_PHONESUPPORT =7;
    public static final int APP_USERSTATE_VOIPSUPPORT =8;
    public static final int APP_USERSTATE_EMAILSUPPORT =9;
    public static final int APP_USERSTATE_HAPPYFACE =99;


    public static final int APP_STATE_LOCK =0x00FFFF;
    public static final int APP_STATE_UNLOCK =0xFFFFFF;

    public static final int SM_STATE_FINISHED = 0;
    public static final int SM_STATE_INITIALIZING = 100;
    public static final int SM_STATE_INITIALIZED = 101;
    public static final int SM_STATE_READY = 102;
    public static final int SM_STATE_REGISTRATION = 700;
    public static final int SM_STATE_REGISTERING = 701;
    public static final int SM_STATE_REGISTERSUCCESS = 702;
    public static final int SM_STATE_REGISTERFAIL = 703;
    public static final int SM_STATE_DEREGISTER = 704;
    public static final int SM_STATE_DEREGISTERFAIL = 705;
    public static final int SM_STATE_DEREGISTERSUCCESS = 706;
    public static final int SM_STATE_DEREGISTERING = 707;

    public static final int SM_STATE_PROBEREQUESTTRYING = 103;
    public static final int SM_STATE_PROBEREQUESTRESPONSE = 104;
    public static final int SM_STATE_PROBEREQUESTSUCCESS = 105;
    public static final int SM_STATE_PROBEREQUESTCONFIRM = 107;
    public static final int SM_STATE_PROBEREQUESTABANDONED =205;
    public static final int SM_STATE_PROBEREQUESTFAIL = 108;
    public static final int SM_STATE_PROBEREQUESTCONFIRMED = 109;
    public static final int SM_STATE_CMACTION = 200;
    public static final int SM_STATE_PROBERECEIVERESPINIT =201;
    public static final int SM_STATE_PROBERESPCONFIRM =202;
    public static final int SM_STATE_PROBERESPSUCCESS =203;
    public static final int SM_STATE_PROBERESPFAIL =204;

    public static final int SM_STATE_PROBEREQRECEIVED = 111;      // client only
    public static final int SM_STATE_PROBEREQSELECT =112;
    public static final int SM_STATE_PROBEMATCHSUCCESS =113;
    public static final int SM_STATE_PROBEREQMATCH_FAIL =114;
    public static final int SM_STATE_PROBEMATCHAUTO =115;

    public static final int SM_STATE_PROBEREQRESP_PHONEIN =301;
    public static final int SM_STATE_PROBEREQRESP_VOIPIN =302;
    public static final int SM_STATE_PROBEREQRESP_PTTIN =303;
    public static final int SM_STATE_PROBEREQRESP_VOICEMSG_ACTIVEIN =304;
    public static final int SM_STATE_PROBEREQRESP_VOICEMSG_SILENTIN =354;
    public static final int SM_STATE_PROBEREQRESP_TEXTMSGIN =305;
    public static final int SM_STATE_PROBEREQRESP_PHONEOUT =306;
    public static final int SM_STATE_PROBEREQRESP_VOIPOUT =307;
    public static final int SM_STATE_PROBEREQRESP_PTTOUT =308;
    public static final int SM_STATE_PROBEREQRESP_VOICEMSG_ACTIVEOUT =309;
    public static final int SM_STATE_PROBEREQRESP_VOICEMSG_SILENTOUT =359;
    public static final int SM_STATE_PROBEREQRESP_TEXTMSGOUT =310;


    public static final int SM_STATE_PTTCALLREQUEST = 105;
    public static final int SM_STATE_PTTTRANSMIT_START =600;
    public static final int SM_STATE_PTTTRANSMIT_FAIL =601;
    public static final int SM_STATE_PTTTRANSMIT_ABANDONED =602;
    public static final int SM_STATE_PTTTRANSMIT_ANNOUNCE =603;
    public static final int SM_STATE_PTTTRANSMIT_CONFIRM =604;
    public static final int SM_STATE_PTTTRANSMIT_RESPONSE =605;
    public static final int SM_STATE_PTTTRANSMIT_ACCEPTED =606;
    public static final int SM_STATE_PTTTRANSMIT_STARTED =607;
    public static final int SM_STATE_PTTTRANSMIT_PROGRESS =608;
    public static final int SM_STATE_PTTTRANSMIT_STOPPING =609;
    public static final int SM_STATE_PTTTRANSMIT_STOP =610;
    public static final int SM_STATE_PTTTRANSMIT_RELEASE =611;
    public static final int SM_STATE_PTTTRANSMIT_SUCCESS =612;
    public static final int SM_STATE_PTTTRANSMIT_ACCEPT =613;
    public static final int SM_STATE_PTTTRANSMIT_REJECT =614;
    public static final int SM_STATE_PTTTRANSMIT_PREPARED =615;
    public static final int SM_STATE_PTTTRANSMIT_COMPLETE =616;

    public static final int SM_STATE_PTTCALLANNOUNCE =620;
    public static final int SM_STATE_PTTRECEIVE_FAIL =621;
    public static final int SM_STATE_PTTRECEIVE_CONFIRM =622;
    public static final int SM_STATE_PTTRECEIVE_ACCEPT =623;
    public static final int SM_STATE_PTTRECEIVE_REJECT =624;
    public static final int SM_STATE_PTTRECEIVE_ABANDONED =625;
    public static final int SM_STATE_PTTRECEIVE_CONFIRMED =626;
    public static final int SM_STATE_PTTRECEIVE_STARTED =627;
    public static final int SM_STATE_PTTRECEIVE_STOPPING =628;
    public static final int SM_STATE_PTTRECEIVE_STOP =629;
    public static final int SM_STATE_PTTRECEIVE_SUCCESS =630;
    public static final int SM_STATE_PTTRECEIVE_PROGRESS =631;

    public static final int SM_STATE_PTTFLOORREQUEST =632;
    public static final int SM_STATE_PTTFLOORANNOUNCE =633;
    public static final int SM_STATE_PTTFLOORGRANTED =634;
    public static final int SM_STATE_PTTFLOOR_PREPARED =635;
    public static final int SM_STATE_PTTFLOORREQUEST_FAIL =636;
    public static final int SM_STATE_PTTFLOOR_COMPLETE =637;
    public static final int SM_STATE_PTTFLOORANNOUNCE_FAIL =638;
    public static final int SM_STATE_PTTFLOORRECEIVE_STARTED =639;
    public static final int SM_STATE_PTTFLOORRECEIVE_SUCCESS =640;

    public static final int SM_STATE_PTTCALLFINISH =652;
    public static final int SM_STATE_PTTCALLSUCCESS =650;
    public static final int SM_STATE_PTTCALLFAIL =651;
    public static final int SM_STATE_PTTHOLDOFF =659;

    public static final int SM_STATE_PTTYOUSAYREQUEST =670;
    public static final int SM_STATE_PTTYOUSAYCONFIRM =671;
    public static final int SM_STATE_PTTYOUSAY_STARTED =672;
    public static final int SM_STATE_PTTYOUSAY_STOP =673;
    public static final int SM_STATE_PTTYOUSAYREQUEST_FAIL =674;
    public static final int SM_STATE_PTTYOUSAY_SUCCESS =675;
    public static final int SM_STATE_PTTYOUSAYREQUEST_COMPLETE =676;

    public static final int SM_STATE_PTTYOUSAYANNOUNCE =680;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_PREPARE =681;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_STOPPING =682;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_STOP =683;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_FAIL =684;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_SUCCESS =685;

    public static final int SM_STATE_PTTYOUSAYREQUEST_ANNOUNCE =690;
    public static final int SM_STATE_PTTYOUSAYCONFIRMED =691;
    public static final int SM_STATE_PTTYOUSAYANNOUNCE_STARTED =693;
    public static final int SM_STATE_PTTYOUSAY_PROGRESS =694;
    public static final int SM_STATE_PTTYOUSAY_STOPPING =695;


    public static final int SM_STATE_PTTYOUSAYANNOUNCE_CONFIRM =697;
    public static final int SM_STATE_PTTYOUSAYANNOUNCE_SUCCESS =698;
    public static final int SM_STATE_PTTYOUSAYANNOUNCE_FAIL =699;



    public static final int SM_STATE_TEXTMSGRECEIVED = 241;
    public static final int SM_STATE_TEXTMSGRECEIVE_RESPONSE = 242;
    public static final int SM_STATE_TEXTMSGRECEIVE_CONFIRM = 243;
    public static final int SM_STATE_TEXTMSGRECEIVE_SUCCESS = 244;
    public static final int SM_STATE_TEXTMSGRECEIVE_FAIL = 245;

    public static final int SM_STATE_TEXTMSGSELECTED = 490;
    public static final int SM_STATE_TEXTMSGTRANSMIT = 491;
    public static final int SM_STATE_TEXTMSGTRANSMIT_CONFIRM = 494;
    public static final int SM_STATE_TEXTMSGTRANSMIT_FINISH = 492;
    public static final int SM_STATE_TEXTMSGTRANSMIT_FAIL = 493;
    public static final int SM_STATE_TEXTMSGTRANSMIT_SUCCESS =495;
    public static final int SM_STATE_TEXTMSGTRANSMIT_PREPARED =496;
    public static final int SM_STATE_TEXTMSGTRANSMIT_COMPLETE =497;

    public static final int SM_STATE_VOIPSELECTED = 350;
    public static final int SM_STATE_PHONESELECTED = 351;

    public static final int SM_STATE_VOICEMSG_ACTIVESELECTED = 500;
    public static final int SM_STATE_VOICEMSG_SILENTSELECTED = 550;
    public static final int SM_STATE_VOICEMSGACTIVETRANSMIT_START = 501;
    public static final int SM_STATE_VOICEMSGSILENTTRANSMIT_START = 512;
    public static final int SM_STATE_VOICEMSGTRANSMIT_STOPPING = 502;
    public static final int SM_STATE_VOICEMSGTRANSMIT_STOP = 503;
    public static final int SM_STATE_VOICEMSGTRANSMIT_STARTED = 504;
    public static final int SM_STATE_VOICEMSGTRANSMIT_FAIL = 505;
    public static final int SM_STATE_VOICEMSGTRANSMIT_ABANDONED = 513;
    public static final int SM_STATE_VOICEMSGTRANSMIT_CONFIRM = 506;
    public static final int SM_STATE_VOICEMSGTRANSMIT_SUCCESS =509;
    public static final int SM_STATE_VOICEMSGTRANSMIT_PREPARED =510;
    public static final int SM_STATE_VOICEMSGTRANSMIT_COMPLETE=511;

    public static final int SM_STATE_VOICEMSGTRANSMIT_RECEIVING =507;
    public static final int SM_STATE_VOICEMSGTRANSMIT_RECEIVINGINITIAL =508;

    public static final int SM_STATE_VOICEMSGACTIVERECEIVE_ANNONCE =520;
    public static final int SM_STATE_VOICEMSGSILENTRECEIVE_ANNONCE =531;
    public static final int SM_STATE_VOICEMSGACTIVERECEIVED =521;
    public static final int SM_STATE_VOICEMSGSILENTRECEIVED =530;
    public static final int SM_STATE_VOICEMSGRECEIVE_CONFIRM =523;
    public static final int SM_STATE_VOICEMSGRECEIVE_STARTED =522;
    public static final int SM_STATE_VOICEMSGRECEIVE_STOP =524;
    public static final int SM_STATE_VOICEMSGRECEIVE_STOPPING =525;
    public static final int SM_STATE_VOICEMSGRECEIVE_STOPPED =526;
    public static final int SM_STATE_VOICEMSGRECEIVE_SUCCESS =527;
    public static final int SM_STATE_VOICEMSGRECEIVE_FAIL = 529;

    public static final int SM_STATE_EMAILSELECTED = 110;


    public static final int SM_STATE_CMSELECT = 113;

    public static final int SM_STATE_WAITINGFORUI_TEXTMSGTRANSMIT = 990;
    public static final int SM_STATE_WAITINGFORUI_VOICEMSGTRANSMIT = 991;
    public static final int SM_STATE_WAITINGFORUI_PTTTRANSMIT = 993;
    public static final int SM_STATE_WAITINGFORUI_PTTFLOOR = 994;
    public static final int SM_STATE_WAITINGFORUI_PROBEMATCH =995;
    public static final int SM_STATE_WAITINGFORUI_PTTYOUSAY =996;
    public static final int SM_STATE_WAITINGFORUI_CMSELECTION = 999;

    /*
     Application client state machine
     */
    private volatile boolean running;
    private volatile int appSmState;
    private volatile boolean appUserState[];
    private volatile int mediaSessionId;

    private String runningOwner;
    private boolean runningAudioTask;
    private String appSmOwner;
    private boolean appAutoAnswer;
    private int mediaSessionCount;
    private short appSeqNum;
    private long lastKeepAliveReceived;
    private long lastKeepAliveSent;

    private static LinkedList<AudioParams> audioParams;

    /*
     Constructor
     */
    AppState(final int smState, final boolean audioTask) {

        running = false;
        runningOwner =null;
        runningAudioTask = audioTask;
        lastKeepAliveReceived =lastKeepAliveSent =System.currentTimeMillis();
        appSmState = smState;
        appSmOwner =null;
        mediaSessionCount = 0;
        mediaSessionId = 0;
        appSeqNum =startFromRandom();
        appAutoAnswer = true;
        appUserState = new boolean[]{false, false, false, false, false, false, false, false, false, true};
        // Roaming, WiFi, Busy, Drive, Night, Reserved, AutoAnswer, PhoneSupport, VoIPSupport, EmailSupport
        audioParams = Audio.findAudioParams();
    }

    private short startFromRandom() {
        Random rn = new Random();
        int result =rn.nextInt((int)(System.currentTimeMillis()%0x7fff))+1;
        return (short)result;
    }

    public synchronized boolean isSyncRunning(final int lock) {
        String threadName = Thread.currentThread().getName();
        boolean result = true;

        switch (lock) {
            case APP_STATE_LOCK: {
                if (runningOwner == null)
                    runningOwner = threadName;
                else if (threadName.compareTo(runningOwner) != 0) {
                    try {
                        while (runningOwner !=null)
                            wait(500);
                        runningOwner = threadName;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                result = running;
                break;
            }
            case APP_STATE_UNLOCK: {
                if (runningOwner !=null) {
                    if (threadName.compareTo(runningOwner) ==0) {
                        runningOwner = null;
                        notifyAll();
                    }
                }
                result = true;
                break;
            }
        }

        return result;
    }

    public synchronized int getSyncSmState(final int lock) {
        String threadName = Thread.currentThread().getName();
        int result;

        /* if (threadName ==null || threadName.equals(""))
            if (Constants.DEBUGGING)
                Log.i("ProtMsgTag", "thread.Name: " + threadName);*/
        switch (lock) {
            case APP_STATE_LOCK: {
                if (appSmOwner == null)
                    appSmOwner = threadName;
                else if (threadName.compareTo(appSmOwner) != 0) {
                    try {
                        while (appSmOwner !=null)
                            wait(500);
                        appSmOwner = threadName;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                result = appSmState;
                break;
            }
            case APP_STATE_UNLOCK: {
                if (appSmOwner !=null) {
                    if (threadName.compareTo(appSmOwner) ==0) {
                        appSmOwner = null;
                        notifyAll();
                    }
                }
                result = appSmState;
                break;
            }
        }

        return appSmState;
    }

    public boolean isServerAlive() {
        return (System.currentTimeMillis() - lastKeepAliveReceived
                < TimerControl.TIMER_KEEPALIVE);
    }

    public boolean isSendKeepAlive() {
        return (System.currentTimeMillis() - lastKeepAliveSent
                > TimerControl.TIMER_KEEPALIVE_FREQ);
    }

    public void updateKeepAliveReceived() {
        lastKeepAliveReceived =System.currentTimeMillis();
    }

    public void updateKeepAliveSent() {
        lastKeepAliveSent =System.currentTimeMillis();
    }

    public boolean isRunning() {
        return running;
    }
    public void setRunning(final boolean run) {
        running = run;
    }
    public boolean isRunningAudioTask() {
        return runningAudioTask;
    }
    public void setRunningAudioTask(final boolean run) {
        runningAudioTask = run;
    }

    public short incAppSeqNum() {
        if (appSeqNum<0x7FFF)
            appSeqNum++;                    // sequence numbers are expected to wrap around
        else appSeqNum=0;

        return appSeqNum;
    }


    public int newMediaSession() {
        Random rn = new Random();
        mediaSessionId = rn.nextInt((int)(System.currentTimeMillis()%0x7fffffff))+1;
        mediaSessionCount ++;

        return mediaSessionId;
    }

    public int newMediaSession(int sessionId) {
        mediaSessionId = sessionId;
        mediaSessionCount ++;

        return mediaSessionId;
    }

    public String getAppServerId() {
        return "i-EchoAppServer";
    }
    /*
     Utilities
     */
    public int getSmState() {  return appSmState; }
    public void setSmState(final int SmState) {  appSmState = SmState; }
    public int getMediaSessionCount() {
        return mediaSessionCount;
    }
    public void setMediaSessionCount(final int sc) {
        mediaSessionCount = sc;
    }
    public int getMediaSessionId() {
        return mediaSessionId;
    }
    public void setMediaSessionId(final int id) { mediaSessionId = id; }
    public short getAppSeqNum() {
        return appSeqNum;
    }
    public void setAppSeqNum(final short sn) { appSeqNum = sn; }
    public boolean getAutoAnswer() {
        return appAutoAnswer;
    }
    public void setAutoAnswer(final boolean auto) {
        appAutoAnswer = auto;
    }
    public boolean[] getUserState() {
        return appUserState;
    }
    public void setUserState(final boolean[] userState) {
        appUserState = userState;
    }
    public AudioParams getAudioParamFirst() {
        return audioParams.getFirst();
    }
    public LinkedList<AudioParams> getAudioParams() {
        return audioParams;
    }
    public void setAudioParams(final LinkedList<AudioParams> params) {
        audioParams = params;
    }

}
