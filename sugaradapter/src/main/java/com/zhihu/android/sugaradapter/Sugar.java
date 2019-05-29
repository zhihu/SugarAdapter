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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum Sugar {
    INSTANCE;

    private ContainerDelegate mContainerDelegate;
    private Map<Class<? extends SugarHolder>, InjectDelegate> mInjectMap;

    @NonNull
    public ContainerDelegate getContainerDelegate() {
        if (mContainerDelegate == null) {
            try {
                Class delegateClass = Class.forName("com.zhihu.android.sugaradapter.ContainerDelegateImpl");
                mContainerDelegate = (ContainerDelegate) delegateClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return mContainerDelegate;
    }

    @Nullable
    public <T extends SugarHolder> InjectDelegate getInjectDelegate(@NonNull T t) {
        if (mInjectMap == null) {
            mInjectMap = new HashMap<>();
        }

        if (mInjectMap.containsKey(t.getClass())) {
            return mInjectMap.get(t.getClass());
        }

        try {
            Class delegateClass = Class.forName(t.getClass().getCanonicalName() + "$InjectDelegateImpl");
            InjectDelegate delegate = (InjectDelegate) delegateClass.newInstance();
            mInjectMap.put(t.getClass(), delegate);
            return delegate;
        } catch (Exception e) {
            // e.printStackTrace();
            mInjectMap.put(t.getClass(), null);
            return null;
        }
    }
}
