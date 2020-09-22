package io.awesome.gagtube.local;

import android.content.Context;

import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.util.OnClickGesture;

public class LocalItemBuilder {
	
	private final Context context;
	private boolean showOptionMenu;
	
	private OnClickGesture<LocalItem> onSelectedListener;
	
	public LocalItemBuilder(Context context, boolean showOptionMenu) {
		
		this.context = context;
		this.showOptionMenu = showOptionMenu;
	}
	
	public Context getContext() {
		return context;
	}
	
	public boolean isShowOptionMenu() {
		return showOptionMenu;
	}
	
	public OnClickGesture<LocalItem> getOnItemSelectedListener() {
		return onSelectedListener;
	}
	
	public void setOnItemSelectedListener(OnClickGesture<LocalItem> listener) {
		this.onSelectedListener = listener;
	}
}
