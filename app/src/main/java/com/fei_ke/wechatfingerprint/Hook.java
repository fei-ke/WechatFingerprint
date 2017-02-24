package com.fei_ke.wechatfingerprint;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by fei on 2017/2/23.
 */

public class Hook implements IXposedHookLoadPackage {
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    private FingerPrintHelper mFingerPrintHelper;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(WECHAT_PACKAGE_NAME)) {
            hook(lpparam);
        }
    }

    private void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class clazzPswDialog = XposedHelpers.findClass(" com.tencent.mm.plugin.wallet_core.ui.l", lpparam.classLoader);

        XposedHelpers.findAndHookMethod(clazzPswDialog, "onCreate", Bundle.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Dialog dialog = (Dialog) param.thisObject;
                    int identifier = dialog.getContext().getResources().getIdentifier("a1b", "id", WECHAT_PACKAGE_NAME);
                    final View layoutKeyboard = dialog.getWindow().findViewById(identifier);

                    ViewGroup container = (ViewGroup) layoutKeyboard.getParent();
                    container.removeView(layoutKeyboard);

                    Context remoteContext = dialog.getContext().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
                    final FingerPrintLayout fingerPrintLayout = new FingerPrintLayout(remoteContext);
                    fingerPrintLayout.addView(layoutKeyboard, 0);

                    container.addView(fingerPrintLayout);


                    int idPwdEditText = dialog.getContext().getResources().getIdentifier("b6", "id", WECHAT_PACKAGE_NAME);
                    final EditText editText = (EditText) dialog.getWindow().findViewById(idPwdEditText);

                    mFingerPrintHelper = new FingerPrintHelper(dialog.getContext());
                    mFingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
                    mFingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
                        @Override
                        public void onSuccess(String value) {
                            editText.setText(value);
                            fingerPrintLayout.authSuccess();
                        }

                        @Override
                        public void onFailure(CharSequence helpString) {
                            fingerPrintLayout.authFailure(helpString);
                        }
                    });

                    mFingerPrintHelper.startAuthenticate();

                } catch (Throwable t) {
                    log(t);
                }
            }
        });


        XposedHelpers.findAndHookMethod(clazzPswDialog, "dismiss", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mFingerPrintHelper != null) {
                    mFingerPrintHelper.stopAuthenticate();
                    mFingerPrintHelper = null;
                }
            }
        });
    }

    private void log(String log) {
        XposedBridge.log(log);
    }

    private void log(Throwable t) {
        XposedBridge.log(t);
    }

}
