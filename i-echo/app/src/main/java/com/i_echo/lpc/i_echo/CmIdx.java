package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 7/6/2015.
 */

public class CmIdx {
    private int Idx;
    private String CmString;
    private String inSub, outSub;

    public CmIdx(int i, String cmStr, String in, String out) {
        Idx =i;
        CmString =cmStr;
        inSub =in;
        outSub =out;
    }

    public int getIdx() {return Idx;}
    public String getCmString() {return CmString;}
    public String getIn() {return inSub;}
    public String getOut() {return outSub;}

}