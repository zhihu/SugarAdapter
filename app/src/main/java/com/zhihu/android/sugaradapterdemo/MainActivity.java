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

package com.zhihu.android.sugaradapterdemo;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.zhihu.android.sugaradapter.SugarAdapter;
import com.zhihu.android.sugaradapter.SugarHolder;
import com.zhihu.android.sugaradapterdemo.holder.BarHolder;
import com.zhihu.android.sugaradapterdemo.holder.FooHolder;
import com.zhihu.android.sugaradapterdemo.holder.FooHolder2;
import com.zhihu.android.sugaradapterdemo.item.Bar;
import com.zhihu.android.sugaradapterdemo.item.Foo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class MainActivity extends AppCompatActivity {
    private static final String TAG = "SugarAdapterDemo";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));

        // Choose one of usage example
        // simpleUsageExample(recyclerView);
        singleDataMultiHolderExample(recyclerView);
        // sugarHolderListenerExample(recyclerView);
        // lifecycleAwareExample(recyclerView);
    }

    // The way of README show off
    private void simpleUsageExample(@NonNull RecyclerView recyclerView) {
        List<Object> list = new ArrayList<>();
        SugarAdapter adapter = SugarAdapter.Builder.with(list)
                .add(FooHolder.class)
                .add(BarHolder.class, holder -> holder.setBarHolderListener(new BarHolder.BarHolderListener() {
                    @Override
                    public void onBarHolderViewAttachedToWindow(@NonNull BarHolder barHolder) {
                        Log.e(TAG, "onBarHolderViewAttachedToWindow -> " + barHolder.getData().getText());
                    }

                    @Override
                    public void onBarHolderViewDetachedFromWindow(@NonNull BarHolder barHolder) {
                        Log.e(TAG, "onBarHolderViewDetachedFromWindow -> " + barHolder.getData().getText());
                    }
                }))
                .build();
        recyclerView.setAdapter(adapter);

        for (int i = 0; i < 100; i++) {
            String text = String.valueOf(i);
            list.add(i % 2 == 0 ? new Foo(text) : new Bar(text));
        }
        adapter.notifyDataSetChanged();
    }

    // If one data need map to different holder, use SugarAdapter#addDispatcher method
    private void singleDataMultiHolderExample(@NonNull RecyclerView recyclerView) {
        List<Object> list = new ArrayList<>();
        SugarAdapter adapter = SugarAdapter.Builder.with(list)
                .add(FooHolder.class)
                .add(FooHolder2.class)
                .build();
        recyclerView.setAdapter(adapter);

        adapter.addDispatcher(new SugarAdapter.Dispatcher<Foo>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            @Nullable
            public Class<? extends SugarHolder> dispatch(@NonNull Foo foo) {
                // return null to use default rule: the latest add the first match
                return Integer.parseInt(foo.getText()) % 2 == 0 ? FooHolder.class : FooHolder2.class;
            }
        });

        for (int i = 0; i < 100; i++) {
            list.add(new Foo(String.valueOf(i)));
        }
        adapter.notifyDataSetChanged();
    }

    // I want to get lifecycle callback of SugarHolder directly from SugarAdapter
    private void sugarHolderListenerExample(@NonNull RecyclerView recyclerView) {
        List<Object> list = new ArrayList<>();
        SugarAdapter adapter = SugarAdapter.Builder.with(list)
                .add(FooHolder.class)
                .add(BarHolder.class)
                .build();
        recyclerView.setAdapter(adapter);

        adapter.addSugarHolderListener(new SugarAdapter.SugarHolderListener<FooHolder>() {
            @Override
            public void onSugarHolderCreated(@NonNull FooHolder holder) {
                Log.e(TAG, "onSugarHolderCreated -> " + holder.getData().getText());
            }

            @Override
            public void onSugarHolderBindData(@NonNull FooHolder holder) {
                Log.e(TAG, "onSugarHolderBindData -> " + holder.getData().getText());
            }

            @Override
            public void onSugarHolderViewAttachedToWindow(@NonNull FooHolder holder) {
                Log.e(TAG, "onSugarHolderViewAttachedToWindow -> " + holder.getData().getText());
            }

            @Override
            public void onSugarHolderViewDetachedFromWindow(@NonNull FooHolder holder) {
                Log.e(TAG, "onSugarHolderViewDetachedFromWindow -> " + holder.getData().getText());
            }

            @Override
            public void onSugarHolderViewRecycled(@NonNull FooHolder holder) {
                Log.e(TAG, "onSugarHolderViewRecycled -> " + holder.getData().getText());
            }
        });

        for (int i = 0; i < 100; i++) {
            String text = String.valueOf(i);
            list.add(i % 2 == 0 ? new Foo(text) : new Bar(text));
        }
        adapter.notifyDataSetChanged();
    }

    // I want to use lifecycle-aware components with SugarHolder
    private void lifecycleAwareExample(@NonNull RecyclerView recyclerView) {
        List<Object> list = new ArrayList<>();
        SugarAdapter adapter = SugarAdapter.Builder.with(list)
                .add(FooHolder.class)
                .add(BarHolder.class, holder -> holder.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                    public void onSugarHolderCreated(@NonNull LifecycleOwner owner) {
                        Log.e(TAG, "onSugarHolderCreated -> " + ((BarHolder) owner).getData().getText());
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_START)
                    public void onSugarHolderBindData(@NonNull LifecycleOwner owner) {
                        Log.e(TAG, "onSugarHolderBindData -> " + ((BarHolder) owner).getData().getText());
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                    public void onSugarHolderAttachedToWindow(@NonNull LifecycleOwner owner) {
                        Log.e(TAG, "onSugarHolderAttachedToWindow -> " + ((BarHolder) owner).getData().getText());
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    public void onSugarHolderDetachedFromWindow(@NonNull LifecycleOwner owner) {
                        Log.e(TAG, "onSugarHolderDetachedFromWindow -> " + ((BarHolder) owner).getData().getText());
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                    public void onSugarHolderDetachedFromWindowToo(@NonNull LifecycleOwner owner) {
                        Log.e("tag", "onSugarHolderDetachedFromWindowToo -> " + ((BarHolder) owner).getData().getText());
                    }

                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void onSugarHolderRecycled(@NonNull LifecycleOwner owner) {
                        Log.e(TAG, "onSugarHolderRecycled -> " + ((BarHolder) owner).getData().getText());
                    }
                }))
                .build();
        recyclerView.setAdapter(adapter);

        for (int i = 0; i < 100; i++) {
            String text = String.valueOf(i);
            list.add(i % 2 == 0 ? new Foo(text) : new Bar(text));
        }
        adapter.notifyDataSetChanged();
    }
}
