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

package com.zhihu.android.sugaradapterdemo.holder;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import com.zhihu.android.sugaradapter.Id;
import com.zhihu.android.sugaradapter.Layout;
import com.zhihu.android.sugaradapter.SugarHolder;
import com.zhihu.android.sugaradapterdemo.R;
import com.zhihu.android.sugaradapterdemo.item.Bar;

@Layout(R.layout.layout_bar)
public final class BarHolder extends SugarHolder<Bar> {
    public interface BarHolderListener {
        void onBarHolderViewAttachedToWindow(@NonNull BarHolder barHolder);
        void onBarHolderViewDetachedFromWindow(@NonNull BarHolder barHolder);
    }

    @Id(R.id.text)
    public AppCompatTextView mTextView;

    private BarHolderListener mListener;

    public BarHolder(@NonNull View view) {
        super(view);
    }

    public void setBarHolderListener(@Nullable BarHolderListener listener) {
        mListener = listener;
    }

    @Override
    protected void onBindData(@NonNull Bar bar) {
        mTextView.setText(bar.getText());
    }

    @Override
    protected void onViewAttachedToWindow() {
        if (mListener != null) {
            mListener.onBarHolderViewAttachedToWindow(this);
        }
    }

    @Override
    protected void onViewDetachedFromWindow() {
        if (mListener != null) {
            mListener.onBarHolderViewDetachedFromWindow(this);
        }
    }

    @Override
    protected void onViewRecycled() {
        // DO SOMETHING
    }

    @Override
    protected boolean onFailedToRecycleView() {
        // DO SOMETHING
        return super.onFailedToRecycleView();
    }
}
