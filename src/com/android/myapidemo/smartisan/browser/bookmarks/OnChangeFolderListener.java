package com.android.myapidemo.smartisan.browser.bookmarks;

    public interface OnChangeFolderListener {
        public boolean onChangeFolderAdapter();

        public boolean isReload();

        public int getCurrentType();

        public void cancelEditState();
    }

