package io.awesome.gultube.fragments.discover.holder;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.time.Duration;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;
import io.awesome.gultube.App;
import io.awesome.gultube.R;
import io.awesome.gultube.fragments.discover.adapter.VideoListAdapter;
import io.awesome.gultube.fragments.discover.model.VideoListResponse;
import io.awesome.gultube.util.AppUtils;
import io.awesome.gultube.util.Constants;
import io.awesome.gultube.util.ExtractorHelper;
import io.awesome.gultube.util.GlideUtils;
import io.awesome.gultube.util.Localization;
import io.awesome.gultube.util.recyclerview.AbstractViewHolder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class VideoHolder extends AbstractViewHolder {
	
	@BindView(R.id.itemThumbnailView) ImageView thumbnails;
	@BindView(R.id.itemDurationView) TextView duration;
	@BindView(R.id.itemVideoTitleView) TextView title;
	@BindView(R.id.itemUploaderThumbnailView) CircleImageView itemUploaderThumbnailView;
	@BindView(R.id.itemAdditionalDetails) TextView additionalInfo;
	@BindView(R.id.btn_action) ImageButton btnMoreOptions;
	
	public VideoHolder(ViewGroup parent, VideoListAdapter.Listener listener) {
		
		super(parent, R.layout.list_stream_item_medium);
		
		itemView.setOnClickListener(view -> listener.onVideoClicked(getAdapterPosition()));
		btnMoreOptions.setOnClickListener(view -> listener.onMoreOption(getAdapterPosition(), view));
	}
	
	@SuppressLint("CheckResult")
	public void set(VideoListResponse.Item item) {
		
		title.setText(item.getSnippet().getTitle());
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			long time = Duration.parse(item.getContentDetails().getDuration()).getSeconds();
			duration.setText(Localization.getDurationString(time));
			
			// if is liveBroadcastContent
			if (!item.getSnippet().getLiveBroadcastContent().equals("none")) {
				duration.setText(R.string.duration_live);
				duration.setBackgroundResource(R.drawable.duration_background_live);
				duration.setVisibility(View.VISIBLE);
			}
			else {
				duration.setText(Localization.getDurationString(time));
				duration.setBackgroundResource(R.drawable.duration_background);
				duration.setVisibility(time > 0 ? View.VISIBLE : View.GONE);
			}
		}
		else {
			duration.setVisibility(View.GONE);
		}
		
		additionalInfo.setText(getAdditionalInfo(item));
		
		ExtractorHelper.getChannelInfo(Constants.YOUTUBE_SERVICE_ID, Constants.CHANNEL_BASE_URL + item.getSnippet().getChannelId(), true)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
				// onNext
				channelInfo -> {
					String avatarUrl = TextUtils.isEmpty(channelInfo.getAvatarUrl()) ? channelInfo.getAvatarUrl() : channelInfo.getAvatarUrl().replace("s100", "s720");
					GlideUtils.loadAvatar(App.applicationContext, itemUploaderThumbnailView, avatarUrl);
				},
				// onError
				throwable -> {
				});
		
		// default thumbnail is shown on error, while loading and if the url is empty
		GlideUtils.loadThumbnail(App.applicationContext, thumbnails, item.getSnippet().getThumbnails().getThumbnailUrl());
	}
	
	private String getAdditionalInfo(VideoListResponse.Item item) {
		
		String detailInfo = item.getSnippet().getChannelTitle() + Localization.DOT_SEPARATOR;
		try {
			long viewCount = Long.parseLong(item.getStatistics().getViewCount());
			if (viewCount > 0) {
				detailInfo = detailInfo + Localization.shortViewCount(additionalInfo.getContext(), viewCount);
			}
			if (!TextUtils.isEmpty(item.getSnippet().getPublishedAt())) {
				String publishedDate = AppUtils.getPublishedDate(additionalInfo.getContext(), item.getSnippet().getPublishedAt());
				if (!TextUtils.isEmpty(publishedDate)) {
					detailInfo += " • " + publishedDate;
				}
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return detailInfo;
	}
}
