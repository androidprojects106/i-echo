package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import java.util.LinkedList;

/**
 * Created by LPC-Home1 on 3/26/2015.
 */
public class CmIdxItems {
    public static final int DIRECTION_CALLIN =0;
    public static final int DIRECTION_CALLOUT =1;

    public static final int CM_TYPE_NONE = 0;
    public static final int CM_TYPE_PHONE = 1;
    public static final int CM_TYPE_VOIP = 2;
    public static final int CM_TYPE_PTT = 3;
    public static final int CM_TYPE_TEXTMSG = 4;
    public static final int CM_TYPE_VOICEMSG_ACTIVE = 5;
    public static final int CM_TYPE_VOICEMSG_SILENT = 6;
    public static final int CM_TYPE_EMAIL = 7;

    public final static LinkedList<CmIdx> cmIdxLinkedList;

    // Indexed IDs for Comm Methods supported by the application
    static {
        CmIdx cmIdx;

        cmIdxLinkedList = new LinkedList<CmIdx>();

        cmIdx = new CmIdx(CM_TYPE_PHONE,"Phone", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_VOIP,"Skype", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_PTT,"Voice Chat", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_TEXTMSG,"Text Message", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_VOICEMSG_ACTIVE,"Voice Message Played", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_VOICEMSG_SILENT,"Voice Message Muted", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_EMAIL,"Email", "In", "Out"); cmIdxLinkedList.add(cmIdx);
        // "In" and "Out" at the called party are not yet implemented
    }

    CmIdxItems() {}              // blank constructor

    public static CmIdx getCmIdx(int idx) {

        for (CmIdx cmidx: cmIdxLinkedList) {
            if (cmidx.getIdx() == idx)
                return cmidx;
        }
        return null;
    }

    public static String getCmIdxString(int idx) {

        for (CmIdx cmidx: cmIdxLinkedList) {
            if (cmidx.getIdx() == idx)
                return cmidx.getCmString();
        }
        return null;
    }

    public static String getCmIn(int idx) {

        for (CmIdx cmidx: cmIdxLinkedList) {
            if (cmidx.getIdx() == idx)
                return cmidx.getIn();
        }
        return null;
    }

    public static String getCmOut(int idx) {

        for (CmIdx cmidx: cmIdxLinkedList) {
            if (cmidx.getIdx() == idx)
                return cmidx.getOut();
        }
        return null;
    }

}


