package com.i_echo.lpc.i_echo.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.i_echo.lpc.i_echo.Constants;

import java.util.List;

/**
 * Created by LPC-Home1 on 7/6/2015.
 */
public class UtilsPhone extends PhoneStateListener {
    public static String LOGTAG = "NetworkPhoneStateListener";

    Context mContext;
    IPhoneServiceStateListener mCallback;

    public UtilsPhone(Context context) {
        mContext =context;
        mCallback = (IPhoneServiceStateListener) context;
    }

    public interface IPhoneServiceStateListener {
        public void onPhoneInService();
        public void onPhoneOutOfService();
    }

    public static boolean isDevicePhone(Context context) {
        TelephonyManager tManager =
                (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        if(tManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE){
            // other constants: PHONE_TYPE_CDMA, PHONE_TYPE_GSM, PHONE_TYPE_SIP
            return false;
        }
        else{
            return true;
        }
    }

    public static boolean isPhoneConnected(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null || !info.isConnected())
            return false;
        else return true;
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);

        if (Constants.DEBUGGING) {
            Log.i(LOGTAG, "onServiceStateChanged: " + serviceState.toString());
            Log.i(LOGTAG, "onServiceStateChanged: getOperatorAlphaLong "
                    + serviceState.getOperatorAlphaLong());
            Log.i(LOGTAG, "onServiceStateChanged: getOperatorAlphaShort "
                    + serviceState.getOperatorAlphaShort());
            Log.i(LOGTAG, "onServiceStateChanged: getOperatorNumeric "
                    + serviceState.getOperatorNumeric());
            Log.i(LOGTAG, "onServiceStateChanged: getIsManualSelection "
                    + serviceState.getIsManualSelection());
            Log.i(LOGTAG, "onServiceStateChanged: getRoaming " + serviceState.getRoaming());
        }

        if (Constants.DEBUGGING) {
            Log.i(LOGTAG, "onServiceStateChanged: ServiceState " + serviceState.getState());
        }
        switch (serviceState.getState()) {
            case ServiceState.STATE_IN_SERVICE: {
                mCallback.onPhoneInService();
                break;
            }
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
            case ServiceState.STATE_POWER_OFF: {
                mCallback.onPhoneOutOfService();
                break;
            }
        }
    }


    @Override
    public void onDataConnectionStateChanged(int state, int networkType) {
        super.onDataConnectionStateChanged(state, networkType);
        switch (state) {
            case TelephonyManager.DATA_CONNECTED: {
                if (Constants.DEBUGGING) {
                    Log.i(LOGTAG, "onDataConnectionStateChanged: DATA_CONNECTED");
                }
                break;
            }
            case TelephonyManager.DATA_DISCONNECTED:
            case TelephonyManager.DATA_CONNECTING:
            case TelephonyManager.DATA_SUSPENDED:
            default: {
                if (Constants.DEBUGGING) {
                    Log.i(LOGTAG, "onDataConnectionStateChanged: " + state);
                }
                break;
            }
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_UMTS: {
                if (Constants.DEBUGGING) {
                    Log.i(LOGTAG, "onDataConnectionStateChanged: " + networkType);
                }
                break;
            }
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default: {
                if (Constants.DEBUGGING) {
                    Log.i(LOGTAG, "onDataConnectionStateChanged: Undefined Network");
                }
                break;
            }
        }
    }

    @Override
    public void onDataActivity(int direction) {
        super.onDataActivity(direction);

        switch (direction) {
            case TelephonyManager.DATA_ACTIVITY_NONE:
            case TelephonyManager.DATA_ACTIVITY_IN:
            case TelephonyManager.DATA_ACTIVITY_OUT:
            case TelephonyManager.DATA_ACTIVITY_INOUT:
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                break;
            default: {
                if (Constants.DEBUGGING) {
                    Log.i(LOGTAG, "onDataActivity: UNKNOWN " + direction);
                }
                break;
            }
        }
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                break;
            default: {
                Log.i(LOGTAG, "UNKNOWN_STATE: " + state);
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCellLocationChanged(CellLocation location) {
        super.onCellLocationChanged(location);

        if (location instanceof GsmCellLocation) {
            GsmCellLocation gcLoc = (GsmCellLocation) location;
            if (Constants.DEBUGGING) {
                Log.i(LOGTAG, "onCellChanged: GsmCell " + gcLoc.toString());
                Log.i(LOGTAG, "onCellChanged: GsmCell getCid " + gcLoc.getCid());
                Log.i(LOGTAG, "onCellChanged: GsmCell getLac " + gcLoc.getLac());
                Log.i(LOGTAG, "onCellChanged: GsmCell getPsc" + gcLoc.getPsc()); // Requires min API 9
            }
        } else if (location instanceof CdmaCellLocation) {
            if (Constants.DEBUGGING) {
                CdmaCellLocation ccLoc = (CdmaCellLocation) location;
                Log.i(LOGTAG, "onCellChanged: CdmaCell " + ccLoc.toString());
                Log.i(LOGTAG, "onCellChanged: CdmaCell getBaseStationId " + ccLoc.getBaseStationId());
                Log.i(LOGTAG, "onCellChanged: CdmaCell getBaseStationLatitude "
                        + ccLoc.getBaseStationLatitude());
                Log.i(LOGTAG, "onCellChanged: CdmaCell getBaseStationLongitude"
                        + ccLoc.getBaseStationLongitude());
                Log.i(LOGTAG, "onCellChanged: CdmaCell getNetworkId "
                        + ccLoc.getNetworkId());
                Log.i(LOGTAG, "onCellChanged: CdmaCell getSystemId "
                        + ccLoc.getSystemId());
            }
        } else {
            if (Constants.DEBUGGING) {
                Log.i(LOGTAG, "onCellChanged: " + location.toString());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onCellInfoChanged(List<CellInfo> cellInfo) {
        super.onCellInfoChanged(cellInfo);
    }

    @Override
    public void onCallForwardingIndicatorChanged(boolean cfi) {
        super.onCallForwardingIndicatorChanged(cfi);
    }

    @Override
    public void onMessageWaitingIndicatorChanged(boolean mwi) {
        super.onMessageWaitingIndicatorChanged(mwi);
    }
}
