SugarAdapter
===

Make [RecyclerView.Adapter](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html "RecyclerView.Adapter") Great Again!

支持 **API v14+**

## 介绍

使用 RecyclerView 时，我们通常都要继承并复写 Adapter 的一些方法，感觉浪费时间又无聊：
 
 - `getItemCount()`
 - `getItemViewType()`
 - `onCreateViewHolder()`
 - `onBindViewHolder()`
 - `onViewAttachedToWindow()`
 - `onViewDetachedFromWindow()`
 - `onViewRecycled()`
 - `onAttachedToRecyclerView()`
 - `onDetachedFromRecyclerView()`
 - `onFailedToRecycleView()`

此外，我们通常需要 View **生命周期**相关的回调，

例如 `onViewAttachedToWindow()` 和 `onViewDetachedFromWindow()` 。

现在，我们使用 **annotationProcessor** 在编译时生成那些无聊的 Adapter 代码，并且给 ViewHolder 加上了一些特性。

完整的使用示例可以参考[这个链接](https://github.com/zhihu/SugarAdapter/tree/master/app "zhihu/SugarAdapter/app/")。

### SugarAdapter

你不需要继承并复写 Adapter ，只需要写几行 Builder 模式的代码即可：

```java
mAdapter = SugarAdapter.Builder.with(mList) // eg. List<Object>
        .add(FooHolder.class) // extends SugarHolder<Foo>
        .add(BarHolder.class, new SugarHolder.OnCreatedCallback<BarHolder>() { // extends SugarHolder<Bar>
            @Override
            public void onCreated(@NonNull BarHolder holder) {
                // holder.SETTER etc.
            }
        })
        .preInflate(true) // 预先解析 ViewHolder 的 XML 提升列表滚动性能
        .build();
mRecyclerView.setAdapter(mAdapter);

// mAdapter.notifyItem* or mAdapter.notifyDataSetChanged()
// mAdapter.setExtraDelegate() with onAttachedToRecyclerView()/onDetachedFromRecyclerView()
```

这样我们就创建了一个 Adapter ，就是这么简单！

### SugarHolder

Layout - ViewType - Data ，三位一体，所以我们要必须这样使用 SugarHolder ：

```java
// 缺省认为 FooHolder 是不可继承的；
// 如果你需要用到继承，请参考 demo 中的做法
@Layout(R.layout.foo) 
public final class FooHolder extends SugarHolder<Foo> {
    // 如果你不想手写 findViewById() 代码，
    // 只需要给定义的 View 加上 @Id() 注解，
    // 并且保证它为 public 的即可
    @Id(R.id.text)
    public TextView mTextView;

    public FooHolder(@NonNull View view) {
        super(view);
    }

    @Override
    pubilc void onBindData(@NonNull Foo foo) {
        mTextView.setText(foo.getText());
    }
}
```

SugarHolder 同时还有一些你可能想要使用或复写的方法：

 - `getAdapter()`   // 获取当前 holder 所在的 adapter 实例，可能不是一个好的设计
 - `getData()`      // 获取当前 holder 持有的 data 实例，所以你不需要自己手动持有了
 - `getLifecycle()` // 支持 lifecycle-aware components
 - `getContext()`   // 获取当前 holder 所需的 context 实例，酷
 - `getColor()`
 - `getDrawable()`
 - `getString()`
 - `getRootView()`
 - `findViewById()`
 - `dp2px()`
 - `sp2px()`
 - `onViewAttachedToWindow()`
 - `onViewDetachedFromWindow()`
 - `onViewRecycled()`
 - `onFailedToRecycleView()`

现在你就可以很轻松地使用 ViewHolder 啦。

## Gradle

```groovy
dependencies {
    // 如果你想要迁移到 AndroidX ，可以使用 1.8.8
    implementation 'com.zhihu.android:sugaradapter:1.7.12'
    annotationProcessor 'com.zhihu.android:sugaradapter-processor:1.7.12'
}
```

## 在 Android Library 中使用

首先，在项目中引入 [ButterKnife 插件](https://github.com/JakeWharton/butterknife#library-projects "ButterKnife 插件") 。

接着，在 **module** 的 `build.config` 中配置：

```groovy
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [moduleNameOfSugarAdapter: 'YOUR_MODULE_NAME']
            }
        }
    }
}

dependencies {
    // 如果你想要迁移到 AndroidX ，可以使用 1.8.8
    implementation 'com.zhihu.android:sugaradapter:1.7.12'
    annotationProcessor 'com.zhihu.android:sugaradapter-processor:1.7.12'
}
```

然后，在 **主工程** 的 `build.config` 中配置：

```groovy
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [subModulesOfSugarAdapter: 'YOUR_MODULE_NAME_1, YOUR_MODULE_NAME_...']
            }
        }
    }
}

dependencies {
    // 如果你想要迁移到 AndroidX ，可以使用 1.8.8
    implementation 'com.zhihu.android:sugaradapter:1.7.12'
    annotationProcessor 'com.zhihu.android:sugaradapter-processor:1.7.12'
}
```

为了触发 AnnotationProcessor ，在主工程中必须存在**至少一个**被 `@Layout` 注解的 SugarHolder 子类。

最后，使用 `R2.layout.*` 和 `R2.id.*` 取代 `R.layout.*` 和 `R.id.*` ，例如 `@Layout(R2.layout.foo)`

注意：我们并不依赖 ButterKnife ，我们只是需要它的 Gradle Plugin 生成 R2.java

## 感谢：

 - [JakeWharton/butterknife](https://github.com/JakeWharton/butterknife "JakeWharton/butterknife")
 - [HendraAnggrian/r-parser](https://github.com/HendraAnggrian/r-parser "HendraAnggrian/r-parser")

## 开源协议

    Copyright 2018 Zhihu Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
