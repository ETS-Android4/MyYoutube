package io.awesome.gagtube.settings;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import butterknife.ButterKnife;
import io.awesome.gagtube.R;
import io.awesome.gagtube.base.BaseActivity;
import io.awesome.gagtube.util.ThemeHelper;

public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
	
	@Override
	protected void onCreate(Bundle savedInstanceBundle) {
		
		setTheme(ThemeHelper.getSettingsThemeStyle(this));
		super.onCreate(savedInstanceBundle);
		setContentView(R.layout.activity_settings);
		ButterKnife.bind(this);
		
		Toolbar toolbar = findViewById(R.id.default_toolbar);
		setSupportActionBar(toolbar);
		
		if (savedInstanceBundle == null) {
			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, new MainSettingsFragment()).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(R.string.settings);
		}
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == android.R.id.home) {
			
			// end here
			finish();
		}
		return true;
	}
	
	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference preference) {
		
		Fragment fragment = Fragment.instantiate(this, preference.getFragment(), preference.getExtras());
		getSupportFragmentManager().beginTransaction().setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out, R.animator.custom_fade_in, R.animator.custom_fade_out)
				.replace(R.id.fragment_holder, fragment)
				.addToBackStack(null)
				.commit();
		return true;
	}
}
