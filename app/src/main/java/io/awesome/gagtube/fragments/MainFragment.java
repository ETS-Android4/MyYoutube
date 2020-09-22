package io.awesome.gagtube.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.awesome.gagtube.R;
import io.awesome.gagtube.base.BaseFragment;
import io.awesome.gagtube.fragments.discover.DiscoverFragment;
import io.awesome.gagtube.fragments.list.main.TrendingFragment;
import io.awesome.gagtube.library.LibraryFragment;
import io.awesome.gagtube.util.Constants;
import io.awesome.gagtube.util.DateTimeUtils;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.ServiceHelper;
import io.awesome.gagtube.util.SharedPrefsHelper;
import io.awesome.gagtube.util.ThemeHelper;
import io.awesome.gagtube.util.dialog.DialogUtils;

public class MainFragment extends BaseFragment {
	
	@BindView(R.id.bottom_navigation)
	AHBottomNavigation mBottomNavigation;
	private FragmentManager fragmentManager;
	private Fragment fragmentActive;
	private Fragment trendingFragment;
	private Fragment discoverFragment;
	private Fragment libraryFragment;
	private String currentFragmentTag = "";
	private static final String CURRENT_TAG_KEY = "CURRENT_TAG";
	private static final String TRENDING_TAG = "TRENDING_TAG";
	private static final String DISCOVER_TAG = "DISCOVER_TAG";
	private static final String LIBRARY_TAG = "LIBRARY_TAG";
	
	private static final String HOME_PAGE = "Trending";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		// attach fragments
		attachFragments(savedInstanceState);
		
		setUpBottomNavigation();
		
		prepareRatingAppDialog2Days();
		prepareRatingAppDialog5Days();
	}
	
	private void setUpBottomNavigation() {
		
		AHBottomNavigationAdapter navigationAdapter = new AHBottomNavigationAdapter(activity, R.menu.menu_navigation);
		navigationAdapter.setupWithBottomNavigation(mBottomNavigation);
		
		mBottomNavigation.setBehaviorTranslationEnabled(false);
		mBottomNavigation.setTranslucentNavigationEnabled(false);
		
		// Force to tint the drawable (useful for font with icon for example)
		mBottomNavigation.setForceTint(true);
		// always show title and icon
		mBottomNavigation.setTitleState(AHBottomNavigation.TitleState.SHOW_WHEN_ACTIVE);
		
		// Change colors
		mBottomNavigation.setAccentColor(ThemeHelper.isLightThemeSelected(getContext()) ? ContextCompat.getColor(activity, R.color.light_bottom_navigation_accent_color) : ContextCompat.getColor(activity, R.color.white));
		mBottomNavigation.setDefaultBackgroundColor(ThemeHelper.isLightThemeSelected(getContext()) ? ContextCompat.getColor(activity, R.color.light_bottom_navigation_background_color) : ContextCompat.getColor(activity, R.color.dark_bottom_navigation_background_color));
	}
	
	private void attachFragments(Bundle savedInstanceState) {
		
		// get child fragment manager
		fragmentManager = getChildFragmentManager();
		
		// if FragmentManager contain fragments already
		if (fragmentManager.getFragments().size() > 0) {
			Stream.of(fragmentManager.getFragments()).forEach(fragment -> {
				if (fragment instanceof TrendingFragment) {
					trendingFragment = fragment;
				}
				else if (fragment instanceof DiscoverFragment) {
					discoverFragment = fragment;
				}
				else if (fragment instanceof LibraryFragment) {
					libraryFragment = fragment;
				}
			});
			// set currentFragment by fragment has been saved instance state
			if (savedInstanceState != null) {
				currentFragmentTag = savedInstanceState.getString(CURRENT_TAG_KEY);
				fragmentActive = fragmentManager.findFragmentByTag(currentFragmentTag);
			}
		}
		// create fragments
		else {
			if (trendingFragment == null) {
				trendingFragment = trendingFragment();
				fragmentActive = trendingFragment;
				// add to fragment manager
				fragmentManager.beginTransaction()
						.add(R.id.frame_container, trendingFragment, TRENDING_TAG)
						.show(trendingFragment)
						.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
						.commit();
			}
			if (discoverFragment == null) {
				discoverFragment = DiscoverFragment.getInstance();
				// add to fragment manager
				fragmentManager.beginTransaction()
						.add(R.id.frame_container, discoverFragment, DISCOVER_TAG)
						.hide(discoverFragment)
						.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
						.commit();
			}
			if (libraryFragment == null) {
				libraryFragment = LibraryFragment.getInstance();
				// add to fragment manager
				fragmentManager.beginTransaction()
						.add(R.id.frame_container, libraryFragment, LIBRARY_TAG)
						.hide(libraryFragment)
						.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
						.commit();
			}
		}
		
		// onTabSelected listener
		mBottomNavigation.setOnTabSelectedListener((position, wasSelected) -> {
			
			switch (position) {
				
				case 0:
					fragmentManager.beginTransaction().hide(fragmentActive).show(trendingFragment).commit();
					fragmentActive = trendingFragment;
					return true;
				
				case 1:
					fragmentManager.beginTransaction().hide(fragmentActive).show(discoverFragment).commit();
					fragmentActive = discoverFragment;
					return true;
				
				case 2:
					fragmentManager.beginTransaction().hide(fragmentActive).show(libraryFragment).commit();
					fragmentActive = libraryFragment;
					return true;
			}
			return false;
		});
	}
	
	private Fragment trendingFragment() {
		
		TrendingFragment trendingFragment = TrendingFragment.getInstance(Constants.YOUTUBE_SERVICE_ID, HOME_PAGE);
		trendingFragment.useAsFrontPage(true);
		return trendingFragment;
	}
	
	public void onSearch() {
		// open SearchFragment
		NavigationHelper.openSearchFragment(getFragmentManager(), ServiceHelper.getSelectedServiceId(activity), "");
	}
	
	private void prepareRatingAppDialog2Days() {
		// installed app more than 2 days
		boolean showedRateAfter2Days = SharedPrefsHelper.getBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_RATING_2_DAYS.name());
		if (!showedRateAfter2Days && DateTimeUtils.isInstalled2Days(activity)) {
			SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_RATING_2_DAYS.name(), true);
			DialogUtils.showEnjoyAppDialog(activity,
										   // positive
										   (dialog, which) -> {
											   // dismiss dialog
											   dialog.dismiss();
											   // should show after installed 5 days
											   SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_RATING_5_DAYS.name(), true);
											   // show dialog ask to rate
											   DialogUtils.showAskRatingAppDialog(activity,
																				  // positive
																				  (dialog1, which1) -> {
																					  // open play store
																					  NavigationHelper.openGooglePlayStore(activity, activity.getPackageName());
																					  // dismiss dialog
																					  dialog1.dismiss();
																				  },
																				  // negative
																				  (dialog1, which1) -> {
																					  // dismiss dialog
																					  dialog1.dismiss();
																				  });
										   },
										   // negative
										   (dialog, which) -> {
											   // dismiss dialog
											   dialog.dismiss();
											   // should show after installed 5 days
											   SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_RATING_5_DAYS.name(), true);
											   // show dialog feedback
											   DialogUtils.showFeedBackDialog(activity,
																			  // positive
																			  (dialog2, which2) -> {
																				  // open email app
																				  NavigationHelper.composeEmail(activity, getString(R.string.app_name) + " Android Feedback");
																				  // dismiss dialog
																				  dialog2.dismiss();
																			  },
																			  // negative
																			  (dialog2, which2) -> {
																				  // dismiss dialog
																				  dialog2.dismiss();
																			  });
										   });
		}
	}
	
	private void prepareRatingAppDialog5Days() {
		// installed app more than 5 days
		boolean showedRateAfter5Days = SharedPrefsHelper.getBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_RATING_5_DAYS.name());
		if (showedRateAfter5Days && DateTimeUtils.isInstalled5Days(activity)) {
			SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_RATING_5_DAYS.name(), false);
			DialogUtils.showEnjoyAppDialog(activity,
										   // positive
										   (dialog, which) -> {
											   // dismiss dialog
											   dialog.dismiss();
											   // show dialog ask to rate
											   DialogUtils.showAskRatingAppDialog(activity,
																				  // positive
																				  (dialog1, which1) -> {
																					  // open play store
																					  NavigationHelper.openGooglePlayStore(activity, activity.getPackageName());
																					  // dismiss dialog
																					  dialog1.dismiss();
																				  },
																				  // negative
																				  (dialog1, which1) -> {
																					  // dismiss dialog
																					  dialog1.dismiss();
																				  });
										   },
										   // negative
										   (dialog, which) -> {
											   // dismiss dialog
											   dialog.dismiss();
											   // show dialog feedback
											   DialogUtils.showFeedBackDialog(activity,
																			  // positive
																			  (dialog2, which2) -> {
																				  // open email app
																				  NavigationHelper.composeEmail(activity, getString(R.string.app_name) + " Android Feedback");
																				  // dismiss dialog
																				  dialog2.dismiss();
																			  },
																			  // negative
																			  (dialog2, which2) -> {
																				  // dismiss dialog
																				  dialog2.dismiss();
																			  });
										   });
		}
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		// save tag of current fragment to bundle
		outState.putString(CURRENT_TAG_KEY, fragmentActive != null ? fragmentActive.getTag() : currentFragmentTag);
	}
	
	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// restore tag
		currentFragmentTag = savedInstanceState.getString(CURRENT_TAG_KEY);
	}
}
