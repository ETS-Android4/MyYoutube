package io.awesome.gagtube.local.subscription;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import butterknife.ButterKnife;
import io.awesome.gagtube.R;
import io.awesome.gagtube.base.BaseActivity;
import io.awesome.gagtube.util.ThemeHelper;

public class SubscriptionActivity extends BaseActivity {
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		
		setTheme(ThemeHelper.getSettingsThemeStyle(this));
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.subscription_activity);
		ButterKnife.bind(this);
		
		init();
	}
	
	private void init() {
		
		// load HistoryFragment
		loadFragment(new SubscriptionFragment());
	}
	
	private void loadFragment(Fragment fragment) {
		
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_holder, fragment);
		transaction.commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		
		super.onBackPressed();
	}
}
