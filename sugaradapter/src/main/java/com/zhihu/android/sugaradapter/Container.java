/*
 * Copyright 2018 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhihu.android.sugaradapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class Container {
    private Class<? extends SugarHolder> mHolderClass;
    private Class<?> mDataClass;
    private int mLayoutRes;
    private SugarHolder.OnCreatedCallback mCallback;
    private Object mData;

    Container(@NonNull Class<? extends SugarHolder> holderClass,
              @NonNull Class<?> dataClass, @LayoutRes int layoutRes,
              @Nullable SugarHolder.OnCreatedCallback callback) {
        mHolderClass = holderClass;
        mDataClass = dataClass;
        mLayoutRes = layoutRes;
        mCallback = callback;
    }

    @NonNull
    Class<? extends SugarHolder> getHolderClass() {
        return mHolderClass;
    }

    @NonNull
    Class<?> getDataClass() {
        return mDataClass;
    }

    @LayoutRes
    int getLayoutRes() {
        return mLayoutRes;
    }

    @Nullable
    SugarHolder.OnCreatedCallback getCallback() {
        return mCallback;
    }

    @NonNull
    Object getData() {
        return mData;
    }

    void setData(@NonNull Object data) {
        mData = data;
    }
}
