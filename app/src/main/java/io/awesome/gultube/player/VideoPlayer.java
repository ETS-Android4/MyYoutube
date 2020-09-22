package io.awesome.gultube.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.video.VideoListener;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import io.awesome.gultube.R;
import io.awesome.gultube.player.helper.PlayerHelper;
import io.awesome.gultube.player.playqueue.PlayQueueItem;
import io.awesome.gultube.player.resolver.MediaSourceTag;
import io.awesome.gultube.player.resolver.VideoPlaybackResolver;
import io.awesome.gultube.util.AnimationUtils;
import io.awesome.gultube.util.Constants;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class VideoPlayer extends BasePlayer implements VideoListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener, Player.EventListener, PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener {
	
	// Player
	protected static final int RENDERER_UNAVAILABLE = -1;
	public static final int DEFAULT_CONTROLS_DURATION = 500; // 500 millis
	public static final int DEFAULT_CONTROLS_HIDE_TIME = 3000;  // 3 seconds
	
	private List<VideoStream> availableStreams;
	private int selectedStreamIndex;
	
	protected boolean wasPlaying = false;
	
	@NonNull final private VideoPlaybackResolver resolver;
	
	// Views
	private View rootView;
	
	private AspectRatioFrameLayout aspectRatioFrameLayout;
	private SurfaceView surfaceView;
	private View surfaceForeground;
	
	private View loadingPanel;
	private ImageView endScreen;
	private ImageView controlAnimationView;
	
	private View controlsRoot;
	private TextView currentDisplaySeek;
	
	private View bottomControlsRoot;
	private SeekBar playbackSeekBar;
	private TextView playbackTime;
	private TextView playbackLiveSync;
	private TextView playbackSpeedTextView;
	
	private View topControlsRoot;
	private TextView qualityTextView;
	
	private SubtitleView subtitleView;
	
	private TextView captionTextView;
	
	private ValueAnimator controlViewAnimator;
	private Handler controlsVisibilityHandler = new Handler();
	
	boolean isSomePopupMenuVisible = false;
	private final int qualityPopupMenuGroupId = 69;
	private PopupMenu qualityPopupMenu;
	
	private final int playbackSpeedPopupMenuGroupId = 79;
	private PopupMenu playbackSpeedPopupMenu;
	
	private final int captionPopupMenuGroupId = 89;
	private PopupMenu captionPopupMenu;
	
	private String playbackCurrentTimeValue = Constants.PLAYBACK_TIME_DEFAULT;
	private String playbackEndTimeValue = Constants.PLAYBACK_TIME_DEFAULT;
	
	public VideoPlayer(String debugTag, Context context) {
		
		super(context);
		this.resolver = new VideoPlaybackResolver(context, dataSource, getQualityResolver());
	}
	
	public void setup(View rootView) {
		
		initViews(rootView);
		setup();
	}
	
	public void initViews(View rootView) {
		
		this.rootView = rootView;
		this.aspectRatioFrameLayout = rootView.findViewById(R.id.aspectRatioLayout);
		this.surfaceView = rootView.findViewById(R.id.surfaceView);
		this.surfaceForeground = rootView.findViewById(R.id.surfaceForeground);
		this.loadingPanel = rootView.findViewById(R.id.loading_panel);
		this.endScreen = rootView.findViewById(R.id.endScreen);
		this.controlAnimationView = rootView.findViewById(R.id.controlAnimationView);
		this.controlsRoot = rootView.findViewById(R.id.playbackControlRoot);
		this.currentDisplaySeek = rootView.findViewById(R.id.currentDisplaySeek);
		this.playbackSeekBar = rootView.findViewById(R.id.playbackSeekBar);
		this.playbackTime = rootView.findViewById(R.id.playbackTime);
		this.playbackLiveSync = rootView.findViewById(R.id.playbackLiveSync);
		this.playbackSpeedTextView = rootView.findViewById(R.id.playbackSpeedTextView);
		this.bottomControlsRoot = rootView.findViewById(R.id.bottomControls);
		this.topControlsRoot = rootView.findViewById(R.id.topControls);
		this.qualityTextView = rootView.findViewById(R.id.qualityTextView);
		
		this.subtitleView = rootView.findViewById(R.id.subtitleView);
		
		final float captionScale = PlayerHelper.getCaptionScale(context);
		final CaptionStyleCompat captionStyle = PlayerHelper.getCaptionStyle(context);
		setupSubtitleView(subtitleView, captionScale, captionStyle);
		
		this.captionTextView = rootView.findViewById(R.id.captionTextView);
		this.playbackSpeedPopupMenu = new PopupMenu(context, playbackSpeedTextView);
		this.qualityPopupMenu = new PopupMenu(context, qualityTextView);
		this.captionPopupMenu = new PopupMenu(context, captionTextView);
	}
	
	protected abstract void setupSubtitleView(@NonNull SubtitleView view, final float captionScale, @NonNull final CaptionStyleCompat captionStyle);
	
	@Override
	public void initListeners() {
		
		super.initListeners();
		
		playbackSeekBar.setOnSeekBarChangeListener(this);
		qualityTextView.setOnClickListener(this);
		captionTextView.setOnClickListener(this);
		playbackLiveSync.setOnClickListener(this);
		playbackSpeedTextView.setOnClickListener(this);
	}
	
	@Override
	public void initPlayer(final boolean playOnReady) {
		
		super.initPlayer(playOnReady);
		
		// Setup video view
		simpleExoPlayer.setVideoSurfaceView(surfaceView);
		simpleExoPlayer.addVideoListener(this);
		
		// Setup subtitle view
		simpleExoPlayer.addTextOutput(cues -> subtitleView.onCues(cues));
		
		// Setup audio session with onboard equalizer
		trackSelector.setParameters(trackSelector.buildUponParameters().setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context)));
	}
	
	@Override
	public void handleIntent(final Intent intent) {
		
		if (intent == null) return;
		
		if (intent.hasExtra(PLAYBACK_QUALITY)) {
			setPlaybackQuality(intent.getStringExtra(PLAYBACK_QUALITY));
		}
		
		super.handleIntent(intent);
	}
	
	// UI Builders
	public void buildQualityMenu() {
		
		if (qualityPopupMenu == null) return;
		
		qualityPopupMenu.getMenu().removeGroup(qualityPopupMenuGroupId);
		for (int i = 0; i < availableStreams.size(); i++) {
			VideoStream videoStream = availableStreams.get(i);
			qualityPopupMenu.getMenu().add(qualityPopupMenuGroupId, i, Menu.NONE, videoStream.resolution);
		}
		if (getSelectedVideoStream() != null) {
			qualityTextView.setText(getSelectedVideoStream().resolution);
		}
		qualityPopupMenu.setOnMenuItemClickListener(this);
		qualityPopupMenu.setOnDismissListener(this);
	}
	
	private void buildPlaybackSpeedMenu() {
		if (playbackSpeedPopupMenu == null) return;
		playbackSpeedPopupMenu.getMenu().removeGroup(playbackSpeedPopupMenuGroupId);
		for (int i = 0; i < PLAYBACK_SPEEDS.length; i++) {
			String title = PlayerHelper.formatSpeed(PLAYBACK_SPEEDS[i]);
			playbackSpeedPopupMenu.getMenu().add(playbackSpeedPopupMenuGroupId, i, Menu.NONE, TextUtils.equals(title, "1x") ? rootView.getContext().getString(R.string.playback_speed_normal) : title);
		}
		playbackSpeedTextView.setText(PlayerHelper.formatSpeed(getPlaybackSpeed()));
		playbackSpeedPopupMenu.setOnMenuItemClickListener(this);
		playbackSpeedPopupMenu.setOnDismissListener(this);
	}
	
	private void buildCaptionMenu(final List<String> availableLanguages) {
		
		if (captionPopupMenu == null) return;
		int captionPopupMenuGroupId = 89;
		captionPopupMenu.getMenu().removeGroup(captionPopupMenuGroupId);
		
		// Add option for turning off caption
		MenuItem captionOffItem = captionPopupMenu.getMenu().add(captionPopupMenuGroupId, 0, Menu.NONE, R.string.caption_none);
		captionOffItem.setOnMenuItemClickListener(menuItem -> {
			
			final int textRendererIndex = getRendererIndex(C.TRACK_TYPE_TEXT);
			if (textRendererIndex != RENDERER_UNAVAILABLE) {
				trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(textRendererIndex, true));
			}
			return true;
		});
		
		// Add all available captions
		for (int i = 0; i < availableLanguages.size(); i++) {
			
			final String captionLanguage = availableLanguages.get(i);
			if (TextUtils.isEmpty(captionLanguage)) return;
			
			MenuItem captionItem = captionPopupMenu.getMenu().add(captionPopupMenuGroupId, i + 1, Menu.NONE, captionLanguage);
			
			captionItem.setOnMenuItemClickListener(menuItem -> {
				
				final int textRendererIndex = getRendererIndex(C.TRACK_TYPE_TEXT);
				if (textRendererIndex != RENDERER_UNAVAILABLE) {
					trackSelector.setPreferredTextLanguage(captionLanguage);
					trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(textRendererIndex, false));
				}
				return true;
			});
		}
		captionPopupMenu.setOnDismissListener(this);
	}
	
	private void updateStreamRelatedViews() {
		
		if (getCurrentMetadata() == null) return;
		
		final MediaSourceTag tag = getCurrentMetadata();
		final StreamInfo metadata = tag.getMetadata();
		
		qualityTextView.setVisibility(View.GONE);
		playbackTime.setVisibility(View.GONE);
		playbackLiveSync.setVisibility(View.GONE);
		playbackSpeedTextView.setVisibility(View.GONE);
		
		switch (metadata.getStreamType()) {
			
			case AUDIO_STREAM:
				surfaceView.setVisibility(View.GONE);
				endScreen.setVisibility(View.VISIBLE);
				playbackTime.setVisibility(View.VISIBLE);
				break;
			
			case AUDIO_LIVE_STREAM:
				surfaceView.setVisibility(View.GONE);
				endScreen.setVisibility(View.VISIBLE);
				playbackTime.setVisibility(View.VISIBLE);
				playbackLiveSync.setVisibility(View.VISIBLE);
				break;
			
			case LIVE_STREAM:
				surfaceView.setVisibility(View.VISIBLE);
				endScreen.setVisibility(View.GONE);
				playbackTime.setVisibility(View.VISIBLE);
				playbackLiveSync.setVisibility(View.VISIBLE);
				break;
			
			case VIDEO_STREAM:
				if (metadata.getVideoStreams().size() + metadata.getVideoOnlyStreams().size() == 0)
					break;
				
				availableStreams = tag.getSortedAvailableVideoStreams();
				selectedStreamIndex = tag.getSelectedVideoStreamIndex();
				buildQualityMenu();
				
				qualityTextView.setVisibility(View.VISIBLE);
				surfaceView.setVisibility(View.VISIBLE);
				playbackTime.setVisibility(View.VISIBLE);
			default:
				endScreen.setVisibility(View.GONE);
				playbackTime.setVisibility(View.VISIBLE);
				break;
		}
		
		// build playback speed menu
		buildPlaybackSpeedMenu();
		playbackSpeedTextView.setVisibility(View.VISIBLE);
	}
	
	// Playback Listener
	protected abstract VideoPlaybackResolver.QualityResolver getQualityResolver();
	
	protected void onMetadataChanged(@NonNull final MediaSourceTag tag) {
		
		super.onMetadataChanged(tag);
		
		updateStreamRelatedViews();
	}
	
	@Override
	@Nullable
	public MediaSource sourceOf(final PlayQueueItem item, final StreamInfo info) {
		return resolver.resolve(info);
	}
	
	// States Implementation
	@Override
	public void onBlocked() {
		
		super.onBlocked();
		
		controlsVisibilityHandler.removeCallbacksAndMessages(null);
		AnimationUtils.animateView(controlsRoot, false, DEFAULT_CONTROLS_DURATION);
		playbackSeekBar.setEnabled(true);
		
		loadingPanel.setBackgroundColor(Color.BLACK);
		AnimationUtils.animateView(loadingPanel, true, 0);
		AnimationUtils.animateView(surfaceForeground, true, 100);
	}
	
	@Override
	public void onPlaying() {
		
		super.onPlaying();
		
		updateStreamRelatedViews();
		showAndAnimateControl(-1, true);
		playbackSeekBar.setEnabled(true);
		
		loadingPanel.setVisibility(View.GONE);
		AnimationUtils.animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 200);
	}
	
	@Override
	public void onBuffering() {
		
		loadingPanel.setBackgroundColor(Color.TRANSPARENT);
	}
	
	@Override
	public void onPaused() {
		
		showControls(200);
		loadingPanel.setVisibility(View.GONE);
	}
	
	@Override
	public void onPausedSeek() {
		
		showAndAnimateControl(-1, true);
	}
	
	@Override
	public void onCompleted() {
		
		super.onCompleted();
		
		showControls(250);
		AnimationUtils.animateView(endScreen, true, 250);
		AnimationUtils.animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 200);
		loadingPanel.setVisibility(View.GONE);
		
		AnimationUtils.animateView(surfaceForeground, true, 100);
	}
	
	// ExoPlayer Video Listener
	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		
		super.onTracksChanged(trackGroups, trackSelections);
		
		onTextTrackUpdate();
	}
	
	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		super.onPlaybackParametersChanged(playbackParameters);
		playbackSpeedTextView.setText(PlayerHelper.formatSpeed(playbackParameters.speed));
	}
	
	@Override
	public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
		
		aspectRatioFrameLayout.setAspectRatio(((float) width) / height);
	}
	
	@Override
	public void onRenderedFirstFrame() {
		AnimationUtils.animateView(surfaceForeground, false, 100);
	}
	
	// ExoPlayer Track Updates
	private void onTextTrackUpdate() {
		
		final int textRenderer = getRendererIndex(C.TRACK_TYPE_TEXT);
		
		if (captionTextView == null) return;
		if (trackSelector.getCurrentMappedTrackInfo() == null || textRenderer == RENDERER_UNAVAILABLE) {
			captionTextView.setVisibility(View.GONE);
			return;
		}
		
		final TrackGroupArray textTracks = trackSelector.getCurrentMappedTrackInfo().getTrackGroups(textRenderer);
		
		// Extract all loaded languages
		List<String> availableLanguages = new ArrayList<>(textTracks.length);
		for (int i = 0; i < textTracks.length; i++) {
			final TrackGroup textTrack = textTracks.get(i);
			if (textTrack.length > 0 && textTrack.getFormat(0) != null) {
				availableLanguages.add(textTrack.getFormat(0).language);
			}
		}
		
		// Normalize mismatching language strings
		final String preferredLanguage = trackSelector.getPreferredTextLanguage();
		
		// Build UI
		buildCaptionMenu(availableLanguages);
		if (trackSelector.getParameters().getRendererDisabled(textRenderer) ||
				preferredLanguage == null || (!availableLanguages.contains(preferredLanguage)
				&& !containsCaseInsensitive(availableLanguages, preferredLanguage))) {
			captionTextView.setText(R.string.caption_none);
		}
		else {
			captionTextView.setText(preferredLanguage);
		}
		captionTextView.setVisibility(availableLanguages.isEmpty() ? View.GONE : View.VISIBLE);
	}
	
	// workaround to match normalized captions like english to English or deutsch to Deutsch
	private static boolean containsCaseInsensitive(List<String> list, String toFind) {
		
		for (String s : list) {
			if (s.equalsIgnoreCase(toFind))
				return true;
		}
		return false;
	}
	
	// General Player
	@Override
	public void onPrepared(boolean playWhenReady) {
		
		if (simpleExoPlayer != null) {
			playbackSeekBar.setMax((int) simpleExoPlayer.getDuration());
			playbackEndTimeValue = PlayerHelper.getTimeString((int) simpleExoPlayer.getDuration());
			playbackSpeedTextView.setText(PlayerHelper.formatSpeed(getPlaybackSpeed()));
		}
		
		super.onPrepared(playWhenReady);
	}
	
	@Override
	public void destroy() {
		
		super.destroy();
		
		if (endScreen != null) endScreen.setImageBitmap(null);
	}
	
	@Override
	public void onUpdateProgress(int currentProgress, int duration, int bufferPercent) {
		
		if (!isPrepared()) return;
		
		if (duration != playbackSeekBar.getMax()) {
			playbackEndTimeValue = PlayerHelper.getTimeString(duration);
			playbackSeekBar.setMax(duration);
		}
		
		if (currentState != STATE_PAUSED && currentState != STATE_PAUSED_SEEK) {
			playbackSeekBar.setProgress(currentProgress);
		}
		
		// update playback time
		playbackCurrentTimeValue = PlayerHelper.getTimeString(currentProgress);
		// isLive = 00:00 • Live else 00:00 / 00:00
		playbackTime.setText(isLive() ? playbackCurrentTimeValue : (playbackCurrentTimeValue + " / " + playbackEndTimeValue));
		
		if (simpleExoPlayer != null) {
			if (simpleExoPlayer.isLoading() || bufferPercent > 90) {
				playbackSeekBar.setSecondaryProgress((int) (playbackSeekBar.getMax() * ((float) bufferPercent / 100)));
			}
		}
		
		playbackLiveSync.setClickable(!isLiveEdge());
	}
	
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		
		super.onLoadingComplete(imageUri, view, loadedImage);
		
		if (loadedImage != null) endScreen.setImageBitmap(loadedImage);
	}
	
	protected void onFullScreenButtonClicked() {
		changeState(STATE_BLOCKED);
	}
	
	protected void onPopupClicked() {
		changeState(STATE_PAUSED);
	}
	
	@Override
	public void onFastRewind() {
		
		super.onFastRewind();
		
		showAndAnimateControl(R.drawable.ic_fast_rewind, true);
	}
	
	@Override
	public void onFastForward() {
		
		super.onFastForward();
		
		showAndAnimateControl(R.drawable.ic_fast_forward, true);
	}
	
	// OnClick related
	@Override
	public void onClick(View v) {
		
		if (v.getId() == qualityTextView.getId()) {
			onQualitySelectorClicked();
		}
		else if (v.getId() == captionTextView.getId()) {
			onCaptionClicked();
		}
		else if (v.getId() == playbackLiveSync.getId()) {
			seekToDefault();
		}
		else if (v.getId() == playbackSpeedTextView.getId()) {
			onPlaybackSpeedClicked();
		}
	}
	
	/**
	 * Called when an item of the quality selector or the playback speed selector is selected
	 */
	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		
		if (qualityPopupMenuGroupId == menuItem.getGroupId()) {
			final int menuItemIndex = menuItem.getItemId();
			if (selectedStreamIndex == menuItemIndex || availableStreams == null || availableStreams.size() <= menuItemIndex) return true;
			
			final String newResolution = availableStreams.get(menuItemIndex).resolution;
			setRecovery();
			setPlaybackQuality(newResolution);
			reload();
			
			qualityTextView.setText(menuItem.getTitle());
			return true;
		}
		else if (playbackSpeedPopupMenuGroupId == menuItem.getGroupId()) {
			int speedIndex = menuItem.getItemId();
			float speed = PLAYBACK_SPEEDS[speedIndex];
			setPlaybackSpeed(speed);
			playbackSpeedTextView.setText(PlayerHelper.formatSpeed(speed));
		}
		return false;
	}
	
	public void changeVideoQuality(String newQuality) {
		setRecovery();
		setPlaybackQuality(newQuality);
		reload();
	}
	
	/**
	 * Called when some popup menu is dismissed
	 */
	@Override
	public void onDismiss(PopupMenu menu) {
		
		isSomePopupMenuVisible = false;
		if (getSelectedVideoStream() != null) {
			qualityTextView.setText(getSelectedVideoStream().resolution);
		}
	}
	
	public void onQualitySelectorClicked() {
		
		qualityPopupMenu.show();
		isSomePopupMenuVisible = true;
		showControls(DEFAULT_CONTROLS_DURATION);
		
		final VideoStream videoStream = getSelectedVideoStream();
		if (videoStream != null) {
			final String qualityText = videoStream.resolution;
			qualityTextView.setText(qualityText);
		}
		
		if (simpleExoPlayer != null) {
			wasPlaying = simpleExoPlayer.getPlayWhenReady();
		}
	}
	
	public void onPlaybackSpeedClicked() {
		
		playbackSpeedPopupMenu.show();
		isSomePopupMenuVisible = true;
		showControls(DEFAULT_CONTROLS_DURATION);
	}
	
	private void onCaptionClicked() {
		
		captionPopupMenu.show();
		isSomePopupMenuVisible = true;
		showControls(DEFAULT_CONTROLS_DURATION);
	}
	
	// SeekBar Listener
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		
		if (fromUser) currentDisplaySeek.setText(PlayerHelper.getTimeString(progress));
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
		if (getCurrentState() != STATE_PAUSED_SEEK) changeState(STATE_PAUSED_SEEK);
		
		if (simpleExoPlayer != null) {
			wasPlaying = simpleExoPlayer.getPlayWhenReady();
			if (isPlaying()) simpleExoPlayer.setPlayWhenReady(false);
		}
		
		showControls(0);
		AnimationUtils.animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, true, DEFAULT_CONTROLS_DURATION);
	}
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
		seekTo(seekBar.getProgress());
		
		if (simpleExoPlayer != null) {
			if (wasPlaying || simpleExoPlayer.getDuration() == seekBar.getProgress()) simpleExoPlayer.setPlayWhenReady(true);
		}
		playbackCurrentTimeValue = PlayerHelper.getTimeString(seekBar.getProgress());
		playbackTime.setText(isLive() ? playbackCurrentTimeValue : (playbackCurrentTimeValue + " / " + playbackEndTimeValue));
		AnimationUtils.animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0);
		
		if (getCurrentState() == STATE_PAUSED_SEEK) changeState(STATE_BUFFERING);
		if (!isProgressLoopRunning()) startProgressLoop();
	}
	
	// Utils
	public int getRendererIndex(final int trackIndex) {
		
		if (simpleExoPlayer == null) return RENDERER_UNAVAILABLE;
		
		for (int t = 0; t < simpleExoPlayer.getRendererCount(); t++) {
			if (simpleExoPlayer.getRendererType(t) == trackIndex) {
				return t;
			}
		}
		
		return RENDERER_UNAVAILABLE;
	}
	
	public boolean isControlsVisible() {
		return controlsRoot != null && controlsRoot.getVisibility() == View.VISIBLE;
	}
	
	/**
	 * Show a animation, and depending on goneOnEnd, will stay on the screen or be gone
	 *
	 * @param drawableId the drawable that will be used to animate, pass -1 to clear any animation that is visible
	 * @param goneOnEnd  will set the animation view to GONE on the end of the animation
	 */
	public void showAndAnimateControl(final int drawableId, final boolean goneOnEnd) {
		
		if (controlViewAnimator != null && controlViewAnimator.isRunning()) {
			controlViewAnimator.end();
		}
		
		if (drawableId == -1) {
			if (controlAnimationView.getVisibility() == View.VISIBLE) {
				controlViewAnimator = ObjectAnimator.ofPropertyValuesHolder(controlAnimationView,
																			PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
																			PropertyValuesHolder.ofFloat(View.SCALE_X, 1.4f, 1f),
																			PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.4f, 1f)).setDuration(DEFAULT_CONTROLS_DURATION);
				controlViewAnimator.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						controlAnimationView.setVisibility(View.GONE);
					}
				});
				controlViewAnimator.start();
			}
			return;
		}
		
		float scaleFrom = goneOnEnd ? 1f : 1f, scaleTo = goneOnEnd ? 1.8f : 1.4f;
		float alphaFrom = goneOnEnd ? 1f : 0f, alphaTo = goneOnEnd ? 0f : 1f;
		
		controlViewAnimator = ObjectAnimator.ofPropertyValuesHolder(controlAnimationView,
																	PropertyValuesHolder.ofFloat(View.ALPHA, alphaFrom, alphaTo),
																	PropertyValuesHolder.ofFloat(View.SCALE_X, scaleFrom, scaleTo),
																	PropertyValuesHolder.ofFloat(View.SCALE_Y, scaleFrom, scaleTo)
		);
		controlViewAnimator.setDuration(goneOnEnd ? 1000 : 500);
		controlViewAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				controlAnimationView.setVisibility(goneOnEnd ? View.GONE : View.VISIBLE);
			}
		});
		
		controlAnimationView.setVisibility(View.VISIBLE);
		controlAnimationView.setImageDrawable(ContextCompat.getDrawable(context, drawableId));
		controlViewAnimator.start();
	}
	
	public boolean isSomePopupMenuVisible() {
		return isSomePopupMenuVisible;
	}
	
	public void showControlsThenHide() {
		
		AnimationUtils.animateView(controlsRoot, true, DEFAULT_CONTROLS_DURATION, 0, () -> hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME));
	}
	
	public void showControls(long duration) {
		
		controlsVisibilityHandler.removeCallbacksAndMessages(null);
		AnimationUtils.animateView(controlsRoot, true, duration);
	}
	
	public void hideControls(final long duration, long delay) {
		
		controlsVisibilityHandler.removeCallbacksAndMessages(null);
		controlsVisibilityHandler.postDelayed(() -> AnimationUtils.animateView(controlsRoot, false, duration), delay);
	}
	
	public void hideControlsAndButton(final long duration, long delay, View button) {
		
		controlsVisibilityHandler.removeCallbacksAndMessages(null);
		controlsVisibilityHandler.postDelayed(hideControlsAndButtonHandler(duration, button), delay);
	}
	
	private Runnable hideControlsAndButtonHandler(long duration, View videoPlayPause) {
		
		return () -> {
			videoPlayPause.setVisibility(View.INVISIBLE);
			AnimationUtils.animateView(controlsRoot, false, duration);
		};
	}
	
	public void setPlaybackQuality(final String quality) {
		this.resolver.setPlaybackQuality(quality);
	}
	
	@Nullable
	public String getPlaybackQuality() {
		return resolver.getPlaybackQuality();
	}
	
	public AspectRatioFrameLayout getAspectRatioFrameLayout() {
		return aspectRatioFrameLayout;
	}
	
	public SurfaceView getSurfaceView() {
		return surfaceView;
	}
	
	public boolean wasPlaying() {
		return wasPlaying;
	}
	
	@Nullable
	public VideoStream getSelectedVideoStream() {
		
		return (selectedStreamIndex >= 0 && availableStreams != null &&
				availableStreams.size() > selectedStreamIndex) ?
				availableStreams.get(selectedStreamIndex) : null;
	}
	
	public List<VideoStream> getAvailableStreams() {
		return availableStreams;
	}
	
	public Handler getControlsVisibilityHandler() {
		return controlsVisibilityHandler;
	}
	
	public View getRootView() {
		return rootView;
	}
	
	public void setRootView(View rootView) {
		this.rootView = rootView;
	}
	
	public View getLoadingPanel() {
		return loadingPanel;
	}
	
	public ImageView getEndScreen() {
		return endScreen;
	}
	
	public ImageView getControlAnimationView() {
		return controlAnimationView;
	}
	
	public View getControlsRoot() {
		return controlsRoot;
	}
	
	public View getBottomControlsRoot() {
		return bottomControlsRoot;
	}
	
	public SeekBar getPlaybackSeekBar() {
		return playbackSeekBar;
	}
	
	public TextView getPlaybackTime() {
		return playbackTime;
	}
	
	public View getTopControlsRoot() {
		return topControlsRoot;
	}
	
	public TextView getQualityTextView() {
		return qualityTextView;
	}
	
	public PopupMenu getQualityPopupMenu() {
		return qualityPopupMenu;
	}
	
	public PopupMenu getPlaybackSpeedPopupMenu() {
		return playbackSpeedPopupMenu;
	}
	
	public View getSurfaceForeground() {
		return surfaceForeground;
	}
	
	public TextView getCurrentDisplaySeek() {
		return currentDisplaySeek;
	}
	
	public SubtitleView getSubtitleView() {
		return subtitleView;
	}
	
	public TextView getCaptionTextView() {
		return captionTextView;
	}
}
