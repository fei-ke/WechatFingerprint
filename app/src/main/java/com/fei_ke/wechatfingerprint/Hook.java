package com.fei_ke.wechatfingerprint;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by fei on 2017/2/23.
 */

public class Hook implements IXposedHookLoadPackage {
    private FingerPrintHelper mFingerPrintHelper;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.tencent.mm")) {
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
                    int identifier = dialog.getContext().getResources().getIdentifier("a1b", "id", "com.tencent.mm");
                    final View layoutKeyboard = dialog.getWindow().findViewById(identifier);

                    ViewGroup viewGroup = (ViewGroup) layoutKeyboard.getParent();
                    viewGroup.removeView(layoutKeyboard);

                    Context thisContext = dialog.getContext().createPackageContext("com.fei_ke.wechatfingerprint", Context.CONTEXT_IGNORE_SECURITY);
                    final FrameLayout rootLayout = (FrameLayout) View.inflate(thisContext, R.layout.layout_fingerprint, null);
                    rootLayout.addView(layoutKeyboard, 0);

                    viewGroup.addView(rootLayout);
                    final View layoutFingerprint = rootLayout.findViewById(R.id.layout_fingerprint);
                    final TextView textViewHint = (TextView) rootLayout.findViewById(R.id.tv_hint);
                    final ImageView imageView = (ImageView) rootLayout.findViewById(R.id.ic_fp);

                    rootLayout.findViewById(R.id.view_header).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TransitionManager.beginDelayedTransition(rootLayout, new Slide());
                            if (layoutFingerprint.getVisibility() != View.VISIBLE) {
                                layoutFingerprint.setVisibility(View.VISIBLE);
                            } else {
                                layoutFingerprint.setVisibility(View.GONE);
                            }
                        }
                    });

                    int idPwdEditText = dialog.getContext().getResources().getIdentifier("b6", "id", "com.tencent.mm");
                    final EditText editText = (EditText) dialog.getWindow().findViewById(idPwdEditText);

                    mFingerPrintHelper = new FingerPrintHelper(dialog.getContext());
                    mFingerPrintHelper.setPurpose(FingerPrintHelper.DECRYPT_MODE);
                    mFingerPrintHelper.setCallback(new FingerPrintHelper.Callback() {
                        @Override
                        public void onSuccess(String value) {
                            editText.setText(value);
                            imageView.setImageResource(R.drawable.ic_fingerprint_success);
                            textViewHint.setText("success");
                        }

                        @Override
                        public void onFailure(CharSequence helpString) {
                            imageView.setImageResource(R.drawable.ic_fingerprint_error);
                            textViewHint.setText(helpString);

                            //reset
                            textViewHint.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageResource(R.drawable.ic_fp);
                                    textViewHint.setText(null);
                                }
                            }, 1000);
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
