package io.awesome.gagtube.fragments.discover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.awesome.gagtube.R;
import io.awesome.gagtube.base.BaseFragment;
import io.awesome.gagtube.fragments.MainFragment;
import io.awesome.gagtube.fragments.discover.adapter.TopViewPagerAdapter;
import io.awesome.gagtube.util.NavigationHelper;

public class DiscoverFragment extends BaseFragment {
	
	@BindView(R.id.tab_layout) TabLayout tabLayout;
	@BindView(R.id.view_pager) ViewPager viewPager;
	@BindView(R.id.fab_play) ExtendedFloatingActionButton fab;
	private TopViewPagerAdapter adapter;
	
	public static DiscoverFragment getInstance() {
		return new DiscoverFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// TopViewPagerAdapter
		adapter = new TopViewPagerAdapter(getChildFragmentManager(), activity);
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.discover_fragment, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		super.initViews(rootView, savedInstanceState);
		initAdapter();
	}
	
	private void initAdapter() {
		// set adapter to viewPager
		viewPager.setOffscreenPageLimit(2);
		viewPager.setAdapter(adapter);
		// setup tabLayout with viewPager
		tabLayout.setupWithViewPager(viewPager);
	}
	
	@OnClick(R.id.action_search)
	void onSearch() {
		MainFragment fragment = (MainFragment) getParentFragment();
		if (fragment != null) {
			fragment.onSearch();
		}
	}
	
	@OnClick(R.id.action_settings)
	void onSettings() {
		// open Settings
		NavigationHelper.openSettings(getContext());
	}
	
	@OnClick(R.id.fab_play)
	void playAll() {
		TopFragment fragment = (TopFragment) adapter.instantiateItem(viewPager, viewPager.getCurrentItem());
		fragment.playAll();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
