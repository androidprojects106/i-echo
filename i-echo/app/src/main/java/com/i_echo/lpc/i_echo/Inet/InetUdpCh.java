package com.i_echo.lpc.i_echo.Inet;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import android.util.Log;

import com.i_echo.lpc.i_echo.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */
public class InetUdpCh extends DatagramSocket {
    public static final int UDP_LOCALPORT = 8081;

    private int _serverUdpPort;
    InetAddress inetServerAddress;

    public InetUdpCh(String _address, int _port) throws IOException {
        super();       // super(UDP_LOCALPORT) - bind to the local port number here

        _serverUdpPort = _port;
        inetServerAddress = InetAddress.getByName(_address);
    }

    public boolean sendDataToServer(final byte[] dataToSend, int dataLength)
    {

        if (Constants.DEBUGGING)
            Log.i("sendDataToServer", "Writing received data (bytes) to datagram socket");

        DatagramPacket packetOutFinal = new DatagramPacket(dataToSend, dataLength, inetServerAddress, _serverUdpPort);
        try {
            /* For debugging only *
            byte[] bytes = new byte[AppProtData.APPPROTOCOLDATA_OVERHEADSIZE];
            System.arraycopy(dataToSend, 0, bytes, 0, AppProtData.APPPROTOCOLDATA_OVERHEADSIZE);
            int sessionId = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | bytes[3] << 24;
            short seqNum = (short)((short)bytes[4] | ((short)bytes[5])<<8);
            if (Constants.DEBUGGING)
                Log.i("ProtMsgTag", "Data: at " + System.currentTimeMillis() + " => " + "sessionId: "+sessionId + " SeqNum: " +seqNum);
            * For debugging only */
            send(packetOutFinal);
        } catch (IOException e) {
            e.printStackTrace();
            if (Constants.DEBUGGING)
                Log.i("sendDataToServer", "Data (bytes) send failed. Caught an exception");
            return false;
        }
        return true;
    }

    public DatagramPacket readDataFromServer (byte[] byteBuf) {
        if (Constants.DEBUGGING)
            Log.i("readDataFromServer","Waiting for server data ...");

        DatagramPacket packetInFinal = new DatagramPacket(byteBuf, byteBuf.length);
        try {
            receive(packetInFinal);
            /* For debugging only *
            byte[] bytes = new byte[AppProtData.APPPROTOCOLDATA_OVERHEADSIZE];
            System.arraycopy(packetIn.getData(), 0, bytes, 0, AppProtData.APPPROTOCOLDATA_OVERHEADSIZE);
            int sessionId = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | bytes[3] << 24;
            short seqNum = (short)((short)bytes[4] | ((short)bytes[5])<<8);
            if (Constants.DEBUGGING)
                Log.i("ProtMsgTag", "Data: at " + System.currentTimeMillis() + " <= " + "sessionId: "+sessionId + " SeqNum: " +seqNum);
            * For debugging only */

            return packetInFinal;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}