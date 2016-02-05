
package com.android.myapidemo;

import com.android.myapidemo.UI.ComboViews;
import com.android.myapidemo.smartisan.browse.BrowserActivity;
import com.android.myapidemo.smartisan.browser.bookmarks.ComboViewActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener{
    
    private Button mBrowerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBrowerButton = (Button)findViewById(R.id.smartisan_browser);
        mBrowerButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mBrowerButton == v) {
            Intent intent = new Intent(this, BrowserActivity.class);
//            intent.putExtra(ComboViewActivity.EXTRA_INITIAL_VIEW,
//                    ComboViews.Bookmarks);
            //intent.putExtra(ComboViewActivity.EXTRA_COMBO_ARGS, extras);
            startActivityForResult(intent, 0);
            overridePendingTransition(
                    R.anim.pop_up_in,
                    R.anim.activity_close_enter_in_call);
        }
    }
}
