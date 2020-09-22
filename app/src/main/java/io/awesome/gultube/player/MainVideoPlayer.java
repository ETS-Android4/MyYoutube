package io.awesome.gultube.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.material.button.MaterialButton;
import com.jakewharton.rxbinding2.view.RxView;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.ButterKnife;
import io.awesome.gultube.App;
import io.awesome.gultube.R;
import io.awesome.gultube.base.BaseActivity;
import io.awesome.gultube.database.subscription.SubscriptionEntity;
import io.awesome.gultube.local.subscription.SubscriptionService;
import io.awesome.gultube.player.helper.PlayerHelper;
import io.awesome.gultube.player.playqueue.SinglePlayQueue;
import io.awesome.gultube.player.relatedvideo.RelatedVideoAdapter;
import io.awesome.gultube.player.resolver.MediaSourceTag;
import io.awesome.gultube.player.resolver.VideoPlaybackResolver;
import io.awesome.gultube.util.AnimationUtils;
import io.awesome.gultube.util.ExtractorHelper;
import io.awesome.gultube.util.GlideUtils;
import io.awesome.gultube.util.ListHelper;
import io.awesome.gultube.util.Localization;
import io.awesome.gultube.util.NavigationHelper;
import io.awesome.gultube.util.PermissionHelper;
import io.awesome.gultube.util.StateSaver;
import io.awesome.gultube.util.ThemeHelper;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public final class MainVideoPlayer extends BaseActivity implements StateSaver.WriteRead {

	private GestureDetector gestureDetector;
	private VideoPlayerImpl playerImpl;
	private SharedPreferences defaultPreferences;

	@NonNull
	private CompositeDisposable disposables = new CompositeDisposable();
	private Disposable subscribeButtonMonitor;
	private SubscriptionService subscriptionService;

	@Nullable
	private PlayerState playerState;
	private boolean isInMultiWindow;

	@Override
	protected void attachBaseContext(Context context) {

		super.attachBaseContext(AudioServiceLeak.preventLeakOf(context));
		subscriptionService = SubscriptionService.getInstance(context);
	}

	// Activity LifeCycle
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {

		setTheme(ThemeHelper.getSettingsThemeStyle(this));

		super.onCreate(savedInstanceState);

		defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
		getWindow().setStatusBarColor(Color.BLACK);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		hideSystemUi();
		setContentView(R.layout.activity_main_player);
		ButterKnife.bind(this);

		playerImpl = new VideoPlayerImpl(this);
		playerImpl.setup(findViewById(android.R.id.content));

		if (savedInstanceState != null && savedInstanceState.get(StateSaver.KEY_SAVED_STATE) != null) {
			return; // We have saved states, stop here to restore it
		}

		final Intent intent = getIntent();
		if (intent != null) {
			playerImpl.handleIntent(intent);
		} else {
			Toast.makeText(this, R.string.general_error, Toast.LENGTH_SHORT).show();
			finish();
		}

	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle bundle) {

		super.onRestoreInstanceState(bundle);
		StateSaver.tryToRestore(bundle, this);
	}

	@Override
	protected void onNewIntent(Intent intent) {

		super.onNewIntent(intent);

		if (intent != null) {
			playerState = null;
			playerImpl.handleIntent(intent);
		}
	}

	@Override
	protected void onResume() {

		super.onResume();

		if (globalScreenOrientationLocked()) {
			boolean lastOrientationWasLandscape = defaultPreferences.getBoolean(getString(R.string.last_orientation_landscape_key), true);
			setLandscape(lastOrientationWasLandscape);
		}

		// Upon going in or out of multiwindow mode, isInMultiWindow will always be false,
		// since the first onResume needs to restore the player.
		// Subsequent onResume calls while multiwindow mode remains the same and the player is
		// prepared should be ignored.
		if (isInMultiWindow) return;
		isInMultiWindow = isInMultiWindow();

		if (playerImpl != null && playerState != null) {
			playerImpl.setPlaybackQuality(playerState.getPlaybackQuality());
			playerImpl.initPlayback(playerState.getPlayQueue(), playerState.getRepeatMode(),
					playerState.getPlaybackSpeed(), playerState.getPlaybackPitch(),
					playerState.isPlaybackSkipSilence(), playerState.wasPlaying());
		}
	}

	@Override
	public void onConfigurationChanged(@NotNull Configuration newConfig) {

		super.onConfigurationChanged(newConfig);

		if (playerImpl.isSomePopupMenuVisible()) {
			playerImpl.getQualityPopupMenu().dismiss();
			playerImpl.getPlaybackSpeedPopupMenu().dismiss();
		}
	}

	@Override
	public void onBackPressed() {

		super.onBackPressed();

		if (playerImpl != null) {
			playerImpl.onFullScreenButtonClicked();
		}
	}

	@Override
	protected void onSaveInstanceState(@NotNull Bundle outState) {

		super.onSaveInstanceState(outState);

		if (playerImpl == null) return;

		playerImpl.setRecovery();
		if (!playerImpl.gotDestroyed()) {
			playerState = new PlayerState(playerImpl.getPlayQueue(), playerImpl.getRepeatMode(),
					playerImpl.getPlaybackSpeed(), playerImpl.getPlaybackPitch(),
					playerImpl.getPlaybackQuality(), playerImpl.getPlaybackSkipSilence(),
					playerImpl.isPlaying());
		}
		StateSaver.tryToSave(isChangingConfigurations(), null, outState, this);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {

		super.onStop();

		if (playerImpl == null) return;

		playerImpl.destroy();
		isInMultiWindow = false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
		disposables.clear();
	}

	// State Saving
	@Override
	public String generateSuffix() {
		return "." + UUID.randomUUID().toString() + ".player";
	}

	@Override
	public void writeTo(Queue<Object> objectsToSave) {

		if (objectsToSave == null) return;
		objectsToSave.add(playerState);
	}

	@Override
	public void readFrom(@NonNull Queue<Object> savedObjects) {
		playerState = (PlayerState) savedObjects.poll();
	}

	// View
	private void showSystemUi() {

		if (playerImpl != null && playerImpl.queueVisible) return;

		final int visibility;
		visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

		@ColorInt final int systemUiColor = ActivityCompat.getColor(getApplicationContext(), R.color.video_overlay_color);
		getWindow().setStatusBarColor(systemUiColor);
		getWindow().setNavigationBarColor(systemUiColor);

		getWindow().getDecorView().setSystemUiVisibility(visibility);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	private void hideSystemUi() {

		int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		getWindow().getDecorView().setSystemUiVisibility(visibility);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	private void setLandscape(boolean landscape) {
		setRequestedOrientation(landscape ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
	}

	private boolean globalScreenOrientationLocked() {
		// 1: Screen orientation changes using acelerometer
		// 0: Screen orientatino is locked
		return !(Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
	}

	private boolean isInMultiWindow() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode();
	}

	private class VideoPlayerImpl extends VideoPlayer implements RelatedVideoAdapter.Listener {

		private TextView titleTextView;
		private ImageButton queueButton;

		private ImageButton playPauseButton;
		private ImageButton playPreviousButton;
		private ImageButton playNextButton;

		private ConstraintLayout queueLayout;
		private ImageButton itemsListCloseButton;
		private RecyclerView itemsList;

		private boolean queueVisible;

		private ImageButton switchPopupButton;
		private ImageButton exitFullScreen;

		private TextView titleTextView2;
		private TextView uploaderTextView;
		private ImageView uploaderThumb;
		private TextView uploaderSubscriberTextView;
		private MaterialButton channelSubscribeButton;


		private RelatedVideoAdapter adapter;

		VideoPlayerImpl(final Context context) {
			super("VideoPlayer", context);
		}

		@Override
		public void initViews(View rootView) {

			super.initViews(rootView);

			this.titleTextView = rootView.findViewById(R.id.titleTextView);
			this.queueButton = rootView.findViewById(R.id.queueButton);

			this.playPauseButton = rootView.findViewById(R.id.playPauseButton);
			this.playPreviousButton = rootView.findViewById(R.id.playPreviousButton);
			this.playNextButton = rootView.findViewById(R.id.playNextButton);

			this.switchPopupButton = rootView.findViewById(R.id.switchPopup);
			this.exitFullScreen = rootView.findViewById(R.id.exitFullScreen);

			this.queueLayout = findViewById(R.id.playQueuePanel);
			this.itemsListCloseButton = findViewById(R.id.playQueueClose);
			this.itemsList = findViewById(R.id.relatedRecyclerView);

			titleTextView2 = rootView.findViewById(R.id.titleTextView2);
			uploaderTextView = rootView.findViewById(R.id.detail_uploader_text_view);
			uploaderThumb = rootView.findViewById(R.id.detail_uploader_thumbnail_view);
			uploaderSubscriberTextView = rootView.findViewById(R.id.detail_uploader_subscriber_text_view);
			channelSubscribeButton = rootView.findViewById(R.id.channel_subscribe_button);

			titleTextView.setSelected(true);
			getRootView().setKeepScreenOn(true);
		}

		@Override
		protected void setupSubtitleView(@NonNull SubtitleView view, final float captionScale, @NonNull final CaptionStyleCompat captionStyle) {

			final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
			final int minimumLength = Math.min(metrics.heightPixels, metrics.widthPixels);
			final float captionRatioInverse = 20f + 4f * (1f - captionScale);
			view.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, (float) minimumLength / captionRatioInverse);
			view.setApplyEmbeddedStyles(captionStyle.equals(CaptionStyleCompat.DEFAULT));
			view.setStyle(captionStyle);
		}

		@Override
		public void initListeners() {

			super.initListeners();

			MySimpleOnGestureListener listener = new MySimpleOnGestureListener();
			gestureDetector = new GestureDetector(context, listener);
			gestureDetector.setIsLongpressEnabled(false);
			getRootView().setOnTouchListener(listener);

			queueButton.setOnClickListener(this);

			playPauseButton.setOnClickListener(this);
			playPreviousButton.setOnClickListener(this);
			playNextButton.setOnClickListener(this);

			switchPopupButton.setOnClickListener(this);
			exitFullScreen.setOnClickListener(this);
		}

		public void minimize() {

			switch (PlayerHelper.getMinimizeOnExitAction(context)) {

				case PlayerHelper.MinimizeMode.MINIMIZE_ON_EXIT_MODE_POPUP:
					onFullScreenButtonClicked();
					break;

				case PlayerHelper.MinimizeMode.MINIMIZE_ON_EXIT_MODE_NONE:
				default:
					break;
			}
		}

		// ExoPlayer Video Listener
		@Override
		public void onRepeatModeChanged(int i) {
			super.onRepeatModeChanged(i);
		}

		@Override
		public void onShuffleClicked() {
			super.onShuffleClicked();
		}

		// Playback Listener
		protected void onMetadataChanged(@NonNull final MediaSourceTag tag) {

			super.onMetadataChanged(tag);
			titleTextView.setText(tag.getMetadata().getName());
		}

		@Override
		public void onPlaybackShutdown() {

			super.onPlaybackShutdown();
			finish();
		}

		// Player Overrides
		@Override
		public void onFullScreenButtonClicked() {

			super.onFullScreenButtonClicked();

			if (simpleExoPlayer == null) return;

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
			destroy();
			finish();
		}

		@Override
		public void onClick(View v) {

			super.onClick(v);
			if (v.getId() == playPauseButton.getId()) {
				onPlayPause();

			} else if (v.getId() == playPreviousButton.getId()) {
				onPlayPrevious();

			} else if (v.getId() == playNextButton.getId()) {
				onPlayNext();

			} else if (v.getId() == queueButton.getId()) {
				onQueueClicked();
				return;
			} else if (v.getId() == switchPopupButton.getId()) {
				onBackPressed();
			} else if (v.getId() == exitFullScreen.getId()) {
				onBackPressed();
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

		private void onQueueClicked() {

			queueVisible = true;
			hideSystemUi();

			if (getCurrentMetadata() != null) {
				initRelatedVideos(getCurrentMetadata().getMetadata());
				initChannelInfo(getCurrentMetadata().getMetadata());
			}


			getControlsRoot().setVisibility(View.INVISIBLE);
			AnimationUtils.animateView(queueLayout, AnimationUtils.Type.SLIDE_AND_ALPHA, true, DEFAULT_CONTROLS_DURATION);

			itemsList.scrollToPosition(playQueue.getIndex());
		}

		private void onQueueClosed() {

			AnimationUtils.animateView(queueLayout, AnimationUtils.Type.SLIDE_AND_ALPHA, false, DEFAULT_CONTROLS_DURATION);
			queueVisible = false;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

			super.onStopTrackingTouch(seekBar);
			if (wasPlaying()) showControlsThenHide();
		}

		@Override
		public void onDismiss(PopupMenu menu) {

			super.onDismiss(menu);
			if (isPlaying()) hideControls(DEFAULT_CONTROLS_DURATION, 0);
			hideSystemUi();
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

		// States
		private void animatePlayButtons(final boolean show, final int duration) {

			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
			AnimationUtils.animateView(playPreviousButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
			AnimationUtils.animateView(playNextButton, AnimationUtils.Type.SCALE_AND_ALPHA, show, duration);
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

			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 80, 0, () -> {
				playPauseButton.setImageResource(R.drawable.ic_pause_white);
				animatePlayButtons(true, 200);
			});

			getRootView().setKeepScreenOn(true);
		}

		@Override
		public void onPaused() {

			super.onPaused();

			AnimationUtils.animateView(playPauseButton, AnimationUtils.Type.SCALE_AND_ALPHA, false, 80, 0, () -> {
				playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
				animatePlayButtons(true, 200);
			});

			showSystemUi();
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

		// Utils
		@Override
		public void showControlsThenHide() {

			if (queueVisible) return;

			super.showControlsThenHide();
		}

		@Override
		public void showControls(long duration) {

			if (queueVisible) return;

			super.showControls(duration);
		}

		@Override
		public void hideControls(final long duration, long delay) {

			getControlsVisibilityHandler().removeCallbacksAndMessages(null);
			getControlsVisibilityHandler().postDelayed(() -> AnimationUtils.animateView(getControlsRoot(), false, duration, 0, MainVideoPlayer.this::hideSystemUi), delay);
		}

		private void initRelatedVideos(StreamInfo streamInfo) {
			adapter = new RelatedVideoAdapter(this);
			itemsList.setLayoutManager(new LinearLayoutManager(MainVideoPlayer.this, LinearLayoutManager.HORIZONTAL, false));

			if (streamInfo != null && streamInfo.getRelatedStreams() != null && !streamInfo.getRelatedStreams().isEmpty()) {
				// set next video
				streamInfo.setVideoStreams(streamInfo.getVideoStreams());

				// enable queue button
				queueButton.setEnabled(true);
				adapter.setItems(streamInfo.getRelatedStreams());
				itemsList.setAdapter(adapter);
			} else {
				// disable queue button
				queueButton.setEnabled(false);
			}

			// queue closed listener
			itemsListCloseButton.setOnClickListener(view -> onQueueClosed());
		}

		public ImageButton getPlayPauseButton() {
			return playPauseButton;
		}

		@Override
		public void onVideoClicked(int position) {
			// dismiss related videos
			onQueueClosed();

			InfoItem infoItem = adapter.getItem(position);
			int serviceId = infoItem.getServiceId();
			String url = infoItem.getUrl();
			String name = infoItem.getName();
			StreamType streamType = StreamType.VIDEO_STREAM;
			StreamInfoItem streamInfoItem = new StreamInfoItem(serviceId, url, name, streamType);
			NavigationHelper.playOnMainPlayer(MainVideoPlayer.this, new SinglePlayQueue(streamInfoItem));
		}

		@SuppressLint("CheckResult")
		private void initChannelInfo(StreamInfo streamInfo) {

			titleTextView2.setText(streamInfo.getName());
			uploaderTextView.setText(streamInfo.getUploaderName());
			if (!TextUtils.isEmpty(streamInfo.getUploaderAvatarUrl())) {
				GlideUtils.loadAvatar(App.applicationContext, uploaderThumb, streamInfo.getUploaderAvatarUrl().replace("s48", "s720"));
			}

			// get channel's subscribers
			if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
			ExtractorHelper.getChannelInfo(streamInfo.getServiceId(), streamInfo.getUploaderUrl(), true)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread()).subscribe(
					// onNext
					channelInfo -> {
						uploaderSubscriberTextView.setText(Localization.localizeSubscribersCount(context, channelInfo.getSubscriberCount()));
						updateSubscription(channelInfo);
						monitorSubscription(channelInfo);
					},
					// onError
					throwable -> uploaderSubscriberTextView.setText(R.string.unknown_content));
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
				} else {
					final SubscriptionEntity subscription = subscriptionEntities.get(0);
					subscribeButtonMonitor = monitorSubscribeButton(channelSubscribeButton, mapOnUnsubscribe(subscription));
				}
			};
		}

		private void updateSubscribeButton(boolean isSubscribed) {

			boolean isButtonVisible = channelSubscribeButton.getVisibility() == View.VISIBLE;
			int backgroundDuration = isButtonVisible ? 100 : 0;
			int textDuration = isButtonVisible ? 100 : 0;

			int subscribeBackground = ContextCompat.getColor(context, R.color.subscribe_background_color);
			int subscribeText = ContextCompat.getColor(context, R.color.subscribe_text_color);
			int subscribedBackground = ContextCompat.getColor(context, R.color.subscribed_background_color);
			int subscribedText = ContextCompat.getColor(context, R.color.subscribed_text_color);

			if (!isSubscribed) {
				channelSubscribeButton.setText(R.string.subscribe_button_title);
				AnimationUtils.animateBackgroundColor(channelSubscribeButton, backgroundDuration, subscribedBackground, subscribeBackground);
				AnimationUtils.animateTextColor(channelSubscribeButton, textDuration, subscribedText, subscribeText);
			} else {
				channelSubscribeButton.setText(R.string.subscribed_button_title);
				AnimationUtils.animateBackgroundColor(channelSubscribeButton, backgroundDuration, subscribeBackground, subscribedBackground);
				AnimationUtils.animateTextColor(channelSubscribeButton, textDuration, subscribeText, subscribedText);
			}

			AnimationUtils.animateView(channelSubscribeButton, AnimationUtils.Type.LIGHT_SCALE_AND_ALPHA, true, 100);
		}


		private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {

			private boolean isMoving;

			@Override
			public boolean onDoubleTap(MotionEvent e) {

				if (e.getX() > playerImpl.getRootView().getWidth() * 2 / 3) {
					playerImpl.onFastForward();
				} else if (e.getX() < playerImpl.getRootView().getWidth() / 3) {
					playerImpl.onFastRewind();
				} else {
					playerImpl.getPlayPauseButton().performClick();
				}

				return true;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {

				if (playerImpl.getCurrentState() == BasePlayer.STATE_BLOCKED) return true;

				if (playerImpl.isControlsVisible()) {
					playerImpl.hideControls(150, 0);
				} else {
					playerImpl.showControlsThenHide();
					showSystemUi();
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
	}
}
	


