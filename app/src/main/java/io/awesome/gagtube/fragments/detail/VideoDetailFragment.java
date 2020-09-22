package io.awesome.gagtube.fragments.detail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jakewharton.rxbinding2.view.RxView;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.preference.PreferenceManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import icepick.State;
import io.awesome.gagtube.App;
import io.awesome.gagtube.R;
import io.awesome.gagtube.activities.ReCaptchaActivity;
import io.awesome.gagtube.adsmanager.AppInterstitialAd;
import io.awesome.gagtube.database.subscription.SubscriptionEntity;
import io.awesome.gagtube.download.ui.DownloadDialog;
import io.awesome.gagtube.fragments.BackPressable;
import io.awesome.gagtube.fragments.BaseStateFragment;
import io.awesome.gagtube.info_list.InfoItemBuilder;
import io.awesome.gagtube.local.dialog.PlaylistAppendDialog;
import io.awesome.gagtube.local.subscription.SubscriptionService;
import io.awesome.gagtube.player.BasePlayer;
import io.awesome.gagtube.player.MainVideoPlayer;
import io.awesome.gagtube.player.PlayerState;
import io.awesome.gagtube.player.PopupVideoPlayer;
import io.awesome.gagtube.player.VideoPlayer;
import io.awesome.gagtube.player.helper.PlayerHelper;
import io.awesome.gagtube.player.playqueue.PlayQueue;
import io.awesome.gagtube.player.playqueue.SinglePlayQueue;
import io.awesome.gagtube.player.resolver.VideoPlaybackResolver;
import io.awesome.gagtube.report.ErrorActivity;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.AnimationUtils;
import io.awesome.gagtube.util.AppUtils;
import io.awesome.gagtube.util.Constants;
import io.awesome.gagtube.util.ExtractorHelper;
import io.awesome.gagtube.util.GlideUtils;
import io.awesome.gagtube.util.InfoCache;
import io.awesome.gagtube.util.ListHelper;
import io.awesome.gagtube.util.Localization;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.OnClickGesture;
import io.awesome.gagtube.util.PermissionHelper;
import io.awesome.gagtube.util.SharedUtils;
import io.awesome.gagtube.util.StateSaver;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class VideoDetailFragment extends BaseStateFragment<StreamInfo> implements BackPressable, SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener, StateSaver.WriteRead {
	
	private static final String TAG = VideoDetailFragment.class.getSimpleName();
	
	// Amount of videos to show on start
	private static final int INITIAL_RELATED_VIDEOS = 16;
	
	private InfoItemBuilder infoItemBuilder = null;
	
	private int updateFlags = 0;
	private static final int RELATED_STREAMS_UPDATE_FLAG = 0x1;
	private static final int RESOLUTIONS_MENU_UPDATE_FLAG = 0x2;
	
	@State
	protected int serviceId = Constants.YOUTUBE_SERVICE_ID;
	@State
	protected String name;
	@State
	protected String url;
	
	private StreamInfo currentInfo;
	private Disposable currentWorker;
	@NonNull
	private CompositeDisposable disposables = new CompositeDisposable();
	
	private Disposable subscribeButtonMonitor;
	private SubscriptionService subscriptionService;
	
	// Views
	@BindView(R.id.video_not_available) ImageView videoNotAvailable;
	@BindView(R.id.loading_panel) View loadingPanel;
	private LinearLayout contentRootLayoutHiding;
	
	private View videoTitleRoot;
	private TextView videoTitleTextView;
	private ImageView videoTitleToggleArrow;
	private TextView videoCountView;
	
	private TextView detailControlsDownload;
	private TextView detailControlsPopup;
	private TextView detailControlsAddToPlaylist;
	
	private LinearLayout videoDescriptionRootLayout;
	private TextView videoUploadDateView;
	private TextView videoDescriptionView;
	
	private View uploaderRootLayout;
	private TextView uploaderTextView;
	private ImageView uploaderThumb;
	private TextView uploaderSubscriberTextView;
	private MaterialButton channelSubscribeButton;
	
	private TextView thumbsUpTextView;
	private TextView thumbsDownTextView;
	
	private View nextStreamTitleView;
	private LinearLayout relatedStreamsView;
	
	private ConstraintLayout videoPlayerLayout;
	
	private VideoPlayerDetail playerImpl;
	private GestureDetector gestureDetector;
	private PlayQueue playQueue;
	@Nullable
	private PlayerState playerState;
	
	// NativeAd
	//@BindView(R.id.template_view) NativeAdView nativeAdView;
	//@BindView(R.id.adView) AdView adView;
	@BindView(R.id.switch_auto_play) SwitchMaterial switchAutoplay;
	
	public static VideoDetailFragment getInstance(StreamInfoItem streamInfoItem, int serviceId, String videoUrl, String name) {
		
		VideoDetailFragment instance = new VideoDetailFragment();
		instance.setInitialData(serviceId, videoUrl, name);
		instance.setStreamInfoItem(streamInfoItem);
		return instance;
	}
	
	@Override
	public void onAttach(@NotNull Context context) {
		super.onAttach(context);
		subscriptionService = SubscriptionService.getInstance(context);
	}
	
	// Fragment's Lifecycle
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_video_detail, container, false);
		ButterKnife.bind(this, view);
		
		// init InterstitialAd
		AppInterstitialAd.getInstance().init(activity);
		
		return view;
	}
	
	@Override
	public void onPause() {
		/*if (adView != null) {
			adView.pause();
		}*/
		super.onPause();
		if (currentWorker != null) currentWorker.dispose();
	}
	
	@Override
	public void onResume() {
		
		super.onResume();
		/*if (adView != null) {
			adView.resume();
		}*/
		
		/*// show native ad
		AppUtils.displayAds(activity, isShowAd -> {
			if (isShowAd) {
				showNativeAd();
			}
		});*/
		
		if (playerImpl != null && playerState != null) {
			playerImpl.setPlaybackQuality(playerState.getPlaybackQuality());
			playerImpl.initPlayback(playerState.getPlayQueue(), playerState.getRepeatMode(),
									playerState.getPlaybackSpeed(), playerState.getPlaybackPitch(),
									playerState.isPlaybackSkipSilence(), playerState.wasPlaying());
		}
		
		if (updateFlags != 0) {
			if (!isLoading.get() && currentInfo != null) {
				if ((updateFlags & RELATED_STREAMS_UPDATE_FLAG) != 0) initRelatedVideos(currentInfo);
			}
			updateFlags = 0;
		}
		
		// Check if it was loading when the fragment was stopped/paused,
		if (wasLoading.getAndSet(false)) {
			selectAndLoadVideo(serviceId, url, name);
		}
	}
	
	@Override
	public void onStop() {
		
		super.onStop();
		
		if (playerImpl != null) {
			playerImpl.onPause();
			playerImpl.destroyPlayer();
		}
	}
	
	@Override
	public void onDestroy() {
		
		super.onDestroy();
		
		if (playerImpl != null) {
			playerImpl.destroy();
		}
		
		PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(this);
		
		if (currentWorker != null) currentWorker.dispose();
		if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
		disposables.clear();
		currentWorker = null;
	}
	
	@Override
	public void onDestroyView() {
		/*if (adView != null) {
			adView.destroy();
		}*/
		super.onDestroyView();
	}
	
	@Override
	public String generateSuffix() {
		return null;
	}
	
	@Override
	public void writeTo(Queue<Object> objectsToSave) {
		
		if (objectsToSave != null) {
			objectsToSave.add(playerState);
		}
	}
	
	@Override
	public void readFrom(@NonNull Queue<Object> savedObjects) {
		playerState = (PlayerState) savedObjects.poll();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		if (key.equals(getString(R.string.default_video_format_key))
				|| key.equals(getString(R.string.default_resolution_key))
				|| key.equals(getString(R.string.show_higher_resolutions_key))
				|| key.equals(getString(R.string.use_external_video_player_key))) {
			updateFlags |= RESOLUTIONS_MENU_UPDATE_FLAG;
		}
		
		boolean autoplay = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(getString(R.string.auto_queue_key), false);
		if (switchAutoplay != null) switchAutoplay.setChecked(autoplay);
	}
	
	// State Saving
	private static final String INFO_KEY = "info_key";
	private static final String STACK_KEY = "stack_key";
	private static final String PLAY_QUEUE_KEY = "play_queue_key";
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		
		super.onSaveInstanceState(outState);
		
		if (!isLoading.get() && currentInfo != null && isVisible()) {
			outState.putSerializable(INFO_KEY, currentInfo);
		}
		
		outState.putSerializable(STACK_KEY, stack);
		
		if (playerImpl != null) {
			playerImpl.setRecovery();
			playerState = new PlayerState(playerImpl.getPlayQueue(), playerImpl.getRepeatMode(),
										  playerImpl.getPlaybackSpeed(), playerImpl.getPlaybackPitch(),
										  playerImpl.getPlaybackQuality(), playerImpl.getPlaybackSkipSilence(),
										  playerImpl.isPlaying());
		}
		
		// save playQueue
		if (playQueue != null) {
			outState.putSerializable(PLAY_QUEUE_KEY, playQueue);
		}
		
		StateSaver.tryToSave(activity.isChangingConfigurations(), null, outState, this);
	}
	
	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedState) {
		
		super.onRestoreInstanceState(savedState);
		
		Serializable serializable = savedState.getSerializable(INFO_KEY);
		if (serializable instanceof StreamInfo) {
			
			currentInfo = (StreamInfo) serializable;
			InfoCache.getInstance().putInfo(serviceId, url, currentInfo, InfoItem.InfoType.STREAM);
		}
		
		serializable = savedState.getSerializable(STACK_KEY);
		if (serializable instanceof Collection) {
			stack.addAll((Collection<? extends StackItem>) serializable);
		}
		
		// restore playQueue
		Serializable playQueueSerializable = savedState.getSerializable(PLAY_QUEUE_KEY);
		if (playQueueSerializable instanceof PlayQueue) {
			playQueue = (PlayQueue) playQueueSerializable;
		}
		
		StateSaver.tryToRestore(savedState, this);
	}
	
	// OnClick
	@Override
	public void onClick(View v) {
		
		if (!isLoading.get() && currentInfo != null) {
			
			switch (v.getId()) {
				
				case R.id.detail_controls_download:
					if (PermissionHelper.checkStoragePermissions(activity, PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE)) {
						openDownloadDialog();
					}
					break;
				
				case R.id.detail_controls_popup:
					AppInterstitialAd.getInstance().showInterstitialAd(() -> {
						playerImpl.onPause();
						playerImpl.onFullScreenButtonClicked();
					});
					break;
				
				case R.id.detail_controls_playlist_append:
					if (getFragmentManager() != null && currentInfo != null) {
						PlaylistAppendDialog.fromStreamInfo(currentInfo).show(getFragmentManager(), TAG);
					}
					break;
				
				case R.id.detail_uploader_root_layout:
					try {
						if (!TextUtils.isEmpty(currentInfo.getUploaderUrl())) {
							NavigationHelper.openChannelFragment(getFragmentManager(), currentInfo.getServiceId(), currentInfo.getUploaderUrl(), currentInfo.getUploaderName());
						}
					}
					catch (Exception e) {
						ErrorActivity.reportUiError(activity, e);
					}
					break;
				
				case R.id.detail_title_root_layout:
					toggleTitleAndDescription();
					break;
			}
		}
	}
	
	public void openDownloadDialog() {
		
		try {
			DownloadDialog downloadDialog = DownloadDialog.newInstance(activity, currentInfo);
			downloadDialog.show(activity.getSupportFragmentManager(), "DownloadDialog");
		}
		catch (Exception e) {
			Toast.makeText(activity, R.string.could_not_setup_download_menu, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private void toggleTitleAndDescription() {
		
		if (videoDescriptionRootLayout.getVisibility() == View.VISIBLE) {
			videoTitleTextView.setMaxLines(2);
			videoDescriptionRootLayout.setVisibility(View.GONE);
			videoTitleToggleArrow.setImageResource(R.drawable.ic_arrow_down);
		}
		else {
			videoTitleTextView.setMaxLines(20);
			videoDescriptionRootLayout.setVisibility(View.VISIBLE);
			videoTitleToggleArrow.setImageResource(R.drawable.ic_arrow_up);
		}
	}
	
	// Init
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		contentRootLayoutHiding = rootView.findViewById(R.id.detail_content_root_hiding);
		
		videoTitleRoot = rootView.findViewById(R.id.detail_title_root_layout);
		videoTitleTextView = rootView.findViewById(R.id.detail_video_title_view);
		videoTitleToggleArrow = rootView.findViewById(R.id.detail_toggle_description_view);
		videoCountView = rootView.findViewById(R.id.detail_view_count_view);
		
		detailControlsDownload = rootView.findViewById(R.id.detail_controls_download);
		detailControlsPopup = rootView.findViewById(R.id.detail_controls_popup);
		detailControlsAddToPlaylist = rootView.findViewById(R.id.detail_controls_playlist_append);
		
		videoDescriptionRootLayout = rootView.findViewById(R.id.detail_description_root_layout);
		videoUploadDateView = rootView.findViewById(R.id.detail_upload_date_view);
		videoDescriptionView = rootView.findViewById(R.id.detail_description_view);
		videoDescriptionView.setMovementMethod(LinkMovementMethod.getInstance());
		videoDescriptionView.setAutoLinkMask(Linkify.WEB_URLS);
		videoDescriptionView.setLinkTextColor(ContextCompat.getColor(activity, R.color.md_blue_500));
		
		thumbsUpTextView = rootView.findViewById(R.id.detail_thumbs_up_count_view);
		thumbsDownTextView = rootView.findViewById(R.id.detail_thumbs_down_count_view);
		
		uploaderRootLayout = rootView.findViewById(R.id.detail_uploader_root_layout);
		uploaderTextView = rootView.findViewById(R.id.detail_uploader_text_view);
		uploaderThumb = rootView.findViewById(R.id.detail_uploader_thumbnail_view);
		uploaderSubscriberTextView = rootView.findViewById(R.id.detail_uploader_subscriber_text_view);
		channelSubscribeButton = rootView.findViewById(R.id.channel_subscribe_button);
		
		nextStreamTitleView = rootView.findViewById(R.id.detail_next_stream_title);
		relatedStreamsView = rootView.findViewById(R.id.detail_related_streams_view);
		
		videoPlayerLayout = rootView.findViewById(R.id.video_player_layout);
		
		// enable/disable Autoplay
		switchAutoplay.setChecked(PlayerHelper.isAutoQueueEnabled(activity));
		
		infoItemBuilder = new InfoItemBuilder(activity);
		
		/*// show ad
		AppUtils.displayAds(activity, isShowAd -> {
			if (isShowAd) {
				showBannerAd();
			}
		});*/
		
		// handling video
		playerImpl = new VideoPlayerDetail(activity);
		playerImpl.setup(activity.findViewById(android.R.id.content));
		if (playQueue == null) {
			videoPlayerLayout.setVisibility(View.INVISIBLE);
			videoNotAvailable.setVisibility(View.VISIBLE);
		}
		else {
			videoPlayerLayout.setVisibility(View.VISIBLE);
			videoNotAvailable.setVisibility(View.INVISIBLE);
			playerImpl.handleVideo(playQueue);
		}
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void initListeners() {
		
		super.initListeners();
		
		infoItemBuilder.setOnStreamSelectedListener(new OnClickGesture<StreamInfoItem>() {
			
			@Override
			public void selected(StreamInfoItem selectedItem) {
				// show ad
				AppUtils.displayAds(activity, isShowAd -> {
					if (isShowAd) {
						AppInterstitialAd.getInstance().showInterstitialAd(() -> {
							prepareLoadVideo(selectedItem);
							//showBannerAd();
							//showNativeAd();
						});
					} else {
						prepareLoadVideo(selectedItem);
					}
					//nativeAdView.setVisibility(isShowAd ? View.VISIBLE : View.GONE);
				});
			}
			
			@Override
			public void more(StreamInfoItem selectedItem, View view) {
				showPopupMenu(selectedItem, view);
			}
		});
		
		videoTitleRoot.setOnClickListener(this);
		uploaderRootLayout.setOnClickListener(this);
		detailControlsDownload.setOnClickListener(this);
		detailControlsPopup.setOnClickListener(this);
		detailControlsAddToPlaylist.setOnClickListener(this);
	}
	
	private void prepareLoadVideo(StreamInfoItem selectedItem) {
		playQueue = new SinglePlayQueue(selectedItem);
		playerImpl.handleVideo(playQueue);
		selectAndLoadVideo(selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
	}
	
	@OnCheckedChanged(R.id.switch_auto_play)
	void onSwitchAutoPlayChecked(boolean checked) {
		PlayerHelper.setAutoQueueEnabled(activity, checked);
	}
	
	private void showPopupMenu(final StreamInfoItem streamInfoItem, final View view) {
		
		PopupMenu popup = new PopupMenu(activity, view, Gravity.END, 0, R.style.mPopupMenu);
		popup.getMenuInflater().inflate(R.menu.menu_popup_detail, popup.getMenu());
		popup.show();
		
		popup.setOnMenuItemClickListener(item -> {
			
			int id = item.getItemId();
			final int index = Math.max(currentInfo.getRelatedStreams().indexOf(streamInfoItem), 0);
			switch (id) {
				
				case R.id.action_play:
					AppInterstitialAd.getInstance().showInterstitialAd(() -> NavigationHelper.playOnMainPlayer(activity, getPlayQueue(index)));
					break;
				
				case R.id.action_append_playlist:
					if (getFragmentManager() != null) {
						PlaylistAppendDialog.fromStreamInfoItems(Collections.singletonList(streamInfoItem)).show(getFragmentManager(), TAG);
					}
					break;
				
				case R.id.action_share:
					SharedUtils.shareUrl(activity, streamInfoItem.getName(), streamInfoItem.getUrl());
					break;
			}
			return true;
		});
	}
	
	private PlayQueue getPlayQueue(final int index) {
		
		final List<InfoItem> infoItems = currentInfo.getRelatedStreams();
		List<StreamInfoItem> streamInfoItems = new ArrayList<>(infoItems.size());
		
		for (final InfoItem item : infoItems) {
			if (item instanceof StreamInfoItem) {
				streamInfoItems.add((StreamInfoItem) item);
			}
		}
		return new SinglePlayQueue(streamInfoItems, index);
	}
	
	private void initThumbnailViews(@NonNull StreamInfo info) {
		
		if (!TextUtils.isEmpty(info.getUploaderAvatarUrl())) {
			GlideUtils.loadAvatar(App.applicationContext, uploaderThumb, info.getUploaderAvatarUrl().replace("s48", "s720"));
		}
	}
	
	private void initRelatedVideos(StreamInfo streamInfo) {
		
		if (relatedStreamsView.getChildCount() > 0) relatedStreamsView.removeAllViews();
		
		if (streamInfo.getRelatedStreams() != null && !streamInfo.getRelatedStreams().isEmpty()) {
			nextStreamTitleView.setVisibility(View.VISIBLE);
			relatedStreamsView.setVisibility(View.VISIBLE);
			// set next video
			streamInfo.setVideoStreams(streamInfo.getVideoStreams());
			
			int maxRelatedVideos = Math.min(streamInfo.getRelatedStreams().size(), INITIAL_RELATED_VIDEOS);
			for (int i = 0; i < maxRelatedVideos; i++) {
				InfoItem infoItem = streamInfo.getRelatedStreams().get(i);
				relatedStreamsView.addView(infoItemBuilder.buildView(relatedStreamsView, infoItem, true));
			}
		}
		else {
			nextStreamTitleView.setVisibility(View.GONE);
			relatedStreamsView.setVisibility(View.GONE);
		}
	}
	
	// OwnStack
	/**
	 * Stack that contains the "navigation history".<br>
	 * The peek is the current video.
	 */
	protected LinkedList<StackItem> stack = new LinkedList<>();
	
	public void clearBackstack() {
		stack.clear();
	}
	
	public void pushToStack(int serviceId, String videoUrl, String name, PlayQueue playQueue) {
		
		if (stack.size() > 0 && stack.peek().getServiceId() == serviceId && stack.peek().getUrl().equals(videoUrl)) {
			return;
		}
		
		stack.push(new StackItem(serviceId, videoUrl, name, playQueue));
	}
	
	public void setTitleToUrl(int serviceId, String videoUrl, String name) {
		
		if (name != null && !name.isEmpty()) {
			for (StackItem stackItem : stack) {
				if (stack.peek().getServiceId() == serviceId && stackItem.getUrl().equals(videoUrl)) {
					stackItem.setTitle(name);
				}
			}
		}
	}
	
	@Override
	public boolean onBackPressed() {
		
		// That means that we are on the start of the stack,
		// return false to let the MainActivity handle the onBack
		if (stack.size() <= 1) {
			return false;
		}
		// Remove top
		stack.pop();
		// Get stack item from the new top
		StackItem peek = stack.peek();
		if (peek == null) return false;
		
		// get playQueue from back stack
		PlayQueue playQueue = peek.getPlayQueue();
		
		// handle video player
		playerImpl.handleVideo(playQueue);
		// load video
		selectAndLoadVideo(peek.getServiceId(), peek.getUrl(), !TextUtils.isEmpty(peek.getTitle()) ? peek.getTitle() : "");
		
		// show ad
		/*AppUtils.displayAds(activity, isShowAd -> {
			if (isShowAd) {
				showBannerAd();
				showNativeAd();
			}
			nativeAdView.setVisibility(isShowAd ? View.VISIBLE : View.GONE);
		});*/
		
		return true;
	}
	
	// Info loading and handling
	@Override
	protected void doInitialLoadLogic() {
		
		if (currentInfo == null) prepareAndLoadInfo(playQueue);
		else prepareAndHandleInfo(currentInfo, playQueue);
	}
	
	public void selectAndLoadVideo(int serviceId, String videoUrl, String name) {
		
		setInitialData(serviceId, videoUrl, name);
		prepareAndLoadInfo(playQueue);
	}
	
	public void prepareAndHandleInfo(final StreamInfo info, PlayQueue playQueue) {
		
		setInitialData(info.getServiceId(), info.getOriginalUrl(), info.getName());
		pushToStack(serviceId, url, name, playQueue);
		showLoading();
		
		AnimationUtils.animateView(contentRootLayoutHiding, false, 0, 0, () -> {
			handleResult(info);
			showContentWithAnimation(0, 0, .01f);
		});
	}
	
	protected void prepareAndLoadInfo(PlayQueue playQueue) {
		
		pushToStack(serviceId, url, name, playQueue);
		startLoading(false);
	}
	
	@Override
	public void startLoading(boolean forceLoad) {
		
		super.startLoading(forceLoad);
		
		currentInfo = null;
		if (currentWorker != null) currentWorker.dispose();
		
		currentWorker = ExtractorHelper.getStreamInfo(serviceId, url, forceLoad)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
						// onNext
						result -> {
							
							isLoading.set(false);
							currentInfo = result;
							showContentWithAnimation(0, 0, 0);
							handleResult(result);
							
							// show / hide video player detail
							videoNotAvailable.setVisibility(View.INVISIBLE);
							loadingPanel.setVisibility(View.GONE);
							videoPlayerLayout.setVisibility(View.VISIBLE);
							
							// reload video if network fail
							if (currentInfo != null && isNetworkRetry) {
								playerImpl.handleVideo(new SinglePlayQueue(currentInfo));
								isNetworkRetry = false;
							}
						},
						// onError
						throwable -> {
							isLoading.set(false);
							onError(throwable);
						});
	}
	
	// Utils
	private void prepareDescription(final String descriptionHtml) {
		
		if (!TextUtils.isEmpty(descriptionHtml)) {
			disposables.add(Single.just(descriptionHtml).map(description -> {
				Spanned parsedDescription;
				if (Build.VERSION.SDK_INT >= 24) {
					parsedDescription = Html.fromHtml(description, 0);
				}
				else {
					parsedDescription = Html.fromHtml(description);
				}
				return parsedDescription;
			}).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe(spanned -> {
				videoDescriptionView.setText(spanned);
				videoDescriptionView.setVisibility(View.VISIBLE);
			}));
		}
	}
	
	private void showContentWithAnimation(long duration, long delay, @FloatRange(from = 0.0f, to = 1.0f) float translationPercent) {
		
		int translationY = (int) (getResources().getDisplayMetrics().heightPixels * (translationPercent > 0.0f ? translationPercent : .06f));
		
		contentRootLayoutHiding.animate().setListener(null).cancel();
		contentRootLayoutHiding.setAlpha(0f);
		contentRootLayoutHiding.setTranslationY(translationY);
		contentRootLayoutHiding.setVisibility(View.VISIBLE);
		contentRootLayoutHiding.animate()
				.alpha(1f)
				.translationY(0)
				.setStartDelay(delay)
				.setDuration(duration)
				.setInterpolator(new FastOutSlowInInterpolator())
				.start();
		
		uploaderRootLayout.animate().setListener(null).cancel();
		uploaderRootLayout.setAlpha(0f);
		uploaderRootLayout.setTranslationY(translationY);
		uploaderRootLayout.setVisibility(View.VISIBLE);
		uploaderRootLayout.animate()
				.alpha(1f)
				.translationY(0)
				.setStartDelay((long) (duration * .5f) + delay)
				.setDuration(duration)
				.setInterpolator(new FastOutSlowInInterpolator())
				.start();
	}
	
	protected void setInitialData(int serviceId, String url, String name) {
		
		this.serviceId = serviceId;
		this.url = url;
		this.name = !TextUtils.isEmpty(name) ? name : "";
	}
	
	protected void setStreamInfoItem(StreamInfoItem streamInfoItem) {
		
		playQueue = new SinglePlayQueue(streamInfoItem);
	}
	
	private void setErrorImage(final int imageResource) {
		
		if (videoNotAvailable != null) {
			videoNotAvailable.setImageDrawable(ContextCompat.getDrawable(activity, imageResource));
			AnimationUtils.animateView(videoNotAvailable, false, 0, 0, () -> AnimationUtils.animateView(videoNotAvailable, true, 0));
		}
	}
	
	@Override
	public void showError(String message, boolean showRetryButton) {
		showError(message, showRetryButton, R.drawable.no_image);
	}
	
	protected void showError(String message, boolean showRetryButton, @DrawableRes int imageError) {
		
		super.showError(message, showRetryButton);
		setErrorImage(imageError);
	}
	
	// Contract
	@Override
	public void showLoading() {
		
		super.showLoading();
		
		AnimationUtils.animateView(contentRootLayoutHiding, false, 0);
		
		videoTitleTextView.setText(name != null ? name : "");
		videoTitleTextView.setMaxLines(2);
		AnimationUtils.animateView(videoTitleTextView, true, 0);
		
		videoDescriptionRootLayout.setVisibility(View.GONE);
		videoTitleToggleArrow.setImageResource(R.drawable.ic_arrow_down);
		videoTitleToggleArrow.setVisibility(View.GONE);
		videoTitleRoot.setClickable(false);
		
		uploaderThumb.setImageBitmap(null);
	}
	
	@SuppressLint("CheckResult")
	@Override
	public void handleResult(@NonNull StreamInfo streamInfo) {
		
		super.handleResult(streamInfo);
		
		setInitialData(streamInfo.getServiceId(), streamInfo.getOriginalUrl(), streamInfo.getName());
		pushToStack(serviceId, url, name, playQueue);
		
		videoTitleTextView.setText(name);
		
		if (!TextUtils.isEmpty(streamInfo.getUploaderName())) {
			uploaderTextView.setText(streamInfo.getUploaderName());
			uploaderTextView.setVisibility(View.VISIBLE);
			uploaderTextView.setSelected(true);
		}
		else {
			uploaderTextView.setVisibility(View.GONE);
		}
		
		if (streamInfo.getViewCount() >= 0) {
			videoCountView.setText(Localization.localizeViewCount(activity, streamInfo.getViewCount()));
			videoCountView.setVisibility(View.VISIBLE);
		}
		else {
			videoCountView.setVisibility(View.GONE);
		}
		
		// get channel's subscribers
		if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
		ExtractorHelper.getChannelInfo(this.serviceId, streamInfo.getUploaderUrl(), true)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
				// onNext
				channelInfo -> {
					if (activity != null) {
						uploaderSubscriberTextView.setText(Localization.localizeSubscribersCount(activity, channelInfo.getSubscriberCount()));
					}
					updateSubscription(channelInfo);
					monitorSubscription(channelInfo);

				},
				// onError
				throwable -> uploaderSubscriberTextView.setText(R.string.unknown_content));
		
		// get like/dislike count
		if (streamInfo.getDislikeCount() == -1 && streamInfo.getLikeCount() == -1) {
			thumbsUpTextView.setText(R.string.unknown_content);
			thumbsDownTextView.setText(R.string.unknown_content);
		}
		else {
			// dislike count
			if (streamInfo.getDislikeCount() >= 0) {
				thumbsDownTextView.setText(Localization.shortCount(activity, streamInfo.getDislikeCount()));
			}
			else {
				thumbsDownTextView.setText(R.string.unknown_content);
			}
			
			// like count
			if (streamInfo.getLikeCount() >= 0) {
				thumbsUpTextView.setText(Localization.shortCount(activity, streamInfo.getLikeCount()));
			}
			else {
				thumbsUpTextView.setText(R.string.unknown_content);
			}
		}
		
		videoTitleRoot.setClickable(true);
		videoTitleToggleArrow.setVisibility(View.VISIBLE);
		videoTitleToggleArrow.setImageResource(R.drawable.ic_arrow_down);
		videoDescriptionView.setVisibility(View.GONE);
		videoDescriptionRootLayout.setVisibility(View.GONE);
		if (streamInfo.getUploadDate() != null) {
			videoUploadDateView.setText(Localization.localizeDate(activity, streamInfo.getUploadDate().date().getTime()));
		}
		prepareDescription(streamInfo.getDescription().getContent());
		initRelatedVideos(streamInfo);
		initThumbnailViews(streamInfo);
		
		setTitleToUrl(streamInfo.getServiceId(), streamInfo.getUrl(), streamInfo.getName());
		setTitleToUrl(streamInfo.getServiceId(), streamInfo.getOriginalUrl(), streamInfo.getName());
		
		if (!streamInfo.getErrors().isEmpty()) {
			showSnackBarError(streamInfo.getErrors(), UserAction.REQUESTED_STREAM, NewPipe.getNameOfService(streamInfo.getServiceId()), streamInfo.getUrl(), 0);
		}
	}
	
	private void monitorSubscription(final ChannelInfo info) {
		
		final Consumer<Throwable> onError = throwable -> {
			AnimationUtils.animateView(channelSubscribeButton, false, 100);
		};
		
		final Observable<List<SubscriptionEntity>> observable = subscriptionService.subscriptionTable()
				.getSubscription(info.getServiceId(), info.getUrl())
				.toObservable();
		
		disposables.add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(getSubscribeUpdateMonitor(info), onError));
		
		disposables.add(observable
								// Some updates are very rapid (when calling the updateSubscription(info), for example)
								// so only update the UI for the latest emission ("sync" the subscribe button's state)
								.debounce(100, TimeUnit.MILLISECONDS)
								.observeOn(AndroidSchedulers.mainThread())
								.subscribe(subscriptionEntities -> updateSubscribeButton(!subscriptionEntities.isEmpty()), onError));
		
	}
	
	private Function<Object, Object> mapOnSubscribe(final SubscriptionEntity subscription) {
		
		return object -> {
			subscriptionService.subscriptionTable().insert(subscription);
			return object;
		};
	}
	
	private Function<Object, Object> mapOnUnsubscribe(final SubscriptionEntity subscription) {
		
		return object -> {
			subscriptionService.subscriptionTable().delete(subscription);
			return object;
		};
	}
	
	private void updateSubscription(final ChannelInfo info) {
		
		final Action onComplete = () -> {
		};
		
		final Consumer<Throwable> onError = throwable -> {
		};
		
		disposables.add(subscriptionService.updateChannelInfo(info)
								.subscribeOn(Schedulers.io())
								.observeOn(AndroidSchedulers.mainThread())
								.subscribe(onComplete, onError));
	}
	
	private Disposable monitorSubscribeButton(final Button subscribeButton, final Function<Object, Object> action) {
		
		final Consumer<Object> onNext = object -> {
		};
		
		final Consumer<Throwable> onError = throwable -> {
		};
		
		/* Emit clicks from main thread unto io thread */
		return RxView.clicks(subscribeButton)
				.subscribeOn(AndroidSchedulers.mainThread())
				.observeOn(Schedulers.io())
				.debounce(100, TimeUnit.MILLISECONDS) // Ignore rapid clicks
				.map(action)
				.subscribe(onNext, onError);
	}
	
	private Consumer<List<SubscriptionEntity>> getSubscribeUpdateMonitor(final ChannelInfo info) {
		
		return subscriptionEntities -> {
			
			if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
			
			if (subscriptionEntities.isEmpty()) {
				SubscriptionEntity channel = new SubscriptionEntity();
				channel.setServiceId(info.getServiceId());
				channel.setUrl(info.getUrl());
				channel.setData(info.getName(), info.getAvatarUrl(), info.getDescription(), info.getSubscriberCount());
				subscribeButtonMonitor = monitorSubscribeButton(channelSubscribeButton, mapOnSubscribe(channel));
			}
			else {
				final SubscriptionEntity subscription = subscriptionEntities.get(0);
				subscribeButtonMonitor = monitorSubscribeButton(channelSubscribeButton, mapOnUnsubscribe(subscription));
			}
		};
	}
	
	private void updateSubscribeButton(boolean isSubscribed) {
		
		boolean isButtonVisible = channelSubscribeButton.getVisibility() == View.VISIBLE;
		int backgroundDuration = isButtonVisible ? 100 : 0;
		int textDuration = isButtonVisible ? 100 : 0;
		
		int subscribeBackground = ContextCompat.getColor(activity, R.color.subscribe_background_color);
		int subscribeText = ContextCompat.getColor(activity, R.color.subscribe_text_color);
		int subscribedBackground = ContextCompat.getColor(activity, R.color.subscribed_background_color);
		int subscribedText = ContextCompat.getColor(activity, R.color.subscribed_text_color);
		
		if (!isSubscribed) {
			channelSubscribeButton.setText(R.string.subscribe_button_title);
			AnimationUtils.animateBackgroundColor(channelSubscribeButton, backgroundDuration, subscribedBackground, subscribeBackground);
			AnimationUtils.animateTextColor(channelSubscribeButton, textDuration, subscribedText, subscribeText);
		}
		else {
			channelSubscribeButton.setText(R.string.subscribed_button_title);
			AnimationUtils.animateBackgroundColor(channelSubscribeButton, backgroundDuration, subscribeBackground, subscribedBackground);
			AnimationUtils.animateTextColor(channelSubscribeButton, textDuration, subscribeText, subscribedText);
		}
		
		AnimationUtils.animateView(channelSubscribeButton, AnimationUtils.Type.LIGHT_SCALE_AND_ALPHA, true, 100);
	}
	
	// Stream Results
	@Override
	protected boolean onError(Throwable exception) {
		
		if (super.onError(exception)) return true;
		
		if (exception instanceof ContentNotAvailableException) {
			showError(getString(R.string.content_not_available), false);
		}
		else {
			int errorId = exception instanceof YoutubeStreamExtractor.DecryptException
					? R.string.youtube_signature_decryption_error
					: exception instanceof ParsingException
					? R.string.parsing_error
					: R.string.general_error;
			onUnrecoverableError(exception, UserAction.REQUESTED_STREAM, NewPipe.getNameOfService(serviceId), url, errorId);
		}
		
		return true;
	}
	
	protected class VideoPlayerDetail extends VideoPlayer {
		
		private ImageButton btnFullScreen;
		private ImageButton playPauseButton;
		private ImageButton playPreviousButton;
		private ImageButton playNextButton;
		private ImageButton btnPopup;
		private ImageButton btnShare;
		private ImageButton btnBack;
		
		VideoPlayerDetail(Context context) {
			super("VideoPlayerDetail", context);
		}
		
		@Override
		public void initViews(View rootView) {
			
			super.initViews(rootView);
			
			btnFullScreen = rootView.findViewById(R.id.btn_play_fullscreen);
			playPauseButton = rootView.findViewById(R.id.playPauseButton);
			playPreviousButton = rootView.findViewById(R.id.playPreviousButton);
			playNextButton = rootView.findViewById(R.id.playNextButton);
			btnPopup = rootView.findViewById(R.id.btn_popup);
			btnShare = rootView.findViewById(R.id.btn_share);
			btnBack = rootView.findViewById(R.id.btn_back);
			
			getRootView().setKeepScreenOn(true);
		}
		
		@SuppressLint("ClickableViewAccessibility")
		@Override
		public void initListeners() {
			
			super.initListeners();
			
			MySimpleOnGestureListener listener = new MySimpleOnGestureListener();
			gestureDetector = new GestureDetector(context, listener);
			gestureDetector.setIsLongpressEnabled(false);
			videoPlayerLayout.setOnTouchListener(listener);
			
			playPauseButton.setOnClickListener(this);
			playPreviousButton.setOnClickListener(this);
			playNextButton.setOnClickListener(this);
			btnFullScreen.setOnClickListener(this);
			btnPopup.setOnClickListener(this);
			btnShare.setOnClickListener(this);
			btnBack.setOnClickListener(this);
		}
		
		@Override
		public void onClick(View v) {
			
			super.onClick(v);
			
			if (v.getId() == playPauseButton.getId()) {
				onPlayPause();
			}
			else if (v.getId() == playPreviousButton.getId()) {
				onBackPressed();
			}
			else if (v.getId() == playNextButton.getId()) {
				
				if (currentInfo != null) {
					
					StreamInfoItem streamInfoItem = (StreamInfoItem) currentInfo.getRelatedStreams().get(0);
					playQueue = new SinglePlayQueue(streamInfoItem);
					playerImpl.handleVideo(playQueue);
					selectAndLoadVideo(streamInfoItem.getServiceId(), streamInfoItem.getUrl(), streamInfoItem.getName());
					
					// show ad
					/*AppUtils.displayAds(activity, isShowAd -> {
						if (isShowAd) {
							showBannerAd();
							showNativeAd();
						}
						nativeAdView.setVisibility(isShowAd ? View.VISIBLE : View.GONE);
					});*/
				}
			}
			else if (v.getId() == btnFullScreen.getId()) {
				openNormalVideoPlayer();
			}
			else if (v.getId() == btnPopup.getId()) {
				AppInterstitialAd.getInstance().showInterstitialAd(() -> {
					playerImpl.onPause();
					playerImpl.onFullScreenButtonClicked();
				});
			}
			else if (v.getId() == btnShare.getId()) {
				if (currentInfo != null) {
					SharedUtils.shareUrl(activity, currentInfo.getName(), currentInfo.getUrl());
				}
			}
			else if (v.getId() == btnBack.getId()) {
				// clear back stack
				clearBackstack();
				// onBackPressed
				activity.onBackPressed();
			}
			
			if (getCurrentState() != STATE_COMPLETED) {
				getControlsVisibilityHandler().removeCallbacksAndMessages(null);
				AnimationUtils.animateView(getControlsRoot(), true, DEFAULT_CONTROLS_DURATION, 0, () -> {
					if (getCurrentState() == STATE_PLAYING && !isSomePopupMenuVisible()) {
						hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
					}
				});
			}
		}
		
		private void openNormalVideoPlayer() {
			
			setRecovery();
			Intent intent = NavigationHelper.getPlayerIntent(
					context,
					MainVideoPlayer.class,
					this.getPlayQueue(),
					this.getRepeatMode(),
					this.getPlaybackSpeed(),
					this.getPlaybackPitch(),
					this.getPlaybackSkipSilence(),
					this.getPlaybackQuality()
			);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
		
		@Override
		public void onFullScreenButtonClicked() {
			
			super.onPopupClicked();
			
			if (!PermissionHelper.isPopupEnabled(context)) {
				PermissionHelper.showPopupEnableToast(context);
				return;
			}
			
			setRecovery();
			final Intent intent = NavigationHelper.getPlayerIntent(
					context,
					PopupVideoPlayer.class,
					this.getPlayQueue(),
					this.getRepeatMode(),
					this.getPlaybackSpeed(),
					this.getPlaybackPitch(),
					this.getPlaybackSkipSilence(),
					this.getPlaybackQuality()
			);
			context.startService(intent);
			
			((View) getControlAnimationView().getParent()).setVisibility(View.GONE);
		}
		
		@Override
		protected void setupSubtitleView(@NonNull SubtitleView view, float captionScale, @NonNull CaptionStyleCompat captionStyle) {
		}
		
		@Override
		protected VideoPlaybackResolver.QualityResolver getQualityResolver() {
			
			return new VideoPlaybackResolver.QualityResolver() {
				
				@Override
				public int getDefaultResolutionIndex(List<VideoStream> sortedVideos) {
					
					return ListHelper.getDefaultResolutionIndex(context, sortedVideos);
				}
				
				@Override
				public int getOverrideResolutionIndex(List<VideoStream> sortedVideos, String playbackQuality) {
					
					return ListHelper.getResolutionIndex(context, sortedVideos, playbackQuality);
				}
			};
		}
		
		@Override
		public void onBlocked() {
			
			super.onBlocked();
			
			playPauseButton.setImageResource(R.drawable.ic_pause_white);
			animatePlayButtons(false, 100);
			getRootView().setKeepScreenOn(true);
		}
		
		@Override
		public void onBuffering() {
			
			super.onBuffering();
			
			getRootView().setKeepScreenOn(true);
		}
		
		@Override
		public void onPlaying() {
			
			super.onPlaying();
			
			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0, 0, () -> {
				playPauseButton.setImageResource(R.drawable.ic_pause_white);
				animatePlayButtons(true, 0);
			});
			
			getRootView().setKeepScreenOn(true);
		}
		
		@Override
		public void onPaused() {
			
			super.onPaused();
			
			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0, 0, () -> {
				playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
				animatePlayButtons(true, 0);
			});
			
			getRootView().setKeepScreenOn(false);
		}
		
		@Override
		public void onPausedSeek() {
			
			super.onPausedSeek();
			
			animatePlayButtons(false, 100);
			getRootView().setKeepScreenOn(true);
		}
		
		@Override
		public void onCompleted() {
			
			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0, 0, () -> {
				playPauseButton.setImageResource(R.drawable.ic_replay_white);
				animatePlayButtons(true, DEFAULT_CONTROLS_DURATION);
			});
			
			getRootView().setKeepScreenOn(false);
			
			super.onCompleted();
		}
		
		private void animatePlayButtons(final boolean show, final int duration) {
			
			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
			AnimationUtils.animateView(playPreviousButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
			AnimationUtils.animateView(playNextButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
		}
		
		public ImageButton getPlayPauseButton() {
			return playPauseButton;
		}
	}
	
	private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
		
		private boolean isMoving;
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			
			if (e.getX() > playerImpl.getRootView().getWidth() * 2 / 3) {
				playerImpl.onFastForward();
			}
			else if (e.getX() < playerImpl.getRootView().getWidth() / 3) {
				playerImpl.onFastRewind();
			}
			else {
				playerImpl.getPlayPauseButton().performClick();
			}
			
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			
			if (playerImpl.getCurrentState() == BasePlayer.STATE_BLOCKED) return true;
			
			if (playerImpl.isControlsVisible()) {
				playerImpl.hideControls(150, 0);
			}
			else {
				playerImpl.showControlsThenHide();
			}
			return true;
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			
			return super.onDown(e);
		}
		
		private void onScrollEnd() {
			
			if (playerImpl.isControlsVisible() && playerImpl.getCurrentState() == BasePlayer.STATE_PLAYING) {
				playerImpl.hideControls(VideoPlayer.DEFAULT_CONTROLS_DURATION, VideoPlayer.DEFAULT_CONTROLS_HIDE_TIME);
			}
		}
		
		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			gestureDetector.onTouchEvent(event);
			if (event.getAction() == MotionEvent.ACTION_UP && isMoving) {
				isMoving = false;
				onScrollEnd();
			}
			return true;
		}
	}
	
	/*private void showNativeAd() {
		
		// ad options
		VideoOptions videoOptions = new VideoOptions.Builder()
				.setStartMuted(true)
				.build();
		
		NativeAdOptions adOptions = new NativeAdOptions.Builder()
				.setVideoOptions(videoOptions)
				.build();
		// if fragment is attached to activity
		if (activity != null) {
			AdLoader adLoader = new AdLoader.Builder(activity, AdUtils.getNativeAdId(activity))
					.forUnifiedNativeAd(unifiedNativeAd -> {
						
						// show the ad
						NativeAdStyle styles = new NativeAdStyle.Builder().build();
						nativeAdView.setStyles(styles);
						nativeAdView.setNativeAd(unifiedNativeAd);
					})
					.withAdListener(new AdListener() {
						
						@Override
						public void onAdFailedToLoad(LoadAdError loadAdError) {
							// gone
							nativeAdView.setVisibility(View.GONE);
						}
						
						@Override
						public void onAdLoaded() {
							
							super.onAdLoaded();
							
							// visible
							nativeAdView.setVisibility(View.VISIBLE);
						}
					})
					.withNativeAdOptions(adOptions)
					.build();
			
			// loadAd
			AdRequest.Builder builder = new AdRequest.Builder();
			adLoader.loadAd(builder.build());
		}
	}
	
	private void showBannerAd() {
		AdRequest adRequest = new AdRequest.Builder().build();
		adView.setAdListener(new AdListener() {
			
			@Override
			public void onAdLoaded() {
				// Code to be executed when an ad finishes loading.
				adView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAdFailedToLoad(LoadAdError loadAdError) {
				// Code to be executed when an ad request fails.
				adView.setVisibility(View.GONE);
			}
		});
		adView.loadAd(adRequest);
	}*/
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == ReCaptchaActivity.RECAPTCHA_REQUEST && resultCode == Activity.RESULT_OK) {
			if (currentInfo != null) {
				StreamInfoItem streamInfoItem = new StreamInfoItem(currentInfo.getServiceId(), currentInfo.getUrl(), currentInfo.getName(), StreamType.VIDEO_STREAM);
				// need to set thumbnail url here
				streamInfoItem.setThumbnailUrl(currentInfo.getThumbnailUrl());
				streamInfoItem.setUploaderName(currentInfo.getUploaderName());
				NavigationHelper.openVideoDetailFragment(streamInfoItem, getFragmentManager(), streamInfoItem.getServiceId(), streamInfoItem.getUrl(), streamInfoItem.getName());
			}
		}
	}
}