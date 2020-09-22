package io.awesome.gagtube.adsmanager;

import android.content.Context;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;

import io.awesome.gagtube.util.AppUtils;

public class AppInterstitialAd {
	
	private static AppInterstitialAd mInstance;
	private InterstitialAd mInterstitialAd;
	private AdClosedListener mAdClosedListener;
	private boolean isReloaded = false;
	
	public interface AdClosedListener {
		
		void onAdClosed();
	}
	
	public static AppInterstitialAd getInstance() {
		
		if (mInstance == null) {
			mInstance = new AppInterstitialAd();
		}
		return mInstance;
	}
	
	public void init(Context context) {
		
		AppUtils.displayAds(context, isShowAd -> {
			if (isShowAd) {
				mInterstitialAd = new InterstitialAd(context);
				mInterstitialAd.setAdUnitId(AdUtils.getInterstitialAdId(context));
				
				// set listeners for the Interstitial Ad
				mInterstitialAd.setAdListener(new AdListener() {
					
					@Override
					public void onAdLoaded() {
						// unimplemented
					}
					
					@Override
					public void onAdFailedToLoad(LoadAdError loadAdError) {
						
						// retry if load failed
						if (!isReloaded) {
							isReloaded = true;
							loadInterstitialAd();
						}
					}
					
					@Override
					public void onAdOpened() {
						// unimplemented
					}
					
					@Override
					public void onAdClicked() {
						// unimplemented
					}
					
					@Override
					public void onAdLeftApplication() {
						// unimplemented
					}
					
					@Override
					public void onAdClosed() {
						
						// auto reload new ad when current ad is closed
						if (mAdClosedListener != null) {
							mAdClosedListener.onAdClosed();
						}
						// load a new interstitial
						loadInterstitialAd();
					}
				});
				
				loadInterstitialAd();
			}
		});
	}
	
	private void loadInterstitialAd() {
		
		if (mInterstitialAd != null && !mInterstitialAd.isLoading() && !mInterstitialAd.isLoaded()) {
			
			AdRequest.Builder builder = new AdRequest.Builder();
			mInterstitialAd.loadAd(builder.build());
		}
	}
	
	public void showInterstitialAd(AdClosedListener mAdClosedListener) {
		
		if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
			
			isReloaded = false;
			this.mAdClosedListener = mAdClosedListener;
			
			// show ads
			mInterstitialAd.show();
		}
		else {
			// reload a new ad for next time
			loadInterstitialAd();
			// call onAdClosed
			mAdClosedListener.onAdClosed();
		}
	}
}