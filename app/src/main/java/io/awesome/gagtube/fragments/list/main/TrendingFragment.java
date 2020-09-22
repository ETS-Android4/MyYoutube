package io.awesome.gagtube.fragments.list.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.awesome.gagtube.R;
import io.awesome.gagtube.adsmanager.AdUtils;
import io.awesome.gagtube.adsmanager.nativead.NativeAdStyle;
import io.awesome.gagtube.adsmanager.nativead.NativeAdView;
import io.awesome.gagtube.fragments.MainFragment;
import io.awesome.gagtube.fragments.list.BaseListInfoFragment;
import io.awesome.gagtube.report.UserAction;
import io.awesome.gagtube.util.AnimationUtils;
import io.awesome.gagtube.util.ExtractorHelper;
import io.awesome.gagtube.util.NavigationHelper;
import io.reactivex.Single;

public class TrendingFragment extends BaseListInfoFragment<KioskInfo> {
	
	// NativeAd
	private NativeAdView nativeAdView;
	
	@NonNull
	public static TrendingFragment getInstance(int serviceId) {
		
		try {
			return getInstance(serviceId, NewPipe.getService(serviceId).getKioskList().getDefaultKioskId());
		}
		catch (ExtractionException e) {
			return new TrendingFragment();
		}
	}
	
	@NonNull
	public static TrendingFragment getInstance(int serviceId, String kioskId) {
		
		try {
			TrendingFragment instance = new TrendingFragment();
			StreamingService service = NewPipe.getService(serviceId);
			
			ListLinkHandlerFactory kioskLinkHandlerFactory = service.getKioskList().getListLinkHandlerFactoryByType(kioskId);
			instance.setInitialData(serviceId, kioskLinkHandlerFactory.fromId(kioskId).getUrl(), kioskId);
			
			return instance;
		}
		catch (ExtractionException e) {
			return new TrendingFragment();
		}
	}
	
	// LifeCycle
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		name = getString(R.string.trending);
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_trending, container, false);
		ButterKnife.bind(this, view);
		
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		View headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
		nativeAdView = headerRootLayout.findViewById(R.id.template_view);
		infoListAdapter.setHeader(headerRootLayout);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// show ad
		showNativeAd();
	}
	
	// Menu
	@Override
	public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
		
		super.onCreateOptionsMenu(menu, inflater);
		
		ActionBar supportActionBar = activity.getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(false);
		}
	}
	
	// Load and handle
	@Override
	public Single<KioskInfo> loadResult(boolean forceReload) {
		
		return ExtractorHelper.getKioskInfo(serviceId, url, forceReload);
	}
	
	@Override
	public Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic() {
		
		return ExtractorHelper.getMoreKioskItems(serviceId, url, currentNextPage);
	}
	
	// Contract
	@Override
	public void showLoading() {
		
		super.showLoading();
		
		AnimationUtils.animateView(itemsList, false, 100);
	}
	
	@Override
	public void handleResult(@NonNull final KioskInfo result) {
		
		super.handleResult(result);
		
		if (!result.getErrors().isEmpty()) {
			showSnackBarError(result.getErrors(), UserAction.REQUESTED_MAIN_CONTENT, NewPipe.getNameOfService(result.getServiceId()), result.getUrl(), 0);
		}
	}
	
	@Override
	public void handleNextItems(ListExtractor.InfoItemsPage result) {
		
		super.handleNextItems(result);
		
		if (!result.getErrors().isEmpty()) {
			showSnackBarError(result.getErrors(), UserAction.REQUESTED_PLAYLIST, NewPipe.getNameOfService(serviceId), "Get next page of: " + url, 0);
		}
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
	
	@Override
	public void onDestroy() {
		
		// destroy ad
		if (nativeAdView != null) {
			nativeAdView.destroyNativeAd();
		}
		
		super.onDestroy();
	}
}
