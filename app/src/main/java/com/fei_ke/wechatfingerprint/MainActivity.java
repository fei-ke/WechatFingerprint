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

        //test();

    }

    //private void test() {
    //    findViewById(R.id.textView).setOnClickListener(new View.OnClickListener() {
    //        @Override
    //        public void onClick(View v) {
    //            SetPasswordFragment fragment = new SetPasswordFragment();
    //            fragment.show(getFragmentManager(), "dlg");
    //        }
    //    });
    //
    //    FingerPrintLayout fingerPrintLayout = (FingerPrintLayout) findViewById(R.id.layoutFingerprint);
    //    FingerPrintHelper fingerPrintHelper = new FingerPrintHelper(this, fingerPrintLayout);
    //}
}
