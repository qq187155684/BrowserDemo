package com.android.myapidemo.smartisan.adapter;

import android.content.Context;

import com.android.myapidemo.smartisan.navigation.DynamicGridUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseDynamicGridAdapter<T> extends AbstractDynamicGridAdapter {
    private Context mContext;

    private ArrayList<T> mItems = new ArrayList<T>();
    private int mColumnCount;

    protected BaseDynamicGridAdapter(Context context, int columnCount) {
        this.mContext = context;
        this.mColumnCount = columnCount;
    }

    public BaseDynamicGridAdapter(Context context, List<T> items, int columnCount) {
        mContext = context;
        mColumnCount = columnCount;
        init(items);
    }

    private void init(List<T> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
    }


    public void set(List<T> items) {
        clear();
        init(items);
        notifyDataSetChanged();
    }

    public void clear() {
        clearStableIdMap();
        mItems.clear();
        notifyDataSetChanged();
    }

    public void add(T item) {
        addStableId(item);
        mItems.add(item);
        notifyDataSetChanged();
    }

    public void add(int position, T item) {
        addStableId(item);
        mItems.add(position, item);
        notifyDataSetChanged();
    }

    public void add(List<T> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
        notifyDataSetChanged();
    }

    public boolean contains(T info) {
        return mItems.contains(info);
    }
    public int index(T info) {
        return mItems.indexOf(info);
    }

    public void remove(Object item) {
        mItems.remove(item);
        removeStableID(item);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public T getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public int getColumnCount() {
        return mColumnCount;
    }

    public void setColumnCount(int columnCount) {
        this.mColumnCount = columnCount;
        notifyDataSetChanged();
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition < getCount()) {
            DynamicGridUtils.reorder(mItems, originalPosition, newPosition);
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean canReorder(int position) {
        return true;
    }

    public List<T> getItems() {
        return mItems;
    }

    protected Context getContext() {
        return mContext;
    }
}
