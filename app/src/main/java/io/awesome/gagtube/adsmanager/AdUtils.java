package io.awesome.gagtube.adsmanager;

import android.app.Activity;
import android.content.Context;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Random;

import io.awesome.gagtube.R;
import io.awesome.gagtube.util.SharedPrefsHelper;

public class AdUtils {

    public static void fetchShowAdsFromRemoteConfig(Activity activity) {
        /*// create FirebaseRemoteConfig
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        // init firebase remote config
        remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build());
        // fetch data from FirebaseRemoteConfig
        remoteConfig.fetchAndActivate().addOnSuccessListener(activity, success -> {
            boolean showAd = remoteConfig.getBoolean("show_ads");
            SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.DISPLAY_ADS.name(), showAd);
        });*/
        SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.DISPLAY_ADS.name(), true);
    }

    public static String getNativeAdId(Context context) {
        String[] array = {context.getString(R.string.native_1), context.getString(R.string.native_2), context.getString(R.string.native_3)};
        return array[new Random().nextInt(3)];
    }

    public static String getInterstitialAdId(Context context) {
        String[] array = {context.getString(R.string.interstitial_1), context.getString(R.string.interstitial_2), context.getString(R.string.interstitial_3)};
        return array[new Random().nextInt(3)];
    }
}
