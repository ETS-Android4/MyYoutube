package io.awesome.gultube.fragments.list.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

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
import io.awesome.gultube.R;

import io.awesome.gultube.fragments.MainFragment;
import io.awesome.gultube.fragments.list.BaseListInfoFragment;
import io.awesome.gultube.report.UserAction;
import io.awesome.gultube.util.AnimationUtils;
import io.awesome.gultube.util.ExtractorHelper;
import io.awesome.gultube.util.NavigationHelper;
import io.reactivex.Single;

public class TrendingFragment extends BaseListInfoFragment<KioskInfo> {

	
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
		

	}
	
	@Override
	public void onResume() {
		super.onResume();
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
		super.onDestroy();
	}
}
