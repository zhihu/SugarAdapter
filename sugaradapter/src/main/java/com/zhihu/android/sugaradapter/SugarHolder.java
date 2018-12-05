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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.recyclerview.widget.RecyclerView;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SugarHolder<T> extends RecyclerView.ViewHolder implements LifecycleOwner {
    public interface OnCreatedCallback<SH extends SugarHolder> {
        void onCreated(@NonNull SH holder);
    }

    protected abstract void onBindData(@NonNull T data);

    private Context mContext;
    private SugarAdapter mAdapter;
    private T mData;
    private LifecycleRegistry mLifecycleRegistry;

    public SugarHolder(@NonNull View view) {
        super(view);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                SugarHolder.this.onViewAttachedToWindow();
                getLifecycleRegistry().handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

                // trick
                if (mAdapter != null) {
                    mAdapter.onSugarHolderViewAttachedToWindow(SugarHolder.this);
                }
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                getLifecycleRegistry().handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
                SugarHolder.this.onViewDetachedFromWindow();
                getLifecycleRegistry().handleLifecycleEvent(Lifecycle.Event.ON_STOP);

                // trick
                if (mAdapter != null) {
                    mAdapter.onSugarHolderViewDetachedFromWindow(SugarHolder.this);
                }
            }
        });

        mContext = view.getContext();
        mLifecycleRegistry = new LifecycleRegistry(this);

        InjectDelegate delegate = Sugar.INSTANCE.getInjectDelegate(this);
        if (delegate != null) {
            delegate.injectView(this, view);
        }
    }

    // <editor-fold desc="Lifecycle>

    @Override
    @NonNull
    public final Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @NonNull
    final LifecycleRegistry getLifecycleRegistry() {
        return mLifecycleRegistry;
    }

    final void setAdapter(@NonNull SugarAdapter adapter) {
        mAdapter = adapter;
    }

    protected final void setData(@NonNull T data) {
        mData = data;
    }

    // </editor-fold>

    // DO NOT call getAdapter() in Constructor, otherwise return null
    @NonNull
    public final SugarAdapter getAdapter() {
        return mAdapter;
    }

    // DO NOT call getData() in Constructor, otherwise return null
    @NonNull
    public final T getData() {
        return mData;
    }

    @NonNull
    public final View getRootView() {
        return itemView;
    }

    @NonNull
    protected final Context getContext() {
        return mContext;
    }

    @ColorInt
    protected final int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getContext(), colorRes);
    }

    @NonNull
    protected final Drawable getDrawable(@DrawableRes int drawableRes) {
        return ContextCompat.getDrawable(getContext(), drawableRes);
    }

    @NonNull
    protected final String getString(@StringRes int stringRes) {
        return getContext().getString(stringRes);
    }

    @NonNull
    protected final String getString(@StringRes int stringRes, @NonNull Object... formatArgs) {
        return getContext().getString(stringRes, formatArgs);
    }

    @SuppressWarnings({"unchecked", "TypeParameterHidesVisibleType"})
    @Nullable
    protected final <T extends View> T findViewById(@IdRes int id) {
        return (T) getRootView().findViewById(id);
    }

    @IntRange(from = 0)
    @Px
    protected final int dp2px(@FloatRange(from = 0.0F) float dp) {
        return (int) (getContext().getResources().getDisplayMetrics().density * dp + 0.5F);
    }

    @IntRange(from = 0)
    @Px
    protected final int sp2px(@FloatRange(from = 0.0F) float sp) {
        return (int) (getContext().getResources().getDisplayMetrics().scaledDensity * sp + 0.5F);
    }

    protected void onViewAttachedToWindow() {
        // DO NOTHING
    }

    protected void onViewDetachedFromWindow() {
        // DO NOTHING
    }

    protected void onViewRecycled() {
        // DO NOTHING
    }

    protected boolean onFailedToRecycleView() {
        // DO NOTHING
        return false;
    }
}
