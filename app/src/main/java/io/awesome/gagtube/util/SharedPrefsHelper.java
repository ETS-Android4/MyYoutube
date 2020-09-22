package io.awesome.gagtube.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import androidx.preference.PreferenceManager;

public class SharedPrefsHelper {
	
	public enum Key {
		START_APP_DATE,
		DISPLAY_ADS,
		SHOW_RATING_2_DAYS,
		SHOW_RATING_5_DAYS
	}
	
	public static void setStringPrefs(Context context, String key, String value) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.apply();
	}
	
	public static String getStringPrefs(Context context, String key) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(key, "");
	}
	
	public static String getStringPrefs(Context context, String key, String defValue) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(key, defValue);
	}
	
	public static void setIntPrefs(Context context, String key, int value) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(key, value);
		editor.apply();
	}
	
	public static int getIntPrefs(Context context, String key) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(key, 0);
	}
	
	public static void setLongPrefs(Context context, String key, long value) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(key, value);
		editor.apply();
	}
	
	public static long getLongPrefs(Context context, String key) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong(key, 0);
	}
	
	public static void setBooleanPrefs(Context context, String key, boolean value) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key, value);
		editor.apply();
	}
	
	public static Boolean getBooleanPrefs(Context context, String key) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(key, false);
	}
	
	public static boolean saveArrayPrefs(Context mContext, String arrayName, String[] array) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(arrayName + "_size", array.length);
		
		for (int i = 0; i < array.length; i++)
			editor.putString(arrayName + "_" + i, array[i]);
		
		return editor.commit();
	}
	
	public static String[] loadArrayPrefs(Context mContext, String arrayName) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		int size = prefs.getInt(arrayName + "_size", 0);
		String[] array = new String[size];
		
		for (int i = 0; i < size; i++)
			array[i] = prefs.getString(arrayName + "_" + i, null);
		
		return array;
	}
	
	public static boolean saveObjectPrefs(Context mContext, String objectName, Object object) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();
		
		Gson gson = new Gson();
		String json = gson.toJson(object);
		editor.putString(objectName, json);
		
		return editor.commit();
	}
	
	public static void applyObjectPrefs(Context mContext, String objectName, Object object) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();
		Gson gson = new Gson();
		String json = gson.toJson(object);
		
		// clear
		editor.remove(objectName);
		editor.apply();
		
		// apply
		editor.putString(objectName, json);
		editor.commit();
	}
	
	public static Object loadObjectPrefs(Context mContext, String objectName, Type type) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		Gson gson = new Gson();
		String objectString = prefs.getString(objectName, "");
		
		return gson.fromJson(objectString, type);
	}
	
	public static void clearPrefs(Context mContext) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.clear();
		editor.apply();
	}
	
	public static void removePrefs(Context mContext, String key) {
		
		if (mContext == null) return;
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.remove(key);
		editor.apply();
	}
}
