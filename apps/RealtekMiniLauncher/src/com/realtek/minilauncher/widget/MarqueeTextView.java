package com.realtek.minilauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatTextView;

public class MarqueeTextView extends AppCompatTextView {

	static final String LOG_TAG = "MarqueeTextView";
	boolean focused = false;
	int m_SelectedStyle=0;
	int m_UnselectedStyle=0;

	Context m_Context;
	boolean m_UpdateViewOnFocusChange=false;
	public MarqueeTextView(Context context) {
		super(context);
		m_Context = context;
	}

	public MarqueeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		m_Context = context;
	}

	public MarqueeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		m_Context = context;
	}

	public void setSelectedChangeStyle(int SelectedStyle, int UnselectedStyle){
		m_SelectedStyle = SelectedStyle;
		m_UnselectedStyle = UnselectedStyle;
		m_UpdateViewOnFocusChange = true;
	}
	public void unsetSelectedChangeStyle(){
		m_UpdateViewOnFocusChange = false;
	}
	@Override
	protected void drawableStateChanged() {
		//Log.d(LOG_TAG,"drawableStateChanged isFocused:"+isFocused()+"isSelected:"+isSelected());
		if (m_UpdateViewOnFocusChange) {
			if (isSelected()) {
				setTextAppearance(m_Context, m_SelectedStyle);
			} else {
				setTextAppearance(m_Context, m_UnselectedStyle);
			}
		}
		super.drawableStateChanged();
	}

	public boolean setFocused(boolean focused) {
		return this.focused = focused;
	}

	public boolean isFocused() {
		return focused;
	}

}
