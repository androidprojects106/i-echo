package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by LPC-Home1 on 3/26/2015.
 */
public class ContactInfoUser implements Parcelable {
    private String appUserIdString;             // Used to identify this user and device and the app instance
    private String nameFirst;
    private String nameLast;
    private String appUserMsdn;                 // MSDN in free form (may be rejected by Phone app)
    private String appUserVoipUrl;                         // VoIP appUserVoipUrl used for VoIP, PTT, and EMail
    private String appUserEmailUrl;

    public ContactInfoUser() {
        appUserIdString = null;
        nameFirst = null;
        nameLast = null;
        appUserMsdn = null;
        appUserVoipUrl = null;
        appUserEmailUrl = null;
    }

    public ContactInfoUser (Parcel in ) {
        appUserIdString = in.readString();
        nameFirst = in.readString();
        nameLast = in.readString();
        appUserMsdn = in.readString();
        appUserVoipUrl = in.readString();
        appUserEmailUrl = in.readString();
    }


    public void updateInfoUser(int cm, String cmInfo) {
        switch (cm) {
            case CmIdxItems.CM_TYPE_PHONE: {
                setAppUserMsdn(cmInfo);
                break;
            }
            case CmIdxItems.CM_TYPE_VOIP: {
                setAppUserVoipUrl(cmInfo);
                break;
            }
            case CmIdxItems.CM_TYPE_EMAIL: {
                setAppUserEmailUrl(cmInfo);
                break;
            }
            case CmIdxItems.CM_TYPE_PTT:
            case CmIdxItems.CM_TYPE_TEXTMSG:
            case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE:
            case CmIdxItems.CM_TYPE_VOICEMSG_SILENT:
            case CmIdxItems.CM_TYPE_NONE: {
                break;
            }
        }
    }

    public static final Parcelable.Creator<ContactInfoUser> CREATOR
            = new Parcelable.Creator<ContactInfoUser>() {
            public ContactInfoUser createFromParcel(Parcel in ) {
                return new ContactInfoUser( in );
            }

            public ContactInfoUser[] newArray(int size) {
                return new ContactInfoUser[size];
            }
        };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(appUserIdString);
        dest.writeString(nameFirst);
        dest.writeString(nameLast);
        dest.writeString(appUserMsdn);
        dest.writeString(appUserVoipUrl);
        dest.writeString(appUserEmailUrl);
    }

    /*
        Utilities
     */

    public String getAppUserId() { return appUserIdString; }
    public void setAppUserId(String Id) {appUserIdString =Id; }
    public String getNameFirst() { return nameFirst; }
    public void setNameFirst(String string) {nameFirst =string; }
    public String getNameLast() { return nameLast; }
    public void setNameLast(String string) {nameLast =string; }
    public String getAppUserMsdn() { return appUserMsdn; }
    public void setAppUserMsdn(String string) { appUserMsdn =string; }
    public String getAppUserVoipUrl() { return appUserVoipUrl; }
    public void setAppUserVoipUrl(String string) {
        appUserVoipUrl =string; }
    public String getAppUserEmailUrl() { return appUserEmailUrl; }
    public void setAppUserEmailUrl(String string) {
        appUserEmailUrl =string; }

    public String getUserDisplayInfo() {
        String displayInfo =appUserIdString+" \n\n"
                +"First Name:                   "+nameFirst+" \n"
                +"Last Name:                   "+nameLast+" \n"
                +"Phone:                           "+ appUserMsdn +" \n"
                +"Skype:                              "+ appUserVoipUrl +" \n\n"
                +"EMail:                   "+ appUserEmailUrl;

        return displayInfo;
    }
}
