package io.awesome.gagtube.util.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class SquaredImageView extends AppCompatImageView {
	
	public SquaredImageView(Context context) {
		
		super(context);
	}
	
	public SquaredImageView(Context context, AttributeSet attrs) {
		
		super(context, attrs);
	}
	
	public SquaredImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		
		super(context, attrs, defStyleAttr);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		// 1:1
		setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
	}
	
}
