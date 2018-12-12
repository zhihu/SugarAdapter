SugarAdapter
===

Make [RecyclerView.Adapter](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html "RecyclerView.Adapter") Great Again!

Support **API v14+**

[中文介绍](https://github.com/zhihu/SugarAdapter/blob/master/README_zh_CN.md "中文介绍")

## Introduction

When we use RecyclerView, we need to extends Adapter and override some methods, which are so boring:
 
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
 
More over, we usually need View's **lifecycle** callback, 

such as `onViewAttachedToWindow()` and `onViewDetachedFromWindow()`.

Now, we use **annotationProcessor** to reduce your boring Adapter code, add some features with your ViewHolder.

An usage sample see [this link](https://github.com/zhihu/SugarAdapter/tree/master/app "zhihu/SugarAdapter/app/").

### SugarAdapter

You don't need to extends and override any code of Adapter, just write few lines of builder code:

```java
mAdapter = SugarAdapter.Builder.with(mList) // eg. List<Object>
            .add(FooHolder.class) // extends SugarHolder<Foo>
            .add(BarHolder.class, new SugarHolder.OnCreatedCallback<BarHolder>() { // extends SugarHolder<Bar>
                @Override
                public void onCreated(@NonNull BarHolder holder) {
                    // holder.SETTER etc.
                }
            })
            .build();
mRecyclerView.setAdapter(mAdapter);

// mAdapter.notifyItem* or mAdapter.notifyDataSetChanged()
// mAdapter.setExtraDelegate() with onAttachedToRecyclerView()/onDetachedFromRecyclerView()
```

That's all!

### SugarHolder

Layout - ViewType - Data, trinity, so we must extends SugarHolder as below:

```java
// Annotated subclass must be public and final; R.layout.foo also as ViewType
@Layout(R.layout.foo) 
public final class FooHolder extends SugarHolder<Foo> {
    // If you don't want to write findViewById(), 
    // just annotate view with @Id(), and make it **public**;
    // @Id() can only work with **final** class too
    @Id(R.id.text);
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

SugarHolder also has some methods which you may want to use or override:

 - `getAdapter()`   // get adapter instance directly in holder, perhaps it's not a good design for your code
 - `getData()`      // get data instance directly in holder, so you don't to hold data by yourself
 - `getLifecycle()` // support lifecycle-aware components
 - `getContext()`   // get context instance directly in holder, cool
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

Now you can use ViewHolder easily.

## Gradle

```groovy
dependencies {
    // migrate to AndroidX, use 1.8.4
    implementation 'com.zhihu.android:sugaradapter:1.7.8'
    annotationProcessor 'com.zhihu.android:sugaradapter-processor:1.7.8'
}
```

## For Android Library

First, add the [ButterKnife's plugin](https://github.com/JakeWharton/butterknife#library-projects "ButterKnife's plugin") to your project.

Second, in your **module's** `build.gradle`:

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
    // migrate to AndroidX, use 1.8.4
    implementation 'com.zhihu.android:sugaradapter:1.7.8' 
    annotationProcessor 'com.zhihu.android:sugaradapter-processor:1.7.8'
}
```

Third, in your **main project's** `build.config`:

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
    // migrate to AndroidX, use 1.8.4
    implementation 'com.zhihu.android:sugaradapter:1.7.8' 
    annotationProcessor 'com.zhihu.android:sugaradapter-processor:1.7.8'
}
```

The **main project** must have **at least one** subclass of SugarHolder with `@Layout` to toggle AnnotationProcessor.

Finally, use `R2.layout.*` and `R2.id.*` to replace `R.layout.*` and `R.id.*`, for example, `@Layout(R2.layout.foo)`

Note: we don't depend the ButterKnife in our library, we just need it's gradle plugin to generate R2.java

## Thanks

 - [JakeWharton/butterknife](https://github.com/JakeWharton/butterknife "JakeWharton/butterknife")
 - [HendraAnggrian/r-parser](https://github.com/HendraAnggrian/r-parser "HendraAnggrian/r-parser")

## License

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
