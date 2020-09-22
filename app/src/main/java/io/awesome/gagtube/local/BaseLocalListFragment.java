package io.awesome.gagtube.local;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.R;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.fragments.BaseStateFragment;
import io.awesome.gagtube.fragments.list.ListViewContract;
import io.awesome.gagtube.util.AnimationUtils;

/**
 * This fragment is design to be used with persistent data such as
 * {@link LocalItem}, and does not cache the data contained
 * in the list adapter to avoid extra writes when the it exits or re-enters its lifecycle.
 * <p>
 * This fragment destroys its adapter and views when {@link Fragment#onDestroyView()} is
 * called and is memory efficient when in backstack.
 */
public abstract class BaseLocalListFragment<I, N> extends BaseStateFragment<I> implements ListViewContract<I, N> {
	
	// Views
	protected View headerRootView;
	protected View footerRootView;
	
	protected LocalItemListAdapter itemListAdapter;
	protected RecyclerView itemsList;
	
	// Lifecycle - Creation
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	// Lifecycle - View
	protected View getListHeader() {
		return null;
	}
	
	protected View getListFooter() {
		return activity.getLayoutInflater().inflate(R.layout.pignate_footer, itemsList, false);
	}
	
	protected RecyclerView.LayoutManager getListLayoutManager() {
		return new LinearLayoutManager(activity);
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		super.initViews(rootView, savedInstanceState);
		
		itemsList = rootView.findViewById(R.id.items_list);
		itemsList.setLayoutManager(getListLayoutManager());
		
		itemListAdapter = new LocalItemListAdapter(activity, true);
		itemListAdapter.setHeader(headerRootView = getListHeader());
		itemListAdapter.setFooter(footerRootView = getListFooter());
		
		itemsList.setAdapter(itemListAdapter);
	}
	
	@Override
	protected void initListeners() {
		super.initListeners();
	}
	
	// Lifecycle - Menu
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		final ActionBar supportActionBar = activity.getSupportActionBar();
		if (supportActionBar == null) return;
		
		supportActionBar.setDisplayShowTitleEnabled(true);
	}
	
	// Lifecycle - Destruction
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		itemsList = null;
		itemListAdapter = null;
	}
	
	// Contract
	@Override
	public void startLoading(boolean forceLoad) {
		super.startLoading(forceLoad);
		resetFragment();
	}
	
	@Override
	public void showLoading() {
		super.showLoading();
		if (itemsList != null) AnimationUtils.animateView(itemsList, false, 200);
		if (headerRootView != null) AnimationUtils.animateView(headerRootView, false, 200);
	}
	
	@Override
	public void hideLoading() {
		super.hideLoading();
		if (itemsList != null) AnimationUtils.animateView(itemsList, true, 200);
		if (headerRootView != null) AnimationUtils.animateView(headerRootView, true, 200);
	}
	
	@Override
	public void showError(String message, boolean showRetryButton) {
		super.showError(message, showRetryButton);
		showListFooter(false);
		
		if (itemsList != null) AnimationUtils.animateView(itemsList, false, 200);
		if (headerRootView != null) AnimationUtils.animateView(headerRootView, false, 200);
	}
	
	@Override
	public void showEmptyState() {
		super.showEmptyState();
		showListFooter(false);
	}
	
	@Override
	public void showListFooter(final boolean show) {
		if (itemsList == null) return;
		itemsList.post(() -> {
			if (itemListAdapter != null) itemListAdapter.showFooter(show);
		});
	}
	
	@Override
	public void handleNextItems(N result) {
		isLoading.set(false);
	}
	
	// Error handling
	protected void resetFragment() {
		if (itemListAdapter != null) itemListAdapter.clearStreamItemList();
	}
	
	@Override
	protected boolean onError(Throwable exception) {
		resetFragment();
		return super.onError(exception);
	}
}
