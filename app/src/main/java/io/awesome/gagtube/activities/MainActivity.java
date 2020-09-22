package io.awesome.gagtube.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AppInterstitialAd;
import io.awesome.gagtube.base.BaseActivity;
import io.awesome.gagtube.fragments.BackPressable;
import io.awesome.gagtube.fragments.MainFragment;
import io.awesome.gagtube.fragments.detail.VideoDetailFragment;
import io.awesome.gagtube.report.ErrorActivity;
import io.awesome.gagtube.util.Constants;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.PermissionHelper;
import io.awesome.gagtube.util.StateSaver;
import io.awesome.gagtube.util.ThemeHelper;
import io.awesome.gagtube.util.dialog.ExitAppDialog;

public class MainActivity extends BaseActivity {
	
	// Activity's LifeCycle
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		setTheme(ThemeHelper.getSettingsThemeStyle(this));
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			initFragments();
		}
		setSupportActionBar(findViewById(R.id.toolbar));
		
		// init InterstitialAd
		AppInterstitialAd.getInstance().init(this);
	}
	
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		if (!isChangingConfigurations()) {
			StateSaver.clearStateFiles();
		}
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (sharedPreferences.getBoolean(Constants.KEY_THEME_CHANGE, false)) {
			sharedPreferences.edit().putBoolean(Constants.KEY_THEME_CHANGE, false).apply();
			NavigationHelper.recreateActivity(this);
		}
		
		if (sharedPreferences.getBoolean(Constants.KEY_CONTENT_CHANGE, false)) {
			sharedPreferences.edit().putBoolean(Constants.KEY_CONTENT_CHANGE, false).apply();
			NavigationHelper.recreateActivity(this);
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		
		super.onNewIntent(intent);
		
		handleIntent(intent);
	}
	
	@Override
	public void onBackPressed() {
		
		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
		// If current fragment implements BackPressable (i.e. can/wanna handle back press) delegate the back press to it
		if (fragment instanceof BackPressable) {
			if (((BackPressable) fragment).onBackPressed()) return;
		}
		
		// if has only fragment in activity
		if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
			// if fragment is instanceof MainFragment
			if (fragment instanceof MainFragment) {
				// show dialog exit the app
				ExitAppDialog.newInstance(this::finish).show(getSupportFragmentManager(), "ExitAppDialog");
			} else {
				// else finish if other fragments
				finish();
			}
		}
		else super.onBackPressed();
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		
		for (int i : grantResults) {
			if (i == PackageManager.PERMISSION_DENIED) {
				return;
			}
		}
		
		switch (requestCode) {
			case PermissionHelper.DOWNLOADS_REQUEST_CODE:
				NavigationHelper.openDownloads(this);
				break;
			case PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE:
				Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
				if (fragment instanceof VideoDetailFragment) {
					((VideoDetailFragment) fragment).openDownloadDialog();
				}
				break;
		}
	}
	
	private void onHomeButtonPressed() {
		
		// If search fragment wasn't found in the backstack...
		if (!NavigationHelper.hasSearchFragmentInBackstack(getSupportFragmentManager())) {
			// go to the main fragment
			NavigationHelper.gotoMainFragment(getSupportFragmentManager());
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == android.R.id.home) {
			onHomeButtonPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Init
	private void initFragments() {
		
		StateSaver.clearStateFiles();
		
		if (getIntent() != null && getIntent().hasExtra(Constants.KEY_LINK_TYPE)) {
			handleIntent(getIntent());
		}
		else {
			NavigationHelper.gotoMainFragment(getSupportFragmentManager());
		}
	}
	
	private void handleIntent(Intent intent) {
		
		try {
			if (intent.hasExtra(Constants.KEY_LINK_TYPE)) {
				
				String url = intent.getStringExtra(Constants.KEY_URL);
				int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
				String title = intent.getStringExtra(Constants.KEY_TITLE);
				String thumbnailUrl = intent.getStringExtra(Constants.KEY_THUMBNAIL_URL);
				
				StreamInfoItem infoItem = new StreamInfoItem(0, url, title, StreamType.VIDEO_STREAM);
				infoItem.setThumbnailUrl(thumbnailUrl);
				
				if (intent.getSerializableExtra(Constants.KEY_LINK_TYPE) == StreamingService.LinkType.STREAM) {
					
					NavigationHelper.openVideoDetailFragment(infoItem, getSupportFragmentManager(), serviceId, url, title, false);
				}
			}
			else {
				NavigationHelper.gotoMainFragment(getSupportFragmentManager());
			}
		}
		catch (Exception ex) {
			ErrorActivity.reportUiError(this, ex);
		}
	}
}
