package io.awesome.gagtube.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import io.awesome.gagtube.R;

public class GAGTubeSettings {
	
	private GAGTubeSettings() {
	}
	
	public static void initSettings(Context context) {
		
		PreferenceManager.setDefaultValues(context, R.xml.gagtube_settings, true);
		
		getVideoDownloadFolder(context);
		getAudioDownloadFolder(context);
	}
	
	private static void getVideoDownloadFolder(Context context) {
		getFolder(context, R.string.download_path_video_key, Environment.DIRECTORY_MOVIES);
	}
	
	private static void getAudioDownloadFolder(Context context) {
		getFolder(context, R.string.download_path_audio_key, Environment.DIRECTORY_MUSIC);
	}
	
	private static void getFolder(Context context, int keyID, String defaultDirectoryName) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final String key = context.getString(keyID);
		String downloadPath = prefs.getString(key, null);
		if (downloadPath != null && !downloadPath.isEmpty()) return;
		
		SharedPreferences.Editor spEditor = prefs.edit();
		spEditor.putString(key, getChildFolderPathForDir(getFolder(defaultDirectoryName)));
		spEditor.apply();
	}
	
	@NonNull
	private static File getFolder(String defaultDirectoryName) {
		return new File(Environment.getExternalStorageDirectory(), defaultDirectoryName);
	}
	
	private static String getChildFolderPathForDir(File dir) {
		return new File(dir, "ProTube").toURI().toString();
	}
}
