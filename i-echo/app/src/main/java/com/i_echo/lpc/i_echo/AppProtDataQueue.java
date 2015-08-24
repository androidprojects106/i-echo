package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import android.util.Log;

import com.i_echo.lpc.i_echo.Inet.InetUdpCh;
import com.i_echo.lpc.i_echo.Utils.UtilsHelpers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */
public class AppProtDataQueue {
    public static final int MAX_UDPRECEIVEPDU_SIZE = 1500;          // (1472-60) octets
    public final static int MAX_DUPLICATERECORDDIZE = 16;
    public final static int MAX_WAITFORUDPCHANNEL = 10000;

    String serverAddr;
    int serverUdpPort;
    private volatile boolean listening;
    InetUdpCh dataChannel;
    private static Semaphore semaphore;
    // used as a Mutex to protect the ActiveReceiveQue
    // accessed by both readMsgFromServQue and inputFromCtrlChannel

    public static LinkedList<AppProtData> appDataReceiveQue;
    public static LinkedList<AppProtData> appDataDuplicateReceiveQue;

    public AppProtDataQueue(String ipAddr, int udpPort) {
        serverAddr = ipAddr;
        serverUdpPort = udpPort;
        dataChannel = null;
        semaphore = new Semaphore(1, true);     // Mutex only (1 resource) and "fair" =true
        appDataReceiveQue = new LinkedList<AppProtData>();
        appDataDuplicateReceiveQue = new LinkedList<AppProtData>();

        new Thread(new udpClient()).start();
    }

    /*
     Run init() in order to send to and listen to the server for the data
     (or media: voice, video, text) in both directions
     */
    private class udpClient implements Runnable {
        @Override
        public void run() {
            // TCP Socket opened in separate thread

            try {
                dataChannel = new InetUdpCh(serverAddr,serverUdpPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startReceiving() {
        listening = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (waitForUdpDataChannel()) {
                        // make sure the channel is initialized successfully
                    try {
                        dataChannel.connect(new InetSocketAddress(serverAddr, serverUdpPort));
                                // limit the binding to this application server only
                        while (listening && isConnected()) {
                            boolean workDone = inputFromDataChannel(dataChannel);
                            if (workDone) {      // Nothing to do in msg queue for now
                                try { sleep(200); }
                                catch (InterruptedException e) { e.printStackTrace(); }
                            }
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stopReceiving() {
        listening = false;
    }

    public boolean waitForUdpDataChannel() {
        long timeStart =System.currentTimeMillis();
        long timePassed = System.currentTimeMillis()-timeStart;

        while (null == dataChannel && timePassed <=MAX_WAITFORUDPCHANNEL) {
                    // Wait until UDP Socket is established
            try {
                sleep(200);
                timePassed = System.currentTimeMillis()-timeStart;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (timePassed <=MAX_WAITFORUDPCHANNEL);
    }

    public boolean isConnected() {
        if (dataChannel==null)
            return false;
        else
            return !dataChannel.isClosed();
            // warning: do not use dataChannel.isConnected()
    }

    public void close() {
        listening = false;

        if (dataChannel !=null) {
            dataChannel.close();
        }
    }

    /*
     Add an AppData (LiteRTP/UDP) to the appDataSendQue at the tail-end
     executed by the Call Processing thread
    */
    public void sendDataToServer(AppProtData data) {
        if (isConnected()) {
            byte[] bytesToSend = data.composeUdpPacket();
            if (Constants.DEBUGGING) {
                Log.i("ProtDataTag", "Data: at " + System.currentTimeMillis() + " => "
                        + "sessionId: " + data.getSessionId()
                        + " SeqNum: " + Short.toString(data.getSeqNum())
                        + " Data Len: " + data.getDataSize()
                        + " Raw Len: " + bytesToSend.length
                        );
            }
            dataChannel.sendDataToServer(bytesToSend, bytesToSend.length);
        }
    }

    /*
     Extract an AppData (LiteRTP/UDP) from the appDataReceiveQue at the head
     of the queue
     executed by the Call Processing thread
    */
    public AppProtData readDataFromServQue(int sessionId) {
        AppProtData appData = null;

        try {
            semaphore.acquire();
            while (appDataReceiveQue!= null && !appDataReceiveQue.isEmpty()) {
                appData = appDataReceiveQue.removeFirst();
                if (appData.getSessionId() ==sessionId) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();

        return appData;
    }

    /*
      Input data packet for socket communications. Executed by
      the CtrlChannel socket communications thread
      return true if any msg queue work is done
     */

    public boolean inputFromDataChannel(InetUdpCh dataChannel)
    {
        // input from socket and place to active receive (queue) tail
        byte[] bytesIn = new byte[MAX_UDPRECEIVEPDU_SIZE];
        DatagramPacket packetIn =dataChannel.readDataFromServer(bytesIn);

        if (packetIn ==null) {
            return true;     // work done (no more packets - at least for now)
        }
        else {
            AppProtData appData =new AppProtData(packetIn.getData(), packetIn.getLength());
            if (Constants.DEBUGGING) {
                Log.i("ProtDataTag", "Data: at " + System.currentTimeMillis() + " <= "
                    + "sessionId: "+appData.getSessionId()
                    + " SeqNum: " +Short.toString(appData.getSeqNum())
                    + " Raw Len: " + packetIn.getLength());
            }
            if (appData.isValid() && !appDataDuplicateReceiveQue.contains(appData)) {
                try {
                    semaphore.acquire();
                    appDataReceiveQue.addLast(appData);
                    semaphore.release();
                    appDataDuplicateReceiveQue.addLast(appData);
                    if (appDataDuplicateReceiveQue.size()>MAX_DUPLICATERECORDDIZE)
                        appDataDuplicateReceiveQue.remove();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;    // work not done yet
        }
    }


    public boolean sendUdpTriggerToServer(byte dataType, int sessionId) {
        if (!waitForUdpDataChannel()) {
            return false;
        }
        else {
            short dataSeqNum = 0;
            byte[] bData = null;
            switch (dataType) {
                case AppProtData.UDPTYPE_TRIGGER: {
                    bData = AppProtData.UDP_TRIGGERSIGNATURE;
                    break;
                }
            }
            // signature 7 bytes to trigger the UDP datagram socket from the client
            AppProtData appData = new AppProtData(dataSeqNum, dataType, bData, bData.length, sessionId);
            // padding packet (waste) to trigger the UDP datagram socket to
            // be received by the server
            sendDataToServer(appData);
            return true;
        }
    }


    public short sendRecordToServer(short dataSeqNum, byte dataType, byte[] bData,
                                     int bSize, int appSessionId) {
        AppProtData appData;
        byte[] bytesBuf = new byte[AppProtData.MAX_UDPTRANSMITPDU_SIZE];
        int offsetBuf =0, sizeBuf =AppProtData.MAX_UDPTRANSMITPDU_SIZE;

        while (offsetBuf <bSize) {
            sizeBuf = (bSize-offsetBuf >= AppProtData.MAX_UDPTRANSMITPDU_SIZE)
                    ? AppProtData.MAX_UDPTRANSMITPDU_SIZE: bSize-offsetBuf;
            System.arraycopy(bData, offsetBuf, bytesBuf, 0, sizeBuf);
            appData = new AppProtData(dataSeqNum, dataType, bytesBuf, sizeBuf, appSessionId);

            sendDataToServer(appData);
/*            if (Constants.DEBUGGING)
                Log.i("ProtDataTag", "sendRecordToServer: offset="+offsetBuf);*/
            offsetBuf +=sizeBuf;
            dataSeqNum = UtilsHelpers.incAppSeqNum(dataSeqNum);
        }
        return dataSeqNum;
    }
}
