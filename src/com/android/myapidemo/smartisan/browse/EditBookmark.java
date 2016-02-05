
package com.android.myapidemo.smartisan.browse;

import android.widget.EditText;

public interface EditBookmark {

    public void getBookmarkTitleEdit(EditText titleEdit);

    public void getBookmarkUrlEdit(EditText urlEdit);

    public void isCreateNewFolder(boolean isCreate);

    public void openFolderPathPage();

    public void setLastTitle(String title);

    public void setLastUrl(String url);

    public void setFolderID(long folderID);

    public void setFolderName(String name);

    public String getLastTitle();

    public String getLastUrl();

    public String getSelectFoldeName();

    public long getRootID();

}
