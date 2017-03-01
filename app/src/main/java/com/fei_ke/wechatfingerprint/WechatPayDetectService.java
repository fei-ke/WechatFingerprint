package com.fei_ke.wechatfingerprint;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;

import javax.crypto.Cipher;

/**
 * Created by fei on 2017/3/1.
 */

public class WechatPayDetectService extends AccessibilityService {
    private static final String TAG = "WechatPayDetectService";

    private static final String TARGET_WINDOW_CLASS_NAME = "com.tencent.mm.plugin.wallet_core.ui.l";

    private WindowManager              mWindowManager;
    private WindowManager.LayoutParams mWmParams;
    private FingerPrintLayout          mFingerPrintLayout;
    private FingerPrintHelper          mFingerPrintHelper;

    private boolean mIsFingerprintLayoutShowing;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = getSystemService(WindowManager.class);
        mWmParams = new WindowManager.LayoutParams();

        mWmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWmParams.format = PixelFormat.RGBA_8888;
        mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        mWmParams.gravity = Gravity.BOTTOM;

        //wmParams.x = 0;
        //wmParams.y = 0;


        mWmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWmParams.height = (int) (getResources().getDisplayMetrics().density * 280);

        mFingerPrintLayout = new FingerPrintLayout(this);
        mFingerPrintHelper = new FingerPrintHelper(this, mFingerPrintLayout);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (TARGET_WINDOW_CLASS_NAME.equals(event.getClassName())) {

                Log.i(TAG, "onAccessibilityEvent: in target window");

                showFingerprintWindow();
            } else {
                dismissFingerprintWindow();
            }
        }
    }

    private synchronized void showFingerprintWindow() {
        if (mIsFingerprintLayoutShowing) {
            mFingerPrintLayout.showFingerPrintLayout();
            return;
        }

        mIsFingerprintLayoutShowing = true;

        final AccessibilityNodeInfo inputEditText = findInputNode(getRootInActiveWindow());
        mWindowManager.addView(mFingerPrintLayout, mWmParams);

        mFingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
            @Override
            public void onSuccess(int purpose, Cipher cipher) {
                String pwd = mFingerPrintHelper.decrypt(cipher);

                if (!TextUtils.isEmpty(pwd)) {
                    fillText(inputEditText, pwd);
                }

                mFingerPrintHelper.setCallback(null);

                dismissFingerprintWindow();
            }

            @Override
            public void onFailure(CharSequence helpString) {

            }
        });

        mFingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
        mFingerPrintHelper.startAuthenticate();
    }

    private synchronized void dismissFingerprintWindow() {
        if (mIsFingerprintLayoutShowing) {
            mIsFingerprintLayoutShowing = false;
            mWindowManager.removeView(mFingerPrintLayout);
        }
    }

    private void fillText(AccessibilityNodeInfo editTextNode, String text) {
        if (editTextNode != null) {

            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

            Log.d(TAG, "fillText: fill text");
        }
    }

    private AccessibilityNodeInfo findInputNode(AccessibilityNodeInfo node) {

        if (node == null) return null;

        if (EditText.class.getName().equals(node.getClassName())) {
            return node;
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = findInputNode(node.getChild(i));
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {

    }

}
