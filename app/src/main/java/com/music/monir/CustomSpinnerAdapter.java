package com.music.monir;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final int mResource;
    private ArrayList<String> arrayList = new ArrayList<>();

    public CustomSpinnerAdapter(@NonNull Context context, @LayoutRes int resource,
                              @NonNull ArrayList<String> arraylist) {
        super(context, resource, 0, arraylist);

        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResource = resource;
        arrayList = arraylist;

    }
    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent){
        final View view = mInflater.inflate(mResource, parent, false);

        TextView filter_index = (TextView) view.findViewById(R.id.simple_pinner_item);

        filter_index.setText(arrayList.get(position));
        return view;
    }
}
