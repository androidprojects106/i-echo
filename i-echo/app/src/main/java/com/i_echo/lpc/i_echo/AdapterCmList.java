package com.i_echo.lpc.i_echo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by LPC-Home1 on 4/4/2015.
 */

public class AdapterCmList extends ArrayAdapter<CmMatch> {
    private final ActivityMain mActivity;
    private ArrayList<CmMatch> mCmItems;

    public AdapterCmList(ActivityMain activity, int resId, ArrayList<CmMatch> items) {
        super(activity, resId, items);

        this.mActivity = activity;
        this.mCmItems =items;
    }

    public void setData( ArrayList<CmMatch> items) {
        this.mCmItems = items;
    }

    @Override
    public int getCount() {
        return mCmItems.size();
    }

    @Override
    public CmMatch getItem(int position) {
        return mCmItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        CmMatch cmMatch = mCmItems.get(position);
        View view = convertView;
        int imageId=0;
        if (null == view) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            view = inflater.inflate(R.layout.row_adapter_cm_list, parent, false);
            holder = new ViewHolder();
            holder.imageView = (ImageView) view.findViewById(R.id.image_cm_item);
            holder.textView = (TextView) view.findViewById(R.id.textview_cm);
            holder.textViewInfo = (TextView) view.findViewById(R.id.textview_user_info);
            holder.imageViewReason1 = (ImageView)view.findViewById(R.id.image_cm_reason1);
            holder.imageViewReason2 = (ImageView)view.findViewById(R.id.image_cm_reason2);
            holder.imageViewReason3 = (ImageView)view.findViewById(R.id.image_cm_reason3);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }
        imageId = UiHelpers.getCmImageId(cmMatch.getCmIdx());
        if (R.drawable.transparent != imageId) {
            holder.imageView.setImageDrawable(mActivity.getResources().getDrawable(imageId));
        }
        else {
            holder.imageView.setVisibility(View.GONE);
        }
        CmIdx cmIdx = CmIdxItems.getCmIdx(cmMatch.getCmIdx());
        String cmString = cmIdx.getCmString();
        holder.textView.setText(cmString);
        if (null == cmMatch.getCmInfoString()) {
            holder.textViewInfo.setVisibility(View.GONE);
        }
        else {
            String cmInfo = cmMatch.getCmInfoString();
            holder.textViewInfo.setText(cmInfo);
        }
        if (null == cmMatch.getCmReasons()) {
            holder.imageViewReason1.setVisibility(View.GONE);
            holder.imageViewReason2.setVisibility(View.GONE);
            holder.imageViewReason3.setVisibility(View.GONE);
        }
        else {
            imageId = UiHelpers.getStateImageId(cmMatch.getCmReasons()[0]);
            if (R.drawable.transparent != imageId) {
                holder.imageViewReason1.setImageDrawable(mActivity.getResources().getDrawable(imageId));
            } else
                holder.imageViewReason1.setVisibility(View.GONE);
            imageId = UiHelpers.getStateImageId(cmMatch.getCmReasons()[1]);
            if (R.drawable.transparent != imageId) {
                holder.imageViewReason2.setImageDrawable(mActivity.getResources().getDrawable(imageId));
            } else
                holder.imageViewReason2.setVisibility(View.GONE);
            imageId = UiHelpers.getStateImageId(cmMatch.getCmReasons()[2]);
            if (R.drawable.transparent != imageId) {
                holder.imageViewReason3.setImageDrawable(mActivity.getResources().getDrawable(imageId));
            } else
                holder.imageViewReason3.setVisibility(View.GONE);
        }

        return view;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView textView;
        TextView textViewInfo;
        ImageView imageViewReason1;
        ImageView imageViewReason2;
        ImageView imageViewReason3;
    }
}