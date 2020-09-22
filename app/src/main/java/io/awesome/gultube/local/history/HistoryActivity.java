package io.awesome.gultube.local.history;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import io.awesome.gultube.R;
import io.awesome.gultube.base.BaseActivity;
import io.awesome.gultube.util.ThemeHelper;

import butterknife.ButterKnife;

public class HistoryActivity extends BaseActivity {
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		
		setTheme(ThemeHelper.getSettingsThemeStyle(this));
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.history_activity);
		ButterKnife.bind(this);
		
		// load HistoryFragment
		loadFragment(new HistoryFragment());
	}
	
	private void loadFragment(Fragment fragment) {
		
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_holder, fragment);
		transaction.commit();
	}
	
	@Override
	public void onBackPressed() {
		
		super.onBackPressed();
	}
}
