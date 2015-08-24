package com.i_echo.lpc.i_echo;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

/**
 * Created by LPC-Home1 on 5/15/2015.
 */
public class AppProtUserMsg implements Parcelable {

    final static long secondsInYear =31556926;

    final static int USERMSG_NONE =0;
    final static int USERMSG_TEXT_FROM =1;
    final static int USERMSG_TEXT_TO =2;
    final static int USERMSG_VOICE_FROM =3;
    final static int USERMSG_VOICE_TO =4;
    final static int USERMSG_VOIP_FROM =5;
    final static int USERMSG_VOIP_TO =6;
    final static int USERMSG_PHONE_FROM =7;
    final static int USERMSG_PHONE_TO =8;
    final static int USERMSG_EMAIL_FROM =9;
    final static int USERMSG_EMAIL_TO =10;
    final static int USERMSG_PTT_FROM =11;
    final static int USERMSG_PTT_TO =12;

    int descriptor;
    boolean state;
    String msgString;
    long time;
    String file;

    // Message is the string is descriptor is Text msg
    // string is the filename if msg is voice message

    public AppProtUserMsg(int desc, boolean state, String msg, long t, String file) {
        this.descriptor = desc;
        this.state = state;
        this.msgString = msg;
        this.time =t;
        this.file =file;
    }
    public AppProtUserMsg(Parcel in) {
        descriptor = in.readInt();
        state = (in.readInt() ==1);
        msgString = in.readString();
        time = in.readLong();
    }

    public static final Parcelable.Creator<AppProtUserMsg> CREATOR
            = new Parcelable.Creator<AppProtUserMsg>() {
        public AppProtUserMsg createFromParcel(Parcel in ) {
            return new AppProtUserMsg( in );
        }

        public AppProtUserMsg[] newArray(int size) {
            return new AppProtUserMsg[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeInt(descriptor);
        dest.writeInt((state) ? 1: 0);
        dest.writeString(msgString);
        dest.writeLong(time);
    }

    public int getUserMsgDescriptor() { return descriptor; }
    public void setUserMsgDescriptor(int desc) { descriptor =desc; }
    public boolean getUserMsgState() { return state; }
    public void setUserMsgState(boolean s) { state =s; }
    public String getUserMsgString() { return msgString; }
    public void setUserMsgString(String str) { msgString =str; }
    public long getUserMsgTime() { return time; }
    public void setUserMsgTime(long t) { time =t; }
    public String getUserMsgFile() { return file; }
    public void setUserMsgFile(String f) { file =f; }
    public String getUserMsgTimeToString(Context context) {
        long timeNow =System.currentTimeMillis();
        int flags =DateUtils.FORMAT_ABBREV_MONTH
                | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;
        if ((timeNow-time) >= secondsInYear*1000)
            flags |=DateUtils.FORMAT_SHOW_YEAR;

        return DateUtils.formatDateTime(context, time, flags);
    }
}
