package com.fei_ke.wechatfingerprint;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import javax.crypto.Cipher;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by fei on 2017/2/23.
 */

public class Hook implements IXposedHookLoadPackage {
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    private Class  classAdapter;
    private Field  fieldAdapterInSettingUI;
    private String methodNameOnItemClickInSettingUI;
    private Field  fieldPreferenceKey;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(WECHAT_PACKAGE_NAME) && lpparam.isFirstApplication && lpparam.processName.equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    hook(lpparam);
                }
            });
        }
    }

    private void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        //find classPswDialog
        Set<Class> classPswDialog = new HashSet<>();
        Class<?> classTmp = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.balance.ui.WalletBalanceFetchPwdInputUI", lpparam.classLoader);
        for (Field field : classTmp.getDeclaredFields()) {
            if (field.getType() != String.class && !field.getType().isPrimitive()) {
                classPswDialog.add(field.getType());
                break;
            }
        }
        classTmp = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pay.ui.WalletLoanRepaymentUI", lpparam.classLoader);
        for (Field field : classTmp.getDeclaredFields()) {
            if (field.getType() != String.class && !field.getType().isPrimitive()) {
                classPswDialog.add(field.getType());
                break;
            }
        }

        for (Class c : classPswDialog) {
            hookPswDialog(c);
        }

        final Class classWalletPasswordSettingUI = XposedHelpers.findClass("com.tencent.mm.plugin.wallet.pwd.ui.WalletPasswordSettingUI", lpparam.classLoader);
        final Class classPreference = XposedHelpers.findClass("com.tencent.mm.ui.base.preference.Preference", lpparam.classLoader);

        //find adapter class
        Method[] methods = classWalletPasswordSettingUI.getMethods();
        for (Method method : methods) {
            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();

            if (returnType == boolean.class && parameterTypes.length == 2 && parameterTypes[1] == classPreference) {
                classAdapter = parameterTypes[0];
                methodNameOnItemClickInSettingUI = method.getName();
                break;
            }
        }

        //find fieldAdapterInSettingUI
        Field[] declaredFields = classWalletPasswordSettingUI.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getType() == classAdapter) {
                field.setAccessible(true);
                fieldAdapterInSettingUI = field;
                break;
            }
        }

        //find fieldPreferenceKey
        Field[] fields = classPreference.getFields();
        for (Field field : fields) {
            if (field.getType() == String.class) {
                fieldPreferenceKey = field;
                break;
            }
        }
        XposedHelpers.findAndHookMethod(classWalletPasswordSettingUI, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object thisObject = param.thisObject;
                Object adapter = fieldAdapterInSettingUI.get(thisObject);

                //create a clickable item
                Object preference = XposedHelpers.newInstance(classPreference, (Context) thisObject);
                XposedHelpers.callMethod(preference, "setKey", "xposed_wechat_fingerprint");
                XposedHelpers.callMethod(preference, "setTitle", (CharSequence) "Xposed 指纹支付");

                //add to settingUI
                int index = (int) XposedHelpers.callMethod(adapter, "indexOf", "wallet_open_gesture_password");
                Method methodAddItem = XposedHelpers.findMethodsByExactParameters(classAdapter, void.class, classPreference, int.class)[0];
                methodAddItem.invoke(adapter, preference, index);
            }
        });


        XposedHelpers.findAndHookMethod(classWalletPasswordSettingUI, methodNameOnItemClickInSettingUI, classAdapter, classPreference, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object preference = param.args[1];
                String key = (String) fieldPreferenceKey.get(preference);
                if ("xposed_wechat_fingerprint".equals(key)) {
                    Activity activity = (Activity) param.thisObject;
                    SetPasswordFragment fragment = new SetPasswordFragment();
                    fragment.show(activity.getFragmentManager(), "dlg");
                }
            }
        });
    }

    private void hookPswDialog(Class classPswDialog) {
        XposedHelpers.findAndHookMethod(classPswDialog, "onCreate", Bundle.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Dialog dialog = (Dialog) param.thisObject;
                final View layoutKeyboard = (View) Util.findViewByClass(dialog.getWindow().getDecorView(), "com.tenpay.android.wechat.MyKeyboardWindow")
                        .getParent();

                ViewGroup container = (ViewGroup) layoutKeyboard.getParent();
                container.removeView(layoutKeyboard);

                Context remoteContext = dialog.getContext().createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
                final FingerPrintLayout fingerPrintLayout = new FingerPrintLayout(remoteContext);
                fingerPrintLayout.addView(layoutKeyboard, 0);

                container.addView(fingerPrintLayout);

                final EditText editText = (EditText) Util.findViewByClass(container, "com.tenpay.android.wechat.TenpaySecureEditText");
                final FingerPrintHelper fingerPrintHelper = new FingerPrintHelper(dialog.getContext(), fingerPrintLayout);
                fingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
                fingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
                    @Override
                    public void onSuccess(int purpose, Cipher cipher) {
                        String pwd = fingerPrintHelper.decrypt(cipher);
                        if (!TextUtils.isEmpty(pwd)) {
                            editText.setText(pwd);
                        }
                    }

                    @Override
                    public void onFailure(CharSequence helpString) {

                    }
                });
                final Message dismissMessage = (Message) XposedHelpers.getObjectField(dialog, "mDismissMessage");
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        fingerPrintHelper.stopAuthenticate();

                        if (dismissMessage != null) {
                            Message.obtain(dismissMessage).sendToTarget();
                        }
                    }
                });

                fingerPrintHelper.startAuthenticate();

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
