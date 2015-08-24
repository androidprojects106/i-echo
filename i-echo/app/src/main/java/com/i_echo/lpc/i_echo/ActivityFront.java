package com.i_echo.lpc.i_echo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.i_echo.lpc.i_echo.Utils.UtilsHelpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by LPC-Home1 on 5/13/2015.
 */
public class ActivityFront extends ActionBarActivity {

    ArrayList<ContactInfoUser> mAppUsers;
    String mServerIp;

    private EditText mEditTextServIP;
    private ListView mUserListView, mDetailListView;
    private int mxCoordLastTouchUp=0, myCoordLastTouchUp=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front);

        // UI items initialization
        mEditTextServIP = (EditText) findViewById(R.id.servIP);
        mUserListView = (ListView) findViewById(R.id.userListView);
        mDetailListView = (ListView) findViewById(R.id.detailListView);

        mUserListView.setOnItemClickListener(listUserOnItemClickListener);
        mUserListView.setOnTouchListener(listUserOnTouchListener);

        mAppUsers = new ArrayList<ContactInfoUser>();
        if (checkPlatformIsEmulator()) {
            // read it from the /res/raw/config.bin source
            // this is editable for each compiler
            readBufferedRawConfig();
        }
        else if (!checkExternalMedia()) {
            // this means that the device is attached in USB debugging mode
            // so read it from memory
            readSdCardConfigFromMemory();
        }
        else {
            // from the saved file on the SD card
            File root = android.os.Environment.getExternalStorageDirectory();
            readSdCardConfig(root.getAbsolutePath()+getResources().getString(R.string.string_configbin));
            // writeToSDFile();
        }
        // readSdCardConfigFromMemory(); // I use this to initalize the config.bin on Samsung GS5
        // writeToSDFile();
        showServerIpAddr();
        mEditTextServIP.setSelection(mEditTextServIP.getText().toString().length());
        showUserList();
        Thread.currentThread().setName("frontactivity");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        UiHelpers.hideKeyboard(this, mEditTextServIP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO
    }

    AdapterView.OnItemClickListener listUserOnItemClickListener =
        new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewItem, int position, long id) {
                TextView idTextView = (TextView) viewItem.findViewById(R.id.user_id);
                TextView infoTextView = (TextView) viewItem.findViewById(R.id.user_info);
                ImageView lookupView = (ImageView) viewItem.findViewById(R.id.user_lookup);

                int actionNext = 0;   // 0 = undefined, 1 = inCalling, and 2= outCalling
                if (mxCoordLastTouchUp != 0 && myCoordLastTouchUp != 0
                        && UtilsHelpers.isViewContains(viewItem, mxCoordLastTouchUp, myCoordLastTouchUp)) {
                    if (UtilsHelpers.isViewContains(idTextView, mxCoordLastTouchUp, myCoordLastTouchUp)
                            || UtilsHelpers.isViewContains(infoTextView, mxCoordLastTouchUp, myCoordLastTouchUp))
                        actionNext = 1;
                    else if (UtilsHelpers.isViewContains(lookupView, mxCoordLastTouchUp, myCoordLastTouchUp))
                        actionNext = 2;
                    switch (actionNext) {
                        case 1: {
                            Bundle bundle = new Bundle();

                            mServerIp =mEditTextServIP.getText().toString();
                            bundle.putString("ipaddr", mServerIp);
                            bundle.putParcelableArrayList("users", mAppUsers);
                            Intent intent = new Intent(getApplicationContext(), ActivityMain.class);
                            intent.putExtras(bundle);
                            startActivity(intent);
                            break;
                        }
                        case 2: {
                            String idString = idTextView.getText().toString();
                            ContactInfoUser display, user = mAppUsers.get(0);

                            if (idString.equalsIgnoreCase(user.getAppUserId()))
                                display = user;
                            else display = mAppUsers.get(1);
                            showUserDetails(display);
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        };

    AdapterView.OnTouchListener listUserOnTouchListener =
            new AdapterView.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();

                    switch (action) {
                        case MotionEvent.ACTION_UP: {
                            mxCoordLastTouchUp =(int)event.getRawX();
                            myCoordLastTouchUp =(int)event.getRawY();
                            break;
                        }
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_CANCEL:
                        default: {
                            mxCoordLastTouchUp =0;
                            myCoordLastTouchUp =0;
                            break;
                        }
                    }
                    return false;
                }
            };


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            deactivateApp();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void deactivateApp() {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage("Exit the application?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent exitIntent = new Intent(Intent.ACTION_MAIN);
                        exitIntent.addCategory(Intent.CATEGORY_HOME);
                        exitIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(exitIntent);
                        System.exit(0);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.cancel();
                    }
                })
                .show();
    }

    private boolean checkPlatformIsEmulator() {
        boolean result =true;
        String platform = System.getProperty("microedition.platform");

        if (platform==null) {
            platform =System.getProperty("http.agent");
        }
        platform = platform.toUpperCase();
        if (platform.contains("J2ME")
                || platform.equalsIgnoreCase("SunMicrosystems_wtk")) {
            result =true;
        }
        else  if (platform.contains("NOKIA")
                    || platform.contains("HUAWEI")
                    || platform.contains("SAMSUNG")
                    || platform.contains("SM-G900V")    // Samsung GS5
                    || platform.contains("SONYERICSSON")
                    || platform.contains("LG")
                    || platform.contains("BLACKBERRY")
                    || platform.contains("RIM")) {
            result =false;
        }

        return result;
    }

    private void showServerIpAddr() {
        runOnUiThread(new Runnable() {
            public void run() {
                mEditTextServIP.setText(mServerIp);
            }
        });
    }

    private void showUserList() {
        runOnUiThread(new Runnable() {
            public void run() {
                String[] matrix = new String[]{"_id", "lookup", "tag", "info"};
                MatrixCursor userCursor = new MatrixCursor(matrix);

                ContactInfoUser user = mAppUsers.get(0);
                ContactInfoUser target = mAppUsers.get(1);
                userCursor.addRow(new Object[]{0, R.drawable.drillin002,
                        user.getAppUserId(), user.getAppUserMsdn()});
                userCursor.addRow(new Object[]{1, R.drawable.drillin002,
                        target.getAppUserId(), target.getAppUserMsdn()});

                String[] from = new String[]{"lookup", "tag", "info"};
                int[] to = new int[] {R.id.user_lookup, R.id.user_id, R.id.user_info};
                SimpleCursorAdapter adapter =
                        new SimpleCursorAdapter(getApplicationContext(),
                                R.layout.row_activity_front_user_db, userCursor, from, to, 0);
                mUserListView.setAdapter(adapter);
                mUserListView.setVisibility(View.VISIBLE);
            }
        });
    }


    private void showUserDetails(final ContactInfoUser display) {
        runOnUiThread(new Runnable() {
            public void run() {
                String[] matrix = new String[]{"_id", "tag", "info"};
                MatrixCursor cursor = new MatrixCursor(matrix);

                cursor.addRow(new Object[]{0, "User ID", display.getAppUserId()});
                cursor.addRow(new Object[]{1, "First Name", display.getNameFirst()});
                cursor.addRow(new Object[]{2, "Last Name", display.getNameLast()});
                cursor.addRow(new Object[]{3, "Phone No.", display.getAppUserMsdn()});
                cursor.addRow(new Object[]{4, "Skype URL", display.getAppUserVoipUrl()});
                cursor.addRow(new Object[]{4, "Email URL", display.getAppUserEmailUrl()});

                String[] from = new String[]{"tag", "info"};
                int[] to = new int[] {R.id.tag_id, R.id.tag_info};
                SimpleCursorAdapter adapter =
                        new SimpleCursorAdapter(getApplicationContext(),
                                R.layout.row_activity_front_user_detail, cursor, from, to, 0);
                mDetailListView.setAdapter(adapter);
                mDetailListView.setVisibility(View.VISIBLE);
            }
        });
    }


    private boolean checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        return (mExternalStorageAvailable && mExternalStorageWriteable);
    }

    private void writeToSDFile(){

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/echoData");
        dir.mkdirs();
        File file = new File(dir, "config.bin");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println("[server IP]:192.168.1.9\r");
            pw.println("[user]\r");
            // this user: me
            pw.println("userid:user1@echo.com\r");
            pw.println("userfirstname:John\r");
            pw.println("userlastname:Doe\r");
            pw.println("phonenumber:+1-408-390-1770\r");
            pw.println("skypeurl:carlfcao1596@skype.com\r");
            pw.println("emailurl:janedoe@gmail.com\r");
            pw.println("[target]\r");
            // target user: other party (calling or called)
            pw.println("userid:user2@echo.com\r");
            pw.println("userfirstname:Jane\r");
            pw.println("userlastname:Doe\r");
            pw.println("phonenumber:+1-408-390-1770\r");
            pw.println("skypeurl:carlfcao@skype.com\r");
            pw.println("emailurl:janedoe@gmail.com\r");
            pw.flush();
            pw.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readSdCardConfig(String fileName) {
        InputStream is;

        try {
            is =new FileInputStream(fileName);
            readConfigFromInputstream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void readBufferedRawConfig() {
        InputStream is =this.getResources().openRawResource(R.raw.config);
        readConfigFromInputstream(is);
    }

    private void readSdCardConfigFromMemory() {
        ContactInfoUser user =new ContactInfoUser();
        ContactInfoUser target =new ContactInfoUser();

        mServerIp = "192.168.1.9";
        user.setAppUserId("user2@echo.com");
        user.setNameFirst("Jane");
        user.setNameLast("Doe");
        user.setAppUserMsdn("+1-408-390-1770");
        user.setAppUserVoipUrl("carlfcao@skype.com");
        user.setAppUserEmailUrl("emailurl:janedoe@gmail.com");

        // target user: other party (calling or called)
        target.setAppUserId("user1@echo.com");
        target.setNameFirst("John");
        target.setNameLast("Doe");
        target.setAppUserMsdn("+1-408-390-1770");
        target.setAppUserVoipUrl("carlfcao1596@skype.com");
        user.setAppUserEmailUrl("emailurl:johndoe@gmail.com");

        mAppUsers.add(user);                                     // appUsers[1st] is the user
        mAppUsers.add(target);                                   // appUsers[2nd] is the target
    }


    private void readConfigFromInputstream(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr, 8192);      // buffer size: 8192

        ContactInfoUser user =new ContactInfoUser();
        ContactInfoUser target =new ContactInfoUser();
        try {
            String lineString;
            String[] segments;

            lineString = br.readLine();
            segments = lineString.split(":");
            if (segments[0].equals("[server IP]")) mServerIp =segments[1];

            lineString = br.readLine();
            if (lineString.equalsIgnoreCase("[user]")) {
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("userid")) user.setAppUserId(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("userfirstname")) user.setNameFirst(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("userlastname")) user.setNameLast(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("phonenumber")) user.setAppUserMsdn(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("skypeurl")) user.setAppUserVoipUrl(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("emailurl")) user.setAppUserEmailUrl(segments[1]);
            }

            lineString = br.readLine();
            if (lineString.equalsIgnoreCase("[target]")) {
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("userid")) target.setAppUserId(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("userfirstname")) target.setNameFirst(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("userlastname")) target.setNameLast(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("phonenumber")) target.setAppUserMsdn(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("skypeurl")) target.setAppUserVoipUrl(segments[1]);
                lineString = br.readLine();
                segments = lineString.split(":");
                if (segments[0].equals("emailurl")) target.setAppUserEmailUrl(segments[1]);
            }
            isr.close();
            is.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAppUsers.add(user);                                     // appUsers[1st] is the user
        mAppUsers.add(target);                                   // appUsers[2nd] is the target
    }

    public void showUIToastMsg(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run () {
                Toast.makeText(ActivityFront.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
