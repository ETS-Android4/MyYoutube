package io.awesome.gagtube.fragments.list.playlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.material.button.MaterialButton;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AdUtils;
import io.awesome.gagtube.adsmanager.AppInterstitialAd;
import io.awesome.gagtube.adsmanager.nativead.NativeAdStyle;
import io.awesome.gagtube.adsmanager.nativead.NativeAdView;
import io.awesome.gagtube.database.GAGTubeDatabase;
import io.awesome.gagtube.database.playlist.model.PlaylistRemoteEntity;
import io.awesome.gagtube.fragments.BackPressable;
import io.awesome.gagtube.fragments.list.BaseListInfoFragment;
import io.awesome.gagtube.local.playlist.RemotePlaylistManager;
import io.awesome.gagtube.player.playqueue.PlayQueue;
import io.awesome.gagtube.player.playqueue.PlaylistPlayQueue;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.AnimationUtils;
import io.awesome.gagtube.util.ExtractorHelper;
import io.awesome.gagtube.util.Localization;
import io.awesome.gagtube.util.NavigationHelper;
import io.awesome.gagtube.util.SharedUtils;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

public class PlaylistFragment extends BaseListInfoFragment<PlaylistInfo> implements BackPressable {
	
	private CompositeDisposable disposables;
	private Subscription bookmarkReactor;
	private AtomicBoolean isBookmarkButtonReady;
	
	private RemotePlaylistManager remotePlaylistManager;
	private PlaylistRemoteEntity playlistEntity;
	
	// Views
	private Toolbar mToolbar;
	private MaterialButton headerPlayAllButton;
	private MaterialButton headerPopupButton;
	private View headerShareButton;
	private MenuItem playlistBookmarkButton;
	
	// NativeAd
	private NativeAdView nativeAdView;
	@BindView(R.id.adView) AdView adView;
	
	public static PlaylistFragment getInstance(int serviceId, String url, String name) {
		
		PlaylistFragment instance = new PlaylistFragment();
		instance.setInitialData(serviceId, url, name);
		return instance;
	}
	
	// LifeCycle
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		disposables = new CompositeDisposable();
		isBookmarkButtonReady = new AtomicBoolean(false);
		remotePlaylistManager = new RemotePlaylistManager(GAGTubeDatabase.getInstance(activity));
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_playlist, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		headerPlayAllButton = rootView.findViewById(R.id.playlist_ctrl_play_all_button);
		headerPopupButton = rootView.findViewById(R.id.playlist_ctrl_play_popup_button);
		headerShareButton = rootView.findViewById(R.id.playlist_ctrl_share);
		
		infoListAdapter.useMiniItemVariants(true);
		
		mToolbar = rootView.findViewById(R.id.default_toolbar);
		activity.getDelegate().setSupportActionBar(mToolbar);
		
		View headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
		nativeAdView = headerRootLayout.findViewById(R.id.template_view);
		infoListAdapter.setHeader(headerRootLayout);
		
		// show ad
		showBannerAd();
	}
	
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
		AppInterstitialAd.getInstance().init(activity);
		// show ad
		showNativeAd();
		if (adView != null) {
			adView.resume();
		}
		// show ad
		showBannerAd();
	}
	
	@Override
	public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
		
		super.onCreateOptionsMenu(menu, inflater);
		
		ActionBar actionBar = activity.getDelegate().getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayShowTitleEnabled(true);
		}
		
		inflater.inflate(R.menu.menu_playlist, menu);
		
		playlistBookmarkButton = menu.findItem(R.id.menu_item_bookmark);
		updateBookmarkButtons();
	}
	
	@Override
	public void onViewCreated(@NonNull View rootView, Bundle savedInstanceState) {
		
		super.onViewCreated(rootView, savedInstanceState);
		
		mToolbar.setNavigationOnClickListener(view -> onPopBackStack());
	}
	
	@Override
	public void onDestroyView() {
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroyView();
		
		if (isBookmarkButtonReady != null) isBookmarkButtonReady.set(false);
		
		if (disposables != null) disposables.clear();
		if (bookmarkReactor != null) bookmarkReactor.cancel();
		
		bookmarkReactor = null;
	}
	
	@Override
	public void onDestroy() {
		
		// destroy ad
		if (nativeAdView != null) {
			nativeAdView.destroyNativeAd();
		}
		
		super.onDestroy();
		
		if (disposables != null) disposables.dispose();
		
		disposables = null;
		remotePlaylistManager = null;
		playlistEntity = null;
		isBookmarkButtonReady = null;
	}
	
	// Load and handle
	@Override
	protected Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic() {
		return ExtractorHelper.getMorePlaylistItems(serviceId, url, currentNextPage);
	}
	
	@Override
	protected Single<PlaylistInfo> loadResult(boolean forceLoad) {
		return ExtractorHelper.getPlaylistInfo(serviceId, url, forceLoad);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == R.id.menu_item_bookmark) {
			onBookmarkClicked();
		}
		return super.onOptionsItemSelected(item);
	}
	
	// Contract
	@Override
	public void showLoading() {
		
		super.showLoading();
		
		AnimationUtils.animateView(itemsList, false, 100);
	}
	
	@Override
	public void handleResult(@NonNull final PlaylistInfo result) {
		
		super.handleResult(result);
		
		mToolbar.setSubtitle(Localization.localizeStreamCount(activity, result.getStreamCount()));
		
		if (!result.getErrors().isEmpty()) {
			showSnackBarError(result.getErrors(), UserAction.REQUESTED_PLAYLIST, NewPipe.getNameOfService(result.getServiceId()), result.getUrl(), 0);
		}
		
		remotePlaylistManager.getPlaylist(result)
				.flatMap(lists -> getUpdateProcessor(lists, result), (lists, id) -> lists)
				.onBackpressureLatest()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(getPlaylistBookmarkSubscriber());
		
		headerPlayAllButton.setOnClickListener(view -> AppInterstitialAd.getInstance().showInterstitialAd(() -> NavigationHelper.playOnMainPlayer(activity, getPlayQueue())));
		
		headerPopupButton.setOnClickListener(view -> AppInterstitialAd.getInstance().showInterstitialAd(() -> NavigationHelper.playOnPopupPlayer(activity, getPlayQueue())));
		
		headerShareButton.setOnClickListener(view -> SharedUtils.shareUrl(activity, name, url));
	}
	
	private PlayQueue getPlayQueue() {
		return getPlayQueue(0);
	}
	
	private PlayQueue getPlayQueue(final int index) {
		final List<StreamInfoItem> infoItems = new ArrayList<>();
		for (InfoItem i : infoListAdapter.getItemsList()) {
			if (i instanceof StreamInfoItem) {
				infoItems.add((StreamInfoItem) i);
			}
		}
		return new PlaylistPlayQueue(
				currentInfo.getServiceId(),
				currentInfo.getUrl(),
				currentInfo.getNextPage(),
				infoItems,
				index
		);
	}
	
	@Override
	public void handleNextItems(ListExtractor.InfoItemsPage result) {
		
		super.handleNextItems(result);
		
		if (!result.getErrors().isEmpty()) {
			showSnackBarError(result.getErrors(), UserAction.REQUESTED_PLAYLIST, NewPipe.getNameOfService(serviceId), "Get next page of: " + url, 0);
		}
	}
	
	// OnError
	@Override
	protected boolean onError(Throwable exception) {
		
		if (super.onError(exception)) return true;
		
		int errorId = exception instanceof ExtractionException ? R.string.parsing_error : R.string.general_error;
		onUnrecoverableError(exception, UserAction.REQUESTED_PLAYLIST, NewPipe.getNameOfService(serviceId), url, errorId);
		return true;
	}
	
	// Utils
	private Flowable<Integer> getUpdateProcessor(@NonNull List<PlaylistRemoteEntity> playlists, @NonNull PlaylistInfo result) {
		
		final Flowable<Integer> noItemToUpdate = Flowable.just(-1);
		if (playlists.isEmpty()) return noItemToUpdate;
		
		final PlaylistRemoteEntity playlistEntity = playlists.get(0);
		if (playlistEntity.isIdenticalTo(result)) return noItemToUpdate;
		
		return remotePlaylistManager.onUpdate(playlists.get(0).getUid(), result).toFlowable();
	}
	
	private Subscriber<List<PlaylistRemoteEntity>> getPlaylistBookmarkSubscriber() {
		
		return new Subscriber<List<PlaylistRemoteEntity>>() {
			
			@Override
			public void onSubscribe(Subscription s) {
				
				if (bookmarkReactor != null) bookmarkReactor.cancel();
				bookmarkReactor = s;
				bookmarkReactor.request(1);
			}
			
			@Override
			public void onNext(List<PlaylistRemoteEntity> playlist) {
				
				playlistEntity = playlist.isEmpty() ? null : playlist.get(0);
				
				updateBookmarkButtons();
				isBookmarkButtonReady.set(true);
				
				if (bookmarkReactor != null) bookmarkReactor.request(1);
			}
			
			@Override
			public void onError(Throwable throwable) {
				PlaylistFragment.this.onError(throwable);
			}
			
			@Override
			public void onComplete() {
				
			}
		};
	}
	
	private void onBookmarkClicked() {
		
		if (isBookmarkButtonReady == null || !isBookmarkButtonReady.get() || remotePlaylistManager == null) return;
		
		final Disposable action;
		
		if (currentInfo != null && playlistEntity == null) {
			action = remotePlaylistManager.onBookmark(currentInfo)
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(ignored -> Toast.makeText(getContext(), getString(R.string.added_playlist_to_bookmark), Toast.LENGTH_SHORT).show(), this::onError);
		}
		else if (playlistEntity != null) {
			action = remotePlaylistManager.deletePlaylist(playlistEntity.getUid())
					.observeOn(AndroidSchedulers.mainThread())
					.doFinally(() -> playlistEntity = null)
					.subscribe(ignored -> Toast.makeText(getContext(), getString(R.string.removed_playlist_from_bookmark), Toast.LENGTH_SHORT).show(), this::onError);
		}
		else {
			action = Disposables.empty();
		}
		
		disposables.add(action);
	}
	
	private void updateBookmarkButtons() {
		
		if (playlistBookmarkButton == null || activity == null) return;
		
		final int iconAttr = playlistEntity == null ? R.drawable.ic_playlist_add_white_24dp : R.drawable.ic_playlist_add_check_white;
		
		final int titleRes = playlistEntity == null ? R.string.bookmark_playlist : R.string.removed_playlist_from_bookmark;
		
		playlistBookmarkButton.setIcon(iconAttr);
		playlistBookmarkButton.setTitle(titleRes);
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
						super.onAdFailedToLoad(loadAdError);
					}
					
					@Override
					public void onAdLoaded() {
						
						super.onAdLoaded();
					}
				})
				.withNativeAdOptions(adOptions)
				.build();
		
		// loadAd
		AdRequest.Builder builder = new AdRequest.Builder();
		adLoader.loadAd(builder.build());
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
	
	private void onPopBackStack() {
		
		// pop back stack
		if (getFragmentManager() != null) {
			getFragmentManager().popBackStack();
		}
	}
	
	@Override
	public boolean onBackPressed() {
		
		onPopBackStack();
		return true;
	}
}