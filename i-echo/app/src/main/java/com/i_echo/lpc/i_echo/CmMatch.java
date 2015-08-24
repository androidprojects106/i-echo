package com.i_echo.lpc.i_echo;

/**
 * Created by LPC-Home1 on 7/6/2015.
 */

public class CmMatch {
    private int cmIdx;
    private int[] cmReasons;
    private String cmInfoString;

    public CmMatch(int cm, int[] reasons, String str) {
        cmIdx =cm;
        cmReasons =reasons;
        cmInfoString =str;
    }

    public int getCmIdx() {return cmIdx;}
    public int[] getCmReasons() {return cmReasons;}
    public String getCmInfoString() {return cmInfoString;}
    public void setCmIdx(int idx) { cmIdx =idx;}
    public void setCmReasons(int[] reasons) { cmReasons =reasons;}
    public void setCmInfoString(String str) { cmInfoString =str;}
}
