package io.awesome.gagtube.player;

import android.content.Intent;
import android.view.MenuItem;

import io.awesome.gagtube.R;

import static io.awesome.gagtube.player.PopupVideoPlayer.ACTION_CLOSE;

public final class PopupVideoPlayerActivity extends ServicePlayerActivity {
	
	@Override
	public String getTag() {
		return "PopupVideoPlayerActivity";
	}
	
	@Override
	public String getSupportActionTitle() {
		return getResources().getString(R.string.title_activity_popup_player);
	}
	
	@Override
	public Intent getBindIntent() {
		return new Intent(this, PopupVideoPlayer.class);
	}
	
	@Override
	public void startPlayerListener() {
		
		if (player instanceof PopupVideoPlayer.VideoPlayerImpl) {
			((PopupVideoPlayer.VideoPlayerImpl) player).setActivityListener(this);
		}
	}
	
	@Override
	public void stopPlayerListener() {
		
		if (player instanceof PopupVideoPlayer.VideoPlayerImpl) {
			((PopupVideoPlayer.VideoPlayerImpl) player).removeActivityListener(this);
		}
	}
	
	@Override
	public boolean onPlayerOptionSelected(MenuItem item) {
		return false;
	}
	
	@Override
	public Intent getPlayerShutdownIntent() {
		return new Intent(ACTION_CLOSE);
	}
}
