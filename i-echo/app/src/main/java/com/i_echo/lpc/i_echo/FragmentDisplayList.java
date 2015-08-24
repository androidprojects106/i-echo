package com.i_echo.lpc.i_echo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.i_echo.lpc.i_echo.Audio.Audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by LPC-Home1 on 6/30/2015.
 */
public class FragmentDisplayList extends Fragment {
    final int TYPE_DATA_TWO_DECIMAL =1;

    private ActivityMain mActivity;
    private DisplayListAdapter mDisplayAdapter;

    private ListView mListView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        mActivity = (ActivityMain)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_display_list, container, false);

        mListView = (ListView)rootView.findViewById(R.id.displayList);
        Bundle bundle = getArguments();
        if (null == bundle) {
            return null;
        }
        ArrayList<AppProtUserMsg> items = bundle.getParcelableArrayList("display");
        if (null == items) {
            return null;
        }
        // use filtered result if filtering is once used by user.
        mDisplayAdapter = new DisplayListAdapter(mActivity,
                R.layout.row_activity_main_fragment_display_list, items);
        // mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mListView.setAdapter(mDisplayAdapter);
        // mListView.smoothScrollToPosition(mDisplayAdapter.getCount() - 1);
        mListView.setOnItemClickListener(displayOnItemClickListener);
        /* mDisplayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mListView.setSelection(mDisplayAdapter.getCount() - 1);
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            //useless here, skip!
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });*/
        /*
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(mListView.getCount()-1);
            }
        }); */

        return rootView;
    }

    public void updateDisplaylist(ArrayList<AppProtUserMsg> newData) {
        mDisplayAdapter.notifyDataSetChanged();
    }

    AdapterView.OnItemClickListener displayOnItemClickListener =
            new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapter, View v,
                                        int position, long id) {
                    String fileFullPath;
                    File root = android.os.Environment.getExternalStorageDirectory();

                    AppProtUserMsg usermsg =mDisplayAdapter.getItem(position);
                    switch (usermsg.getUserMsgDescriptor()) {
                        case AppProtUserMsg.USERMSG_VOICE_FROM:
                        case AppProtUserMsg.USERMSG_VOICE_TO: {
                            fileFullPath = root.getAbsolutePath() + "/echoData/" +
                                    usermsg.getUserMsgFile();
                            break;
                        }
                        default: return;
                    }

                    BufferedInputStream bufI=null;
                    try {
                        bufI= new BufferedInputStream(new FileInputStream(fileFullPath),8*1024);
                        Audio.readAndPlayVoiceMsgFromFile(mActivity.getAppState(), bufI);
                    } catch (IOException e) {
                        mActivity.getUiHolder().showUIToastMsg(getResources().getString(R.string.string_nosdfile));
                        // e.printStackTrace();
                    }
                    finally {
                        try {
                            if (bufI !=null)
                                bufI.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

    public class DisplayListAdapter extends ArrayAdapter<AppProtUserMsg> {
        private final Context mContext;
        private ArrayList<AppProtUserMsg> mItems;

        public DisplayListAdapter(ActivityMain context, int resId, ArrayList<AppProtUserMsg> items) {
            super(context, resId, items);

            this.mContext = context;
            this.mItems = items;
        }

        public void setData( ArrayList<AppProtUserMsg> items) {
            this.mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public AppProtUserMsg getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            AppProtUserMsg userMsg = mItems.get(position);
            View view = convertView;
            if (null == view) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                view = inflater.inflate(R.layout.row_activity_main_fragment_display_list, parent, false);
                holder = new ViewHolder();
                holder.rlContainer_Left =(RelativeLayout) view.findViewById(R.id.container_left_display_item);
                holder.imageViewLeft =(ImageView) view.findViewById(R.id.image_left_display_item);
                holder.itemTextViewLeft =(TextView) view.findViewById(R.id.textview_left_display);
                holder.imageViewLeftFlag =(ImageView) view.findViewById(R.id.image_left_flag);
                holder.timeTextViewLeft =(TextView) view.findViewById(R.id.textview_left_time);
                holder.rlContainer_Right =(RelativeLayout) view.findViewById(R.id.container_right_display_item);
                holder.imageViewRight =(ImageView) view.findViewById(R.id.image_right_display_item);
                holder.itemTextViewRight =(TextView) view.findViewById(R.id.textview_right_display);
                holder.imageViewRightFlag =(ImageView) view.findViewById(R.id.image_right_flag);
                holder.timeTextViewRight =(TextView) view.findViewById(R.id.textview_right_time);
                view.setTag(holder);
            } else {
                holder = (ViewHolder)view.getTag();
            }
            switch(userMsg.getUserMsgDescriptor()) {
                case AppProtUserMsg.USERMSG_TEXT_FROM:
                case AppProtUserMsg.USERMSG_VOICE_FROM:
                case AppProtUserMsg.USERMSG_VOIP_FROM:
                case AppProtUserMsg.USERMSG_PHONE_FROM:
                case AppProtUserMsg.USERMSG_PTT_FROM: {
                    holder.rlContainer_Right.setVisibility(View.GONE);
                    holder.rlContainer_Left.setVisibility(View.VISIBLE);
                    int imageResId1 = UiHelpers.getMsgImageId(userMsg.getUserMsgDescriptor());
                    holder.imageViewLeft.setImageDrawable(getResources().getDrawable(imageResId1));
                    holder.itemTextViewLeft.setText(userMsg.getUserMsgString());
                    int imageResId2 = UiHelpers.getImageId(userMsg.getUserMsgState());
                    holder.imageViewLeftFlag.setImageDrawable(getResources().getDrawable(imageResId2));
                    holder.timeTextViewLeft.setText(userMsg.getUserMsgTimeToString(mActivity));
                    break;
                }
                case AppProtUserMsg.USERMSG_TEXT_TO:
                case AppProtUserMsg.USERMSG_VOICE_TO:
                case AppProtUserMsg.USERMSG_VOIP_TO:
                case AppProtUserMsg.USERMSG_PHONE_TO:
                case AppProtUserMsg.USERMSG_PTT_TO: {
                    holder.rlContainer_Left.setVisibility(View.GONE);
                    holder.rlContainer_Right.setVisibility(View.VISIBLE);
                    int imageResId1 = UiHelpers.getMsgImageId(userMsg.getUserMsgDescriptor());
                    holder.imageViewRight.setImageDrawable(getResources().getDrawable(imageResId1));
                    holder.itemTextViewRight.setText(userMsg.getUserMsgString());
                    int imageResId2 = UiHelpers.getImageId(userMsg.getUserMsgState());
                    holder.imageViewRightFlag.setImageDrawable(getResources().getDrawable(imageResId2));
                    holder.timeTextViewRight.setText(userMsg.getUserMsgTimeToString(mActivity));
                    break;
                }
            }
            return view;
        }

        private class ViewHolder {
            RelativeLayout rlContainer_Left;
            ImageView imageViewLeft;
            TextView itemTextViewLeft;
            ImageView imageViewLeftFlag;
            TextView timeTextViewLeft;
            RelativeLayout rlContainer_Right;
            ImageView imageViewRight;
            TextView itemTextViewRight;
            ImageView imageViewRightFlag;
            TextView timeTextViewRight;
        }
    }
}
