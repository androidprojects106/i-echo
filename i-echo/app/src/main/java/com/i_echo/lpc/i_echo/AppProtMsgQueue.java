package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import android.util.Log;

import com.i_echo.lpc.i_echo.Inet.InetTcpCh;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 3/16/2015.
 */
public class AppProtMsgQueue {

    public final static int MAX_DUPLICATERECORDDIZE = 16;
    // track the last 16 non-duplicate messages from the server
    public static final int MAX_TIMEPOLLING = 200;
    // interval to listen to pages/messages from the server
    public final static int MAX_WAITFORTCPCHANNEL = 2000;

    private static String serverAddr;
    private static int serverTcpPort;

    private InetTcpCh ctrlChannel;
    private volatile boolean listening;
    private Semaphore semaphoreQueue;
    // Used as a Mutex to protect the ActiveReceiveQue
    // Accessed by both readMsgFromServQue and inputFromCtrlChannel

    public LinkedList<AppProtMsg> appMsgActiveReceiveQue;
    public LinkedList<AppProtMsg> appMsgDuplicateReceiveQue;
    public LinkedList<AppProtMsg> appMsgPendingReceiveQue;
    public ConcurrentHashMap<String, ArrayList<AppProtUserMsg>> appUserMsgQueMap;

    public AppProtMsgQueue(String ipAddr, int tcpPort) {
        serverAddr =ipAddr;
        serverTcpPort =tcpPort;
        ctrlChannel =null;
        semaphoreQueue = new Semaphore(1, true);     // Mutex only (1 resource) and "fair" =true
        appMsgActiveReceiveQue = new LinkedList<AppProtMsg>();
        appMsgDuplicateReceiveQue = new LinkedList<AppProtMsg>();
        appMsgPendingReceiveQue = new LinkedList<AppProtMsg>();
        appUserMsgQueMap = new ConcurrentHashMap<String, ArrayList<AppProtUserMsg>>();

        /*
         Start the TCP client for control channel: send and listen to the server
         for the control msgs in both directions
        */
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TCP Socket opened in separate thread
                ctrlChannel = new InetTcpCh(serverAddr, serverTcpPort);

            }}).start();
    }

    /*
     Save the text msg (or the text/filename representation of voice msg) in user database
     sourceId is the key to the indexing of the messages
     */
    public void saveUserMsg(String sourceKey, Boolean state, String msgString, int descriptor) {
        ArrayList<AppProtUserMsg> userMsgQue= appUserMsgQueMap.get(sourceKey);
        long time =System.currentTimeMillis();

        if (userMsgQue!=null) {
            userMsgQue.add(new AppProtUserMsg(descriptor, state, msgString, time, null));
        }
        else {
            userMsgQue = new ArrayList<AppProtUserMsg>();
            userMsgQue.add(new AppProtUserMsg(descriptor, state, msgString, time, null));
            appUserMsgQueMap.put(sourceKey, userMsgQue);
        }
    }

    public void saveUserMsg(String sourceKey, Boolean state, String msgString, int descriptor, String file) {
        ArrayList<AppProtUserMsg> userMsgQue= appUserMsgQueMap.get(sourceKey);
        long time =System.currentTimeMillis();

        if (userMsgQue!=null) {
            userMsgQue.add(new AppProtUserMsg(descriptor, state, msgString, time, file));
        }
        else {
            userMsgQue = new ArrayList<AppProtUserMsg>();
            userMsgQue.add(new AppProtUserMsg(descriptor, state, msgString, time, file));
            appUserMsgQueMap.put(sourceKey, userMsgQue);
        }
    }

    public ArrayList<AppProtUserMsg> getUserMsgList(String userKey) {
        return appUserMsgQueMap.get(userKey);
    }

    /*
     Start listen to the server for incoming control msgs
    */
    public void executeListening() {
        listening =true;

        new Thread (new readingCtrlChannel()).start();
        // this thread is blocked when there is no incoming message
    }

    public String getServerIpAddr() {
        return serverAddr;
    }

    /*
      Input control for socket communications. Executed by
      the ctrlChannel socket communications thread
      return true if any msg queue work is done
     */
    class readingCtrlChannel implements Runnable {
        @Override
        public void run() {
            while (listening) {
                // Wait for until TCP Socket is established
                if (ctrlChannel == null) {
                    try { sleep(250); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
                else if (!ctrlChannel.isConnected() && ctrlChannel.isClosed()) {
                    break;                           // listening = false;
                }
                else if (!ctrlChannel.isConnected() && !ctrlChannel.isClosed()) {
                    try { sleep(250); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
                else {      // isConnected and not isClosed
                    int workDone = inputFromCtrlChannel();
                    if (workDone ==-1) {
                        break;                   // listening = false;
                    }
                    else if (workDone ==0) {      // Nothing to do in msg queue for now
                        try { sleep(MAX_TIMEPOLLING); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
            }
        }
    }

    public int inputFromCtrlChannel()
    {
        // input from socket and place to active receive (queue) tail

        String msgString =ctrlChannel.readMsgStringFromServer();

        if (Constants.DEBUGGING)
            Log.i("i-Echo: readingCtrlCh", "read: " + msgString);

        if (msgString ==null) {
            return 0;
        }
        else if (msgString.equals("*******END*")) {
            return -1;
        }
        else {
            AppProtMsg appMsg =new AppProtMsg(msgString);
            if (!appMsgDuplicateReceiveQue.contains(appMsg)) {
                // the duplicate queue action is only executed by the control
                // channel reading thread (i.e., a single thread, and not the
                // call processing thread) - there is no semaphore protection
                try {
                    semaphoreQueue.acquire();
                    appMsgActiveReceiveQue.addLast(appMsg);
                    semaphoreQueue.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                appMsgDuplicateReceiveQue.addLast(appMsg);
                if (appMsgDuplicateReceiveQue.size()>MAX_DUPLICATERECORDDIZE)
                    appMsgDuplicateReceiveQue.removeFirst();
            }
            return 1;    // did not read anything
        }
    }

    public boolean waitForTcpCtrlChannel() {
        long timeStart =System.currentTimeMillis();
        long timePassed = System.currentTimeMillis()-timeStart;

        while (ctrlChannel ==null && timePassed <=MAX_WAITFORTCPCHANNEL) {
            // Wait until UDP Socket is established
            try {
                sleep(200);
                timePassed = System.currentTimeMillis()-timeStart;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (timePassed >MAX_WAITFORTCPCHANNEL)
            return false;
        else if (ctrlChannel ==null)
            return false;
        else if (ctrlChannel.isTcpConnected())
            return true;
        else if (ctrlChannel.isTcpFailed())
            return false;
        else return false;
    }

    public boolean isConnected() {
        return (ctrlChannel != null && ctrlChannel.isConnected() && !ctrlChannel.isClosed());
    }

    public boolean isClosed() {
        return (ctrlChannel != null || ctrlChannel.isClosed());
    }

    public InetTcpCh getCtrlChannel() {
        return (ctrlChannel);
    }

    public void closeConnection() {
        appMsgActiveReceiveQue.clear();
        appMsgDuplicateReceiveQue.clear();
        appMsgPendingReceiveQue.clear();

        ctrlChannel.sendMsgStringToServer("*******END*");
        try { sleep(2000); } catch (InterruptedException e) {
            e.printStackTrace();
        }
        close();
    }

    public void close() {
        if (ctrlChannel!=null) {
            listening =false;
            if (ctrlChannel.isConnected() && !ctrlChannel.isClosed()) {
                ctrlChannel.close();
                ctrlChannel =null;
            }
        }
    }

    /*
     Add an AppProtocolMsg message to the appMsgActiveSendQue at the tail-end
     executed by the Call Processing thread
    */
    public boolean sendMsgToServer(short SeqNum, byte MsgType,
                                   String idAppUserSrc, String idAppServerDst) {

        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idAppUserSrc,idAppServerDst);
        String msgString = appMsg.composeProtocolMsg();
        // primitive messages are composed
        if (ctrlChannel !=null && msgString!=null) {
            ctrlChannel.sendMsgStringToServer(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToServer(short SeqNum, byte MsgType,
                                   String idAppUserSrc, String idAppServerDst, String msgText) {

        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idAppUserSrc,idAppServerDst);
        String msgString = appMsg.composeProtocolMsg(msgText);
        // piggybacked text message are composed
        if (ctrlChannel !=null && msgString!=null) {
            ctrlChannel.sendMsgStringToServer(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToServer(short SeqNum, byte MsgType,
                                   String idAppUserSrc, String idAppServerDst, AppState appState) {

        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idAppUserSrc,idAppServerDst);
        String msgString = appMsg.composeProtocolMsg(appState);
        // piggybacked this user status and communications methods are composed
        if (ctrlChannel !=null && msgString!=null) {
            ctrlChannel.sendMsgStringToServer(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToServer(short SeqNum, byte MsgType,
                                   String idAppUserSrc,String idAppServerDst, int mediaSessionId) {

        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idAppUserSrc,idAppServerDst);
        String msgString = appMsg.composeProtocolMsg(mediaSessionId);
        // piggybacked this user status and communications methods are composed
        if (ctrlChannel !=null && msgString!=null) {
            ctrlChannel.sendMsgStringToServer(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToServer(short SeqNum, byte MsgType,
                                   String idAppUserSrc, String idAppServerDst,
                                   int[] cmList, String[] cmInfo) {

        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idAppUserSrc,idAppServerDst);
        String msgString = appMsg.composeProtocolMsg(cmList, cmInfo);
        // piggybacked this user status and communications methods are composed
        if (ctrlChannel !=null && msgString!=null) {
            ctrlChannel.sendMsgStringToServer(msgString);
            return true;
        }
        else
            return false;
    }

    /*
     Extract an AppProtocolMsg from the appMsgActiveReceiveQue at the head
     of the queue
     executed by the Call Processing thread
    */
    public AppProtMsg readMsgFromServActiveQue() {
                // read from the current active queue
        AppProtMsg appMsg = null;

        try {
            semaphoreQueue.acquire();
            if (appMsgActiveReceiveQue !=null && !appMsgActiveReceiveQue.isEmpty())
                appMsg = appMsgActiveReceiveQue.removeFirst();
            semaphoreQueue.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return appMsg;
    }

    public AppProtMsg readMsgFromServPendedQue() {
                // read from the pended queue
        AppProtMsg appMsg = null;
        // the pending queue action is only executed by the call
        // processing thread (i.e., a single thread, and not the control
        // channel reading thread) - there is no semaphore protection
        if (appMsgPendingReceiveQue!= null && !appMsgPendingReceiveQue.isEmpty())
            appMsg = appMsgPendingReceiveQue.removeFirst();
        return appMsg;
    }

    /*
     Add the message to the pending queue for further processing
     (in READY state) when the current processing is finished, this
     action is only executed by the call processing thread (i.e., a
     single thread, and not the control channel reading thread)
     */
    public boolean pendMsgIncomingFromServ(AppProtMsg appServMsg) {
        if (appServMsg.isPendable()) {
            appMsgPendingReceiveQue.addLast(appServMsg);
            return true;
        }
        else return false;
    }

}
