package com.i_echo.lpc.i_echo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;

import com.i_echo.lpc.i_echo.Inet.NetworkStateReceiver;
import com.i_echo.lpc.i_echo.Utils.UtilsConnectivity;
import com.i_echo.lpc.i_echo.Utils.UtilsHelpers;
import com.i_echo.lpc.i_echo.Utils.UtilsLocationTracker;
import com.i_echo.lpc.i_echo.Utils.UtilsPhone;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ActivityMain extends ActionBarActivity
        implements ProcessorCallP.ICallProcListener, ProcessorPttChat.IPttProcListener,
        NetworkStateReceiver.INetworkStateReceiverListener,
        UtilsPhone.IPhoneServiceStateListener {

    public static final String ACTION_WIFISTATE_CHANGED="android.net.wifi.WIFI_STATE_CHANGED";
    public static final String ACTION_DAYNIGHT_CHANGED ="com.i_echo.lpc.i_echo.DAYNIGHT_CHANGED";
    public static final String ACTION_BROADCAST_SPEED = "com.i_echo.action.SPEED_UPDATE";
    public static int MIN_DRIVING_SPEED = 20;               // 20KMph counting as driving
    public static final int APP_SERVERTCPPORT = 8081;
    public static final int APP_SERVERUDPPORT = 8081;


    private ActivityMain mActivity;
    private FragmentDisplayList mFragmentDisplayList;
    private AdapterCmList mAdapterCmList;
    private static AppState mAppState;
    private static boolean mAppStarted = false;
    private static ArrayList<ContactInfoUser> mAppUsers =null;
    private static String mServerIp =null;
    private static AppProtMsgQueue mAppMsgQue =null;
    private static ActivityMainUiManager mUiHolder;
    private static ProcessorVoiceMsgAsyncTask mAudioRecordingTask;
    private static ProcessorPttAsyncTask mPttRecordingTask;
    private static PttAudioChannel mPttChannel;
    private static Timer mCheckDayNightChangeTimer =null;
    private static TelephonyManager mTelManager =null;
    private static Handler mHandler = new Handler();


    private IntentFilter mNetworkStateFilter =null;
    private NetworkStateReceiver mNetworkStateReceiver;
    private IntentFilter mLocationUpdateFilter =null;
    private BroadcastReceiver mSpeedUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mActivity = this;

        // Obtaining user and target(s) information
        if (mAppUsers ==null && mServerIp ==null) {
            Bundle extras = getIntent().getExtras();
            mServerIp = extras.getString("ipaddr");
            mAppUsers =extras.getParcelableArrayList("users");
        }

        // UI items initialization
        mUiHolder = new ActivityMainUiManager(this);
        mUiHolder = setButtonHandlers(mUiHolder);

        // Initializing of the app states and connection to server
        mAppState = new AppState(AppState.SM_STATE_INITIALIZING, false);
        setUserStatus(mAppState);

        if (null == mNetworkStateFilter) {
            mNetworkStateFilter = new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
            mNetworkStateReceiver = new NetworkStateReceiver(this);
            mNetworkStateReceiver.addListener(this);
            registerReceiver(mNetworkStateReceiver, mNetworkStateFilter);
        }
        if (null == mLocationUpdateFilter) {
            mLocationUpdateFilter = new IntentFilter(ACTION_BROADCAST_SPEED);
            mSpeedUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equalsIgnoreCase(ACTION_BROADCAST_SPEED)) {
                        int speed = intent.getIntExtra("speed", 0);
                        if (speed > MIN_DRIVING_SPEED)
                            setCbDriving(getActivity(), true);
                    }
                }
            };
            registerReceiver(mSpeedUpdateReceiver, mLocationUpdateFilter);
            // TODO: start location tracking service here
            Intent intent = new Intent(this, UtilsLocationTracker.class);
            startService(intent);
        }

        if (null == mCheckDayNightChangeTimer) {
            mCheckDayNightChangeTimer = new Timer();
            long timeToGo = UtilsHelpers.calculateTimeTo(6, 22); // morning = 6 o'clock, night = 22 o'clock

            mCheckDayNightChangeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent(ACTION_DAYNIGHT_CHANGED);
                    sendOrderedBroadcast(intent, null, new dayNightChangedReceiver(), null,
                            Activity.RESULT_OK, null, null);
                }
            }, timeToGo*60*1000);
        }
        if (null == mTelManager) {
            mTelManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            mTelManager.listen(new UtilsPhone(this),
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                            | PhoneStateListener.LISTEN_CALL_STATE
                            | PhoneStateListener.LISTEN_CELL_INFO // Requires API 17
                            | PhoneStateListener.LISTEN_CELL_LOCATION
                            | PhoneStateListener.LISTEN_DATA_ACTIVITY
                            | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                            | PhoneStateListener.LISTEN_SERVICE_STATE
                            | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                            | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                            | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR);
        }
        Thread.currentThread().setName("mainactivity");
        // Now ready to update the server with the current user app status
        if (savedInstanceState==null) {
            mAppStarted =activateApp();
            if (mAppStarted) {
                Bundle bundle;                  // initialize message history display fragment
                mFragmentDisplayList =new FragmentDisplayList();
                bundle = new Bundle();
                String userId =mAppUsers.get(1).getAppUserId();
                bundle.putParcelableArrayList("display", mAppMsgQue.getUserMsgList(userId));
                mFragmentDisplayList.setArguments(bundle);
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.container_display_fragment, mFragmentDisplayList)
                        .commit();

                /*
                Invoke call processing thread to conduct communications with the
                application server (while-loop in the thread to listen, send, to the
                server, and perform applications processing
                */
                mAppState.isSyncRunning(AppState.APP_STATE_LOCK);
                mAppState.setRunning(true);
                mAppState.isSyncRunning(AppState.APP_STATE_UNLOCK);
                Thread callpThread = new Thread(new ProcessorCallP(mActivity));
                callpThread.start();
                callpThread.setName("callp");
                mUiHolder.showUIDefaultEcho();         // Clean up the UI
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void notifyDisplayListUpdate(final String userId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getFragmentDisplayList().updateDisplaylist(mAppMsgQue.getUserMsgList(userId));
            }
        });
    }

    @Override
    public void onPhoneInService() {
        if (null == mTelManager) {

            mTelManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            mTelManager.listen(new UtilsPhone(this),
                    PhoneStateListener.LISTEN_SERVICE_STATE
                            | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                            | PhoneStateListener.LISTEN_DATA_ACTIVITY
                            // | PhoneStateListener.LISTEN_CALL_STATE
                            // | PhoneStateListener.LISTEN_CELL_INFO // Requires API 17
                            // | PhoneStateListener.LISTEN_CELL_LOCATION
                            // | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                            // | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                            // | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                    );
        }
        // mTelManager.getNetworkType();
        setCbPhoneSupport(getActivity(), true);
    }

    @Override
    public void onPhoneOutOfService() {
        if (null == mTelManager) {
            mTelManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            mTelManager.listen(new UtilsPhone(this),
                    PhoneStateListener.LISTEN_SERVICE_STATE
                            | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                            | PhoneStateListener.LISTEN_DATA_ACTIVITY
                    // | PhoneStateListener.LISTEN_CALL_STATE
                    // | PhoneStateListener.LISTEN_CELL_INFO // Requires API 17
                    // | PhoneStateListener.LISTEN_CELL_LOCATION
                    // | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    // | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                    // | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
            );
        }
        // mTelManager.getNetworkType();
        setCbPhoneSupport(getActivity(), false);
    }

    @Override
    public void onCmAvailable(final ArrayList<CmMatch> cmItems) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null == getAdapterCmList()) {
                    AdapterCmList adapter =
                            new AdapterCmList(getActivity(), R.layout.cm_list_row_custom, cmItems);
                    setAdapterCmList(adapter);
                } else {
                    getAdapterCmList().setData(cmItems);
                    getAdapterCmList().notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onPttChatSessionBegin() {
        mPttChannel =new PttAudioChannel(mActivity);
    }

    @Override
    public PttAudioChannel onPttCallProc() {
        return mPttChannel;
    }

    @Override
    public void onPttChatSessionFinish() {
        if (null != mPttChannel) {
            mPttChannel.close();
            mPttChannel =null;
        }
    }


    @Override
    public void onCallProcFinish(int returnCode) {
        mAppMsgQue.closeConnection();

        Intent exitIntent = new Intent(Intent.ACTION_MAIN);
        exitIntent.addCategory(Intent.CATEGORY_HOME);
        exitIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(exitIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (null != mCheckDayNightChangeTimer) {
            mCheckDayNightChangeTimer.cancel();
            mCheckDayNightChangeTimer =null;
        }
        // stop the location tracking service
        Intent intent = new Intent(this, UtilsLocationTracker.class);
        stopService(intent);

        super.onDestroy();
    }

    @Override
    protected void onResume(){
        super.onResume();

        if (null == mNetworkStateFilter  && null == mNetworkStateReceiver) {
            mNetworkStateFilter = new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
            mNetworkStateReceiver = new NetworkStateReceiver(this);
            mNetworkStateReceiver.addListener(this);
        }
        registerReceiver(mNetworkStateReceiver, mNetworkStateFilter);
        if (null == mLocationUpdateFilter && null == mSpeedUpdateReceiver) {
            mLocationUpdateFilter = new IntentFilter(ACTION_BROADCAST_SPEED);
            mSpeedUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equalsIgnoreCase(ACTION_BROADCAST_SPEED)) {
                        int speed = intent.getIntExtra("speed", 0);
                        if (speed > MIN_DRIVING_SPEED)
                            setCbDriving(getActivity(), true);
                    }
                }
            };
        }
        registerReceiver(mSpeedUpdateReceiver, mLocationUpdateFilter);
    }

    @Override
    protected void onPause(){
        super.onPause();

        // put your code here...
        if (null != mNetworkStateReceiver)
            unregisterReceiver(mNetworkStateReceiver);
        if (null != mSpeedUpdateReceiver)
            unregisterReceiver(mSpeedUpdateReceiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode && mAppStarted) {
            switch(mAppState.getSyncSmState(AppState.APP_STATE_LOCK)) {
                case AppState.SM_STATE_WAITINGFORUI_CMSELECTION: {
                    mAppState.setSmState(AppState.SM_STATE_READY);
                    mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                    mUiHolder.showUIDefaultEcho();
                    break;
                }
                case AppState.SM_STATE_PTTHOLDOFF: {
                    mAppState.setSmState(AppState.SM_STATE_PTTCALLFINISH);
                    mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                    mUiHolder.showUIDefaultEcho();
                    break;
                }
                default: {
                    mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                    deactivateApp();
                    break;
                }
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }



    /*
     * Determine the view topoffset for use by subsequent functions
    View globalView = (View)findViewById(R.id.appview); // the main view of my activity/application
    DisplayMetrics dm = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(dm);
    appViewYOffset = dm.heightPixels - globalView.getMeasuredHeight();
    */

    private boolean activateApp() {
        /*
         Establish connection to server for client-server application
         Update the user application status to the server
         Start listening to server for incoming call requests
        */
        mAppState.setSmState(AppState.SM_STATE_REGISTRATION);
        if (!UtilsConnectivity.isConnectedNetwork(this, UtilsConnectivity.NETWORK_ANY)) {
            mUiHolder.showUIToastMsg(getResources().getString(R.string.string_networknotavailable));
            mAppState.isSyncRunning(AppState.APP_STATE_LOCK);
            mAppState.setRunning(false);
            mAppState.isSyncRunning(AppState.APP_STATE_UNLOCK);
            return false;
        }
        else if (mServerIp ==null) {
            mUiHolder.showUIToastMsg(getResources().getString(R.string.string_unknownserver));
            return false;
        }
        else {
            // Start up the ctrl (TCP) channel with the server and listen to the socket
            mAppMsgQue = new AppProtMsgQueue(mServerIp, APP_SERVERTCPPORT);
            if (!mAppMsgQue.waitForTcpCtrlChannel()) {
                mUiHolder.showUIToastMsg(getResources().getString(R.string.string_couldnotconnect));
                if (mAppMsgQue != null) {
                    mAppMsgQue.close();
                    mAppMsgQue =null;
                }
                return false;
            }
            else {
                /*
                 Start the socket listening/reading thread for incoming msgs from the server
                 after the waitForTcpCtrlChannel is established
                 */
                mAppMsgQue.executeListening();
                /*
                Invoke call processing thread to conduct communications with the
                application server (while-loop in the thread to listen, send, to the
                server, and perform applications processing
                */
                mAppMsgQue.saveUserMsg(getAppTarget(), true, getString(R.string.string_starting),
                        AppProtUserMsg.USERMSG_TEXT_FROM);
                mAppMsgQue.saveUserMsg(getAppTarget(), true, getString(R.string.string_thatsrigtht),
                        AppProtUserMsg.USERMSG_TEXT_TO);
                return true;
            }
        }
    }

    private void deactivateApp() {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(getResources().getString(R.string.string_existapp))
                .setPositiveButton(getResources().getString(R.string.string_ok), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAppState.getSyncSmState(AppState.APP_STATE_LOCK);
                        mAppState.setSmState(AppState.SM_STATE_DEREGISTER);
                        mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.string_cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.cancel();
                    }
                })
                .show();
    }


    /*
     Methods setup for UI listeners and initialization
    */
    private ActivityMainUiManager setButtonHandlers(final ActivityMainUiManager uiMgr) {

        uiMgr.mBtnAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mAppState.getSyncSmState(AppState.APP_STATE_LOCK)) {
                    case AppState.SM_STATE_WAITINGFORUI_TEXTMSGTRANSMIT: {
                        mAppState.setSmState(AppState.SM_STATE_TEXTMSGTRANSMIT_COMPLETE);
                        break;
                    }
                    default: break;
                }
                mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
            }
        });
        uiMgr.mBtnAction.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = false;
                int action = event.getAction();

                switch (mAppState.getSyncSmState(AppState.APP_STATE_LOCK)) {
                    case AppState.SM_STATE_WAITINGFORUI_VOICEMSGTRANSMIT: {
                        if (!mAppState.isRunningAudioTask()) {
                            if (MotionEvent.ACTION_DOWN == action) {
                                mAudioRecordingTask = new ProcessorVoiceMsgAsyncTask(mActivity);
                                mAudioRecordingTask.execute();
                                mAppState.setRunningAudioTask(true);
                                result = true;
                            }
                        } else if (MotionEvent.ACTION_UP == action
                                || MotionEvent.ACTION_CANCEL == action) {
                            mAppState.setSmState(AppState.SM_STATE_VOICEMSGTRANSMIT_COMPLETE);
                            if (AsyncTask.Status.FINISHED != mAudioRecordingTask.getStatus())
                                mAudioRecordingTask.cancel(false);
                            mAppState.setRunningAudioTask(false);
                            result = true;
                        }
                        break;
                    }
                    default:
                        break;
                }
                mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                return result;
            }
        });
        uiMgr.mBtnProbe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mAppMsgQue == null || !mAppMsgQue.isConnected()) {
                    mUiHolder.showUIToastMsg(getResources().getString(R.string.string_notconnectserver));
                    return;
                }
                switch (mAppState.getSyncSmState(AppState.APP_STATE_LOCK)) {
                    case AppState.SM_STATE_READY: {
                        mAppState.setSmState(AppState.SM_STATE_PROBEREQUESTTRYING);
                        // repeat button presses (AppState.SM_STATE_PROBEREQUESTTRYING) are ignored
                        break;
                    }
                    default: break;
                }
                mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
            }
        });
        uiMgr.mCmListView.setOnItemClickListener(new ListenerCmListHandling(getActivity()));
        uiMgr.mBtnYouPttChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = false;
                int action = event.getAction();

                if (null == mPttChannel) {
                    return result;
                }
                switch (mAppState.getSyncSmState(AppState.APP_STATE_LOCK)) {
                    case AppState.SM_STATE_PTTHOLDOFF: {
                        if (MotionEvent.ACTION_DOWN == action) {
                            mUiHolder.mBtnYouPttChat.setPressed(true);
                            // for onClick event use setSelected and also state_selected in selector XML
                            mAppState.setSmState(AppState.SM_STATE_PTTYOUSAYREQUEST);
                            result = true;
                        }
                        break;
                    }
                    case AppState.SM_STATE_WAITINGFORUI_PTTYOUSAY: {
                        if (MotionEvent.ACTION_UP == action
                            || MotionEvent.ACTION_CANCEL == action) {
                            mUiHolder.mBtnYouPttChat.setPressed(false);
                            mAppState.setSmState(AppState.SM_STATE_PTTYOUSAYREQUEST_COMPLETE);
                            result = true;
                        }
                        break;
                    }
                }
                mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                return result;
            }
        });
        uiMgr.mBtnPttChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = false;
                int action = event.getAction();

                if (null == mPttChannel) {
                    return result;
                }
                switch (mAppState.getSyncSmState(AppState.APP_STATE_LOCK)) {
                    case AppState.SM_STATE_PTTHOLDOFF: {
                        if (MotionEvent.ACTION_DOWN == action) {
                            mUiHolder.mBtnPttChat.setPressed(true);
                            // for onClick event use setSelected and also state_selected in selector XML
                            mAppState.setSmState(AppState.SM_STATE_PTTFLOORREQUEST);
                            result = true;
                        }
                        break;
                    }
                    case AppState.SM_STATE_WAITINGFORUI_PTTFLOOR: {
                        if (MotionEvent.ACTION_UP == action) {
                            // || MotionEvent.ACTION_CANCEL == action
                            mUiHolder.mBtnPttChat.setPressed(false);
                            mAppState.setSmState(AppState.SM_STATE_PTTFLOOR_COMPLETE);
                            result = true;
                        }
                        break;
                    }
                    case AppState.SM_STATE_WAITINGFORUI_PTTTRANSMIT: {
                        if (!mAppState.isRunningAudioTask()) {
                            if (MotionEvent.ACTION_DOWN == action) {
                                mUiHolder.mBtnPttChat.setPressed(true);
                                mPttRecordingTask = new ProcessorPttAsyncTask(mActivity,
                                        mPttChannel, getAppState().getMediaSessionId());
                                mPttRecordingTask.execute();
                                mAppState.setRunningAudioTask(true);

                                result = true;
                            }
                        } else {
                            if (MotionEvent.ACTION_UP == action || MotionEvent.ACTION_CANCEL == action) {
                                mUiHolder.mBtnPttChat.setPressed(false);
                                mAppState.setSmState(AppState.SM_STATE_PTTTRANSMIT_COMPLETE);
                                if (AsyncTask.Status.FINISHED != mPttRecordingTask.getStatus())
                                    mPttRecordingTask.cancel(false);
                                mAppState.setRunningAudioTask(false);

                                result = true;
                            }
                        }
                        break;
                    }
                }
                mAppState.getSyncSmState(AppState.APP_STATE_UNLOCK);
                return result;
            }
        });

        return uiMgr;
    }

    public void setAdapterCmList(AdapterCmList adapter) {
        mAdapterCmList =adapter;
        mUiHolder.mCmListView.setAdapter(adapter);
    }

    @Override
    public void onNetworkStateChanged(final boolean[] networkChanged, final boolean[] networkState) {
        for (int network = 0; network < networkChanged.length; network++) {
            switch (network) {
                case UtilsConnectivity.NETWORK_WIFI: {
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            setCbWifiSupport(getActivity());
                        }
                    }, 5000);
                    break;
                }
                case UtilsConnectivity.NETWORK_LTE:
                case UtilsConnectivity.NETWORK_CDMA:
                case UtilsConnectivity.NETWORK_3G:
                case UtilsConnectivity.NETWORK_GSM:
                case UtilsConnectivity.NETWORK_MOBILE:{
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            setCbPhoneSupport(getActivity(), true);
                        }
                    }, 5000);
                    break;
                }
            }
        }
    }

    private class dayNightChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_DAYNIGHT_CHANGED.equals(action)) {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setCbSleep(context);
                    }
                }, 60000);      // do this in one minute
                long timeToGo = UtilsHelpers.calculateTimeTo(6, 22);
                mCheckDayNightChangeTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(ACTION_DAYNIGHT_CHANGED);
                        sendOrderedBroadcast(intent, null, new dayNightChangedReceiver(), null,
                                Activity.RESULT_OK, null, null);
                    }
                }, timeToGo*60*1000);
            }
        }
    }

    private void setUserStatus(AppState state) {
        // Roaming, WiFi, Busy, Drive, Night, MsgOnly, AutoAnswer, PhoneSupport, VoIPSupport, EmailSupport
        boolean[] userState = {true, true, false, true, false, false, false, true, true, true};
        boolean checkStatus;

        state.setUserState(userState);
        setCbAutoAnswer(getActivity());
        setCbBusy(getActivity());

        setCbWifiSupport(getActivity());
        setCbPhoneSupport(getActivity(), true);
        setCbVoipSupport(getActivity());
        setCbVoipSupport(getActivity());

        setCbSleep(getActivity());
        setCbRoaming(getActivity());
        setCbDriving(getActivity(), false);
        // these are set up for the demo only (canned)
    }


    private void setCbAutoAnswer(Context context) {
        CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
        boolean isSleep = cbSleep.isChecked();
        CheckBox cbDrive = (CheckBox) findViewById(R.id.cbDrive);
        boolean isDriving = cbDrive.isChecked();
        CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
        boolean isBusy = cbBusy.isChecked();

        boolean checkStatus;
        CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
        checkStatus =isSleep || isDriving || isBusy;
        if (checkStatus)
            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
        cbAutoAnswer.setChecked(mAppState.getUserState()[AppState.APP_USERSTATE_AUTO]);
        cbAutoAnswer.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
                        boolean isSleep = cbSleep.isChecked();
                        CheckBox cbDrive = (CheckBox) findViewById(R.id.cbDrive);
                        boolean isDriving = cbDrive.isChecked();
                        CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
                        boolean isBusy = cbBusy.isChecked();

                        boolean checkStatus;
                        CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                        checkStatus =isSleep || isDriving || isBusy;
                        if (checkStatus) {    // Rule#1
                            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] = true;
                            cbAutoAnswer.setChecked(true);
                        }
                        else {
                            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =
                                    !mAppState.getUserState()[AppState.APP_USERSTATE_AUTO];
                            cbAutoAnswer.setChecked(mAppState.getUserState()[AppState.APP_USERSTATE_AUTO]);
                        }
                    }
                });
    }

    private void setCbBusy(Context context) {
        CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
        boolean checkStatus = mAppState.getUserState()[AppState.APP_USERSTATE_BUSY];

        cbBusy.setChecked(checkStatus);
        cbBusy.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean checkStatus =((CheckBox)v).isChecked();
                        mAppState.getUserState()[AppState.APP_USERSTATE_BUSY] =checkStatus;
                        if (checkStatus) {    // Rule#2
                            CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
                            mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =false;
                            cbSleep.setChecked(false);
                            CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
                            cbAutoAnswer.setChecked(true);
                        }
                    }
                });

        if (checkStatus) {    // Rule#2
            CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
            mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =false;
            cbSleep.setChecked(false);
            CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
            cbAutoAnswer.setChecked(true);
        }
    }

    private void setCbRoaming(Context context) {
        CheckBox cbRoaming = (CheckBox) findViewById(R.id.cbHomeRoam);
        boolean isConnected = UtilsPhone.isPhoneConnected(context);
        boolean isRoaming = UtilsConnectivity.isRoaming(context);
        boolean checkStatus = isConnected && isRoaming;

        mAppState.getUserState()[AppState.APP_USERSTATE_ROAMING] =checkStatus;
        cbRoaming.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean checkStatus =((CheckBox)v).isChecked();
                        mAppState.getUserState()[AppState.APP_USERSTATE_ROAMING] =checkStatus;
                    }
                });
        cbRoaming.setChecked(checkStatus);
        // cbRoaming.setClickable(false);
    }

    private void setCbSleep(Context context) {
        CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
        boolean checkStatus = UtilsHelpers.isNightTime(22);       // 22 hours of 24 hours
                // mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING];

        cbSleep.setChecked(checkStatus);
        cbSleep.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean checkStatus =((CheckBox)v).isChecked();
                        mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =checkStatus;
                        if (checkStatus) {
                            CheckBox cbDrive = (CheckBox) findViewById(R.id.cbDrive);
                            mAppState.getUserState()[AppState.APP_USERSTATE_DRIVING] =false;
                            cbDrive.setChecked(false);
                            CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
                            mAppState.getUserState()[AppState.APP_USERSTATE_BUSY] =false;
                            cbBusy.setChecked(false);
                            CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
                            cbAutoAnswer.setChecked(true);
                        }
                    }
                });
        if (checkStatus) {    // Rule#3
            CheckBox cbDrive = (CheckBox) findViewById(R.id.cbDrive);
            mAppState.getUserState()[AppState.APP_USERSTATE_DRIVING] =false;
            cbDrive.setChecked(false);
            CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
            mAppState.getUserState()[AppState.APP_USERSTATE_BUSY] =false;
            cbBusy.setChecked(false);
            CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
            cbAutoAnswer.setChecked(true);
        }
    }

    private void setCbDriving(Context context, boolean isKnownDriving) {
        CheckBox cbDriving = (CheckBox) findViewById(R.id.cbDrive);
        boolean hasGpsSupport = UtilsConnectivity.hasGpsSupport(context);
        boolean isDriving = (isKnownDriving |UtilsHelpers.isDriving(context));
        boolean checkStatus = hasGpsSupport && isDriving;

        mAppState.getUserState()[AppState.APP_USERSTATE_DRIVING] =checkStatus;
        cbDriving.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean checkStatus =((CheckBox)v).isChecked();
                        mAppState.getUserState()[AppState.APP_USERSTATE_DRIVING] =checkStatus;
                        if (checkStatus) {    // Rule#4

                            CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
                            mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =false;
                            cbSleep.setChecked(false);
                            CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
                            cbAutoAnswer.setChecked(true);
                        }
                    }
                });
        cbDriving.setChecked(checkStatus);
        // cbDriving.setClickable(false);

        if (checkStatus) {    // Rule#4

            CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
            mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =false;
            cbSleep.setChecked(false);
            CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
            mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
            cbAutoAnswer.setChecked(true);
        }
    }

    private void setCbWifiSupport(Context context) {
        CheckBox cbWifiStatus = (CheckBox) findViewById(R.id.cbWiFi);
        boolean checkStatus = UtilsConnectivity.isConnectedDataWifi(context);

        mAppState.getUserState()[AppState.APP_USERSTATE_WIFI] =checkStatus;
        cbWifiStatus.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean checkStatus = mAppState.getUserState()[AppState.APP_USERSTATE_WIFI];
                        ((CheckBox)v).setChecked(checkStatus);
                    }
                });
        cbWifiStatus.setChecked(checkStatus);
        cbWifiStatus.setClickable(false);
    }

    private void setCbPhoneSupport(Context context, boolean isInService) {
        CheckBox cbPhoneSupport = (CheckBox) findViewById(R.id.cbPhoneSupport);
        boolean isPhone = UtilsPhone.isDevicePhone(context);
        boolean isConected = UtilsPhone.isPhoneConnected(context);
        boolean isAirplaneModeOn = UtilsConnectivity.isAirplaneModeOn(context);
        boolean checkStatus = isPhone && isConected && !isAirplaneModeOn && isInService;

        mAppState.getUserState()[AppState.APP_USERSTATE_PHONESUPPORT] =checkStatus;
        cbPhoneSupport.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean isPhone = UtilsPhone.isDevicePhone(getApplicationContext());
                        boolean isConected = UtilsPhone.isPhoneConnected(getApplicationContext());
                        boolean checkStatus =isPhone && isConected;

                    /*
                    mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
                    ((CheckBox)v).setChecked(checkStatus);
                    */
                        checkStatus =((CheckBox)v).isChecked();     // in the demo, make sure Phone is checkable
                        mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
                    }
                });
        cbPhoneSupport.setChecked(checkStatus);
        // cbPhoneSupport.setClickable(false);
        cbPhoneSupport.setClickable(true);
    }

    private void setCbVoipSupport(Context context) {
        CheckBox cbVoipSupport = (CheckBox) findViewById(R.id.cbVoipSupport);
        boolean isInstalled =
                UtilsHelpers.appInstalledOrNot(context,context.getString(R.string.string_skypeurl));
        boolean isLoggedIn =true;
        // I do not have a good way of evaluating
        // login status so for now always true
        boolean checkStatus = isInstalled && isLoggedIn;

        mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
        cbVoipSupport.setChecked(checkStatus);
        cbVoipSupport.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean isInstalled =
                                UtilsHelpers.appInstalledOrNot(getApplicationContext(),
                                        getApplicationContext().getString(R.string.string_skype));
                        boolean isLoggedIn =true;
                        // I do not have a good way of evaluating
                        // login status so for now always true
                        boolean checkStatus = isInstalled && isLoggedIn;

                        mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
                        ((CheckBox)v).setChecked(checkStatus);
                    }
                });
        cbVoipSupport.setClickable(false);
    }


/*
    private View.OnClickListener cbRoamOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    boolean checkStatus =((CheckBox)v).isChecked();
                    mAppState.getUserState()[AppState.APP_USERSTATE_ROAMING] =checkStatus;
                }
            };

    private View.OnClickListener cbBusyOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    boolean checkStatus =((CheckBox)v).isChecked();
                    mAppState.getUserState()[AppState.APP_USERSTATE_BUSY] =checkStatus;
                    if (checkStatus) {
                        CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
                        mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =false;
                        cbSleep.setChecked(false);
                        CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                        mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
                        cbAutoAnswer.setChecked(true);
                    }
                }
            };

    private View.OnClickListener cbDriveOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    boolean checkStatus =((CheckBox)v).isChecked();
                    mAppState.getUserState()[AppState.APP_USERSTATE_DRIVING] =checkStatus;
                    if (checkStatus) {
                        CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
                        mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =false;
                        cbSleep.setChecked(false);
                        CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                        mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
                        cbAutoAnswer.setChecked(true);
                    }
                }
            };

    private View.OnClickListener cbSleepingOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    boolean checkStatus =((CheckBox)v).isChecked();
                    mAppState.getUserState()[AppState.APP_USERSTATE_SLEEPING] =checkStatus;
                    if (checkStatus) {
                        CheckBox cbDrive = (CheckBox) findViewById(R.id.cbDrive);
                        mAppState.getUserState()[AppState.APP_USERSTATE_DRIVING] =false;
                        cbDrive.setChecked(false);
                        CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
                        mAppState.getUserState()[AppState.APP_USERSTATE_BUSY] =false;
                        cbBusy.setChecked(false);
                        CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                        mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =true;
                        cbAutoAnswer.setChecked(true);
                    }
                }
            };

    private View.OnClickListener cbAutoAnswerOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cbSleep = (CheckBox) findViewById(R.id.cbSleep);
                    boolean isSleep = cbSleep.isChecked();
                    CheckBox cbDrive = (CheckBox) findViewById(R.id.cbDrive);
                    boolean isDriving = cbDrive.isChecked();
                    CheckBox cbBusy = (CheckBox) findViewById(R.id.cbBusy);
                    boolean isBusy = cbBusy.isChecked();

                    boolean checkStatus;
                    CheckBox cbAutoAnswer = (CheckBox) findViewById(R.id.cbAutoAnswer);
                    checkStatus =isSleep || isDriving || isBusy;
                    if (checkStatus) {
                        mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] = true;
                        cbAutoAnswer.setChecked(true);
                    }
                    else {
                        mAppState.getUserState()[AppState.APP_USERSTATE_AUTO] =
                                !mAppState.getUserState()[AppState.APP_USERSTATE_AUTO];
                        cbAutoAnswer.setChecked(mAppState.getUserState()[AppState.APP_USERSTATE_AUTO]);
                    }
                }
            };

    private View.OnClickListener cbWiFiOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    boolean checkStatus = mAppState.getUserState()[AppState.APP_USERSTATE_WIFI];
                    ((CheckBox)v).setChecked(checkStatus);
                }
            };

    private View.OnClickListener cbPhoneSupportOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    boolean isPhone = UtilsPhone.isDevicePhone(getApplicationContext());
                    boolean isConected = UtilsPhone.isPhoneConnected(getApplicationContext());
                    boolean checkStatus =isPhone && isConected;

                    */
/*
                    mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
                    ((CheckBox)v).setChecked(checkStatus);
                    *//*

                    checkStatus =((CheckBox)v).isChecked();     // in the demo, make sure Phone is checkable
                    mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
                }
            };

    private View.OnClickListener cbVoipSupportOnClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                   boolean isInstalled =
                            UtilsHelpers.appInstalledOrNot(getApplicationContext(),
                                    getApplicationContext().getString(R.string.string_skype));
                    boolean isLoggedIn =true;
                    // I do not have a good way of evaluating
                    // login status so for now always true
                    boolean checkStatus = isInstalled && isLoggedIn;

                    mAppState.getUserState()[AppState.APP_USERSTATE_VOIPSUPPORT] =checkStatus;
                    ((CheckBox)v).setChecked(checkStatus);
                }
            };
*/

    /*
    * Utilities
    */

    public ActivityMain getActivity() { return mActivity; }

    public AppState getAppState() {
        return mAppState;
    }

    public AppProtMsgQueue getAppMsgQue() {
        return mAppMsgQue;
    }

    public FragmentDisplayList getFragmentDisplayList() { return mFragmentDisplayList; }

    public AdapterCmList getAdapterCmList() {
        return mAdapterCmList;
    }


    public ActivityMainUiManager getUiHolder() {
        return mUiHolder;
    }

    public ProgressBar getProgressBar() {
        return mUiHolder.mBarProgress;
    }

    public String getAppServerId() {
        return getResources().getString(R.string.string_iechoserverid);
    }

    public int getServerTcpPort() {
        return APP_SERVERTCPPORT;
    }

    public int getServerUdpPort() {
        return APP_SERVERUDPPORT;
    }

    public String getAppServerIpAddress() {
        return mServerIp;
    }

    public ArrayList<ContactInfoUser> getAppUserAndTarget() {
        return mAppUsers;
    }

    public String getAppUser() {
        return mAppUsers.get(0).getAppUserId();
    }

    public String getAppTarget() {
        return mAppUsers.get(1).getAppUserId();
    }


}


