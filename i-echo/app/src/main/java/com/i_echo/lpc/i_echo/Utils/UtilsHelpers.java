package com.i_echo.lpc.i_echo.Utils;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */
public class UtilsHelpers {

    private UtilsHelpers() { }

    public static String fileMediaFormat(String userId,int sessionIdIn) {
        return userId.replace('@','_')+Integer.toString(sessionIdIn)
                +"_audio_record.bin";
    }

    public static void writeVoiceMsgDataToFile(byte[] bytes, String fileName) {
        try {
            BufferedOutputStream bufO=
                    new BufferedOutputStream(new FileOutputStream(fileName, true));
            bufO.write(bytes);
            bufO.flush();
            bufO.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeVoiceMsgDataToStream(byte[] bytes, BufferedOutputStream bufO) {
        try {
            bufO.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean appInstalledOrNot(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    public static boolean isDriving(Context context) {
        return true; // for now
    }

    public static boolean isViewContains(View view, int rx, int ry) {
        int[] locThisView = new int[2];
        view.getLocationOnScreen(locThisView);

        int x = locThisView[0];
        int y = locThisView[1]; // y = locThisView[1] -MainActivity.getAppViewYOffset();
        int w = view.getWidth();
        int h = view.getHeight();
        Rect rect = new Rect(x, y, x + w, y + h);

        return rect.contains(rx, ry);
    }

    public static boolean isNightTime(int hour) {
        Calendar cal =Calendar.getInstance();

        // int millisecond = cal.get(Calendar.MILLISECOND);
        // int second = cal.get(Calendar.SECOND);
        // int minute = cal.get(Calendar.MINUTE);
        // int hour = cal.get(Calendar.HOUR);           //12 hour format
        // int dayofyear = cal.get(Calendar.DAY_OF_YEAR);
        // int year = cal.get(Calendar.YEAR);
        // int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
        // int dayofmonth = cal.get(Calendar.DAY_OF_MONTH);

        int hourofday = cal.get(Calendar.HOUR_OF_DAY);  //24 hour format

        if ((hourofday >hour && hourofday <24)
            ||(hourofday >=0 && hourofday <7))
            return true;
        else
            return false;
    }

    public static int calculateTimeTo(int morning, int night) {
        Calendar cal = Calendar.getInstance();
        int hourofday = cal.get(Calendar.HOUR_OF_DAY);  //24 hour format
        int minute = cal.get(Calendar.MINUTE);
        int timeToGo =0;

        if (morning < hourofday && hourofday <= night)
            timeToGo =60*(night-hourofday) +minute;
        else if (night< hourofday && hourofday <= 23)
            timeToGo = 60*(24-hourofday + morning) +minute;
        else if (0 <= hourofday && hourofday < morning)
            timeToGo = 60*(morning-hourofday) +minute;

        return timeToGo;
    }

    public static short incAppSeqNum(short appSeqNum) {
        if (appSeqNum<0x7FFF)
            appSeqNum++;                    // sequence numbers are expected to wrap around
        else
            appSeqNum=0;
        return appSeqNum;
    }

}
