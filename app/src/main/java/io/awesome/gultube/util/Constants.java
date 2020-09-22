package io.awesome.gultube.util;

import org.schabi.newpipe.extractor.ServiceList;

import okhttp3.OkHttpClient;

public class Constants {
	
	public static final String KEY_SERVICE_ID = "key_service_id";
	public static final String KEY_URL = "key_url";
	public static final String KEY_TITLE = "key_title";
	public static final String KEY_LINK_TYPE = "key_link_type";
	public static final String KEY_THUMBNAIL_URL = "key_thumbnail_url";
	
	public static final String KEY_THEME_CHANGE = "key_theme_change";
	public static final String KEY_CONTENT_CHANGE = "key_content_change";
	
	public static final int YOUTUBE_SERVICE_ID = ServiceList.YouTube.getServiceId();
	
	// change your email here
	public static final String FEEDBACK_EMAIL = "your_email_here";
	public static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
	public static final String VIDEO_BASE_URL = "https://www.youtube.com/watch?v=";
	public static final String CHANNEL_BASE_URL = "https://www.youtube.com/channel/";
	public static final String THUMBNAIL_URL_DEFAULT = "https://firebasestorage.googleapis.com/v0/b/protube-a987b.appspot.com/o/no_image.png?alt=media";
	
	public static final String COUNTRY_CODE = "country_code";
	public static final String LANGUAGE_CODE = "language_code";
	public static final String CATEGORY_ID = "category_id";
	public static final String CATEGORY_NAME = "category_name";
	
	// Change your YouTube API Key here, these are sample API Keys
	// Because one API Key has 10,000 quotas, we will create 5 API Keys to get more quotas for API.
	// Important note: One gmail account should be created only one YouTube API key.
	// Example: 5 gmail accounts => 5 YouTube API Keys
	public final static String[] YOUTUBE_API_KEYS = {
			"AIzaSyDHCTagrIAIC7sjbq3jb_oz79uVmP65jBc",
			"AIzaSyAjNfnKx2xA7ZHAsvV6LJqKZPe7OYj-pFU",
			"AIzaSyBV9A_dsMXKFNdTPrQ69M_yDQEj805OqnQ",
			"AIzaSyDhsUU1G0-qUAAXASGWpb5bqA4LWcs6NiE",
			"AIzaSyBqmZ2XHR0eQTS9EQGMdWlJU0DeSjZjUnY"
	};
	
	public final static String PLAYBACK_TIME_DEFAULT = "00:00";
	
	// shared connection pool
	private OkHttpClient okHttpClient;
	
	// singleton
	private static Constants myself;
	
	public synchronized static Constants getInstance() {
		
		if (myself == null) {
			myself = new Constants();
		}
		return myself;
	}
	
	private Constants() {
		
		// connection pool
		okHttpClient = new OkHttpClient();
	}
	
	public OkHttpClient.Builder getOkHttpBuilder() {
		return okHttpClient.newBuilder();
	}
}
