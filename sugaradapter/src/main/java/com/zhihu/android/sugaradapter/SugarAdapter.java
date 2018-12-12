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

import android.arch.lifecycle.Lifecycle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class SugarAdapter extends RecyclerView.Adapter<SugarHolder> {
    private static final String TAG = "SugarAdapter";

    public static final class Builder {
        private List<?> mList;
        private SparseArray<Container> mArray;

        @NonNull
        public static Builder with(@NonNull List<?> list) {
            return new Builder(list);
        }

        private Builder(@NonNull List<?> list) {
            mList = list;
            mArray = new SparseArray<>();
        }

        @NonNull
        public <SH extends SugarHolder> Builder add(@NonNull Class<SH> holderClass) {
            return add(holderClass, null);
        }

        @NonNull
        public <SH extends SugarHolder> Builder add(
                @NonNull Class<SH> holderClass, @Nullable SugarHolder.OnCreatedCallback<SH> callback) {
            ContainerDelegate delegate = Sugar.INSTANCE.getContainerDelegate();
            Class dataClass = delegate.getDataClass(holderClass);
            int layoutRes = delegate.getLayoutRes(holderClass);

            if (layoutRes == 0) {
                throw new IllegalStateException(holderClass.getCanonicalName()
                        + " must have an annotation @Layout(R.layout.*)");
            }

            mArray.put(holderClass.hashCode(), new Container(holderClass, dataClass, layoutRes, callback));
            return this;
        }

        @NonNull
        public SugarAdapter build() {
            if (mArray.size() <= 0) {
                throw new IllegalStateException("must add at least one Class<? extends SugarHolder>");
            }

            return new SugarAdapter(mList, mArray);
        }
    }

    public interface ExtraDelegate {
        void onAttachedToRecyclerView(@NonNull RecyclerView view);
        void onDetachedFromRecyclerView(@NonNull RecyclerView view);
    }

    public static abstract class Dispatcher<T> {
        // return null to use default rule
        @Nullable
        public abstract Class<? extends SugarHolder> dispatch(@NonNull T data);

        // https://stackoverflow.com/q/3437897
        @NonNull
        private Class<T> ofType() {
            try {
                // noinspection ConstantConditions
                String className = ((ParameterizedType) getClass().getGenericSuperclass())
                        .getActualTypeArguments()[0].toString().split(" ")[1];
                // noinspection unchecked
                return (Class<T>) Class.forName(className);
            } catch (@NonNull Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static abstract class SugarHolderListener<SH extends SugarHolder> {
        private Class<SH> mSugarHolderClass = ofType();

        // https://stackoverflow.com/q/3437897
        @NonNull
        private Class<SH> ofType() {
            try {
                // noinspection ConstantConditions
                String className = ((ParameterizedType) getClass().getGenericSuperclass())
                        .getActualTypeArguments()[0].toString().split(" ")[1];
                // noinspection unchecked
                return (Class<SH>) Class.forName(className);
            } catch (@NonNull Exception e) {
                throw new RuntimeException(e);
            }
        }

        private boolean isInstance(@Nullable Object object) {
            return mSugarHolderClass.isInstance(object);
        }

        public void onSugarHolderCreated(@NonNull SH holder) {}
        public void onSugarHolderBindData(@NonNull SH holder) {}
        public void onSugarHolderViewAttachedToWindow(@NonNull SH holder) {}
        public void onSugarHolderViewDetachedFromWindow(@NonNull SH holder) {}
        public void onSugarHolderViewRecycled(@NonNull SH holder) {}
    }

    private List<?> mList;
    private SparseArray<Container> mArray;
    private Map<Class<?>, Dispatcher<?>> mDispatcherMap;
    private List<ExtraDelegate> mExtraDelegateList;
    private List<SugarHolderListener<?>> mSugarHolderListenerList;

    private SugarAdapter(@NonNull List<?> list, @NonNull SparseArray<Container> array) {
        mList = list;
        mArray = array;
        mDispatcherMap = new HashMap<>();
        mExtraDelegateList = new ArrayList<>();
        mSugarHolderListenerList = new ArrayList<>();
    }

    // <editor-fold desc="Dispatcher">

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter addDispatcher(@NonNull Dispatcher<?> dispatcher) {
        Class<?> key = dispatcher.ofType();
        if (mDispatcherMap.containsKey(key)) {
            Log.d(TAG, "addDispatcher repeated"
                    + ", SugarAdapter already has a dispatcher of " + key.getCanonicalName()
                    + ", new dispatcher will cover the old one");
        }

        mDispatcherMap.put(key, dispatcher);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter removeDispatcher(@NonNull Dispatcher<?> dispatcher) {
        mDispatcherMap.remove(dispatcher.ofType());
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter clearDispatcher() {
        mDispatcherMap.clear();
        return this;
    }

    // </editor-fold>

    // <editor-fold desc="ExtraDelegate">

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter addExtraDelegate(@NonNull ExtraDelegate delegate) {
        if (!mExtraDelegateList.contains(delegate)) {
            mExtraDelegateList.add(delegate);
        }

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter removeExtraDelegate(@NonNull ExtraDelegate delegate) {
        mExtraDelegateList.remove(delegate);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter clearExtraDelegate() {
        mExtraDelegateList.clear();
        return this;
    }

    // </editor-fold>

    // <editor-fold desc="SugarHolderListener">

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public <SH extends SugarHolder> SugarAdapter addSugarHolderListener(@NonNull SugarHolderListener<SH> listener) {
        if (!mSugarHolderListenerList.contains(listener)) {
            mSugarHolderListenerList.add(listener);
        }

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter removeSugarHolderListener(@NonNull SugarHolderListener<?> listener) {
        mSugarHolderListenerList.remove(listener);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public SugarAdapter clearSugarHolderListener() {
        mSugarHolderListenerList.clear();
        return this;
    }

    // </editor-fold>

    @NonNull
    public List<?> getList() {
        return mList;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public int getItemViewType(@IntRange(from = 0) int position) {
        Object data = mList.get(position);

        Class<? extends SugarHolder> holderClass = null;
        if (mDispatcherMap.containsKey(data.getClass())) {
            Dispatcher dispatcher = mDispatcherMap.get(data.getClass());
            holderClass = dispatcher.dispatch(data);
        }

        if (holderClass != null) {
            int key = holderClass.hashCode();
            if (mArray.indexOfKey(key) < 0) {
                throw new RuntimeException("getItemViewType failed, holder: " + holderClass.getCanonicalName()
                        + ", please make sure you have added it when build SugarAdapter");
            }

            mArray.get(key).setData(data);
            return key;
        }

        for (int i = 0; i < mArray.size(); i++) {
            int key = mArray.keyAt(i);
            Container container = mArray.get(key);
            if (container.getDataClass() == data.getClass()) {
                container.setData(data);
                return key;
            }
        }

        throw new RuntimeException("getItemViewType failed, data: " + data.getClass().getCanonicalName()
                + ", please make sure you have associated it with a Class<? extends SugarHolder>");
    }

    @SuppressWarnings("unchecked")
    @Override
    @NonNull
    public SugarHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Container container = mArray.get(viewType);

        try {
            View view = LayoutInflater.from(parent.getContext()).inflate(container.getLayoutRes(), parent, false);
            SugarHolder holder = container.getHolderClass().getDeclaredConstructor(View.class).newInstance(view);
            holder.setAdapter(this);
            holder.setData(container.getData()); // makes SugarHolder#getData non-null

            SugarHolder.OnCreatedCallback callback = container.getCallback();
            if (callback != null) {
                callback.onCreated(holder);
            }

            holder.getLifecycleRegistry().handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
            for (SugarHolderListener listener : mSugarHolderListenerList) {
                if (listener.isInstance(holder)) {
                    listener.onSugarHolderCreated(holder);
                }
            }

            return holder;
        } catch (@NonNull Exception e) {
            Log.e(TAG, "onCreateViewHolder failed, holder: " + container.getHolderClass().getCanonicalName());
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull SugarHolder holder, int position) {
        Object data = mList.get(position);
        holder.setData(data); // double check
        holder.onBindData(data);
        holder.getLifecycleRegistry().handleLifecycleEvent(Lifecycle.Event.ON_START);

        for (SugarHolderListener listener : mSugarHolderListenerList) {
            if (listener.isInstance(holder)) {
                listener.onSugarHolderBindData(holder);
            }
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull SugarHolder holder) {
        // holder.onViewAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull SugarHolder holder) {
        // holder.onViewDetachedFromWindow();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewRecycled(@NonNull SugarHolder holder) {
        holder.onViewRecycled();
        holder.getLifecycleRegistry().handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        for (SugarHolderListener listener : mSugarHolderListenerList) {
            if (listener.isInstance(holder)) {
                listener.onSugarHolderViewRecycled(holder);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView view) {
        for (ExtraDelegate delegate : mExtraDelegateList) {
            if (delegate != null) {
                delegate.onAttachedToRecyclerView(view);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void onSugarHolderViewAttachedToWindow(@NonNull SugarHolder holder) {
        for (SugarHolderListener listener : mSugarHolderListenerList) {
            if (listener.isInstance(holder)) {
                listener.onSugarHolderViewAttachedToWindow(holder);
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView view) {
        for (ExtraDelegate delegate : mExtraDelegateList) {
            if (delegate != null) {
                delegate.onDetachedFromRecyclerView(view);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void onSugarHolderViewDetachedFromWindow(@NonNull SugarHolder holder) {
        for (SugarHolderListener listener : mSugarHolderListenerList) {
            if (listener.isInstance(holder)) {
                listener.onSugarHolderViewDetachedFromWindow(holder);
            }
        }
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull SugarHolder holder) {
        return holder.onFailedToRecycleView();
    }
}
