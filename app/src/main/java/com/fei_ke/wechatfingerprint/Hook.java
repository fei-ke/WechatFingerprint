package com.fei_ke.wechatfingerprint;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import javax.crypto.Cipher;

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

                    mFingerPrintHelper = new FingerPrintHelper(dialog.getContext(), fingerPrintLayout);
                    mFingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
                    mFingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
                        @Override
                        public void onSuccess(int purpose, Cipher cipher) {
                            String pwd = mFingerPrintHelper.decrypt(cipher);
                            if (!TextUtils.isEmpty(pwd)) {
                                editText.setText(pwd);
                            }
                        }

                        @Override
                        public void onFailure(CharSequence helpString) {

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

        Class classWalletPasswordSettingUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pwd.ui.WalletPasswordSettingUI", lpparam.classLoader);
        final Class classPreference = XposedHelpers.findClass("com.tencent.mm.ui.base.preference.Preference", lpparam.classLoader);
        Class classAdapter = XposedHelpers.findClass("com.tencent.mm.ui.base.preference.f", lpparam.classLoader);

        XposedHelpers.findAndHookMethod(classWalletPasswordSettingUI, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object thisObject = param.thisObject;
                Object adapter = XposedHelpers.getObjectField(thisObject, "dzO");

                Object preference = XposedHelpers.newInstance(classPreference, (Context) thisObject);
                XposedHelpers.callMethod(preference, "setKey", "xposed_wechat_fingerprint");
                XposedHelpers.callMethod(preference, "setTitle", (CharSequence) "Xposed 指纹支付");

                int index = (int) XposedHelpers.callMethod(adapter, "indexOf", "wallet_open_gesture_password");
                XposedHelpers.callMethod(adapter, "a", new Class[]{classPreference, int.class}, preference, index);
            }
        });

        XposedHelpers.findAndHookMethod(classWalletPasswordSettingUI, "a", classAdapter, classPreference, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object preference = param.args[1];
                String key = (String) XposedHelpers.getObjectField(preference, "dqE");
                if ("xposed_wechat_fingerprint".equals(key)) {
                    Activity activity = (Activity) param.thisObject;
                    SetPasswordFragment fragment = new SetPasswordFragment();
                    fragment.show(activity.getFragmentManager(), "dlg");
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
