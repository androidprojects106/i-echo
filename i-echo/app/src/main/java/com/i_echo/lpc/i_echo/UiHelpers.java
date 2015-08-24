package com.i_echo.lpc.i_echo;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.i_echo.lpc.i_echo.AppProtUserMsg;
import com.i_echo.lpc.i_echo.AppState;
import com.i_echo.lpc.i_echo.CmIdxItems;
import com.i_echo.lpc.i_echo.R;

import java.io.IOException;

/**
 * Created by LPC-Home1 on 6/24/2015.
 */
public class UiHelpers {

    private UiHelpers() {}

    public static void hideKeyboard(Context context, EditText view) {
        // hide keyboard
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void scrollListViewToBottom(final ListView lv, final SimpleCursorAdapter adapter) {
        lv.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                lv.setSelection(adapter.getCount() - 1);
            }
        });
    }


    public static void vibrateNotification(Context context) {
        // Get instance of Vibrator from current Context
        final Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Output yes if can vibrate, no otherwise
        if (v.hasVibrator()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Start without a delay
                    // Each element then alternates between vibrate, sleep, vibrate, sleep...
                    long[] pattern = {0, 100, 500, 300, 200, 100, 500, 200, 100};

                    // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
                    v.vibrate(pattern, -1);
                }
            }).start();
        }
    }

    public static void playNotificationSound(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone rt = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
            rt.play();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void playMediaPlayerSound(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mplayer = new MediaPlayer();
            mplayer.setDataSource(context, notification);
            final AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (am.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mplayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mplayer.setLooping(true);
                mplayer.prepare();
                mplayer.start();
            }
        }
        catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void playNotificationAndSound(Context context, String title, String message) {
        try {
            //Define Notification Manager
            NotificationManager nManager
                    = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            //Define sound URI
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context.getApplicationContext())
                            .setSmallIcon(R.drawable.iechoicon)
                            .setContentTitle(context.getString(R.string.app_name))
                            .setContentText(message)
                            .setSound(notification); //This sets the sound to play
            //Display notification
            nManager.notify(0, mBuilder.build());
        }
        catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static int getStateImageId(int value) {
        switch (value) {
            case AppState.APP_USERSTATE_HAPPYFACE:
                return R.drawable.happy006;
            case AppState.APP_USERSTATE_WIFI:
                return R.drawable.wifion019;
            case AppState.APP_USERSTATE_WIFI_NO:
                return R.drawable.wifino018;
            case AppState.APP_USERSTATE_ROAMING:
                return R.drawable.athomenot;
            case AppState.APP_USERSTATE_BUSY:
                return R.drawable.busy016;
            case AppState.APP_USERSTATE_SLEEPING:
                return R.drawable.sleeping015;
            case AppState.APP_USERSTATE_DRIVING:
                return R.drawable.driving003;
            case AppState.APP_USERSTATE_AUTO:
                return R.drawable.roboticon3;
            default:
                return R.drawable.transparent;
        }
    }

    public static int getMsgImageId(int value) {
        switch (value) {
            case AppProtUserMsg.USERMSG_PHONE_FROM:
            case AppProtUserMsg.USERMSG_PHONE_TO:
                return R.drawable.phone;
            case AppProtUserMsg.USERMSG_VOIP_FROM:
            case AppProtUserMsg.USERMSG_VOIP_TO:
                return R.drawable.skypeicon;
            case AppProtUserMsg.USERMSG_TEXT_FROM:
                return R.drawable.lefttext009;
            case AppProtUserMsg.USERMSG_TEXT_TO:
                return R.drawable.righttext014;
            case AppProtUserMsg.USERMSG_VOICE_FROM:
                return R.drawable.leftaudioicon;
            case AppProtUserMsg.USERMSG_VOICE_TO:
                return R.drawable.rightaudioicon;
            case AppProtUserMsg.USERMSG_EMAIL_FROM:
            case AppProtUserMsg.USERMSG_EMAIL_TO:
                return R.drawable.emailicon3;
            case AppProtUserMsg.USERMSG_PTT_FROM:
            case AppProtUserMsg.USERMSG_PTT_TO:
                return R.drawable.talk001;
            default:
                return R.drawable.transparent;
        }
    }

    public static int getCmImageId(int value) {
        switch (value) {
            case CmIdxItems.CM_TYPE_PHONE:
                return R.drawable.phone;
            case CmIdxItems.CM_TYPE_VOIP:
                return R.drawable.skypeicon;
            case CmIdxItems.CM_TYPE_TEXTMSG:
                return R.drawable.lefttext009;
            case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE:
                return R.drawable.leftaudioicon;
            case CmIdxItems.CM_TYPE_VOICEMSG_SILENT:
                return R.drawable.noaudio011;
            case CmIdxItems.CM_TYPE_EMAIL:
                return R.drawable.emailicon3;
            case CmIdxItems.CM_TYPE_PTT:
                return R.drawable.talk001;
            case CmIdxItems.CM_TYPE_NONE:
            default:
                return R.drawable.transparent;
        }
    }

    public static int getImageId(boolean flag) {
        if (flag) {
            return R.drawable.greenokicon;
        } else {
            return R.drawable.rednotokicon;
        }
    }
}
