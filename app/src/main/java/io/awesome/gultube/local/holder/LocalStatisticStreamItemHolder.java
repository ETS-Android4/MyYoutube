package io.awesome.gultube.local.holder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;

import io.awesome.gultube.App;
import io.awesome.gultube.R;
import io.awesome.gultube.database.LocalItem;
import io.awesome.gultube.database.stream.StreamStatisticsEntry;
import io.awesome.gultube.local.LocalItemBuilder;
import io.awesome.gultube.util.GlideUtils;
import io.awesome.gultube.util.Localization;

public class LocalStatisticStreamItemHolder extends LocalItemHolder {
	
	public final ImageView itemThumbnailView;
	public final TextView itemVideoTitleView;
	public final TextView itemUploaderView;
	public final TextView itemDurationView;
	public final TextView itemAdditionalDetails;
	private final ImageButton itemMoreActions;
	
	public LocalStatisticStreamItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
		
		super(infoItemBuilder, R.layout.list_stream_item, parent);
		
		itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
		itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
		itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
		itemDurationView = itemView.findViewById(R.id.itemDurationView);
		itemAdditionalDetails = itemView.findViewById(R.id.itemAdditionalDetails);
		itemMoreActions = itemView.findViewById(R.id.btn_action);
	}
	
	private String getStreamInfoDetailLine(final StreamStatisticsEntry entry, final DateFormat dateFormat) {
		
		final String watchCount = Localization.shortViewCount(itemBuilder.getContext(), entry.watchCount);
		final String uploadDate = dateFormat.format(entry.latestAccessDate);
		return Localization.concatenateStrings(watchCount, uploadDate);
	}
	
	@Override
	public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
		
		if (!(localItem instanceof StreamStatisticsEntry)) return;
		final StreamStatisticsEntry item = (StreamStatisticsEntry) localItem;
		
		itemVideoTitleView.setText(item.title);
		itemUploaderView.setText(item.uploader);
		
		if (item.duration > 0) {
			itemDurationView.setText(Localization.getDurationString(item.duration));
			itemDurationView.setBackgroundResource(R.drawable.duration_background);
			itemDurationView.setVisibility(View.VISIBLE);
		}
		else {
			itemDurationView.setVisibility(View.GONE);
		}
		
		itemAdditionalDetails.setText(getStreamInfoDetailLine(item, dateFormat));
		
		// Default thumbnail is shown on error, while loading and if the url is empty
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.thumbnailUrl);
		
		itemView.setOnClickListener(view -> {
			if (itemBuilder.getOnItemSelectedListener() != null) {
				itemBuilder.getOnItemSelectedListener().selected(item);
			}
		});
		
		itemMoreActions.setOnClickListener(view -> {
			if (itemBuilder.getOnItemSelectedListener() != null) {
				itemBuilder.getOnItemSelectedListener().more(item, view);
			}
		});
	}
}
