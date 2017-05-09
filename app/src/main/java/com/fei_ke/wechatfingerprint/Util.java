package com.fei_ke.wechatfingerprint;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by fei on 2017/5/9.
 */

public class Util {
    public static View findViewByClass(View view, String className) {
        if (view.getClass().getName().equals(className)) {
            return view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findViewByClass(group.getChildAt(i), className);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
