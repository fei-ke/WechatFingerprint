package com.fei_ke.wechatfingerprint;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import javax.crypto.Cipher;

/**
 * SetPasswordFragment
 *
 * Created by fei on 17/2/25.
 */

public class SetPasswordFragment extends DialogFragment {
    private FingerPrintHelper mFingerPrintHelper;
    private FingerPrintLayout mFingerPrintLayout;

    private EditText mEditText;
    private Context  mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            mContext = getContext().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
            float density = mContext.getResources().getDisplayMetrics().density;

            FrameLayout rootLayout = new FrameLayout(mContext);
            rootLayout.setPadding(0, (int) (density * 20), 0, 0);
            mFingerPrintLayout = new FingerPrintLayout(mContext);
            mFingerPrintLayout.disableSwitchVisibility();
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                    (int) (density * 200));
            rootLayout.addView(mFingerPrintLayout, params);

            mEditText = new EditText(mContext);
            mEditText.setTextColor(mContext.getResources().getColor(R.color.color_fp_normal));
            mEditText.setHintTextColor(Color.parseColor("#8d8d8d"));
            mEditText.setTextSize(24);
            mEditText.setHint(R.string.pwd_input_hint);
            mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            mEditText.setInputType(EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);
            mEditText.setGravity(Gravity.CENTER);
            rootLayout.addView(mEditText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            mFingerPrintHelper = new FingerPrintHelper(getContext(), mFingerPrintLayout);

            return rootLayout;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().getDecorView().setBackgroundColor(Color.parseColor("#F5F5F5"));
        return dialog;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                getContext().getSystemService(InputMethodManager.class)
                        .showSoftInput(mEditText, InputMethodManager.SHOW_FORCED);
            }
        });


        mFingerPrintHelper.stopAuthenticate();
        mFingerPrintHelper.setPurpose(FingerPrintHelper.ENCRYPT_MODE);
        mFingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
            @Override
            public void onSuccess(int purpose, Cipher cipher) {
                mEditText.setEnabled(false);

                String password = mEditText.getText().toString().trim();

                //加密保存
                mFingerPrintHelper.encrypt(cipher, password);

                Toast.makeText(mContext, R.string.set_psw_success, Toast.LENGTH_SHORT).show();

                //delay dismiss
                mFingerPrintLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                }, 600);

            }

            @Override
            public void onFailure(CharSequence helpString) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerPrintHelper.startAuthenticate();
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerPrintHelper.stopAuthenticate();
    }
}
