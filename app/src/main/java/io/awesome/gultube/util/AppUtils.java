package io.awesome.gultube.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;

import com.annimon.stream.Optional;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.preference.PreferenceManager;
import hirondelle.date4j.DateTime;
import io.awesome.gultube.R;
import io.awesome.gultube.util.chrometabs.CustomTabActivityHelper;

public class AppUtils {
	
	public static void displayPopup(Activity activity, View view) {
		
		// create FirebaseRemoteConfig
		FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
		
		// init firebase remote config
		remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
													.setMinimumFetchIntervalInSeconds(0)
													.build());
		// fetch data from FirebaseRemoteConfig
		remoteConfig.fetchAndActivate().addOnSuccessListener(activity, success -> {
			
			String key = remoteConfig.getString("show_popup");
			if (view instanceof ExtendedFloatingActionButton) {
				if (Boolean.parseBoolean(key)) {
					((ExtendedFloatingActionButton) view).show();
				}
				else {
					((ExtendedFloatingActionButton) view).hide();
				}
			}
			else {
				view.setVisibility(Boolean.parseBoolean(key) ? View.VISIBLE : View.GONE);
			}
		});
	}
	



	
	public static void openChromeTabs(Activity activity, String url) {
		
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
		CustomTabActivityHelper.openCustomTab(activity, customTabsIntent, Uri.parse(url), (_activity, uri) -> {
			
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			activity.startActivity(intent);
		});
	}
	
	public static boolean isOnline(@NonNull Context context) {
		
		// true if online
		return Optional.ofNullable(((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)))
				.map(ConnectivityManager::getActiveNetworkInfo)
				.map(NetworkInfo::isConnected)
				.orElse(false);
	}
	
	public static String getPublishedDate(Context context, String publishedAt) throws ParseException {
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
		// get date
		Date date = dateFormat.parse(publishedAt);
		if (date == null) return "";
		DateTime mDateTime = DateTime.forInstant(date.getTime(), TimeZone.getDefault());
		long time = date.getTime();
		
		final long MINUTE_MILLIS = 60 * 1000;
		final long HOUR_MILLIS = 60 * 60 * 1000;
		final long DAY_MILLIS = 24 * HOUR_MILLIS;
		final long MONTH_MILLIS = 30 * DAY_MILLIS;
		final long YEAR_MILLIS = 12 * MONTH_MILLIS;
		
		if (time < 1000000000000L) {
			time *= 1000;
		}
		
		long now = System.currentTimeMillis();
		final long diff = now - time;
		
		if (diff < HOUR_MILLIS) {
			return String.format(Locale.ENGLISH, context.getString(R.string.minutes_ago), diff / MINUTE_MILLIS);
		}
		else if (diff < DAY_MILLIS) {
			return String.format(Locale.ENGLISH, context.getString(R.string.hours_ago), diff / HOUR_MILLIS);
		}
		else if (diff < MONTH_MILLIS) {
			return String.format(Locale.ENGLISH, context.getString(R.string.days_ago), diff / DAY_MILLIS);
		}
		else if (diff < YEAR_MILLIS) {
			return String.format(Locale.ENGLISH, context.getString(R.string.months_ago), diff / MONTH_MILLIS);
		}
		else {
			return mDateTime.format("dd-MM-yyyy", Locale.ENGLISH);
		}
	}
	
	public static String getDeviceCountryIso(Context context) {
		
		// get device country by sim card (most accurate)
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceCountry = null;
		if (tm != null) {
			deviceCountry = tm.getSimCountryIso().toUpperCase();
		}
		
		// if no deviceCountry by sim card, try locale
		if (TextUtils.isEmpty(deviceCountry)) {
			deviceCountry = Locale.getDefault().getCountry();
		}
		
		return deviceCountry;
	}
	
	public static int dpToPx(Context context, float dip) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
	}
	
	public static String getCountryCode(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(Constants.COUNTRY_CODE, Locale.getDefault().getCountry());
	}
	
	public static String getLanguageCode(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(Constants.LANGUAGE_CODE, Locale.getDefault().getLanguage());
	}
}