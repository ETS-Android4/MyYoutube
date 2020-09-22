package io.awesome.gagtube.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.preference.PreferenceManager;
import io.awesome.gagtube.R;
import io.awesome.gagtube.download.DownloaderImpl;
import io.awesome.gagtube.util.ThemeHelper;

public class ReCaptchaActivity extends AppCompatActivity {
	public static final int RECAPTCHA_REQUEST = 9001;
	public static final String RECAPTCHA_URL_EXTRA = "RECAPTCHA_URL_EXTRA";
	public static final String YOUTUBE_URL = "https://www.youtube.com";
	public static final String RECAPTCHA_COOKIES_KEY = "recaptcha_cookies";
	
	private WebView webView;
	private String foundCookies = "";
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		ThemeHelper.setTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recaptcha);
		Toolbar toolbar = findViewById(R.id.default_toolbar);
		setSupportActionBar(toolbar);
		
		String url = getIntent().getStringExtra(RECAPTCHA_URL_EXTRA);
		if (url == null || url.isEmpty()) {
			url = YOUTUBE_URL;
		}
		
		// set return to Cancel by default
		setResult(RESULT_CANCELED);
		
		webView = findViewById(R.id.reCaptchaWebView);
		
		// enable Javascript
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		
		webView.setWebViewClient(new WebViewClient() {
			
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
				String url = request.getUrl().toString();
				handleCookiesFromUrl(url);
				return false;
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
				handleCookiesFromUrl(url);
				return false;
			}
			
			@Override
			public void onPageFinished(final WebView view, final String url) {
				super.onPageFinished(view, url);
				handleCookiesFromUrl(url);
			}
		});
		
		// cleaning cache, history and cookies from webView
		webView.clearCache(true);
		webView.clearHistory();
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookies(callback -> {
		});
		
		webView.loadUrl(url);
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.menu_recaptcha, menu);
		
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.title_activity_recaptcha);
			actionBar.setSubtitle(R.string.subtitle_activity_recaptcha);
		}
		
		return true;
	}
	
	@Override
	public void onBackPressed() {
		saveCookiesAndFinish();
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		int id = item.getItemId();
        if (id == R.id.menu_item_done) {
            saveCookiesAndFinish();
            return true;
        }
        return false;
    }
	
	private void saveCookiesAndFinish() {
		// try to get cookies of unclosed page
		handleCookiesFromUrl(webView.getUrl());
		
		if (!foundCookies.isEmpty()) {
			// save cookies to preferences
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			final String key = getString(R.string.recaptcha_cookies_key);
			prefs.edit().putString(key, foundCookies).apply();
			
			// give cookies to Downloader class
			DownloaderImpl.getInstance().setCookie(RECAPTCHA_COOKIES_KEY, foundCookies);
			setResult(RESULT_OK);
		}
		
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		NavUtils.navigateUpTo(this, intent);
	}
	
	private void handleCookiesFromUrl(@Nullable final String url) {
		if (url == null) {
			return;
		}
		
		String cookies = CookieManager.getInstance().getCookie(url);
		handleCookies(cookies);
		
		// sometimes cookies are inside the url
		int abuseStart = url.indexOf("google_abuse=");
		if (abuseStart != -1) {
			int abuseEnd = url.indexOf("+path");
			try {
				String abuseCookie = url.substring(abuseStart + 13, abuseEnd);
				abuseCookie = URLDecoder.decode(abuseCookie, "UTF-8");
				handleCookies(abuseCookie);
			}
			catch (UnsupportedEncodingException | StringIndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void handleCookies(@Nullable final String cookies) {
		if (cookies == null) {
			return;
		}
		addYoutubeCookies(cookies);
		// add here methods to extract cookies for other services
	}
	
	private void addYoutubeCookies(@NonNull final String cookies) {
		if (cookies.contains("s_gl=") || cookies.contains("goojf=") || cookies.contains("VISITOR_INFO1_LIVE=") || cookies.contains("GOOGLE_ABUSE_EXEMPTION=")) {
			// youtube seems to also need the other cookies
			addCookie(cookies);
		}
	}
	
	private void addCookie(final String cookie) {
		if (foundCookies.contains(cookie)) {
			return;
		}
		
		if (foundCookies.isEmpty() || foundCookies.endsWith("; ")) {
			foundCookies += cookie;
		}
		else if (foundCookies.endsWith(";")) {
			foundCookies += " " + cookie;
		}
		else {
			foundCookies += "; " + cookie;
		}
	}
}
