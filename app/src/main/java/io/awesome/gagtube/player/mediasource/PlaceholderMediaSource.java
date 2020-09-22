package io.awesome.gagtube.player.mediasource;

import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.awesome.gagtube.player.playqueue.PlayQueueItem;

public class PlaceholderMediaSource extends BaseMediaSource implements ManagedMediaSource {
	
	@Override
	public void maybeThrowSourceInfoRefreshError() {
		// unimplemented
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
	public boolean shouldBeReplacedWith(@NonNull PlayQueueItem newIdentity, final boolean isInterruptable) {
		return true;
	}
	
	@Override
	public boolean isStreamEqual(@NonNull PlayQueueItem stream) {
		return false;
	}
}
