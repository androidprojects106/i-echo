package com.i_echo.lpc.i_echo.Inet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LPC-Home1 on 7/6/2015.
 */

/*
 Possible networks:

mobile
WIFI
mobile_mms
mobile_supl
mobile_dun
mobile_hipri
BLUETOOTH_TETHER
ETHERNET
mobile_ims
wifi_p2p
mobile_dm
mobile_wap
mobile_net
mobile_cmmail
mobile_bip0
mobile_bip1
mobile_bip2
mobile_bip3
mobile_bip4
mobile_bip5
mobile_bip6
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    public static final int NUMBER_OF_NETWORKS = 6;

    public static final int NETWORK_WIFI = 0;              // constants for network Ids
    public static final int NETWORK_GSM = 1;               // constants for network Ids
    public static final int NETWORK_3G = 2;                // constants for network Ids
    public static final int NETWORK_CDMA = 3;                // constants for network Ids
    public static final int NETWORK_LTE = 4;
    public static final int NETWORK_MOBILE = 5;
    public static final int NETWORK_ANY = 100;              // constants for network Ids

    private ConnectivityManager mManager;
    private List<INetworkStateReceiverListener> mListeners;
    private boolean[] mChanged = new boolean[] {false, false, false, false, false, false};
    private boolean[] mConnected = new boolean[] {false, false, false, false, false, false};
    // WiFi, mConnectedGsm, mConnectedL3G, mConnectedCdma, mConnectedLTE;

    public NetworkStateReceiver(Context context) {
        mListeners = new ArrayList<INetworkStateReceiverListener>();
        mManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkStateChanged();
    }


    public void onReceive(Context context, Intent intent) {
        if (null == intent || null == intent.getExtras())
            return;

        if (checkStateChanged())
            notifyStateToAll();
    }

    private boolean checkStateChanged() {
        boolean prevState, stateChanged = false;
        NetworkInfo[] netInfo = mManager.getAllNetworkInfo();

        for (NetworkInfo ni : netInfo) {
            String networkType =ni.getTypeName().toUpperCase();
            if (("WIFI").equals(networkType)) {
                prevState =mConnected[NETWORK_WIFI];
                mConnected[NETWORK_WIFI] =ni.isConnected();
                mChanged[NETWORK_WIFI] = (prevState !=mConnected[NETWORK_WIFI]);
                stateChanged |= mChanged[NETWORK_WIFI];
            }
            if (("GSM").equals(networkType)
                    || ("GPRS").equals(networkType)) {
                prevState =mConnected[NETWORK_GSM];
                mConnected[NETWORK_GSM] = ni.isConnected();
                mConnected[NETWORK_MOBILE] = ni.isConnected();
                mChanged[NETWORK_GSM] = (prevState !=mConnected[NETWORK_GSM]);
                stateChanged |= mChanged[NETWORK_GSM];
            }
            if (("UMTS").equals(networkType)
                    || ("3G").equals(networkType)
                    || ("HSPA").equals(ni.getTypeName())) {
                prevState =mConnected[NETWORK_3G];
                mConnected[NETWORK_3G] =ni.isConnected();
                mConnected[NETWORK_MOBILE] = ni.isConnected();
                mChanged[NETWORK_3G] = (prevState !=mConnected[NETWORK_3G]);
                stateChanged |= mChanged[NETWORK_3G];
            }
            if (("CDMA").equals(networkType)) {
                prevState =mConnected[NETWORK_CDMA];
                mConnected[NETWORK_CDMA] =ni.isConnected();
                mConnected[NETWORK_MOBILE] = ni.isConnected();
                mChanged[NETWORK_CDMA] = (prevState !=mConnected[NETWORK_CDMA]);
                stateChanged |= mChanged[NETWORK_CDMA];
            }
            if (("LTE").equals(networkType)) {
                prevState =mConnected[NETWORK_LTE];
                mConnected[NETWORK_LTE] =ni.isConnected();
                mConnected[NETWORK_MOBILE] = ni.isConnected();
                mChanged[NETWORK_LTE] = (prevState !=mConnected[NETWORK_LTE]);
                stateChanged |= mChanged[NETWORK_LTE];
            }
            if (("MOBILE").equals(networkType)) {
                prevState =mConnected[NETWORK_LTE];
                mConnected[NETWORK_MOBILE] = ni.isConnected();
                mChanged[NETWORK_MOBILE] = (prevState !=mConnected[NETWORK_MOBILE]);
                stateChanged |= mChanged[NETWORK_MOBILE];
            }
        }
        return stateChanged;
    }

    private void notifyStateToAll() {
        for (INetworkStateReceiverListener listener : mListeners) {
            notifyState(listener);
        }
    }

    private void notifyState(INetworkStateReceiverListener listener) {
        if (listener != null) {
            listener.onNetworkStateChanged(mChanged, mConnected);
        }
    }

    public void addListener(INetworkStateReceiverListener l) {
        mListeners.add(l);
        notifyState(l);
    }

    public void removeListener(INetworkStateReceiverListener l) {
        mListeners.remove(l);
    }

    public interface INetworkStateReceiverListener {
        public void onNetworkStateChanged(boolean[] networkChanged, boolean[] networkState);
    }

    /*
    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equalsIgnoreCase(WIFI_STATE_CHANGE)) {
                switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)) {
                    case WifiManager.WIFI_STATE_ENABLED:
                    case WifiManager.WIFI_STATE_DISABLED: {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable(){
                            public void run() {
                                setCbWifiSupport(getActivity());
                            }
                        }, 5000);
                        break;
                    }
                    case WifiManager.WIFI_STATE_DISABLING:
                    case WifiManager.WIFI_STATE_ENABLING:
                    case WifiManager.WIFI_STATE_UNKNOWN:
                    default:
                        break;
                }
            }
        }
    };
     */

}

