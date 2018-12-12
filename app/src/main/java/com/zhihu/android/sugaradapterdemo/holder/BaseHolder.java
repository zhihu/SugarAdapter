package com.zhihu.android.sugaradapterdemo.holder;

import android.support.annotation.NonNull;
import android.view.View;

import com.zhihu.android.sugaradapter.SugarHolder;

// Do something in BaseHolder, but don't use @Layout and @Id
abstract class BaseHolder<T> extends SugarHolder<T> {
    BaseHolder(@NonNull View view) {
        super(view);
    }
}
