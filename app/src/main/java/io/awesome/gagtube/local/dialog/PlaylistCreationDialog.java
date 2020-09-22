package io.awesome.gagtube.local.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AdUtils;
import io.awesome.gagtube.adsmanager.nativead.NativeAdStyle;
import io.awesome.gagtube.adsmanager.nativead.NativeAdView;
import io.awesome.gagtube.database.GAGTubeDatabase;
import io.awesome.gagtube.database.stream.model.StreamEntity;
import io.awesome.gagtube.local.playlist.LocalPlaylistManager;
import io.reactivex.android.schedulers.AndroidSchedulers;

public final class PlaylistCreationDialog extends PlaylistDialog {
	
	@BindView(R.id.template_view) NativeAdView nativeAdView;
	@BindView(R.id.playlist_name) TextInputEditText editText;
	private Activity activity;
	
	public static PlaylistCreationDialog newInstance(final List<StreamEntity> streams) {
		
		PlaylistCreationDialog dialog = new PlaylistCreationDialog();
		dialog.setInfo(streams);
		return dialog;
	}
	
	public static PlaylistCreationDialog newInstance() {
		return new PlaylistCreationDialog();
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = getActivity();
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		
		View dialogView = View.inflate(getContext(), R.layout.dialog_playlist_name, null);
		ButterKnife.bind(this, dialogView);
		
		// show ad
		showNativeAd();
		
		@SuppressLint("CheckResult") final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext())
				.setTitle(R.string.create_new_playlist)
				.setView(dialogView)
				.setCancelable(true)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.create, (dialogInterface, i) -> {
					
					final String name = editText.getText().toString();
					final LocalPlaylistManager playlistManager = new LocalPlaylistManager(GAGTubeDatabase.getInstance(dialogView.getContext()));
					final Toast successToast = Toast.makeText(getActivity(), R.string.playlist_creation_success, Toast.LENGTH_SHORT);
					
					// create playlist
					if (getStreams() != null) {
						playlistManager.createPlaylist(name, getStreams()).observeOn(AndroidSchedulers.mainThread()).subscribe(longs -> successToast.show());
					}
					else {
						playlistManager.createPlaylist(name).observeOn(AndroidSchedulers.mainThread()).subscribe(longs -> successToast.show());
					}
				});
		
		return dialogBuilder.create();
	}
	
	private void showNativeAd() {
		
		// ad options
		VideoOptions videoOptions = new VideoOptions.Builder()
				.setStartMuted(true)
				.build();
		
		NativeAdOptions adOptions = new NativeAdOptions.Builder()
				.setVideoOptions(videoOptions)
				.build();
		
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
