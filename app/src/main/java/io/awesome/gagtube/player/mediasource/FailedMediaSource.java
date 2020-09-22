package io.awesome.gagtube.player.mediasource;

import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.awesome.gagtube.player.playqueue.PlayQueueItem;

public class FailedMediaSource extends BaseMediaSource implements ManagedMediaSource {
	
	private final PlayQueueItem playQueueItem;
	private final FailedMediaSourceException error;
	private final long retryTimestamp;
	
	public FailedMediaSource(@NonNull final PlayQueueItem playQueueItem, @NonNull final FailedMediaSourceException error) {
		
		this.playQueueItem = playQueueItem;
		this.error = error;
		this.retryTimestamp = Long.MAX_VALUE;
	}
	
	public PlayQueueItem getStream() {
		return playQueueItem;
	}
	
	public FailedMediaSourceException getError() {
		return error;
	}
	
	private boolean canRetry() {
		return System.currentTimeMillis() >= retryTimestamp;
	}
	
	@Override
	public void maybeThrowSourceInfoRefreshError() throws IOException {
		throw new IOException(error);
	}
	
	@Override
	public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
		return null;
	}
	
	@Override
	public void releasePeriod(MediaPeriod mediaPeriod) {
		// unimplemented
	}
	
	@Override
	protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
		// unimplemented
	}
	
	@Override
	protected void releaseSourceInternal() {
		// unimplemented
	}
	
	@Override
	public boolean shouldBeReplacedWith(@NonNull final PlayQueueItem newIdentity, final boolean interruptible) {
		return newIdentity != playQueueItem || canRetry();
	}
	
	@Override
	public boolean isStreamEqual(@NonNull PlayQueueItem stream) {
		return playQueueItem == stream;
	}
	
	public static class FailedMediaSourceException extends Exception {
		
		FailedMediaSourceException(String message) {
			super(message);
		}
		
		FailedMediaSourceException(Throwable cause) {
			super(cause);
		}
	}
	
	public static final class MediaSourceResolutionException extends FailedMediaSourceException {
		public MediaSourceResolutionException(String message) {
			super(message);
		}
	}
	
	public static final class StreamInfoLoadException extends FailedMediaSourceException {
		public StreamInfoLoadException(Throwable cause) {
			super(cause);
		}
	}
}
