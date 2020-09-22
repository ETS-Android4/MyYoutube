package io.awesome.gagtube.adsmanager;

import android.app.Activity;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import io.awesome.gagtube.R;

public class AppRewardAd {
	
	private static AppRewardAd mInstance;
	private RewardedVideoAd mRewardedVideoAd;
	private AdClosedListener mAdClosedListener;
	private boolean isReloaded = false;
	private String rewardAdId;
	
	public interface AdClosedListener {
		
		void onAdClosed();
	}
	
	public static AppRewardAd getInstance() {
		
		if (mInstance == null) {
			mInstance = new AppRewardAd();
		}
		return mInstance;
	}
	
	public void init(Activity activity) {
		
		rewardAdId = activity.getString(R.string.rewarded);
		mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(activity);
		
		// set listeners for the rewarded ad
		mRewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
			
			@Override
			public void onRewardedVideoAdLoaded() {
				// unimplemented
			}
			
			@Override
			public void onRewardedVideoAdOpened() {
				// unimplemented
			}
			
			@Override
			public void onRewardedVideoStarted() {
				// unimplemented
			}
			
			@Override
			public void onRewardedVideoAdClosed() {
				
				// auto reload new ad when current ad is closed
				if (mAdClosedListener != null) {
					mAdClosedListener.onAdClosed();
				}
				// load a new rewarded ad
				loadRewardedVideoAd();
			}
			
			@Override
			public void onRewarded(RewardItem rewardItem) {
				
				// show toast with thanks
				Toast.makeText(activity, R.string.thanks_for_your_supports, Toast.LENGTH_LONG).show();
			}
			
			@Override
			public void onRewardedVideoAdLeftApplication() {
				// unimplemented
			}
			
			@Override
			public void onRewardedVideoAdFailedToLoad(int errorCode) {
				
				// retry if load failed
				if (!isReloaded) {
					isReloaded = true;
					loadRewardedVideoAd();
				}
			}
			
			@Override
			public void onRewardedVideoCompleted() {
				// unimplemented
			}
		});
		
		loadRewardedVideoAd();
	}
	
	private void loadRewardedVideoAd() {
		
		if (mRewardedVideoAd != null && !mRewardedVideoAd.isLoaded() && rewardAdId != null) {
			
			AdRequest.Builder builder = new AdRequest.Builder();
			mRewardedVideoAd.loadAd(rewardAdId, builder.build());
		}
	}
	
	public void showRewardedVideoAd(AdClosedListener mAdClosedListener) {
		
		if (mRewardedVideoAd != null && mRewardedVideoAd.isLoaded()) {
			
			isReloaded = false;
			this.mAdClosedListener = mAdClosedListener;
			
			// show ads
			mRewardedVideoAd.show();
		}
		else {
			// reload a new ad for next time
			loadRewardedVideoAd();
			// call onAdClosed
			mAdClosedListener.onAdClosed();
		}
	}
}