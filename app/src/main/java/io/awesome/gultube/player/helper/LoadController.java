package io.awesome.gultube.player.helper;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;

import org.jetbrains.annotations.NotNull;

public class LoadController implements LoadControl {

    private final long initialPlaybackBufferUs;
    private final LoadControl internalLoadControl;
    
    public LoadController() {
        
        this(PlayerHelper.getPlaybackStartBufferMs(), PlayerHelper.getPlaybackMinimumBufferMs(), PlayerHelper.getPlaybackOptimalBufferMs());
    }

    private LoadController(final int initialPlaybackBufferMs, final int minimumPlaybackBufferMs, final int optimalPlaybackBufferMs) {
        
        this.initialPlaybackBufferUs = initialPlaybackBufferMs * 1000;

        DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
        builder.setBufferDurationsMs(minimumPlaybackBufferMs, optimalPlaybackBufferMs, initialPlaybackBufferMs, initialPlaybackBufferMs);
        internalLoadControl = builder.createDefaultLoadControl();
    }
    
    @Override
    public void onPrepared() {
        internalLoadControl.onPrepared();
    }

    @Override
    public void onTracksSelected(@NotNull Renderer[] renderers, @NotNull TrackGroupArray trackGroupArray, @NotNull TrackSelectionArray trackSelectionArray) {
        internalLoadControl.onTracksSelected(renderers, trackGroupArray, trackSelectionArray);
    }

    @Override
    public void onStopped() {
        internalLoadControl.onStopped();
    }

    @Override
    public void onReleased() {
        internalLoadControl.onReleased();
    }

    @NotNull
    @Override
    public Allocator getAllocator() {
        return internalLoadControl.getAllocator();
    }

    @Override
    public long getBackBufferDurationUs() {
        return internalLoadControl.getBackBufferDurationUs();
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return internalLoadControl.retainBackBufferFromKeyframe();
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs, float playbackSpeed) {
        return internalLoadControl.shouldContinueLoading(bufferedDurationUs, playbackSpeed);
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, float playbackSpeed, boolean rebuffering) {
        
        final boolean isInitialPlaybackBufferFilled = bufferedDurationUs >= this.initialPlaybackBufferUs * playbackSpeed;
        final boolean isInternalStartingPlayback = internalLoadControl.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering);
        
        return isInitialPlaybackBufferFilled || isInternalStartingPlayback;
    }
}
