package com.zhihu.android.sugaradapterlibrary.holder;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import com.zhihu.android.sugaradapter.Id;
import com.zhihu.android.sugaradapter.Layout;
import com.zhihu.android.sugaradapter.SugarHolder;
import com.zhihu.android.sugaradapterlibrary.R2;
import com.zhihu.android.sugaradapterlibrary.item.LibraryItem;

@Layout(R2.layout.layout_library)
public final class LibraryHolder extends SugarHolder<LibraryItem> {
    @Id(R2.id.text)
    public AppCompatTextView mTextView;

    public LibraryHolder(@NonNull View view) {
        super(view);
    }

    @Override
    protected void onBindData(@NonNull LibraryItem item) {
        mTextView.setText(item.getText());
    }
}
