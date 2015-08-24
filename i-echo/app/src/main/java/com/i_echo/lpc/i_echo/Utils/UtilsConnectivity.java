package com.i_echo.lpc.i_echo.Utils;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.net.NetworkInfo;

import java.util.List;

/**
 * Created by LPC-Home1 on 7/6/2015.
 */

public class UtilsConnectivity {

    public static final int NUMBER_OF_NETWORKS = 6;

    public static final int NETWORK_WIFI = 0;              // constants for network Ids
    public static final int NETWORK_GSM = 1;               // constants for network Ids
    public static final int NETWORK_3G = 2;                // constants for network Ids
    public static final int NETWORK_CDMA = 3;                // constants for network Ids
    public static final int NETWORK_LTE = 4;
    public static final int NETWORK_MOBILE = 5;
    public static final int NETWORK_ANY = 100;              // constants for network Ids

    public static final int ADDRESS_IPv4 = 0;
    public static final int ADDRESS_IPv6 = 1;

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isAirplaneModeOn(Context context) {
        boolean result;
        ContentResolver contentResolver = context.getContentResolver();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            result = Settings.System.getInt(contentResolver, Settings.System.AIRPLANE_MODE_ON, 0)
                    != 0;
        } else {
            result = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
                    != 0;
        }
        return result;
    }

    public static boolean hasGpsSupport(Context context)  {
        PackageManager pManager = context.getPackageManager();
        return pManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    public static boolean hasGpsSupportOld(Context context)  {
        final LocationManager lManager =
                (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        if ( lManager == null )
            return false;

        final List<String> providers = lManager.getAllProviders();
        if ( providers == null )
            return false;
        return
                providers.contains(LocationManager.GPS_PROVIDER);
    }

    /**
     * Check if there is any connectivity
     * @param context
     * @return
     */

    public static boolean isRoaming(Context context) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null || !info.isConnected())
            return false;
        else return info.isRoaming();
    }

    public static boolean isConnectedDataWifi(Context context) {
        return isConnectedNetwork(context, NETWORK_WIFI);
    }

    public static boolean isConnectedDataMobile(Context context) {
        return isConnectedNetwork(context, NETWORK_MOBILE);
    }

    public static boolean isConnectedNetwork(Context context, int network) {
        boolean isConnectedWifi = false;
        boolean isConnectedMobile = false;

        ConnectivityManager cMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cMgr == null)
            return false;
        else {
            NetworkInfo[] netInfo = cMgr.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (("WIFI").equals(ni.getTypeName()))
                    if (ni.isConnected())
                        isConnectedWifi = true;
                if (("MOBILE").equals(ni.getTypeName()))
                    if (ni.isConnected())
                        isConnectedMobile = true;
            }
            switch (network) {
                case NETWORK_WIFI:
                    return isConnectedWifi;
                case NETWORK_MOBILE:
                    return isConnectedMobile;
                case NETWORK_ANY:
                    return isConnectedWifi || isConnectedMobile;
                default:
                    return false;
            }
        }
    }

    /**
     * Check if there is fast connectivity
     * @param context
     * @return
     */
    public static boolean isConnectedFast(Context context){
        NetworkInfo info = UtilsConnectivity.getNetworkInfo(context);
        return (info != null && info.isConnected() && UtilsConnectivity.isConnectionFast(info.getType(), info.getSubtype()));
    }

    /**
     * Check if the connection is fast
     * @param type
     * @param subType
     * @return
     */
    public static boolean isConnectionFast(int type, int subType){
        if(type==ConnectivityManager.TYPE_WIFI){
            return true;
        }else if(type== ConnectivityManager.TYPE_MOBILE){
            switch(subType){
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
            /*
             * Above API level 7, make sure to set android:targetSdkVersion
             * to appropriate level to use these
             */
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        }
        else{
            return false;
        }
    }


    /**
     * Get the network info
     * @param context
     * @return
     */
    public static NetworkInfo getNetworkInfo(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }
}
