package io.awesome.gultube.player.relatedvideo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import butterknife.BindView;
import io.awesome.gultube.App;
import io.awesome.gultube.R;
import io.awesome.gultube.info_list.InfoItemBuilder;
import io.awesome.gultube.util.GlideUtils;
import io.awesome.gultube.util.Localization;
import io.awesome.gultube.util.recyclerview.AbstractViewHolder;

public class RelatedVideoViewHolder extends AbstractViewHolder {
	
	@BindView(R.id.itemThumbnailView) ImageView itemThumbnailView;
	@BindView(R.id.itemVideoTitleView) TextView itemVideoTitleView;
	@BindView(R.id.itemUploaderView) TextView itemUploaderView;
	@BindView(R.id.itemDurationView) TextView itemDurationView;
	private InfoItemBuilder infoItemBuilder;
	
	public RelatedVideoViewHolder(ViewGroup parent, RelatedVideoAdapter.Listener listener) {
		super(parent, R.layout.list_stream_item_horizontal2);
		infoItemBuilder = new InfoItemBuilder(parent.getContext());
		itemView.setOnClickListener(view -> listener.onVideoClicked(getAdapterPosition()));
	}
	
	public void set(final InfoItem infoItem) {
		if (!(infoItem instanceof StreamInfoItem)) return;
		final StreamInfoItem item = (StreamInfoItem) infoItem;
		
		itemVideoTitleView.setText(item.getName());
		itemUploaderView.setText(item.getUploaderName());
		
		if (item.getDuration() > 0) {
			itemDurationView.setText(Localization.getDurationString(item.getDuration()));
			itemDurationView.setBackgroundResource(R.drawable.duration_background);
			itemDurationView.setVisibility(View.VISIBLE);
		}
		else if (item.getStreamType() == StreamType.LIVE_STREAM) {
			itemDurationView.setText(R.string.duration_live);
			itemDurationView.setBackgroundResource(R.drawable.duration_background_live);
			itemDurationView.setVisibility(View.VISIBLE);
		}
		else {
			itemDurationView.setVisibility(View.GONE);
		}
		
		// Default thumbnail is shown on error, while loading and if the url is empty
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.getThumbnailUrl().split("hqdefault.jpg")[0] + "hqdefault.jpg");
	}
}
