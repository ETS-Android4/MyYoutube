package io.awesome.gagtube.util.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AdUtils;
import io.awesome.gagtube.adsmanager.nativead.NativeAdStyle;
import io.awesome.gagtube.adsmanager.nativead.NativeAdView;
import io.awesome.gagtube.util.NavigationHelper;

public final class ExitAppDialog extends DialogFragment {
	
	@BindView(R.id.template_view) NativeAdView nativeAdView;
	
	private Activity activity;
	private Runnable callback;
	
	public ExitAppDialog(Runnable callback) {
		this.callback = callback;
	}
	
	public static ExitAppDialog newInstance(Runnable callback) {
		return new ExitAppDialog(callback);
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = getActivity();
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		
		View dialogView = View.inflate(activity, R.layout.dialog_exit_app, null);
		ButterKnife.bind(this, dialogView);
		
		// show ad
		showNativeAd();
		
		@SuppressLint("CheckResult") final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.dialog_exit_app_msg)
				.setView(dialogView)
				.setCancelable(true)
				.setNeutralButton(R.string.setting_rate_me_now, (dialog, which) -> NavigationHelper.openGooglePlayStore(activity, activity.getPackageName()))
				.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
				.setPositiveButton(R.string.yes, (dialog, which) -> callback.run());
		
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
					public void onAdFailedToLoad(int errorCode) {
						super.onAdFailedToLoad(errorCode);
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
