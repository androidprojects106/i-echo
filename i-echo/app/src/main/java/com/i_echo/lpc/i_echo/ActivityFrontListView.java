package com.i_echo.lpc.i_echo;

import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by LPC-Home1 on 5/13/2015.
 */

public class ActivityFrontListView extends ListView {

    public ActivityFrontListView(Context context) {
        super(context);
    }

    public ActivityFrontListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ActivityFrontListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
