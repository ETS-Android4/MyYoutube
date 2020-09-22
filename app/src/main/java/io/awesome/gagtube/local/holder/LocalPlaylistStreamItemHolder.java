package io.awesome.gagtube.local.holder;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;

import io.awesome.gagtube.App;
import io.awesome.gagtube.R;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.database.playlist.PlaylistStreamEntry;
import io.awesome.gagtube.local.LocalItemBuilder;
import io.awesome.gagtube.util.GlideUtils;
import io.awesome.gagtube.util.Localization;

public class LocalPlaylistStreamItemHolder extends LocalItemHolder {
	
	public final ImageView itemThumbnailView;
	public final TextView itemVideoTitleView;
	public final TextView itemAdditionalDetailsView;
	public final TextView itemDurationView;
	public final View itemHandleView;
	
	public LocalPlaylistStreamItemHolder(LocalItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
		
		super(infoItemBuilder, layoutId, parent);
		
		itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
		itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
		itemAdditionalDetailsView = itemView.findViewById(R.id.itemAdditionalDetails);
		itemDurationView = itemView.findViewById(R.id.itemDurationView);
		itemHandleView = itemView.findViewById(R.id.itemHandle);
	}
	
	public LocalPlaylistStreamItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
		
		this(infoItemBuilder, R.layout.list_stream_playlist_item, parent);
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
		
		if (!(localItem instanceof PlaylistStreamEntry)) return;
		final PlaylistStreamEntry item = (PlaylistStreamEntry) localItem;
		
		itemVideoTitleView.setText(item.title);
		itemAdditionalDetailsView.setText(Localization.concatenateStrings(item.uploader));
		
		if (item.duration > 0) {
			itemDurationView.setText(Localization.getDurationString(item.duration));
			itemDurationView.setBackgroundResource(R.drawable.duration_background);
			itemDurationView.setVisibility(View.VISIBLE);
		}
		else {
			itemDurationView.setVisibility(View.GONE);
		}
		
		// Default thumbnail is shown on error, while loading and if the url is empty
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.thumbnailUrl);
		
		itemView.setOnClickListener(view -> {
			
			if (itemBuilder.getOnItemSelectedListener() != null) {
				itemBuilder.getOnItemSelectedListener().selected(item);
			}
		});
		
		itemView.setLongClickable(true);
		itemView.setOnLongClickListener(view -> {
			
			if (itemBuilder.getOnItemSelectedListener() != null) {
				itemBuilder.getOnItemSelectedListener().held(item, view);
			}
			return true;
		});
		
		itemHandleView.setOnTouchListener(getOnTouchListener(item));
	}
	
	private View.OnTouchListener getOnTouchListener(final PlaylistStreamEntry item) {
		
		return (view, motionEvent) -> {
			
			view.performClick();
			
			if (itemBuilder != null && itemBuilder.getOnItemSelectedListener() != null && motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
				itemBuilder.getOnItemSelectedListener().drag(item, LocalPlaylistStreamItemHolder.this);
			}
			return false;
		};
	}
}
