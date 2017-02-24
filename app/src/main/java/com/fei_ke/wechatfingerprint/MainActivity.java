package com.fei_ke.wechatfingerprint;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.textView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetPasswordFragment fragment = new SetPasswordFragment();
                fragment.show(getFragmentManager(), "dlg");

            }
        });

    }
}
