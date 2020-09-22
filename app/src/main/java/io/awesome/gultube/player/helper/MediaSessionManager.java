package io.awesome.gultube.player.helper;

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;

import androidx.annotation.NonNull;
import io.awesome.gultube.player.mediasession.MediaSessionCallback;
import io.awesome.gultube.player.mediasession.PlayQueueNavigator;
import io.awesome.gultube.player.mediasession.PlayQueuePlaybackController;

public class MediaSessionManager {
	private static final String TAG = "MediaSessionManager";
	
	@NonNull
	private final MediaSessionCompat mediaSession;
	@NonNull
	private final MediaSessionConnector sessionConnector;
	
	public MediaSessionManager(@NonNull final Context context, @NonNull final Player player, @NonNull final MediaSessionCallback callback) {
		
		this.mediaSession = new MediaSessionCompat(context, TAG);
		this.mediaSession.setActive(true);
		
		this.sessionConnector = new MediaSessionConnector(mediaSession);
		this.sessionConnector.setControlDispatcher(new PlayQueuePlaybackController(callback));
		this.sessionConnector.setQueueNavigator(new PlayQueueNavigator(mediaSession, callback));
		this.sessionConnector.setPlayer(player);
	}
	
	/**
	 * Should be called on player destruction to prevent leakage.
	 */
	public void dispose() {
		this.sessionConnector.setPlayer(null);
		this.sessionConnector.setQueueNavigator(null);
		this.mediaSession.setActive(false);
		this.mediaSession.release();
	}
}
