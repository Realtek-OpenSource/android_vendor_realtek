package com.realtek.minilauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class RtkFileBrowserGridView extends GridView {
    private static final String LOG_TAG = "RtkFileBrowserGridView";
    public boolean isOnMeasure = false;
    public RtkFileBrowserGridView(Context context) {
        super(context);
    }

    public RtkFileBrowserGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RtkFileBrowserGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        isOnMeasure = true;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        isOnMeasure = false;
        super.onLayout(changed, l, t, r, b);
    }
}
