package com.i_echo.lpc.i_echo;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by LPC-Home1 on 6/24/2015.
 */
public class ActivityMainUiManager {

    ActivityMain mActivity;
    int mWidthPixels;
    int mHeightPixels;

    Button mBtnAction;
    Button mBtnProbe;
    EditText mEditMsgView;
    ProgressBar mBarProgress;
    ListView mCmListView;
    TextView mCmAnnounceText;
    RelativeLayout mContainerCmList;

    Button mBtnPttChat;
    Button mBtnYouPttChat;
    RelativeLayout mContainerPttButton;

    ProgressDialog mProgressDialog;

    public ActivityMainUiManager(ActivityMain activity) {
        mActivity = activity;
        mProgressDialog = null;

        getScreenInfo();
        initUI();
    }

    private ActivityMainUiManager initUI() {
        /*
         Class attributes for Button and text processing
        */
        mEditMsgView = (EditText) mActivity.findViewById(R.id.textMsg);
        mBtnAction = (Button) mActivity.findViewById(R.id.btnAction);
        mBtnProbe = (Button) mActivity.findViewById(R.id.btnProbe);
        mBarProgress = (ProgressBar) mActivity.findViewById(R.id.progressBar);
        mCmAnnounceText = (TextView) mActivity.findViewById(R.id.cmAnnounce);
        mCmListView = (ListView) mActivity.findViewById(R.id.cmListView);
        mContainerCmList = (RelativeLayout) mActivity.findViewById(R.id.container_bottom);

        mBtnPttChat = (Button) mActivity.findViewById(R.id.btnPtt);
        mBtnYouPttChat = (Button) mActivity.findViewById(R.id.btnYouPtt);
        mContainerPttButton = (RelativeLayout) mActivity.findViewById(R.id.container_button_ptt);
        return this;
    }

    private void getScreenInfo() {
        WindowManager w = mActivity.getWindowManager();
        Display d = w.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        d.getMetrics(metrics);
        // since SDK_INT = 1;
        mWidthPixels = metrics.widthPixels;
        mHeightPixels = metrics.heightPixels;
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17)
            try {
                mWidthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(d);
                mHeightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(d);
            } catch (Exception ignored) {
            }
        // includes window decorations (statusbar bar/menu bar)
        if (Build.VERSION.SDK_INT >= 17)
            try {
                Point realSize = new Point();
                Display.class.getMethod("getRealSize", Point.class).invoke(d, realSize);
                mWidthPixels = realSize.x;
                mHeightPixels = realSize.y;
            } catch (Exception ignored) {
            }
    }

    public void showUIToastMsg(final String msg) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run () {
                Toast toast= Toast.makeText(mActivity, msg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, mHeightPixels/4);
                toast.show();
            }
        });
    }

    public void showUIDefaultEcho() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setCmlistVisibility(View.GONE, null);
                setRecordingButtonVisibility(View.GONE);
                setTextMsgButtonVisibility(View.GONE);
                setPttButtonVisibility(View.GONE);

                UiHelpers.hideKeyboard(mActivity, mEditMsgView);
                mBtnProbe.setVisibility(View.VISIBLE);
            }
        });
    }

    public void showUICmReadyList() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setRecordingButtonVisibility(View.GONE);
                setTextMsgButtonVisibility(View.GONE);
                mBtnProbe.setVisibility(View.GONE);
                setPttButtonVisibility(View.GONE);

                setCmlistVisibility(View.VISIBLE, null);
            }
        });
    }


    public void showUICmSelectionList(String userId) {
        final String announce =mActivity.getResources().getString(R.string.string_cmannounce)
                + "\"" + userId + "\"";

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setRecordingButtonVisibility(View.GONE);
                setTextMsgButtonVisibility(View.GONE);
                mBtnProbe.setVisibility(View.GONE);
                setPttButtonVisibility(View.GONE);

                setCmlistVisibility(View.VISIBLE, announce);
            }
        });
    }

    public void showUITextMsgSend() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run () {

                setCmlistVisibility(View.GONE, null);
                setRecordingButtonVisibility(View.GONE);
                mBtnProbe.setVisibility(View.GONE);
                setPttButtonVisibility(View.GONE);

                setTextMsgButtonVisibility(View.VISIBLE);
            }
        });
    }

    public void showUIPttButton() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setCmlistVisibility(View.GONE, null);
                setRecordingButtonVisibility(View.GONE);
                setTextMsgButtonVisibility(View.GONE);
                mBtnProbe.setVisibility(View.GONE);

                setPttButtonVisibility(View.VISIBLE);
            }
        });
    }

    public void showUIVoiceRecording() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setCmlistVisibility(View.GONE, null);
                setTextMsgButtonVisibility(View.GONE);
                mBtnProbe.setVisibility(View.GONE);
                setPttButtonVisibility(View.GONE);

                setRecordingButtonVisibility(View.VISIBLE);
            }
        });
    }

    public void showUIVoiceReceiving(final String userId, final boolean flagShowPlayingDialog) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                setCmlistVisibility(View.GONE, null);
                setRecordingButtonVisibility(View.GONE);
                setTextMsgButtonVisibility(View.GONE);
                setPttButtonVisibility(View.GONE);

                if (flagShowPlayingDialog) {
                    mProgressDialog = ProgressDialog.show(mActivity,
                            mActivity.getResources().getString(R.string.string_playing),
                            mActivity.getResources().getString(R.string.string_voicemessagefrom) + "\"" + userId + "\"");
                    mBtnProbe.setVisibility(View.GONE);
                }
                else {
                    mBtnProbe.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void showUIVoiceReceivingDone(final boolean flagShowPlayingDialog) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (flagShowPlayingDialog) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        });
    }

    private void setRecordingButtonVisibility(int visibility) {
        if (View.VISIBLE ==visibility) {
            mBtnAction.setText(mActivity.getResources().getString(R.string.string_record));
        }
        mBtnAction.setVisibility(visibility);
        mBarProgress.setVisibility(visibility);
    }

    private void setTextMsgButtonVisibility(int visibility) {
        if (View.VISIBLE ==visibility) {
            mEditMsgView.setSelection(0);
            mBtnAction.setText(mActivity.getResources().getString(R.string.string_send));
        }
        mBtnAction.setVisibility(visibility);
        mEditMsgView.setVisibility(visibility);
    }

    private void setPttButtonVisibility(int visibility) {
        mBtnPttChat.setVisibility(visibility);
        mBtnYouPttChat.setVisibility(visibility);
        mContainerPttButton.setVisibility(visibility);
    }


    private void setCmlistVisibility(int visibility, String announce) {
        final int PIXEL_SCALE =300;
        final int MAX_CMLIST_ITEMS =4;

        switch (visibility) {
            case View.VISIBLE: {
                setListViewHeightBasedOnItems(mCmListView, MAX_CMLIST_ITEMS);

/*                // recalculate the height of the cm list container view to fit the size
                // of the child views to a maximum of 4 items
                int paddingHeight = mContainerCmList.getPaddingTop()
                        +  mContainerCmList.getPaddingBottom();
                ViewGroup.LayoutParams params = mContainerCmList.getLayoutParams();
                params.height =paddingHeight + setListViewHeightBasedOnItems(mCmListView, MAX_CMLIST_ITEMS);
                if (null != announce) {
                    float px = PIXEL_SCALE * (mContainerCmList.getResources().getDisplayMetrics().density);
                    mCmAnnounceText.setText(announce);
                    mCmAnnounceText.measure(View.MeasureSpec.makeMeasureSpec((int)px, View.MeasureSpec.AT_MOST),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    params.height +=mCmAnnounceText.getMeasuredHeight();
                }
                mContainerCmList.setLayoutParams(params);
                mContainerCmList.requestLayout();*/

                mContainerCmList.setVisibility(View.VISIBLE);
                if (null != announce)
                    mCmAnnounceText.setVisibility(View.VISIBLE);
                mCmListView.setVisibility(View.VISIBLE);
                break;
            }
            case View.GONE:
            default: {
                mContainerCmList.setVisibility(View.GONE);
                mCmAnnounceText.setVisibility(View.GONE);
                mCmListView.setVisibility(View.GONE);
                break;
            }
        }
    }

    public void setPttButtonPlain() {
        BitmapDrawable bitMap = writeOnDrawable(R.drawable.a_button_rounded_plain, "Press to Talk");
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mBtnPttChat.setBackgroundDrawable(bitMap);
        } else {
            mBtnPttChat.setBackground(bitMap);
        }
    }

    public void setPttButtonHolo() {
        BitmapDrawable bitMap = writeOnDrawable(R.drawable.a_button_rounded_holo, "Talking");
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mBtnPttChat.setBackgroundDrawable(bitMap);
        } else {
            mBtnPttChat.setBackground(bitMap);
        }
    }

    public BitmapDrawable writeOnDrawable(int drawableId, String text){
/*
Comment that this does not work with a xml defined drawable.
As that kind of drawable is a GradientDrawable.
 */
        Bitmap bm = BitmapFactory.decodeResource(mActivity.getResources(),
                drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(48);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, 0, bm.getHeight()/2, paint);

        return new BitmapDrawable(mActivity.getResources(),bm);
    }


    public static int setListViewHeightBasedOnItems(ListView listView, int max) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            int numberOfItems = listAdapter.getCount();

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < ((max < numberOfItems) ? max: numberOfItems); itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                float px = 300 * (listView.getResources().getDisplayMetrics().density);
                item.measure(View.MeasureSpec.makeMeasureSpec((int)px, View.MeasureSpec.AT_MOST),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);
            // Get padding
            int totalPadding = listView.getPaddingTop() + listView.getPaddingBottom();

            // Set list height.
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight + totalPadding;
            listView.setLayoutParams(params);
            listView.requestLayout();

            return params.height;

        } else {
            return 0;
        }
    }
}
