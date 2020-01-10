package com.rtk.media.ext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.Button;
import android.view.View;
import android.widget.ListView;
import android.util.Log;

public class RtkListView extends ListView {

	static final String LOG_TAG = "RtkListView";

    public RtkListView(Context context) {
        super(context);
    }

    public RtkListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RtkListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
}
