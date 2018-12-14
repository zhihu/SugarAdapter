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

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;

import com.zhihu.android.sugaradapter.Id;
import com.zhihu.android.sugaradapter.Layout;
import com.zhihu.android.sugaradapterdemo.R;
import com.zhihu.android.sugaradapterdemo.item.Foo;

@Layout(R.layout.layout_foo)
public class FooHolder extends BaseHolder<Foo> {
    @SuppressWarnings("WeakerAccess")
    @Id(R.id.text)
    public AppCompatTextView mTextView;

    public FooHolder(@NonNull View view) {
        super(view);
    }

    @Override
    public void onBindData(@NonNull Foo foo) {
        mTextView.setText(foo.getText());
    }

    @Override
    protected void onViewAttachedToWindow() {
        // DO SOMETHING
    }

    @Override
    protected void onViewDetachedFromWindow() {
        // DO SOMETHING
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
