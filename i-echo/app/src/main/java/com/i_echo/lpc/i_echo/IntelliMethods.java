package com.i_echo.lpc.i_echo;

import java.util.ArrayList;

/**
 * Created by LPC-Home1 on 5/20/2015.
 */
public class IntelliMethods {

    private IntelliMethods() { }

    /*
    * Propose CM list from this user
    */
    public static int[] getProposedCmListAtSrc(boolean[] userState) {
        return produceCmListFromExpertSystem(userState, true);
    }

    /*
    * Match the proposed CM list from the source user at this (target) user
     */
    public static ArrayList<CmMatch> getMatchedCmListAtDst(int cmList[], int cmNum,
                boolean[] userState, ContactInfoUser appUser) {
        int[] cmAvailable =produceCmListFromExpertSystem(userState, false);
        int limit =(cmAvailable.length<cmNum) ? cmNum:cmAvailable.length;
        int[] cmUnion =getCmListUnion(cmAvailable, cmList, limit);

        if (cmUnion==null)
            return getDefaultCmReturn(appUser);
        else {
            ArrayList<CmMatch> cmMatches = new ArrayList<CmMatch>();
            for (int i =0; i<cmUnion.length && cmUnion[i] !=0; i++) {
                        // reached the end of the int array if value =0
                int index=0;
                int reasons[] = new int[]
                        {AppState.APP_USERSTATE_NONE,
                        AppState.APP_USERSTATE_NONE,
                        AppState.APP_USERSTATE_NONE};
                switch (cmUnion[i]) {
                    case CmIdxItems.CM_TYPE_PHONE: {
                        reasons[index++] =AppState.APP_USERSTATE_HAPPYFACE;
                        if (!userState[AppState.APP_USERSTATE_WIFI])
                            reasons[index] =AppState.APP_USERSTATE_WIFI_NO;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_PHONE,
                                reasons, appUser.getAppUserMsdn());
                    }
                    case CmIdxItems.CM_TYPE_VOIP: {
                        if (userState[AppState.APP_USERSTATE_ROAMING])
                            reasons[index++] =AppState.APP_USERSTATE_ROAMING;
                        if (userState[AppState.APP_USERSTATE_WIFI])
                            reasons[index] =AppState.APP_USERSTATE_WIFI;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_VOIP,
                                reasons, appUser.getAppUserVoipUrl());
                        break;
                    }
                    case CmIdxItems.CM_TYPE_PTT: {
                        if (userState[AppState.APP_USERSTATE_ROAMING])
                            reasons[index++] =AppState.APP_USERSTATE_ROAMING;
                        if (userState[AppState.APP_USERSTATE_WIFI])
                            reasons[index] =AppState.APP_USERSTATE_WIFI;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_PTT,
                                reasons, appUser.getAppUserId());
                        break;
                    }
                    case CmIdxItems.CM_TYPE_TEXTMSG: {
                        if (userState[AppState.APP_USERSTATE_SLEEPING])
                            reasons[index++] =AppState.APP_USERSTATE_SLEEPING;
                        if (userState[AppState.APP_USERSTATE_BUSY])
                            reasons[index++] =AppState.APP_USERSTATE_BUSY;
                        if (userState[AppState.APP_USERSTATE_DRIVING])
                            reasons[index] =AppState.APP_USERSTATE_DRIVING;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_TEXTMSG,
                                reasons, appUser.getAppUserId());
                        break;
                    }
                    case CmIdxItems.CM_TYPE_VOICEMSG_SILENT: {
                        if (userState[AppState.APP_USERSTATE_WIFI])
                            reasons[index++] =AppState.APP_USERSTATE_WIFI;
                        if (userState[AppState.APP_USERSTATE_SLEEPING])
                            reasons[index++] =AppState.APP_USERSTATE_SLEEPING;
                        if (userState[AppState.APP_USERSTATE_BUSY])
                            reasons[index++] =AppState.APP_USERSTATE_BUSY;
                        if (userState[AppState.APP_USERSTATE_DRIVING])
                            reasons[index] =AppState.APP_USERSTATE_DRIVING;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_VOICEMSG_SILENT,
                                reasons, appUser.getAppUserId());
                        break;
                    }
                    case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE: {
                        if (userState[AppState.APP_USERSTATE_WIFI])
                            reasons[index++] =AppState.APP_USERSTATE_WIFI;
                        if (userState[AppState.APP_USERSTATE_DRIVING])
                            reasons[index] =AppState.APP_USERSTATE_DRIVING;
                        if (userState[AppState.APP_USERSTATE_BUSY])
                            reasons[index] =AppState.APP_USERSTATE_BUSY;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE,
                                reasons, appUser.getAppUserId());
                        break;
                    }
                    case CmIdxItems.CM_TYPE_EMAIL: {
                        if (userState[AppState.APP_USERSTATE_SLEEPING])
                            reasons[index++] =AppState.APP_USERSTATE_SLEEPING;
                        if (userState[AppState.APP_USERSTATE_BUSY])
                            reasons[index++] =AppState.APP_USERSTATE_BUSY;
                        if (userState[AppState.APP_USERSTATE_DRIVING])
                            reasons[index] =AppState.APP_USERSTATE_DRIVING;
                        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_EMAIL,
                                reasons, appUser.getAppUserEmailUrl());
                        break;
                    }
                }
            }
            return cmMatches;
        }
    }

    public static int[] getCmListFromCmArray(ArrayList<CmMatch> cms) {
        int[] cmList = new int[cms.size()];

        for (int i=0; i< cms.size(); i++) {
            cmList[i] =cms.get(i).getCmIdx();
        }
        return cmList;
    }

    public static String[] getCmInfoFromCmArray(ArrayList<CmMatch> cms) {
        String[] cmInfoString = new String[cms.size()];

        for (int i=0; i< cms.size(); i++) {
            cmInfoString[i] =cms.get(i).getCmInfoString();
        }
        return cmInfoString;
    }

    public static int[] getCmListUnion(int[] cmMatch1, int[] cmMatch2, int limit) {
        int[] cmProposal = new int[] {0, 0, 0, 0, 0, 0, 0};     // length =7

        int index =0;
        for (int i=0; i< ((limit<7)?limit:7) && cmMatch1[i] !=0; i++) {
            if (cmContains(cmMatch1[i],cmMatch2, cmMatch2.length))
                cmProposal[index++] =cmMatch1[i];
        }
        if (index>0)
            return cmProposal;
        else
            return null;
    }

    // By default, when there are no other methods available
    // return Email method
    private static ArrayList<CmMatch> getDefaultCmReturn(ContactInfoUser appUser) {
        ArrayList<CmMatch> cmMatches = new ArrayList<CmMatch>();
        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_VOICEMSG_SILENT,
                new int[]{AppState.APP_USERSTATE_HAPPYFACE},
                appUser.getAppUserEmailUrl());
        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_TEXTMSG,
                new int[]{AppState.APP_USERSTATE_HAPPYFACE},
                appUser.getAppUserEmailUrl());
        cmMatches =cmListAddMethod(cmMatches, CmIdxItems.CM_TYPE_EMAIL,
                new int[]{AppState.APP_USERSTATE_HAPPYFACE},
                appUser.getAppUserEmailUrl());

        return cmMatches;
    }

    public static String[] getCmInfoStrings(int[] cmList, ContactInfoUser appUser) {
        String[] result= new String[cmList.length];

        for (int i=0; i< cmList.length; i++) result[i] =null;
        for (int i=0; i< cmList.length; i++) {
            if (cmList[i] != CmIdxItems.CM_TYPE_NONE) {
                switch(cmList[i]) {
                    case CmIdxItems.CM_TYPE_PHONE: {
                        result[i] =appUser.getAppUserMsdn();
                        break;
                    }
                    case CmIdxItems.CM_TYPE_VOIP: {
                        result[i] =appUser.getAppUserVoipUrl();
                        break;
                    }
                    case CmIdxItems.CM_TYPE_EMAIL: {
                        result[i] =appUser.getAppUserEmailUrl();
                        break;
                    }
                    case CmIdxItems.CM_TYPE_PTT:
                    case CmIdxItems.CM_TYPE_TEXTMSG:
                    case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE:
                    default: {
                        result[i] =appUser.getAppUserId();
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static boolean cmContains(int cm, int[] cmList, int cmNum) {
        boolean result =false;

        for (int i =0; i< cmNum; i++) {
            if (cmList[i] == cm) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static ArrayList<CmMatch> cmListAddMethod(ArrayList<CmMatch> cmMatches,
                                 int cm, int[] reasons, String cmInfo){
        CmMatch cmMatch = new CmMatch(cm, reasons, cmInfo);
        cmMatches.add(cmMatch);

        return cmMatches;
    }

    /*
    * return the target user selected CM to the source (initial calling party user)
     */
    public static int[] getCmListResponseAtDst(int cmMethod) {
        int[] result = new int[] {cmMethod};
            // single item for now
        return result;
    }

    /*
    Expert System that takes the user's conditions and produces a list of Communications
    Methods - the conditions are the user/phone's parameter variable/value pairs

    The following are user status parameters:
    userState[AppState.APP_USERSTATE_ROAMING]
    userState[AppState.APP_USERSTATE_SLEEPING]
    userState[AppState.APP_USERSTATE_DRIVING]

    The following are device capability parameters:
    userState[AppState.APP_USERSTATE_WIFI]
    userState[AppState.APP_USERSTATE_PHONESUPPORT]
    userState[AppState.APP_USERSTATE_VOIPSUPPORT]
    userState[AppState.APP_USERSTATE_EMAILSUPPORT]

    The following are user-setted preferences:
    userState[AppState.APP_USERSTATE_BUSY]
    userState[AppState.APP_USERSTATE_AUTOANSWER]

    We have the following communications methods:
    CmIdxList.CM_TYPE_VOICEMSG_SILENT
    CmIdxList.CM_TYPE_VOICEMSG_ACTIVE
    CmIdxList.CM_TYPE_TEXTMSG
    CmIdxList.CM_TYPE_PHONE
    CmIdxList.CM_TYPE_VOIP
    CmIdxList.CM_TYPE_EMAIL
    */


    public static int[] produceCmListFromExpertSystem(final boolean[] userState, final boolean bMask) {
        final boolean bStateDriving = userState[AppState.APP_USERSTATE_DRIVING];
        final boolean bStateBusy = userState[AppState.APP_USERSTATE_BUSY];
        final boolean bStateSleeping = (!bMask) && userState[AppState.APP_USERSTATE_SLEEPING];
        final boolean bStateRoaming = userState[AppState.APP_USERSTATE_ROAMING];
        final boolean bStateWiFi = userState[AppState.APP_USERSTATE_WIFI];
        final boolean bStatePhone = userState[AppState.APP_USERSTATE_PHONESUPPORT];
        final boolean bStateVoip = userState[AppState.APP_USERSTATE_VOIPSUPPORT];
        final boolean bStateEmail = userState[AppState.APP_USERSTATE_EMAILSUPPORT];
        final boolean bStateAuto = userState[AppState.APP_USERSTATE_AUTO];

        final byte vMsgSilent = CmIdxItems.CM_TYPE_VOICEMSG_SILENT;
        final byte vMsgActive = CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE;
        final byte tMsg = CmIdxItems.CM_TYPE_TEXTMSG;
        final byte ptt = CmIdxItems.CM_TYPE_PTT;
        final byte phone = CmIdxItems.CM_TYPE_PHONE;
        final byte voip = CmIdxItems.CM_TYPE_VOIP;
        final byte email = CmIdxItems.CM_TYPE_EMAIL;

        int[] cmProposal = new int[] {0, 0, 0, 0, 0, 0, 0};     // length =7

        if (bStateBusy && !bStateDriving) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }

        if (bStateDriving && !bStateSleeping) {
            addComMethod(cmProposal, ptt);
            addComMethod(cmProposal, vMsgActive);
        }

        if (bStateDriving && bStateSleeping) {
            // cmProposal[i] = NONE; // Contradictory - should assume to be non-existent
            addComMethod(cmProposal, vMsgSilent);
        }

        if (bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateSleeping
                && !bStateBusy
                && !bStateVoip
                && !bStateEmail) {   // Maybe can remove email support
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStateSleeping
                && !bStateEmail) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (bStateRoaming // how would this differ from the previous case
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStateSleeping
                && bStateEmail) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }
        if (bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStateSleeping
                && bStateVoip
                && !bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, voip);
        }
        if (bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStateSleeping
                && !bStateVoip
                && bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }
        if (bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStateSleeping
                && bStateVoip
                && bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, voip);
            addComMethod(cmProposal, email);
        }
        if (bStateRoaming
                && !bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStateSleeping) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (bStateRoaming
                && !bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStateSleeping) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (!bStateRoaming
                && !bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStatePhone
                && !bStateSleeping) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, phone);
        }
        if (!bStateRoaming
                && !bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStatePhone
                && !bStateSleeping) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (!bStateRoaming
                && !bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStateSleeping) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStateSleeping
                && !bStateEmail) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStateSleeping
                && bStateEmail) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStatePhone
                && !bStateSleeping
                && !bStateVoip
                && !bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStatePhone
                && !bStateSleeping
                && !bStateVoip
                && !bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, phone);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStatePhone
                && !bStateSleeping
                && bStateVoip
                && !bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, voip);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStatePhone
                && !bStateSleeping
                && !bStateVoip
                && bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStatePhone
                && !bStateSleeping
                && bStateVoip
                && !bStateEmail) {
            addComMethod(cmProposal, phone);
            addComMethod(cmProposal, voip);
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStatePhone
                && !bStateSleeping
                && !bStateVoip
                && bStateEmail) {
            addComMethod(cmProposal, phone);
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && !bStatePhone
                && !bStateSleeping
                && bStateVoip
                && bStateEmail) {
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, voip);
            addComMethod(cmProposal, email);
        }
        if (!bStateRoaming
                && bStateWiFi
                && !bStateDriving
                && !bStateBusy
                && bStatePhone
                && !bStateSleeping
                && bStateVoip
                && bStateEmail) {
            addComMethod(cmProposal, phone);
            addComMethod(cmProposal, voip);
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }

        if (!bStateRoaming
                && bStatePhone
                && !bStateDriving
                && !bStateBusy
                && !bStateSleeping) {
            addComMethod(cmProposal, phone);
            addComMethod(cmProposal, vMsgActive);
            addComMethod(cmProposal, tMsg);
        }

        if (isEmpty(cmProposal)) {
            addComMethod(cmProposal, vMsgSilent);
            addComMethod(cmProposal, tMsg);
            addComMethod(cmProposal, email);
        }

        return cmProposal;
    }

    private static void addComMethod(int[] cmArray, int cm) {
        int i;
        boolean found = false;

        for (i=0; i< cmArray.length; i++) {
            if (cmArray[i] == 0) {      // reached the end of the initialized int array
                break;
            }
            else if (cmArray[i] == cm) {
                    found = true;
                    break;
            }
        }
        if (!found && i< cmArray.length)
            cmArray[i] =cm;
    }

    private static boolean isEmpty(int[] cmArray) {
        boolean result =true;

        for (int i=0; i< cmArray.length; i++) {
            if (cmArray[i] > 0) {      // reached the end of the initialized int array
                result =false;
                break;
            }
        }
        return result;
    }
}
