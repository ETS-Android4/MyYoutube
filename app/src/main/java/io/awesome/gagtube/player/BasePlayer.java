package io.awesome.gagtube.player;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.view.View;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import io.awesome.gagtube.R;
import io.awesome.gagtube.local.history.HistoryRecordManager;
import io.awesome.gagtube.player.helper.LoadController;
import io.awesome.gagtube.player.helper.MediaSessionManager;
import io.awesome.gagtube.player.helper.PlayerDataSource;
import io.awesome.gagtube.player.helper.PlayerHelper;
import io.awesome.gagtube.player.playback.BasePlayerMediaSession;
import io.awesome.gagtube.player.playback.CustomTrackSelector;
import io.awesome.gagtube.player.playback.MediaSourceManager;
import io.awesome.gagtube.player.playback.PlaybackListener;
import io.awesome.gagtube.player.playqueue.PlayQueue;
import io.awesome.gagtube.player.playqueue.PlayQueueAdapter;
import io.awesome.gagtube.player.playqueue.PlayQueueItem;
import io.awesome.gagtube.player.resolver.MediaSourceTag;
import io.awesome.gagtube.util.ImageDisplayConstants;
import io.awesome.gagtube.util.SerializedCache;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.SerialDisposable;

import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_INTERNAL;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_PERIOD_TRANSITION;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK;
import static com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT;

@SuppressWarnings({"WeakerAccess"})
public abstract class BasePlayer implements Player.EventListener, PlaybackListener, ImageLoadingListener {
	
	@NonNull final protected Context context;
	@NonNull final protected BroadcastReceiver broadcastReceiver;
	@NonNull final protected IntentFilter intentFilter;
	@NonNull final protected HistoryRecordManager recordManager;
	@NonNull final protected CustomTrackSelector trackSelector;
	@NonNull final protected PlayerDataSource dataSource;
	@NonNull final private LoadControl loadControl;
	@NonNull final private RenderersFactory renderFactory;
	@NonNull final private SerialDisposable progressUpdateReactor;
	@NonNull final private CompositeDisposable databaseUpdateReactor;
	
	// Intent
	@NonNull public static final String REPEAT_MODE = "repeat_mode";
	@NonNull public static final String PLAYBACK_SPEED = "playback_speed";
	@NonNull public static final String PLAYBACK_PITCH = "playback_pitch";
	@NonNull public static final String PLAYBACK_SKIP_SILENCE = "playback_skip_silence";
	@NonNull public static final String PLAYBACK_QUALITY = "playback_quality";
	@NonNull public static final String PLAY_QUEUE_KEY = "play_queue_key";
	@NonNull public static final String APPEND_ONLY = "append_only";
	@NonNull public static final String SELECT_ON_APPEND = "select_on_append";
	
	protected static final float[] PLAYBACK_SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
	
	protected PlayQueue playQueue;
	protected PlayQueueAdapter playQueueAdapter;
	
	@Nullable protected MediaSourceManager playbackManager;
	
	@Nullable private PlayQueueItem currentItem;
	@Nullable private MediaSourceTag currentMetadata;
	@Nullable private Bitmap currentThumbnail;
	
	@Nullable protected Toast errorToast;
	
	// Player
	protected final static int FAST_FORWARD_REWIND_AMOUNT_MILLIS = 10000; // 10 Seconds
	protected final static int PROGRESS_LOOP_INTERVAL_MILLIS = 500;
	
	protected SimpleExoPlayer simpleExoPlayer;
	protected MediaSessionManager mediaSessionManager;
	
	private boolean isPrepared = false;
	
	public BasePlayer(@NonNull final Context context) {
		
		this.context = context;
		
		this.broadcastReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				onBroadcastReceived(intent);
			}
		};
		this.intentFilter = new IntentFilter();
		setupBroadcastReceiver(intentFilter);
		context.registerReceiver(broadcastReceiver, intentFilter);
		
		this.recordManager = new HistoryRecordManager(context);
		
		this.progressUpdateReactor = new SerialDisposable();
		this.databaseUpdateReactor = new CompositeDisposable();
		
		final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
		this.dataSource = new PlayerDataSource(context, "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0", bandwidthMeter);
		
		final TrackSelection.Factory trackSelectionFactory = PlayerHelper.getQualitySelector();
		this.trackSelector = new CustomTrackSelector(trackSelectionFactory);
		
		this.loadControl = new LoadController();
		this.renderFactory = new DefaultRenderersFactory(context);
	}
	
	public void setup() {
		
		if (simpleExoPlayer == null) {
			initPlayer(true);
		}
		initListeners();
	}
	
	public void initPlayer(final boolean playOnReady) {
		
		simpleExoPlayer = new SimpleExoPlayer.Builder(context, renderFactory).setTrackSelector(trackSelector).setLoadControl(loadControl).build();
		simpleExoPlayer.addListener(this);
		simpleExoPlayer.setPlayWhenReady(playOnReady);
		simpleExoPlayer.setSeekParameters(SeekParameters.EXACT);
		simpleExoPlayer.setWakeMode(C.WAKE_MODE_NETWORK);
		simpleExoPlayer.setHandleAudioBecomingNoisy(true);
		
		mediaSessionManager = new MediaSessionManager(context, simpleExoPlayer, new BasePlayerMediaSession(this));
		
		registerBroadcastReceiver();
	}
	
	public void initListeners() {
	}
	
	public void handleIntent(Intent intent) {
		
		if (intent == null) return;
		
		// Resolve play queue
		if (!intent.hasExtra(PLAY_QUEUE_KEY)) return;
		final String intentCacheKey = intent.getStringExtra(PLAY_QUEUE_KEY);
		final PlayQueue queue = SerializedCache.getInstance().take(intentCacheKey, PlayQueue.class);
		if (queue == null) return;
		
		// Resolve append intents
		if (intent.getBooleanExtra(APPEND_ONLY, false) && playQueue != null) {
			int sizeBeforeAppend = playQueue.size();
			playQueue.append(queue.getStreams());
			
			if ((intent.getBooleanExtra(SELECT_ON_APPEND, false) || getCurrentState() == STATE_COMPLETED) && queue.getStreams().size() > 0) {
				playQueue.setIndex(sizeBeforeAppend);
			}
			return;
		}
		
		final int repeatMode = intent.getIntExtra(REPEAT_MODE, getRepeatMode());
		final float playbackSpeed = intent.getFloatExtra(PLAYBACK_SPEED, getPlaybackSpeed());
		final float playbackPitch = intent.getFloatExtra(PLAYBACK_PITCH, getPlaybackPitch());
		final boolean playbackSkipSilence = intent.getBooleanExtra(PLAYBACK_SKIP_SILENCE, getPlaybackSkipSilence());
		
		// Good to go...
		initPlayback(queue, repeatMode, playbackSpeed, playbackPitch, playbackSkipSilence, true);
	}
	
	public void handleVideo(PlayQueue playQueue) {
		
		int repeatMode = getRepeatMode();
		final float playbackSpeed = getPlaybackSpeed();
		final float playbackPitch = getPlaybackPitch();
		final boolean playbackSkipSilence = getPlaybackSkipSilence();
		
		if (playQueue != null) {
			// Good to go...
			initPlayback(playQueue, repeatMode, playbackSpeed, playbackPitch, playbackSkipSilence, true);
		}
	}
	
	public void initPlayback(@NonNull final PlayQueue queue, @Player.RepeatMode final int repeatMode, final float playbackSpeed, final float playbackPitch, final boolean playbackSkipSilence, final boolean playOnReady) {
		
		destroyPlayer();
		initPlayer(playOnReady);
		setRepeatMode(repeatMode);
		setPlaybackParameters(playbackSpeed, playbackPitch, playbackSkipSilence);
		
		playQueue = queue;
		playQueue.init();
		
		if (playbackManager != null) playbackManager.dispose();
		playbackManager = new MediaSourceManager(this, playQueue);
		
		if (playQueueAdapter != null) playQueueAdapter.dispose();
		playQueueAdapter = new PlayQueueAdapter(playQueue);
	}
	
	public void destroyPlayer() {
		
		if (simpleExoPlayer != null) {
			simpleExoPlayer.removeListener(this);
			simpleExoPlayer.stop();
			simpleExoPlayer.release();
		}
		if (isProgressLoopRunning()) stopProgressLoop();
		if (playQueue != null) playQueue.dispose();
		if (playbackManager != null) playbackManager.dispose();
		if (mediaSessionManager != null) mediaSessionManager.dispose();
		
		if (playQueueAdapter != null) {
			playQueueAdapter.unsetSelectedListener();
			playQueueAdapter.dispose();
		}
	}
	
	public void destroy() {
		
		destroyPlayer();
		unregisterBroadcastReceiver();
		
		databaseUpdateReactor.clear();
		progressUpdateReactor.set(null);
	}
	
	private void initThumbnail(final String url) {
		
		if (url == null || url.isEmpty()) return;
		
		ImageLoader.getInstance().resume();
		ImageLoader.getInstance().loadImage(url, ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS, this);
	}
	
	@Override
	public void onLoadingStarted(String imageUri, View view) {
		// unimplemented
	}
	
	@Override
	public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
		currentThumbnail = null;
	}
	
	@Override
	public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
		currentThumbnail = loadedImage;
	}
	
	@Override
	public void onLoadingCancelled(String imageUri, View view) {
		currentThumbnail = null;
	}
	
	/**
	 * Add your action in the intentFilter
	 *
	 * @param intentFilter intent filter that will be used for register the receiver
	 */
	protected void setupBroadcastReceiver(IntentFilter intentFilter) {
		intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
	}
	
	public void onBroadcastReceived(Intent intent) {
		
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			onPause();
		}
	}
	
	private void registerBroadcastReceiver() {
		
		// Try to unregister current first
		unregisterBroadcastReceiver();
		context.registerReceiver(broadcastReceiver, intentFilter);
	}
	
	public void unregisterBroadcastReceiver() {
		
		try {
			context.unregisterReceiver(broadcastReceiver);
		}
		catch (final IllegalArgumentException ignored) {
		}
	}
	
	public static final int STATE_PREFLIGHT = -1;
	public static final int STATE_BLOCKED = 123;
	public static final int STATE_PLAYING = 124;
	public static final int STATE_BUFFERING = 125;
	public static final int STATE_PAUSED = 126;
	public static final int STATE_PAUSED_SEEK = 127;
	public static final int STATE_COMPLETED = 128;
	
	protected int currentState = STATE_PREFLIGHT;
	
	public void changeState(int state) {
		
		currentState = state;
		switch (state) {
			
			case STATE_BLOCKED:
				onBlocked();
				break;
			
			case STATE_PLAYING:
				onPlaying();
				break;
			
			case STATE_BUFFERING:
				onBuffering();
				break;
			
			case STATE_PAUSED:
				onPaused();
				break;
			
			case STATE_PAUSED_SEEK:
				onPausedSeek();
				break;
			
			case STATE_COMPLETED:
				onCompleted();
				break;
		}
	}
	
	public void onBlocked() {
		if (!isProgressLoopRunning()) startProgressLoop();
	}
	
	public void onPlaying() {
		if (!isProgressLoopRunning()) startProgressLoop();
	}
	
	public void onBuffering() {
		// unimplemented
	}
	
	public void onPaused() {
		if (isProgressLoopRunning()) stopProgressLoop();
	}
	
	public void onPausedSeek() {
		// unimplemented
	}
	
	public void onCompleted() {
		if (PlayerHelper.isAutoQueueEnabled(context)) {
			playQueue.offsetIndex(+1);
		}
		else if (isProgressLoopRunning()) {
			Stream.of(playQueue.getStreams())
					.filter(playQueueItem -> playQueueItem == playQueue.getItem())
					.toList().clear();
			stopProgressLoop();
		}
	}
	
	// Repeat and shuffle
	public void onRepeatClicked() {
		
		final int mode;
		switch (getRepeatMode()) {
			
			case Player.REPEAT_MODE_OFF:
				mode = Player.REPEAT_MODE_ONE;
				break;
			
			case Player.REPEAT_MODE_ONE:
				mode = Player.REPEAT_MODE_ALL;
				break;
			
			case Player.REPEAT_MODE_ALL:
			default:
				mode = Player.REPEAT_MODE_OFF;
				break;
		}
		
		setRepeatMode(mode);
	}
	
	public void onShuffleClicked() {
		
		if (simpleExoPlayer != null) {
			simpleExoPlayer.setShuffleModeEnabled(!simpleExoPlayer.getShuffleModeEnabled());
		}
	}
	
	public abstract void onUpdateProgress(int currentProgress, int duration, int bufferPercent);
	
	protected void startProgressLoop() {
		progressUpdateReactor.set(getProgressReactor());
	}
	
	protected void stopProgressLoop() {
		progressUpdateReactor.set(null);
	}
	
	public void triggerProgressUpdate() {
		
		if (simpleExoPlayer == null) return;
		
		onUpdateProgress(Math.max((int) simpleExoPlayer.getCurrentPosition(), 0), (int) simpleExoPlayer.getDuration(), simpleExoPlayer.getBufferedPercentage());
	}
	
	private Disposable getProgressReactor() {
		
		return Observable.interval(PROGRESS_LOOP_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(ignored -> triggerProgressUpdate(), error -> {
				});
	}
	
	// ExoPlayer Listener
	@Override
	public void onTimelineChanged(Timeline timeline, final int reason) {
		maybeUpdateCurrentMetadata();
	}
	
	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
		maybeUpdateCurrentMetadata();
	}
	
	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		// unimplemented
	}
	
	@Override
	public void onLoadingChanged(final boolean isLoading) {
		
		if (!isLoading && getCurrentState() == STATE_PAUSED && isProgressLoopRunning()) {
			stopProgressLoop();
		}
		else if (isLoading && !isProgressLoopRunning()) {
			startProgressLoop();
		}
		
		maybeUpdateCurrentMetadata();
	}
	
	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		
		if (getCurrentState() == STATE_PAUSED_SEEK) {
			return;
		}
		
		switch (playbackState) {
			
			case Player.STATE_IDLE:
				isPrepared = false;
				break;
			
			case Player.STATE_BUFFERING:
				if (isPrepared) {
					changeState(STATE_BUFFERING);
				}
				break;
			
			case Player.STATE_READY:
				maybeUpdateCurrentMetadata();
				maybeCorrectSeekPosition();
				if (!isPrepared) {
					isPrepared = true;
					onPrepared(playWhenReady);
					break;
				}
				changeState(playWhenReady ? STATE_PLAYING : STATE_PAUSED);
				break;
			
			case Player.STATE_ENDED:
				changeState(STATE_COMPLETED);
				if (currentMetadata != null) {
					resetPlaybackState(currentMetadata.getMetadata());
				}
				isPrepared = false;
				break;
		}
	}
	
	private void maybeCorrectSeekPosition() {
		
		if (playQueue != null && simpleExoPlayer != null && currentMetadata != null) {
			
			final PlayQueueItem currentSourceItem = playQueue.getItem();
			if (currentSourceItem == null) return;
			
			final StreamInfo currentInfo = currentMetadata.getMetadata();
			final long presetStartPositionMillis = currentInfo.getStartPosition() * 1000;
			if (presetStartPositionMillis > 0L) {
				seekTo(presetStartPositionMillis);
			}
		}
	}
	
	/**
	 * Processes the exceptions produced by {@link com.google.android.exoplayer2.ExoPlayer ExoPlayer}.
	 * There are multiple types of errors: <br><br>
	 * <p>
	 * {@link ExoPlaybackException#TYPE_SOURCE TYPE_SOURCE}: <br><br>
	 * <p>
	 * {@link ExoPlaybackException#TYPE_UNEXPECTED TYPE_UNEXPECTED}: <br><br>
	 * If a runtime error occurred, then we can try to recover it by restarting the playback
	 * after setting the timestamp recovery. <br><br>
	 * <p>
	 * {@link ExoPlaybackException#TYPE_RENDERER TYPE_RENDERER}: <br><br>
	 * If the renderer failed, treat the error as unrecoverable.
	 *
	 * @see #processSourceError(IOException)
	 * @see Player.EventListener#onPlayerError(ExoPlaybackException)
	 */
	@SuppressLint("SwitchIntDef")
	@Override
	public void onPlayerError(ExoPlaybackException error) {
		
		if (errorToast != null) {
			errorToast.cancel();
			errorToast = null;
		}
		
		savePlaybackState();
		
		switch (error.type) {
			
			case ExoPlaybackException.TYPE_SOURCE:
				processSourceError(error.getSourceException());
				showStreamError(error);
				break;
			
			case ExoPlaybackException.TYPE_UNEXPECTED:
				showRecoverableError(error);
				setRecovery();
				reload();
				break;
			
			default:
				showUnrecoverableError(error);
				onPlaybackShutdown();
				break;
		}
	}
	
	private void processSourceError(final IOException error) {
		
		if (simpleExoPlayer == null || playQueue == null) return;
		
		setRecovery();
		
		if (error instanceof BehindLiveWindowException) {
			reload();
		}
		else {
			playQueue.error();
		}
	}
	
	@SuppressLint("SwitchIntDef")
	@Override
	public void onPositionDiscontinuity(@Player.DiscontinuityReason final int reason) {
		
		if (playQueue == null) return;
		
		// Refresh the playback if there is a transition to the next video
		final int newWindowIndex = simpleExoPlayer.getCurrentWindowIndex();
		switch (reason) {
			
			case DISCONTINUITY_REASON_PERIOD_TRANSITION:
				// When player is in single repeat mode and a period transition occurs,
				// we need to register a view count here since no metadata has changed
				if (getRepeatMode() == Player.REPEAT_MODE_ONE && newWindowIndex == playQueue.getIndex()) {
					registerView();
					break;
				}
			case DISCONTINUITY_REASON_SEEK:
			case DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
			case DISCONTINUITY_REASON_INTERNAL:
				if (playQueue.getIndex() != newWindowIndex) {
					resetPlaybackState(playQueue.getItem());
					playQueue.setIndex(newWindowIndex);
				}
				break;
		}
		
		maybeUpdateCurrentMetadata();
	}
	
	@Override
	public void onRepeatModeChanged(@Player.RepeatMode final int reason) {
		// unimplemented
	}
	
	@Override
	public void onShuffleModeEnabledChanged(final boolean shuffleModeEnabled) {
		
		if (playQueue == null) return;
		if (shuffleModeEnabled) {
			playQueue.shuffle();
		}
		else {
			playQueue.unshuffle();
		}
	}
	
	@Override
	public void onSeekProcessed() {
		
		if (isPrepared) {
			savePlaybackState();
		}
	}
	
	// Playback Listener
	@Override
	public boolean isApproachingPlaybackEdge(final long timeToEndMillis) {
		
		// If live, then not near playback edge
		// If not playing, then not approaching playback edge
		if (simpleExoPlayer == null || isLive() || !isPlaying()) return false;
		
		final long currentPositionMillis = simpleExoPlayer.getCurrentPosition();
		final long currentDurationMillis = simpleExoPlayer.getDuration();
		return currentDurationMillis - currentPositionMillis < timeToEndMillis;
	}
	
	@Override
	public void onPlaybackBlock() {
		
		if (simpleExoPlayer == null) return;
		
		currentItem = null;
		currentMetadata = null;
		simpleExoPlayer.stop();
		isPrepared = false;
		
		changeState(STATE_BLOCKED);
	}
	
	@Override
	public void onPlaybackUnblock(final MediaSource mediaSource) {
		
		if (simpleExoPlayer == null) return;
		
		if (getCurrentState() == STATE_BLOCKED) changeState(STATE_BUFFERING);
		
		simpleExoPlayer.prepare(mediaSource);
	}
	
	public void onPlaybackSynchronize(@NonNull final PlayQueueItem item) {
		
		if (simpleExoPlayer == null || playQueue == null) return;
		
		final boolean onPlaybackInitial = currentItem == null;
		final boolean hasPlayQueueItemChanged = currentItem != item;
		
		final int currentPlayQueueIndex = playQueue.indexOf(item);
		final int currentPlaylistIndex = simpleExoPlayer.getCurrentWindowIndex();
		
		// If nothing to synchronize
		if (!hasPlayQueueItemChanged) return;
		currentItem = item;
		
		if (currentPlaylistIndex != currentPlayQueueIndex || onPlaybackInitial || !isPlaying()) {
			
			if (item.getRecoveryPosition() != PlayQueueItem.RECOVERY_UNSET) {
				simpleExoPlayer.seekTo(currentPlayQueueIndex, item.getRecoveryPosition());
				playQueue.unsetRecovery(currentPlayQueueIndex);
			}
			else {
				simpleExoPlayer.seekToDefaultPosition(currentPlayQueueIndex);
			}
		}
	}
	
	protected void onMetadataChanged(@NonNull final MediaSourceTag tag) {
		
		final StreamInfo info = tag.getMetadata();
		
		initThumbnail(info.getThumbnailUrl());
		registerView();
	}
	
	@Override
	public void onPlaybackShutdown() {
		
		destroy();
	}
	
	// General Player
	public void showStreamError(Exception exception) {
		
		exception.printStackTrace();
		
		if (errorToast == null) {
			errorToast = Toast.makeText(context, R.string.player_stream_failure, Toast.LENGTH_SHORT);
			errorToast.show();
		}
	}
	
	public void showRecoverableError(Exception exception) {
		
		exception.printStackTrace();
		
		if (errorToast == null) {
			errorToast = Toast.makeText(context, R.string.player_recoverable_failure, Toast.LENGTH_SHORT);
			errorToast.show();
		}
	}
	
	public void showUnrecoverableError(Exception exception) {
		
		exception.printStackTrace();
		
		if (errorToast != null) {
			errorToast.cancel();
		}
		errorToast = Toast.makeText(context, R.string.player_unrecoverable_failure, Toast.LENGTH_SHORT);
		errorToast.show();
	}
	
	public void onPrepared(boolean playWhenReady) {
		
		changeState(playWhenReady ? STATE_PLAYING : STATE_PAUSED);
	}
	
	public void onPlay() {
		
		if (playQueue == null || simpleExoPlayer == null) return;
		
		if (getCurrentState() == STATE_COMPLETED) {
			if (playQueue.getIndex() == 0) {
				seekToDefault();
			}
			else {
				playQueue.setIndex(0);
			}
		}
		
		simpleExoPlayer.setPlayWhenReady(true);
	}
	
	public void onPause() {
		
		if (simpleExoPlayer == null) return;
		
		simpleExoPlayer.setPlayWhenReady(false);
	}
	
	public void onPlayPause() {
		
		if (!isPlaying()) {
			onPlay();
		}
		else {
			onPause();
		}
	}
	
	public void onFastRewind() {
		
		seekBy(-FAST_FORWARD_REWIND_AMOUNT_MILLIS);
	}
	
	public void onFastForward() {
		
		seekBy(FAST_FORWARD_REWIND_AMOUNT_MILLIS);
	}
	
	public void onPlayPrevious() {
		
		if (simpleExoPlayer == null || playQueue == null) return;
		
		if (playQueue.getIndex() == 0) {
			seekToDefault();
			playQueue.offsetIndex(0);
		}
		else {
			savePlaybackState();
			playQueue.offsetIndex(-1);
		}
	}
	
	public void onPlayNext() {
		
		if (playQueue == null) return;
		
		savePlaybackState();
		playQueue.offsetIndex(+1);
	}
	
	public void onSelected(final PlayQueueItem item) {
		
		if (playQueue == null || simpleExoPlayer == null) return;
		
		final int index = playQueue.indexOf(item);
		if (index == -1) return;
		
		if (playQueue.getIndex() == index && simpleExoPlayer.getCurrentWindowIndex() == index) {
			seekToDefault();
		}
		else {
			savePlaybackState();
		}
		playQueue.setIndex(index);
	}
	
	public void seekTo(long positionMillis) {
		
		if (simpleExoPlayer != null) simpleExoPlayer.seekTo(positionMillis);
	}
	
	public void seekBy(long offsetMillis) {
		
		if (simpleExoPlayer == null) return;
		
		seekTo(simpleExoPlayer.getCurrentPosition() + offsetMillis);
	}
	
	public boolean isCurrentWindowValid() {
		return simpleExoPlayer != null && simpleExoPlayer.getDuration() >= 0 && simpleExoPlayer.getCurrentPosition() >= 0;
	}
	
	public void seekToDefault() {
		
		if (simpleExoPlayer != null) {
			simpleExoPlayer.seekToDefaultPosition();
		}
	}
	
	// Utils
	private void registerView() {
		
		if (currentMetadata == null) return;
		final StreamInfo currentInfo = currentMetadata.getMetadata();
		final Disposable viewRegister = recordManager.onViewed(currentInfo).onErrorComplete().subscribe();
		databaseUpdateReactor.add(viewRegister);
	}
	
	protected void reload() {
		
		if (playbackManager != null) {
			playbackManager.dispose();
		}
		
		if (playQueue != null) {
			playbackManager = new MediaSourceManager(this, playQueue);
		}
	}
	
	protected void savePlaybackState(final StreamInfo info, final long progress) {
		
		if (info == null) return;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(context.getString(R.string.enable_watch_history_key), true)) {
			final Disposable stateSaver = recordManager.saveStreamState(info, progress)
					.observeOn(AndroidSchedulers.mainThread())
					.onErrorComplete().subscribe();
			databaseUpdateReactor.add(stateSaver);
		}
	}
	
	private void resetPlaybackState(final PlayQueueItem queueItem) {
		
		if (queueItem == null) return;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(context.getString(R.string.enable_watch_history_key), true)) {
			final Disposable stateSaver = queueItem.getStream()
					.flatMapCompletable(info -> recordManager.saveStreamState(info, 0))
					.observeOn(AndroidSchedulers.mainThread())
					.onErrorComplete()
					.subscribe();
			databaseUpdateReactor.add(stateSaver);
		}
	}
	
	public void resetPlaybackState(final StreamInfo info) {
		savePlaybackState(info, 0);
	}
	
	public void savePlaybackState() {
		
		if (simpleExoPlayer != null && currentMetadata != null) {
			final StreamInfo currentInfo = currentMetadata.getMetadata();
			savePlaybackState(currentInfo, simpleExoPlayer.getCurrentPosition());
		}
	}
	
	private void maybeUpdateCurrentMetadata() {
		
		if (simpleExoPlayer == null) return;
		
		final MediaSourceTag metadata;
		try {
			metadata = (MediaSourceTag) simpleExoPlayer.getCurrentTag();
		}
		catch (IndexOutOfBoundsException | ClassCastException error) {
			return;
		}
		
		if (metadata == null) return;
		maybeAutoQueueNextStream(metadata);
		
		if (currentMetadata == metadata) return;
		currentMetadata = metadata;
		onMetadataChanged(metadata);
	}
	
	private void maybeAutoQueueNextStream(@NonNull final MediaSourceTag currentMetadata) {
		
		if (playQueue == null || playQueue.getIndex() != playQueue.size() - 1 || getRepeatMode() != Player.REPEAT_MODE_OFF || !PlayerHelper.isAutoQueueEnabled(context)) {
			return;
		}
		
		// auto queue when starting playback on the last item when not repeating
		final PlayQueue autoQueue = PlayerHelper.autoQueueOf(currentMetadata.getMetadata(), playQueue.getStreams());
		if (autoQueue != null) playQueue.append(autoQueue.getStreams());
	}
	
	public SimpleExoPlayer getPlayer() {
		return simpleExoPlayer;
	}
	
	public int getCurrentState() {
		return currentState;
	}
	
	@Nullable
	public MediaSourceTag getCurrentMetadata() {
		return currentMetadata;
	}
	
	@NonNull
	public String getVideoUrl() {
		return currentMetadata == null ? context.getString(R.string.unknown_content) : currentMetadata.getMetadata().getUrl();
	}
	
	@NonNull
	public String getVideoTitle() {
		return currentMetadata == null ? context.getString(R.string.unknown_content) : currentMetadata.getMetadata().getName();
	}
	
	@NonNull
	public String getUploaderName() {
		return currentMetadata == null ? context.getString(R.string.unknown_content) : currentMetadata.getMetadata().getUploaderName();
	}
	
	@Nullable
	public Bitmap getThumbnail() {
		return currentThumbnail == null ? BitmapFactory.decodeResource(context.getResources(), R.drawable.no_image) : currentThumbnail;
	}
	
	/**
	 * Checks if the current playback is a livestream AND is playing at or beyond the live edge
	 */
	public boolean isLiveEdge() {
		
		if (simpleExoPlayer == null || !isLive()) return false;
		
		final Timeline currentTimeline = simpleExoPlayer.getCurrentTimeline();
		final int currentWindowIndex = simpleExoPlayer.getCurrentWindowIndex();
		if (currentTimeline.isEmpty() || currentWindowIndex < 0 || currentWindowIndex >= currentTimeline.getWindowCount()) {
			return false;
		}
		
		Timeline.Window timelineWindow = new Timeline.Window();
		currentTimeline.getWindow(currentWindowIndex, timelineWindow);
		return timelineWindow.getDefaultPositionMs() <= simpleExoPlayer.getCurrentPosition();
	}
	
	public boolean isLive() {
		
		if (simpleExoPlayer == null) return false;
		
		try {
			return simpleExoPlayer.isCurrentWindowDynamic();
		}
		catch (IndexOutOfBoundsException ignored) {
			return false;
		}
	}
	
	public boolean isPlaying() {
		
		if (simpleExoPlayer == null) return false;
		final int state = simpleExoPlayer.getPlaybackState();
		return (state == Player.STATE_READY || state == Player.STATE_BUFFERING) && simpleExoPlayer.getPlayWhenReady();
	}
	
	@Player.RepeatMode
	public int getRepeatMode() {
		return simpleExoPlayer == null ? Player.REPEAT_MODE_OFF : simpleExoPlayer.getRepeatMode();
	}
	
	public void setRepeatMode(@Player.RepeatMode final int repeatMode) {
		if (simpleExoPlayer != null) simpleExoPlayer.setRepeatMode(repeatMode);
	}
	
	public float getPlaybackSpeed() {
		return getPlaybackParameters().speed;
	}
	
	public float getPlaybackPitch() {
		return getPlaybackParameters().pitch;
	}
	
	public boolean getPlaybackSkipSilence() {
		return getPlaybackParameters().skipSilence;
	}
	
	public void setPlaybackSpeed(float speed) {
		setPlaybackParameters(speed, getPlaybackPitch(), getPlaybackSkipSilence());
	}
	
	public PlaybackParameters getPlaybackParameters() {
		
		if (simpleExoPlayer == null) return PlaybackParameters.DEFAULT;
		
		final PlaybackParameters parameters = simpleExoPlayer.getPlaybackParameters();
		return parameters == null ? PlaybackParameters.DEFAULT : parameters;
	}
	
	public void setPlaybackParameters(float speed, float pitch, boolean skipSilence) {
		simpleExoPlayer.setPlaybackParameters(new PlaybackParameters(speed, pitch, skipSilence));
	}
	
	public PlayQueue getPlayQueue() {
		return playQueue;
	}
	
	public PlayQueueAdapter getPlayQueueAdapter() {
		return playQueueAdapter;
	}
	
	public boolean isPrepared() {
		return isPrepared;
	}
	
	public boolean isProgressLoopRunning() {
		return progressUpdateReactor.get() != null;
	}
	
	public void setRecovery() {
		
		if (playQueue == null || simpleExoPlayer == null) return;
		
		final int queuePos = playQueue.getIndex();
		final long windowPos = simpleExoPlayer.getCurrentPosition();
		
		if (windowPos > 0 && windowPos <= simpleExoPlayer.getDuration()) {
			setRecovery(queuePos, windowPos);
		}
	}
	
	public void setRecovery(final int queuePos, final long windowPos) {
		
		if (playQueue.size() <= queuePos) return;
		
		playQueue.setRecovery(queuePos, windowPos);
	}
	
	public boolean gotDestroyed() {
		return simpleExoPlayer == null;
	}
}
