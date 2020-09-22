package io.awesome.gultube.fragments.list;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.Page;

import java.util.Queue;

import androidx.annotation.NonNull;
import icepick.State;
import io.awesome.gultube.util.Constants;
import io.awesome.gultube.util.ExtractorHelper;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseListInfoFragment<I extends ListInfo> extends BaseListFragment<I, ListExtractor.InfoItemsPage> {
	
	@State
	protected int serviceId = Constants.YOUTUBE_SERVICE_ID;
	@State
	protected String name;
	@State
	protected String url;
	
	protected I currentInfo;
	protected Page currentNextPage;
	protected Disposable currentWorker;
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		
		super.initViews(rootView, savedInstanceState);
		
		setTitle(name);
		showListFooter(hasMoreItems());
	}
	
	@Override
	public void onPause() {
		
		super.onPause();
		
		if (currentWorker != null) currentWorker.dispose();
	}
	
	@Override
	public void onResume() {
		
		super.onResume();
		
		// Check if it was loading when the fragment was stopped/paused,
		if (wasLoading.getAndSet(false)) {
			
			if (hasMoreItems() && infoListAdapter.getItemsList().size() > 0) {
				loadMoreItems();
			}
			else {
				doInitialLoadLogic();
			}
		}
	}
	
	@Override
	public void onDestroy() {
		
		super.onDestroy();
		
		if (currentWorker != null) currentWorker.dispose();
		currentWorker = null;
	}
	
	// State Saving
	@Override
	public void writeTo(Queue<Object> objectsToSave) {
		
		super.writeTo(objectsToSave);
		
		objectsToSave.add(currentInfo);
		objectsToSave.add(currentNextPage);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
		
		super.readFrom(savedObjects);
		
		currentInfo = (I) savedObjects.poll();
		currentNextPage = (Page) savedObjects.poll();
	}
	
	// Load and handle
	protected void doInitialLoadLogic() {
		
		if (currentInfo == null) {
			startLoading(false);
		}
		else handleResult(currentInfo);
	}
	
	/**
	 * Implement the logic to load the info from the network.<br/>
	 * You can use the default implementations from {@link ExtractorHelper}.
	 *
	 * @param forceLoad allow or disallow the result to come from the cache
	 */
	protected abstract Single<I> loadResult(boolean forceLoad);
	
	@Override
	public void startLoading(boolean forceLoad) {
		
		super.startLoading(forceLoad);
		
		showListFooter(false);
		currentInfo = null;
		if (currentWorker != null) currentWorker.dispose();
		currentWorker = loadResult(forceLoad)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
						// onNext
						(@NonNull I result) -> {
							isLoading.set(false);
							currentInfo = result;
							currentNextPage = result.getNextPage();
							handleResult(result);
						},
						// onError
						this::onError);
	}
	
	/**
	 * Implement the logic to load more items<br/>
	 * You can use the default implementations from {@link ExtractorHelper}
	 */
	protected abstract Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic();
	
	protected void loadMoreItems() {
		
		isLoading.set(true);
		
		if (currentWorker != null) currentWorker.dispose();
		currentWorker = loadMoreItemsLogic()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
						// onNext
						(@NonNull ListExtractor.InfoItemsPage InfoItemsPage) -> {
							isLoading.set(false);
							handleNextItems(InfoItemsPage);
						},
						// onError
						(@NonNull Throwable throwable) -> {
							isLoading.set(false);
							onError(throwable);
						});
	}
	
	@Override
	public void handleNextItems(ListExtractor.InfoItemsPage result) {
		
		super.handleNextItems(result);
		
		currentNextPage = result.getNextPage();
		infoListAdapter.addInfoItemList(result.getItems());
		
		showListFooter(hasMoreItems());
	}
	
	@Override
	protected boolean hasMoreItems() {
		return Page.isValid(currentNextPage);
	}
	
	// Contract
	@Override
	public void handleResult(@NonNull I result) {
		
		super.handleResult(result);
		
		name = result.getName();
		setTitle(name);
		
		if (infoListAdapter.getItemsList().size() == 0) {
			
			if (result.getRelatedItems().size() > 0) {
				infoListAdapter.addInfoItemList(result.getRelatedItems());
				showListFooter(hasMoreItems());
			}
			else {
				infoListAdapter.clearStreamItemList();
				showEmptyState();
			}
		}
	}
	
	protected void setInitialData(int serviceId, String url, String name) {
		
		this.serviceId = serviceId;
		this.url = url;
		this.name = !TextUtils.isEmpty(name) ? name : "";
	}
}
