package com.fei_ke.wechatfingerprint;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";


    private FingerPrintHelper mFingerPrintHelper;
    private FingerPrintLayout mFingerPrintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFingerPrintLayout = (FingerPrintLayout) findViewById(R.id.layout_fingerprint);

        mFingerPrintHelper = new FingerPrintHelper(this);

        findViewById(R.id.buttonAuth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
                mFingerPrintHelper.startAuthenticate();
            }
        });
        findViewById(R.id.buttonSetPassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFingerPrintHelper.setPurpose(FingerPrintHelper.ENCRYPT_MODE);
                mFingerPrintHelper.startAuthenticate();
            }
        });

        mFingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
            @Override
            public void onSuccess(String value) {
                Toast.makeText(getApplication(), value, Toast.LENGTH_SHORT).show();
                mFingerPrintLayout.authSuccess();
            }

            @Override
            public void onFailure(CharSequence helpString) {
                mFingerPrintLayout.authFailure(helpString);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFingerPrintHelper.stopAuthenticate();
    }
}
