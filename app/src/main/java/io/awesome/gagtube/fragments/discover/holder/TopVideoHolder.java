package io.awesome.gagtube.fragments.discover.holder;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.Duration;

import butterknife.BindView;
import io.awesome.gagtube.App;
import io.awesome.gagtube.R;
import io.awesome.gagtube.fragments.discover.adapter.TopVideoAdapter;
import io.awesome.gagtube.fragments.discover.model.VideoListResponse;
import io.awesome.gagtube.util.GlideUtils;
import io.awesome.gagtube.util.Localization;
import io.awesome.gagtube.util.recyclerview.AbstractViewHolder;

public class TopVideoHolder extends AbstractViewHolder {
	
	@BindView(R.id.itemThumbnailView) ImageView itemThumbnailView;
	@BindView(R.id.itemVideoTitleView) TextView itemVideoTitleView;
	@BindView(R.id.itemUploaderView) TextView itemUploaderView;
	@BindView(R.id.itemDurationView) TextView itemDurationView;
	
	public TopVideoHolder(ViewGroup parent, TopVideoAdapter.Listener listener) {
		super(parent, R.layout.list_stream_item_horizontal);
		itemView.setOnClickListener(view -> listener.onVideoClicked(getAdapterPosition()));
	}
	
	public void set(VideoListResponse.Item item) {
		
		itemVideoTitleView.setText(item.getSnippet().getTitle());
		itemUploaderView.setText(item.getSnippet().getChannelTitle());
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			long time = Duration.parse(item.getContentDetails().getDuration()).getSeconds();
			itemDurationView.setText(Localization.getDurationString(time));
			
			// if is liveBroadcastContent
			if (!item.getSnippet().getLiveBroadcastContent().equals("none")) {
				itemDurationView.setText(R.string.duration_live);
				itemDurationView.setBackgroundResource(R.drawable.duration_background_live);
				itemDurationView.setVisibility(View.VISIBLE);
			}
			else {
				itemDurationView.setText(Localization.getDurationString(time));
				itemDurationView.setBackgroundResource(R.drawable.duration_background);
				itemDurationView.setVisibility(time > 0 ? View.VISIBLE : View.GONE);
			}
		}
		else {
			itemDurationView.setVisibility(View.GONE);
		}
		
		// default thumbnail is shown on error, while loading and if the url is empty
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.getSnippet().getThumbnails().getThumbnailUrl());
	}
}
