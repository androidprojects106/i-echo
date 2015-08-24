package com.i_echo.lpc.i_echo.Inet;

/**
 * Created by LPC-Home1 on 4/13/2015.
 */

import android.util.Log;

import com.i_echo.lpc.i_echo.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */

public class InetTcpCh extends Socket {

    public static final int MAX_TCPPDUSIZE =1400;
    public static final int TCP_TOSERVER_UNDEFINED =0;
    public static final int TCP_TOSERVER_CONNECTED =1;
    public static final int TCP_TOSERVER_FAILED =-1;

    private String _serverAddress;
    private int _serverTcpPort;
    private int _isTcpConnectedToServer;

    InputStream serverInputStream;
    private BufferedReader inFromServer;        // Network Input Stream
    private DataOutputStream outToServer;       // Network out put Stream

    public InetTcpCh(String addr, int port){
        super();

        _isTcpConnectedToServer =TCP_TOSERVER_UNDEFINED;
        _serverAddress = addr;
        _serverTcpPort = port;
        inFromServer =null;
        outToServer =null;
        SocketAddress sockAddr =
                new InetSocketAddress(_serverAddress, _serverTcpPort);
        try {
            setTcpNoDelay(true);
            connect(sockAddr);
            // this will block until a socket connection to the server is made
            setKeepAlive(true);
            _isTcpConnectedToServer =TCP_TOSERVER_CONNECTED;
            initChannel();
        } catch (IOException e) {
            // this deals with the cases where the IP (wifi) connection exists
            // but the user cannot be authenticated
            _isTcpConnectedToServer =TCP_TOSERVER_FAILED;
            e.printStackTrace();
        }
    }

    public boolean isTcpFailed() {
        return _isTcpConnectedToServer ==TCP_TOSERVER_FAILED;
    }

    public boolean isTcpConnected() {
        return _isTcpConnectedToServer ==TCP_TOSERVER_CONNECTED;
    }


    private boolean initChannel() {

        try {
            outToServer = new DataOutputStream(getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            serverInputStream = getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (serverInputStream !=null) {
            try {
                inFromServer = new BufferedReader(new
                        InputStreamReader(serverInputStream, "UTF-8"), MAX_TCPPDUSIZE);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public void close()
    {
        try { inFromServer.close();
        } catch (IOException e) { e.printStackTrace(); }
        try { outToServer.close();
        } catch (IOException e) { e.printStackTrace(); }

        try {  super.close();
        } catch (IOException e) {  e.printStackTrace(); }
    }

    public boolean sendMsgStringToServer(final String msgToSend)
    {
        if (outToServer==null)
            return false;
        try
        {
            outToServer.writeBytes(msgToSend + "\n");
            if (Constants.DEBUGGING)
                Log.i("ProtMsgTag", "Message: at " + System.currentTimeMillis() + " => " + msgToSend);
        } catch (Exception e) {
            e.printStackTrace();
            if (Constants.DEBUGGING)
                Log.i("ProtMsgTag", "Message: at (failed) " + System.currentTimeMillis() + " => " + msgToSend);
            return false;
        }
        return true;
    }

    public String readMsgStringFromServer () {
        String packetString = null;
        try {
            packetString = inFromServer.readLine();
            // this will block until there is data to read
            if (packetString != null && packetString.compareTo("") !=0) {
                if (Constants.DEBUGGING)
                    Log.i("ProtMsgTag", "Message: at " + System.currentTimeMillis() + " <= " + packetString);
                return packetString;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void writeStringToFile(String str, String fileName) {
        FileWriter fw = null;
        BufferedWriter bw =null;

        try {
            fw = new FileWriter(new File(fileName).getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(str);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}