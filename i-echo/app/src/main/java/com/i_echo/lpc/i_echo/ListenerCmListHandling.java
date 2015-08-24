package com.i_echo.lpc.i_echo;

import android.view.View;
import android.widget.AdapterView;

/**
 * Created by LPC-Home1 on 7/4/2015.
 */

public class ListenerCmListHandling implements AdapterView.OnItemClickListener {
    ActivityMain mActivity;

    public ListenerCmListHandling(ActivityMain activity) {
        mActivity =activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View viewItem, int position, long id) {
        AppState appState = mActivity.getAppState();

        int state = appState.getSyncSmState(AppState.APP_STATE_LOCK);
        switch (state) {
            case AppState.SM_STATE_WAITINGFORUI_CMSELECTION: {
                handleCmSelectionAtSrc(appState, (AdapterCmList)(parent.getAdapter()), position);
                break;
            }
            case AppState.SM_STATE_WAITINGFORUI_PROBEMATCH: {
                handleCmMatchAtDst(appState, (AdapterCmList)(parent.getAdapter()), position);
                break;
            }
        }
        appState.getSyncSmState(AppState.APP_STATE_UNLOCK);
    }

    /*
    * Com Method (CM) selection at the source of the communications attempt
    * after the CM matched and returned from the destination/target
    **/
    private void handleCmSelectionAtSrc(AppState appState, AdapterCmList adapter, int position) {
        int idx =adapter.getItem(position).getCmIdx();

        appState.getSyncSmState(AppState.APP_STATE_LOCK);
        switch (idx) {
            case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE: {
                appState.setSmState(AppState.SM_STATE_VOICEMSG_ACTIVESELECTED); break;
            }
            case CmIdxItems.CM_TYPE_VOICEMSG_SILENT: {
                appState.setSmState(AppState.SM_STATE_VOICEMSG_SILENTSELECTED); break;
            }
            case CmIdxItems.CM_TYPE_TEXTMSG: {
                appState.setSmState(AppState.SM_STATE_TEXTMSGSELECTED); break;
            }
            case CmIdxItems.CM_TYPE_VOIP: {
                appState.setSmState(AppState.SM_STATE_VOIPSELECTED); break;
            }
            case CmIdxItems.CM_TYPE_PHONE: {
                appState.setSmState(AppState.SM_STATE_PHONESELECTED); break;
            }
            case CmIdxItems.CM_TYPE_PTT: {
                appState.setSmState(AppState.SM_STATE_PTTCALLREQUEST); break;
            }
            case CmIdxItems.CM_TYPE_EMAIL: {
                appState.setSmState(AppState.SM_STATE_EMAILSELECTED); break;
            }
            default: {       // Including 0 (do nothing)
                appState.setSmState(AppState.SM_STATE_READY); break;
            }
        }
        appState.getSyncSmState(AppState.APP_STATE_UNLOCK);
    }

    /*
    * Com Method (CM) matching at the target/destination of the communications attempt
    * after the CM received from the source
    **/
    private void handleCmMatchAtDst(AppState appState, AdapterCmList adapter, int position) {
        int idx =adapter.getItem(position).getCmIdx();

        appState.getSyncSmState(AppState.APP_STATE_LOCK);
        switch (idx) {
            case CmIdxItems.CM_TYPE_VOICEMSG_ACTIVE: {
                appState.setSmState(AppState.SM_STATE_PROBEREQRESP_VOICEMSG_ACTIVEIN); break;
            }
            case CmIdxItems.CM_TYPE_VOICEMSG_SILENT: {
                appState.setSmState(AppState.SM_STATE_PROBEREQRESP_VOICEMSG_SILENTIN); break;
            }
            case CmIdxItems.CM_TYPE_TEXTMSG: {
                appState.setSmState(AppState.SM_STATE_PROBEREQRESP_TEXTMSGIN); break;
            }
            case CmIdxItems.CM_TYPE_VOIP: {
                appState.setSmState(AppState.SM_STATE_PROBEREQRESP_VOIPIN); break;
            }
            case CmIdxItems.CM_TYPE_PHONE: {
                appState.setSmState(AppState.SM_STATE_PROBEREQRESP_PHONEIN); break;
            }
            case CmIdxItems.CM_TYPE_PTT: {
                appState.setSmState(AppState.SM_STATE_PROBEREQRESP_PTTIN); break;
            }
            case CmIdxItems.CM_TYPE_EMAIL:
            default:        // Including 0 (do nothing)
                break;
        }
        appState.getSyncSmState(AppState.APP_STATE_UNLOCK);
    }
}
