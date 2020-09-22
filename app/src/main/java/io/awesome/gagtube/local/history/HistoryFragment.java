package io.awesome.gagtube.local.history;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AppInterstitialAd;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.database.history.model.SearchHistoryEntry;
import io.awesome.gagtube.database.stream.StreamStatisticsEntry;
import io.awesome.gagtube.fragments.list.search.SuggestionItem;
import io.awesome.gagtube.fragments.list.search.SuggestionListAdapter2;
import io.awesome.gagtube.local.BaseLocalListFragment;
import io.awesome.gagtube.player.playqueue.PlayQueue;
import io.awesome.gagtube.player.playqueue.SinglePlayQueue;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.OnClickGesture;
import io.awesome.gagtube.util.ServiceHelper;
import io.awesome.gagtube.util.SharedUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class HistoryFragment extends BaseLocalListFragment<List<StreamStatisticsEntry>, Void> implements TabLayout.OnTabSelectedListener, SuggestionListAdapter2.OnSuggestionItemSelected {
	
	@BindView(R.id.default_toolbar) Toolbar toolbar;
	@BindView(R.id.tab_layout) TabLayout tabLayout;
	@BindView(R.id.search_history_list) RecyclerView searchHistoryList;
	@BindView(R.id.empty_message) TextView emptyMessage;
	@BindView(R.id.fab_play) ExtendedFloatingActionButton fabPlay;
	@BindView(R.id.loading_progress_bar) ProgressBar progressBar;
	@BindView(R.id.adView) AdView adView;
	
	// search history
	private SuggestionListAdapter2 suggestionListAdapter2;
	
	// Used for independent events
	private Subscription databaseSubscription;
	private HistoryRecordManager recordManager;
	
	public enum StatisticSortMode {
		MOST_PLAYED,
		RECENTLY_PLAYED,
		SEARCH_HISTORY
	}
	
	private StatisticSortMode sortMode = StatisticSortMode.MOST_PLAYED;
	
	protected List<StreamStatisticsEntry> processResult(List<StreamStatisticsEntry> results) {
		
		switch (sortMode) {
			
			case MOST_PLAYED:
				return Stream.of(results).filter(item -> item.streamType == StreamType.VIDEO_STREAM).sorted((left, right) -> Long.compare(right.watchCount, left.watchCount)).toList();
			
			case RECENTLY_PLAYED:
				return Stream.of(results).filter(item1 -> item1.streamType == StreamType.VIDEO_STREAM).sorted((left1, right1) -> right1.latestAccessDate.compareTo(left1.latestAccessDate)).toList();
			
			case SEARCH_HISTORY:
			default:
				return null;
		}
	}
	
	// Fragment LifeCycle - Creation
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		recordManager = new HistoryRecordManager(activity);
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_history, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	// Fragment LifeCycle - Views
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		// toolbar
		activity.setSupportActionBar(toolbar);
		toolbar.setTitle(R.string.title_activity_history);
		
		// history adapter
		searchHistoryList.setLayoutManager(getListLayoutManager());
		suggestionListAdapter2 = new SuggestionListAdapter2(this);
		searchHistoryList.setAdapter(suggestionListAdapter2);
		
		// selected tab
		if (sortMode == StatisticSortMode.MOST_PLAYED) {
			tabLayout.getTabAt(0).select();
		}
		else if (sortMode == StatisticSortMode.RECENTLY_PLAYED) {
			tabLayout.getTabAt(1).select();
		}
		else if (sortMode == StatisticSortMode.SEARCH_HISTORY) {
			tabLayout.getTabAt(2).select();
		}
		
		// onTabSelected listener
		tabLayout.addOnTabSelectedListener(this);
		
		// init InterstitialAd
		AppInterstitialAd.getInstance().init(activity);
		
		// show ad
		showBannerAd();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == android.R.id.home) {
			
			activity.onBackPressed();
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void initListeners() {
		
		super.initListeners();
		
		itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
			
			@Override
			public void selected(LocalItem selectedItem) {
				
				if (selectedItem instanceof StreamStatisticsEntry) {
					StreamInfoItem infoItem = ((StreamStatisticsEntry) selectedItem).toStreamInfoItem();
					NavigationHelper.openVideoDetailFragment(infoItem, activity.getSupportFragmentManager(),
															 infoItem.getServiceId(),
															 infoItem.getUrl(),
															 infoItem.getName());
				}
			}
			
			@Override
			public void more(LocalItem selectedItem, View view) {
				if (selectedItem instanceof StreamStatisticsEntry) {
					showPopupMenu((StreamStatisticsEntry) selectedItem, view);
				}
			}
		});
	}
	
	// Fragment LifeCycle - Loading
	@Override
	public void startLoading(boolean forceLoad) {
		
		super.startLoading(forceLoad);
		
		// for empty message
		emptyMessage.setText(getErrorMessageRes());
		
		// search history
		if (sortMode == StatisticSortMode.SEARCH_HISTORY) {
			recordManager.getRelatedSearches("", 3, 100)
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(getHistorySearchObserver());
		}
		// video played history
		else {
			recordManager.getStreamStatistics()
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(getHistoryObserver());
		}
	}
	
	private int getErrorMessageRes() {
		
		switch (tabLayout.getSelectedTabPosition()) {
			
			case 0:
			case 1:
				return R.string.no_videos;
			
			case 2:
				return R.string.no_keywords;
			
			default:
				return R.string.no_videos;
		}
	}
	
	// Fragment LifeCycle - Destruction
	@Override
	public void onPause() {
		if (adView != null) {
			adView.pause();
		}
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (adView != null) {
			adView.resume();
		}
		showBannerAd();
	}
	
	@Override
	public void onDestroyView() {
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroyView();
		
		if (itemListAdapter != null) itemListAdapter.unsetSelectedListener();
		
		if (databaseSubscription != null) databaseSubscription.cancel();
		databaseSubscription = null;
	}
	
	@Override
	public void onDestroy() {
		
		super.onDestroy();
		
		recordManager = null;
	}
	
	// Statistics Loader
	private Subscriber<List<StreamStatisticsEntry>> getHistoryObserver() {
		
		return new Subscriber<List<StreamStatisticsEntry>>() {
			
			@Override
			public void onSubscribe(Subscription s) {
				
				showLoading();
				
				// hide searchHistoryList
				searchHistoryList.setVisibility(View.GONE);
				// show itemList
				itemsList.setVisibility(View.VISIBLE);

				if (databaseSubscription != null) databaseSubscription.cancel();
				databaseSubscription = s;
				databaseSubscription.request(1);
			}
			
			@Override
			public void onNext(List<StreamStatisticsEntry> streams) {
				
				handleResult(streams);
				if (databaseSubscription != null) databaseSubscription.request(1);
			}
			
			@Override
			public void onError(Throwable exception) {
				HistoryFragment.this.onError(exception);
			}
			
			@Override
			public void onComplete() {
			}
		};
	}
	
	// Statistics Loader
	private Subscriber<List<SearchHistoryEntry>> getHistorySearchObserver() {
		
		return new Subscriber<List<SearchHistoryEntry>>() {
			
			@Override
			public void onSubscribe(Subscription s) {
				
				// show loading
				progressBar.setVisibility(View.VISIBLE);
				
				// show searchHistoryList
				searchHistoryList.setVisibility(View.VISIBLE);
				// hide itemList
				itemsList.setVisibility(View.GONE);
				// hide fab
				fabPlay.hide();
				
				if (databaseSubscription != null) databaseSubscription.cancel();
				databaseSubscription = s;
				databaseSubscription.request(1);
			}
			
			@Override
			public void onNext(List<SearchHistoryEntry> searchHistoryEntries) {
				
				// hide loading
				progressBar.setVisibility(View.GONE);
				
				// handle result
				if (suggestionListAdapter2 == null) return;
				
				suggestionListAdapter2.clearItems();
				if (searchHistoryEntries.isEmpty()) {
					showEmptyState();
					return;
				}
				
				List<SuggestionItem> suggestionItems = Stream.of(searchHistoryEntries)
						// map to SuggestionItem
						.map(searchHistoryEntry -> new SuggestionItem(true, searchHistoryEntry.getSearch()))
						// toList
						.toList();
				
				suggestionListAdapter2.setItems(suggestionItems);
				
				if (databaseSubscription != null) databaseSubscription.request(1);
			}
			
			@Override
			public void onError(Throwable exception) {
				
				// hide loading
				progressBar.setVisibility(View.GONE);
				
				HistoryFragment.this.onError(exception);
			}
			
			@Override
			public void onComplete() {
			}
		};
	}
	
	@Override
	public void handleResult(@NonNull List<StreamStatisticsEntry> results) {
		
		super.handleResult(results);
		if (itemListAdapter == null) return;
		
		itemListAdapter.clearStreamItemList();
		
		// if results empty don't need to do anything
		if (results.isEmpty()) {
			showEmptyState();
			return;
		}
		
		itemListAdapter.addItems(processResult(results));
		
		hideLoading();
	}
	
	// Fragment Error Handling
	@Override
	protected void resetFragment() {
		
		super.resetFragment();
		
		if (databaseSubscription != null) databaseSubscription.cancel();
	}
	
	@Override
	protected boolean onError(Throwable exception) {
		
		if (super.onError(exception)) return true;
		
		onUnrecoverableError(exception, UserAction.SOMETHING_ELSE, "none", "History Statistics", R.string.general_error);
		return true;
	}
	
	// Utils
	private void showPopupMenu(final StreamStatisticsEntry streamStatisticsEntry, View view) {
		
		final Context context = getContext();
		if (context == null || context.getResources() == null || getActivity() == null) return;
		
		PopupMenu popup = new PopupMenu(getContext(), view, Gravity.END, 0, R.style.mPopupMenu);
		popup.getMenuInflater().inflate(R.menu.menu_popup_history, popup.getMenu());
		popup.show();
		
		popup.setOnMenuItemClickListener(item -> {
			int id = item.getItemId();
			final int index = Math.max(itemListAdapter.getItemsList().indexOf(streamStatisticsEntry), 0);
			switch (id) {
				
				case R.id.action_play:
					AppInterstitialAd.getInstance().showInterstitialAd(() -> NavigationHelper.playOnMainPlayer(context, getPlayQueue(index)));
					break;
				
				case R.id.action_share:
					SharedUtils.shareUrl(getContext(), streamStatisticsEntry.toStreamInfoItem().getName(), streamStatisticsEntry.toStreamInfoItem().getUrl());
					break;
				
				case R.id.action_delete:
					deleteEntry(index);
					if (itemListAdapter.getItemsList().isEmpty()) showEmptyState();
					break;
			}
			return true;
		});
	}
	
	@SuppressLint("CheckResult")
	private void deleteEntry(final int index) {
		
		final LocalItem infoItem = (LocalItem) itemListAdapter.getItemsList().get(index);
		if (infoItem instanceof StreamStatisticsEntry) {
			
			final StreamStatisticsEntry entry = (StreamStatisticsEntry) infoItem;
			recordManager.deleteStreamHistory(entry.streamId)
					.observeOn(AndroidSchedulers.mainThread()).subscribe(
					// onNext
					ignored -> Toast.makeText(activity, R.string.msg_delete_successfully, Toast.LENGTH_SHORT).show(),
					// onError
					throwable -> showSnackBarError(throwable, UserAction.DELETE_FROM_HISTORY, "none", "Deleting item failed", R.string.general_error));
			
		}
	}
	
	private PlayQueue getPlayQueue() {
		return getPlayQueue(0);
	}
	
	private PlayQueue getPlayQueue(final int index) {
		if (itemListAdapter == null) {
			return new SinglePlayQueue(Collections.emptyList(), 0);
		}
		
		final List<Object> infoItems = itemListAdapter.getItemsList();
		List<StreamInfoItem> streamInfoItems = new ArrayList<>(infoItems.size());
		for (final Object item : infoItems) {
			if (item instanceof StreamStatisticsEntry) {
				streamInfoItems.add(((StreamStatisticsEntry) item).toStreamInfoItem());
			}
		}
		return new SinglePlayQueue(streamInfoItems, index);
	}
	
	@Override
	public void onTabSelected(TabLayout.Tab tab) {
		
		onTab(tab);
	}
	
	@Override
	public void onTabUnselected(TabLayout.Tab tab) {
		// unimplemented
	}
	
	@Override
	public void onTabReselected(TabLayout.Tab tab) {
		// unimplemented
	}
	
	private void onTab(TabLayout.Tab tab) {
		
		switch (tab.getPosition()) {
			
			case 0:
				sortMode = StatisticSortMode.MOST_PLAYED;
				// clear search history
				if (suggestionListAdapter2 != null) suggestionListAdapter2.clearItems();
				// loading from history
				startLoading(true);
				// show fab
				fabPlay.show();
				break;
			
			case 1:
				sortMode = StatisticSortMode.RECENTLY_PLAYED;
				// clear search history
				if (suggestionListAdapter2 != null) suggestionListAdapter2.clearItems();
				// loading from history
				startLoading(true);
				// show fab
				fabPlay.show();
				break;
			
			case 2:
				sortMode = StatisticSortMode.SEARCH_HISTORY;
				// clear itemList
				if (itemListAdapter != null) itemListAdapter.clearStreamItemList();
				// loading from history
				startLoading(true);
				// hide fab
				fabPlay.hide();
				break;
		}
	}
	
	@Override
	public void onSuggestionItemRemoved(SuggestionItem item) {
		
		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.dialog_warning_title)
				.setMessage(R.string.delete_item_search_history)
				.setCancelable(true)
				.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
				.setPositiveButton(R.string.delete, (dialog, which) -> recordManager.deleteSearchHistory(item.query)
						.observeOn(AndroidSchedulers.mainThread()).subscribe(
								// onNext
								ignored -> Toast.makeText(activity, R.string.msg_delete_successfully, Toast.LENGTH_SHORT).show(),
								// onError
								throwable -> showSnackBarError(throwable,
															   UserAction.DELETE_FROM_HISTORY, "none",
															   "Deleting item failed", R.string.general_error)
						))
				.show();
	}
	
	@Override
	public void onSuggestionItemClicked(SuggestionItem item) {
		
		// open SearchFragment
		NavigationHelper.openSearchFragment(getFragmentManager(), ServiceHelper.getSelectedServiceId(activity), item.query);
	}
	
	@OnClick(R.id.fab_play)
	void onFabPlay() {
		
		if (!itemListAdapter.getItemsList().isEmpty()) {
			AppInterstitialAd.getInstance().showInterstitialAd(() -> NavigationHelper.playOnPopupPlayer(activity, getPlayQueue()));
		}
	}
	
	private void showBannerAd() {
		AdRequest adRequest = new AdRequest.Builder().build();
		adView.setAdListener(new AdListener() {
			
			@Override
			public void onAdLoaded() {
				// Code to be executed when an ad finishes loading.
				adView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAdFailedToLoad(LoadAdError loadAdError) {
				// Code to be executed when an ad request fails.
				adView.setVisibility(View.GONE);
			}
		});
		adView.loadAd(adRequest);
	}
}

