package io.awesome.gagtube.library;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AdUtils;
import io.awesome.gagtube.adsmanager.nativead.NativeAdStyle;
import io.awesome.gagtube.adsmanager.nativead.NativeAdView;
import io.awesome.gagtube.database.AppDatabase;
import io.awesome.gagtube.database.GAGTubeDatabase;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.database.playlist.PlaylistLocalItem;
import io.awesome.gagtube.database.playlist.PlaylistMetadataEntry;
import io.awesome.gagtube.database.playlist.model.PlaylistRemoteEntity;
import io.awesome.gagtube.fragments.MainFragment;
import io.awesome.gagtube.local.BaseLocalListFragment;
import io.awesome.gagtube.local.dialog.PlaylistCreationDialog;
import io.awesome.gagtube.local.history.HistoryActivity;
import io.awesome.gagtube.local.playlist.LocalPlaylistManager;
import io.awesome.gagtube.local.playlist.RemotePlaylistManager;
import io.awesome.gagtube.local.subscription.SubscriptionActivity;
import io.awesome.gagtube.report.ErrorActivity;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.OnClickGesture;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class LibraryFragment extends BaseLocalListFragment<List<PlaylistLocalItem>, Void> {
	
	@BindView(R.id.empty_message) TextView emptyMessage;
	@BindView(R.id.template_view) NativeAdView nativeAdView;
	@BindView(R.id.adView) AdView adView;
	
	private Subscription databaseSubscription;
	private CompositeDisposable disposables = new CompositeDisposable();
	private LocalPlaylistManager localPlaylistManager;
	private RemotePlaylistManager remotePlaylistManager;
	
	public static LibraryFragment getInstance() {
		return new LibraryFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		final AppDatabase database = GAGTubeDatabase.getInstance(activity);
		localPlaylistManager = new LocalPlaylistManager(database);
		remotePlaylistManager = new RemotePlaylistManager(database);
		disposables = new CompositeDisposable();
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.library_fragment, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		// for empty message
		emptyMessage.setText(R.string.no_playlists);
		
		// show ad
		showBannerAd();
	}
	
	@Override
	protected void initListeners() {
		
		super.initListeners();
		
		itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {
			
			@Override
			public void selected(LocalItem selectedItem) {
				
				try {
					// Requires the parent fragment to find holder for fragment replacement
					if (getParentFragment() == null) return;
					final FragmentManager fragmentManager = getParentFragment().getFragmentManager();
					
					if (selectedItem instanceof PlaylistMetadataEntry) {
						
						final PlaylistMetadataEntry entry = ((PlaylistMetadataEntry) selectedItem);
						NavigationHelper.openLocalPlaylistFragment(fragmentManager, entry.uid, entry.name);
						
					}
					else if (selectedItem instanceof PlaylistRemoteEntity) {
						
						final PlaylistRemoteEntity entry = ((PlaylistRemoteEntity) selectedItem);
						NavigationHelper.openPlaylistFragment(fragmentManager, entry.getServiceId(), entry.getUrl(), entry.getName());
					}
				}
				catch (Exception e) {
					ErrorActivity.reportUiError((AppCompatActivity) getActivity(), e);
				}
			}
			
			@Override
			public void more(LocalItem selectedItem, View view) {
				
				showPopupMenu(selectedItem, view);
			}
		});
	}
	
	protected void showPopupMenu(final LocalItem localItem, final View view) {
		
		final Context context = getContext();
		if (context == null || context.getResources() == null || getActivity() == null) return;
		
		PopupMenu popup = new PopupMenu(getContext(), view, Gravity.END, 0, R.style.mPopupMenu);
		popup.getMenuInflater().inflate(R.menu.menu_remove_playlist_bookmark, popup.getMenu());
		popup.show();
		
		popup.setOnMenuItemClickListener(item -> {
			
			int id = item.getItemId();
			if (id == R.id.action_remove) {
				
				if (localItem instanceof PlaylistMetadataEntry) {
					showLocalDeleteDialog((PlaylistMetadataEntry) localItem);
				}
				else if (localItem instanceof PlaylistRemoteEntity) {
					showRemoteDeleteDialog((PlaylistRemoteEntity) localItem);
				}
			}
			return true;
		});
	}
	
	// Utils
	private void showLocalDeleteDialog(final PlaylistMetadataEntry item) {
		showDeleteDialog(localPlaylistManager.deletePlaylist(item.uid));
	}
	
	private void showRemoteDeleteDialog(final PlaylistRemoteEntity item) {
		showDeleteDialog(remotePlaylistManager.deletePlaylist(item.getUid()));
	}
	
	private void showDeleteDialog(final Single<Integer> deleteReactor) {
		
		if (activity == null || disposables == null) return;
		
		new MaterialAlertDialogBuilder(activity)
				.setTitle(R.string.dialog_warning_title)
				.setMessage(R.string.delete_playlist_prompt)
				.setCancelable(true)
				.setPositiveButton(R.string.delete, (dialog, i) ->
						
						// delete
						disposables.add(deleteReactor.observeOn(AndroidSchedulers.mainThread()).subscribe(
								// onNext
								ignored -> Toast.makeText(activity, R.string.msg_delete_successfully, Toast.LENGTH_SHORT).show(),
								// onError
								this::onError))
				)
				.setNegativeButton(R.string.cancel, null)
				.show();
	}
	
	private static List<PlaylistLocalItem> merge(final List<PlaylistMetadataEntry> localPlaylists, final List<PlaylistRemoteEntity> remotePlaylists) {
		
		List<PlaylistLocalItem> items = new ArrayList<>(localPlaylists.size() + remotePlaylists.size());
		items.addAll(localPlaylists);
		items.addAll(remotePlaylists);
		
		return items;
	}
	
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// show ad
		if (adView != null) {
			adView.resume();
		}
		showNativeAd();
	}
	
	// Fragment LifeCycle - Loading
	@Override
	public void startLoading(boolean forceLoad) {
		
		super.startLoading(forceLoad);
		
		Flowable.combineLatest(localPlaylistManager.getPlaylists(), remotePlaylistManager.getPlaylists(), LibraryFragment::merge)
				.onBackpressureLatest()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(getPlaylistsSubscriber());
	}
	
	// Subscriptions Loader
	private Subscriber<List<PlaylistLocalItem>> getPlaylistsSubscriber() {
		
		return new Subscriber<List<PlaylistLocalItem>>() {
			
			@Override
			public void onSubscribe(Subscription s) {
				
				showLoading();
				if (databaseSubscription != null) databaseSubscription.cancel();
				databaseSubscription = s;
				databaseSubscription.request(1);
			}
			
			@Override
			public void onNext(List<PlaylistLocalItem> subscriptions) {
				
				handleResult(subscriptions);
				if (databaseSubscription != null) databaseSubscription.request(1);
			}
			
			@Override
			public void onError(Throwable exception) {
				LibraryFragment.this.onError(exception);
			}
			
			@Override
			public void onComplete() {
			}
		};
	}
	
	@Override
	public void handleResult(@NonNull List<PlaylistLocalItem> result) {
		
		super.handleResult(result);
		
		itemListAdapter.clearStreamItemList();
		
		if (!result.isEmpty()) {
			// set items to adapter
			itemListAdapter.addItems(result);
			hideLoading();
		}
		else {
			showEmptyState();
		}
	}
	
	// Fragment Error Handling
	@Override
	protected boolean onError(Throwable exception) {
		
		if (super.onError(exception)) return true;
		
		onUnrecoverableError(exception, UserAction.SOMETHING_ELSE, "none", "Library", R.string.general_error);
		return true;
	}
	
	@Override
	protected void resetFragment() {
		
		super.resetFragment();
		
		if (disposables != null) disposables.clear();
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
	
	private void showNativeAd() {
		
		// ad options
		VideoOptions videoOptions = new VideoOptions.Builder()
				.setStartMuted(true)
				.build();
		
		NativeAdOptions adOptions = new NativeAdOptions.Builder()
				.setVideoOptions(videoOptions)
				.build();
		
		AdLoader adLoader = new AdLoader.Builder(activity, AdUtils.getNativeAdId(activity))
				.forUnifiedNativeAd(unifiedNativeAd -> {
					
					// show the ad
					NativeAdStyle styles = new NativeAdStyle.Builder().build();
					nativeAdView.setStyles(styles);
					nativeAdView.setNativeAd(unifiedNativeAd);
				})
				.withAdListener(new AdListener() {
					
					@Override
					public void onAdFailedToLoad(LoadAdError loadAdError) {
						// gone
						nativeAdView.setVisibility(View.GONE);
					}
					
					@Override
					public void onAdLoaded() {
						
						super.onAdLoaded();
						
						// visible
						nativeAdView.setVisibility(View.VISIBLE);
					}
				})
				.withNativeAdOptions(adOptions)
				.build();
		
		// loadAd
		AdRequest.Builder builder = new AdRequest.Builder();
		adLoader.loadAd(builder.build());
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
	
	@OnClick(R.id.history)
	void onHistory() {
		
		// open HistoryActivity
		startActivity(new Intent(getActivity(), HistoryActivity.class));
	}
	
	@OnClick(R.id.subscription)
	void onSubscription() {
		
		// open SubscriptionActivity
		startActivity(new Intent(getActivity(), SubscriptionActivity.class));
	}
	
	@OnClick(R.id.download)
	void onDownload() {
		// open DownloadActivity
		NavigationHelper.openDownloads(activity);
	}
	
	@OnClick(R.id.create_new_playlist)
	void onCreateNewPlaylist() {
		
		if (getFragmentManager() != null) {
			
			PlaylistCreationDialog.newInstance().show(getFragmentManager(), LibraryFragment.class.getName());
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
	public void onDestroyView() {
		
		// destroy ad
		if (adView != null) {
			adView.destroy();
		}
		if (nativeAdView != null) {
			nativeAdView.destroyNativeAd();
		}
		
		super.onDestroyView();
		
		if (itemListAdapter != null) itemListAdapter.unsetSelectedListener();
		
		if (disposables != null) disposables.clear();
		if (databaseSubscription != null) databaseSubscription.cancel();
		
		databaseSubscription = null;
	}
	
	@Override
	public void onDestroy() {
		
		super.onDestroy();
		if (disposables != null) disposables.dispose();
		
		disposables = null;
		localPlaylistManager = null;
		remotePlaylistManager = null;
	}
}
