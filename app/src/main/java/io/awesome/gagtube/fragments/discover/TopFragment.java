package io.awesome.gagtube.fragments.discover;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.Stream;


import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.awesome.gagtube.R;
import io.awesome.gagtube.base.BaseFragment;
import io.awesome.gagtube.fragments.discover.adapter.VideoListAdapter;
import io.awesome.gagtube.fragments.discover.model.VideoListResponse;
import io.awesome.gagtube.local.dialog.PlaylistAppendDialog;
import io.awesome.gagtube.player.playqueue.PlayQueue;
import io.awesome.gagtube.player.playqueue.SinglePlayQueue;
import io.awesome.gagtube.retrofit.Retrofit2;
import io.awesome.gagtube.util.AppUtils;
import io.awesome.gagtube.util.Constants;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.SharedUtils;

public class TopFragment extends BaseFragment implements VideoListAdapter.Listener {
	
	@BindView(R.id.recycler_view) RecyclerView recyclerView;
	@BindView(R.id.progress_bar) ProgressBar progressBar;
	@BindView(R.id.empty_state_view) View emptyView;
	@BindView(R.id.error_panel) View errorView;
	@BindView(R.id.error_message_view) TextView errorMessageView;
	

	
	private VideoListAdapter adapter;
	private int categoryId;
	private String categoryName;
	
	public TopFragment() {
	}
	
	public static TopFragment getInstance(int categoryId, String categoryName) {
		
		TopFragment topFragment = new TopFragment();
		Bundle bundle = new Bundle();
		bundle.putInt(Constants.CATEGORY_ID, categoryId);
		bundle.putString(Constants.CATEGORY_NAME, categoryName);
		topFragment.setArguments(bundle);
		
		return topFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
	}
	
	private void init() {
		categoryId = getArguments() != null ? getArguments().getInt(Constants.CATEGORY_ID) : 10;
		categoryName = getArguments() != null ? getArguments().getString(Constants.CATEGORY_NAME) : getString(R.string.music);
	}
	
	private void initRecyclerView() {
		// VideoListAdapter
		adapter = new VideoListAdapter(this);
		
		// LinearLayoutManager
		recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(adapter);

	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.top_fragment, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		super.initViews(rootView, savedInstanceState);

		
		initRecyclerView();
		getVideos();
	}
	
	private void getVideos() {
		
		Random random = new Random();
		String apiKey = Constants.YOUTUBE_API_KEYS[random.nextInt(Constants.YOUTUBE_API_KEYS.length)];
		
		String countryCode = AppUtils.getCountryCode(activity);
		String languageCode = AppUtils.getLanguageCode(activity);
		Retrofit2.restApi().getVideosByCategory(apiKey, countryCode, languageCode, categoryId, 50)
				// apply schedulers
				.compose(Retrofit2.applySchedulers())
				// start, show progress
				.doOnSubscribe(() -> setProgressVisible(true))
				// terminate, hide progress
				.doOnTerminate(() -> setProgressVisible(false)).subscribe(
				// onNext
				videoListResponse -> {
					// set items to videoCategoryAdapter
					adapter.setItems(videoListResponse.getItems());
					
					// show emptyView if empty
					emptyView.setVisibility(videoListResponse.getItems().isEmpty() ? View.VISIBLE : View.GONE);
					recyclerView.setVisibility(videoListResponse.getItems().isEmpty() ? View.GONE : View.VISIBLE);
					errorView.setVisibility(View.GONE);
				},
				// onError
				throwable -> {
					// set error message
					errorMessageView.setText(String.format(getString(R.string.msg_nothing), categoryName));
					// show errorView
					errorView.setVisibility(View.VISIBLE);
				});
	}
	
	private void setProgressVisible(boolean progressVisible) {
		errorView.setVisibility(View.GONE);
		progressBar.setVisibility(progressVisible ? View.VISIBLE : View.GONE);
	}
	
	@OnClick(R.id.error_button_retry)
	void onRetry() {
		// retry if failed
		getVideos();
	}
	
	private PlayQueue getPlayQueue() {
		
		List<StreamInfoItem> streamInfoItems = Stream.of(adapter.getItems())
				// map to StreamInfoItem
				.map(item -> {
					
					StreamInfoItem streamInfoItem = new StreamInfoItem(Constants.YOUTUBE_SERVICE_ID, Constants.VIDEO_BASE_URL + item.getId(), item.getSnippet().getTitle(), StreamType.VIDEO_STREAM);
					// need to set thumbnail url here
					streamInfoItem.setThumbnailUrl(item.getSnippet().getThumbnails().getThumbnailUrl());
					streamInfoItem.setUploaderName(item.getSnippet().getChannelTitle());
					
					return streamInfoItem;
				})
				// toList
				.toList();
		
		return new SinglePlayQueue(streamInfoItems, 0);
	}
	
	public void playAll() {
		if (!adapter.getItems().isEmpty()) {
			 NavigationHelper.playOnPopupPlayer(activity, getPlayQueue() );
		}
	}
	
	@Override
	public void onVideoClicked(int position) {
		VideoListResponse.Item item = adapter.getItem(position);
		StreamInfoItem streamInfoItem = new StreamInfoItem(Constants.YOUTUBE_SERVICE_ID, Constants.VIDEO_BASE_URL + item.getId(), item.getSnippet().getTitle(), StreamType.VIDEO_STREAM);
		// need to set thumbnail url here
		streamInfoItem.setThumbnailUrl(item.getSnippet().getThumbnails().getThumbnailUrl());
		streamInfoItem.setUploaderName(item.getSnippet().getChannelTitle());

		NavigationHelper.openVideoDetailFragment(streamInfoItem, activity.getSupportFragmentManager(), streamInfoItem.getServiceId(), streamInfoItem.getUrl(), streamInfoItem.getName());
	}
	
	@Override
	public void onMoreOption(int position, View view) {
		VideoListResponse.Item item = adapter.getItem(position);
		
		StreamInfoItem infoItem = new StreamInfoItem(0, Constants.VIDEO_BASE_URL + item.getId(), item.getSnippet().getTitle(), StreamType.VIDEO_STREAM);
		infoItem.setThumbnailUrl(item.getSnippet().getThumbnails().getThumbnailUrl());
		infoItem.setUploaderName(item.getSnippet().getChannelTitle());
		
		// show popup menu
		showPopupMenu(infoItem, view);
	}
	
	private void showPopupMenu(StreamInfoItem infoItem, View view) {
		
		PopupMenu popup = new PopupMenu(activity, view, Gravity.END, 0, R.style.mPopupMenu);
		popup.getMenuInflater().inflate(R.menu.menu_popup, popup.getMenu());
		popup.show();
		
		popup.setOnMenuItemClickListener(item -> {
			
			switch (item.getItemId()) {
				
				case R.id.action_play:
					 NavigationHelper.playOnMainPlayer(activity, new SinglePlayQueue(Collections.singletonList(infoItem), 0) );
					break;
				
				case R.id.action_append_playlist:
					PlaylistAppendDialog.fromStreamInfoItems(Collections.singletonList(infoItem)).show(getChildFragmentManager(), "TopFragment");
					break;
				
				case R.id.action_share:
					SharedUtils.shareUrl(activity, infoItem.getName(), infoItem.getUrl());
					break;
			}
			return true;
		});
	}

	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
}
