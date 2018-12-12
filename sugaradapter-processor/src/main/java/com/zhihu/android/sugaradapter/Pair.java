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

class Pair {
    private String mFirst;
    private String mSecond;

    Pair(@NonNull String first, @NonNull String second) {
        mFirst = first;
        mSecond = second;
    }

    @NonNull
    String getFirst() {
        return mFirst;
    }

    @NonNull
    String getSecond() {
        return mSecond;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        // noinspection SimplifiableIfStatement
        if (!mFirst.equals(pair.mFirst)) return false;
        return mSecond.equals(pair.mSecond);
    }

    @Override
    public int hashCode() {
        int result = mFirst.hashCode();
        result = 31 * result + mSecond.hashCode();
        return result;
    }
}
