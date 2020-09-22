package io.awesome.gagtube.local.subscription;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.State;
import io.awesome.gagtube.R;
import io.awesome.gagtube.database.subscription.SubscriptionEntity;
import io.awesome.gagtube.fragments.BaseStateFragment;
import io.awesome.gagtube.info_list.InfoListAdapter;
import io.awesome.gagtube.report.ErrorActivity;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.AnimationUtils;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.OnClickGesture;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SubscriptionFragment extends BaseStateFragment<List<SubscriptionEntity>> {
	
	@BindView(R.id.default_toolbar) Toolbar toolbar;
	@BindView(R.id.empty_message) TextView emptyMessage;

	
	private RecyclerView itemsList;
	@State
	protected Parcelable itemsListState;
	private InfoListAdapter infoListAdapter;
	
	@State
	protected Parcelable importExportOptionsState;
	
	private CompositeDisposable disposables = new CompositeDisposable();
	private SubscriptionService subscriptionService;
	
	// Fragment LifeCycle
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onAttach(@NotNull Context context) {
		
		super.onAttach(context);
		
		infoListAdapter = new InfoListAdapter(activity, true);
		subscriptionService = SubscriptionService.getInstance(activity);
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_subscription, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		
		super.onPause();
		
		if (itemsList.getLayoutManager() != null) {
			itemsListState = itemsList.getLayoutManager().onSaveInstanceState();
		}
	}
	
	@Override
	public void onDestroyView() {
		
		if (disposables != null) disposables.clear();
		
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {

		if (disposables != null) disposables.dispose();
		disposables = null;
		subscriptionService = null;
		
		super.onDestroy();
	}
	
	// Menu
	@Override
	public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
		
		super.onCreateOptionsMenu(menu, inflater);
		
		ActionBar supportActionBar = activity.getDelegate().getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setTitle(R.string.tab_subscriptions);
			supportActionBar.setDisplayShowTitleEnabled(true);
			supportActionBar.setDisplayShowTitleEnabled(true);
		}
		
		inflater.inflate(R.menu.menu_subscription, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == R.id.menu_what_news) {
			NavigationHelper.openWhatsNewFragment(activity.getSupportFragmentManager());
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Fragment Views
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		// toolbar
		activity.getDelegate().setSupportActionBar(toolbar);
		
		itemsList = rootView.findViewById(R.id.items_list);
		itemsList.setLayoutManager(new LinearLayoutManager(activity));
		
		infoListAdapter.useMiniItemVariants(true);
		itemsList.setAdapter(infoListAdapter);
		
		// for empty message
		emptyMessage.setText(R.string.empty_message_no_channel_subscription);

	}
	
	@Override
	protected void initListeners() {
		
		super.initListeners();
		
		// onClick listener
		infoListAdapter.setOnChannelSelectedListener(new OnClickGesture<ChannelInfoItem>() {
			
			@Override
			public void selected(ChannelInfoItem selectedItem) {
				
				try {
					// Requires the parent fragment to find holder for fragment replacement
					NavigationHelper.openChannelFragment(activity.getSupportFragmentManager(),
														 selectedItem.getServiceId(),
														 selectedItem.getUrl(),
														 selectedItem.getName());
				}
				catch (Exception e) {
					ErrorActivity.reportUiError(activity, e);
				}
			}
			
			@Override
			public void swipe(ChannelInfoItem selectedItem) {
				
				// unsubscribe channel
				unsubscribeChannel(selectedItem);
			}
		});
	}
	
	private void resetFragment() {
		
		if (disposables != null) disposables.clear();
		if (infoListAdapter != null) infoListAdapter.clearStreamItemList();
	}
	
	// Subscriptions Loader
	@Override
	public void startLoading(boolean forceLoad) {
		
		super.startLoading(forceLoad);
		
		resetFragment();
		
		subscriptionService.getSubscription().toObservable()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(getSubscriptionObserver());
	}
	
	private Observer<List<SubscriptionEntity>> getSubscriptionObserver() {
		
		return new Observer<List<SubscriptionEntity>>() {
			
			@Override
			public void onSubscribe(Disposable d) {
				
				showLoading();
				disposables.add(d);
			}
			
			@Override
			public void onNext(List<SubscriptionEntity> subscriptions) {
				handleResult(subscriptions);
			}
			
			@Override
			public void onError(Throwable exception) {
				SubscriptionFragment.this.onError(exception);
			}
			
			@Override
			public void onComplete() {
			}
		};
	}
	
	@Override
	public void handleResult(@NonNull List<SubscriptionEntity> result) {
		
		super.handleResult(result);
		
		infoListAdapter.clearStreamItemList();
		
		if (result.isEmpty()) {
			showEmptyState();
			return;
		}
		
		infoListAdapter.addInfoItemList(getSubscriptionItems(result));
		if (itemsListState != null && itemsList.getLayoutManager() != null) {
			itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
			itemsListState = null;
		}
		
		hideLoading();
	}
	
	private List<InfoItem> getSubscriptionItems(List<SubscriptionEntity> subscriptions) {
		
		List<InfoItem> items = new ArrayList<>();
		for (final SubscriptionEntity subscription : subscriptions) {
			items.add(subscription.toChannelInfoItem());
		}
		
		Collections.sort(items, (InfoItem o1, InfoItem o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
		return items;
	}
	
	private void unsubscribeChannel(ChannelInfoItem channelInfoItem) {
		
		subscriptionService.subscriptionTable()
				.getSubscription(channelInfoItem.getServiceId(), channelInfoItem.getUrl())
				.toObservable()
				.observeOn(Schedulers.io())
				.subscribe(getDeleteObserver());
	}
	
	private Observer<List<SubscriptionEntity>> getDeleteObserver(){
		
		return new Observer<List<SubscriptionEntity>>() {
		
			@Override
			public void onSubscribe(Disposable disposable) {
				disposables.add(disposable);
			}
			
			@Override
			public void onNext(List<SubscriptionEntity> subscriptionEntities) {
				subscriptionService.subscriptionTable().delete(subscriptionEntities);
			}
			
			@Override
			public void onError(Throwable exception) {
				SubscriptionFragment.this.onError(exception);
			}
			
			@Override
			public void onComplete() {
			}
		};
	}
	
	// Contract
	@Override
	public void showLoading() {
		
		super.showLoading();
		
		AnimationUtils.animateView(itemsList, false, 100);
	}
	
	@Override
	public void hideLoading() {
		
		super.hideLoading();
		
		AnimationUtils.animateView(itemsList, true, 200);
	}
	
	// Fragment Error Handling
	@Override
	protected boolean onError(Throwable exception) {
		
		resetFragment();
		if (super.onError(exception)) return true;
		
		onUnrecoverableError(exception, UserAction.SOMETHING_ELSE, "none", "Subscriptions", R.string.general_error);
		return true;
	}

}
