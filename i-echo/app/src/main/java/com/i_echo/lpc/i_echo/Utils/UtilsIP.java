package com.i_echo.lpc.i_echo.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.i_echo.lpc.i_echo.Constants;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by LPC-Home1 on 7/6/2015.
 */
public class UtilsIP {

    public static final int ADDRESS_IPv4 = 0;
    public static final int ADDRESS_IPv6 = 1;

    private UtilsIP() { }

    // For now we are working with IPv4 addresses only
    public static String getLocalIpAddress(int useIPver) {
        String address = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        address = inetAddress.getHostAddress().toUpperCase();
                        if (inetAddress instanceof Inet4Address &&
                                address.length() < 18) {
                            if (useIPver == ADDRESS_IPv4)
                                return inetAddress.getHostAddress();
                        } else if (useIPver == ADDRESS_IPv6) {      // use IPv6
                            int delim = address.indexOf('%'); // drop IPv6 port suffix
                            return delim < 0 ? address : address.substring(0, delim);
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            if (Constants.DEBUGGING)
                Log.e("getLocalIpAddress", ex.toString());
            //do nothing
        }
        return null;
    }


}
