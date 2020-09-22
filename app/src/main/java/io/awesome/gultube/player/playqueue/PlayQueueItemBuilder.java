package io.awesome.gultube.player.playqueue;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import io.awesome.gultube.App;
import io.awesome.gultube.util.GlideUtils;
import io.awesome.gultube.util.Localization;

public class PlayQueueItemBuilder {
	
	public interface OnSelectedListener {
		void selected(PlayQueueItem item, View view);
		
		void held(PlayQueueItem item, View view);
		
		void onStartDrag(PlayQueueItemHolder viewHolder);
	}
	
	private OnSelectedListener onItemClickListener;
	
	public PlayQueueItemBuilder() {
	}
	
	public void setOnSelectedListener(OnSelectedListener listener) {
		this.onItemClickListener = listener;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	public void buildStreamInfoItem(final PlayQueueItemHolder holder, final PlayQueueItem item) {
		
		if (!TextUtils.isEmpty(item.getTitle())) holder.itemVideoTitleView.setText(item.getTitle());
		holder.itemAdditionalDetailsView.setText(Localization.concatenateStrings(item.getUploader()));
		
		if (item.getDuration() > 0) {
			holder.itemDurationView.setText(Localization.getDurationString(item.getDuration()));
		}
		else {
			holder.itemDurationView.setVisibility(View.GONE);
		}
		
		GlideUtils.loadThumbnail(App.applicationContext, holder.itemThumbnailView, item.getThumbnailUrl());
		
		holder.itemRoot.setOnClickListener(view -> {
			if (onItemClickListener != null) {
				onItemClickListener.selected(item, view);
			}
		});
		
		holder.itemRoot.setOnLongClickListener(view -> {
			if (onItemClickListener != null) {
				onItemClickListener.held(item, view);
				return true;
			}
			return false;
		});
		
		holder.itemHandle.setOnTouchListener(getOnTouchListener(holder));
	}
	
	private View.OnTouchListener getOnTouchListener(final PlayQueueItemHolder holder) {
		
		return (view, motionEvent) -> {
			
			view.performClick();
			if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN && onItemClickListener != null) {
				onItemClickListener.onStartDrag(holder);
			}
			return false;
		};
	}
}
