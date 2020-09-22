package io.awesome.gagtube.player.playqueue;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

abstract class AbstractInfoPlayQueue<T extends ListInfo, U extends InfoItem> extends PlayQueue {
	
	boolean isInitial;
	private boolean isComplete;
	
	int serviceId;
	String baseUrl;
	Page nextPage;
	
	private transient Disposable fetchReactor;
	
	AbstractInfoPlayQueue(final U item) {
		this(item.getServiceId(), item.getUrl(), null, Collections.<StreamInfoItem>emptyList(), 0);
	}
	
	AbstractInfoPlayQueue(final int serviceId, final String url, final Page nextPage, final List<StreamInfoItem> streams, final int index) {
		
		super(index, extractListItems(streams));
		
		this.baseUrl = url;
		this.nextPage = nextPage;
		this.serviceId = serviceId;
		
		this.isInitial = streams.isEmpty();
		this.isComplete = !isInitial && !Page.isValid(nextPage);
	}
	
	abstract protected String getTag();
	
	@Override
	public boolean isComplete() {
		return isComplete;
	}
	
	SingleObserver<T> getHeadListObserver() {
		
		return new SingleObserver<T>() {
			
			@Override
			public void onSubscribe(@NonNull Disposable disposable) {
				
				if (isComplete || !isInitial || (fetchReactor != null && !fetchReactor.isDisposed())) {
					disposable.dispose();
				}
				else {
					fetchReactor = disposable;
				}
			}
			
			@Override
			public void onSuccess(@NonNull T result) {
				
				isInitial = false;
				if (!result.hasNextPage()) isComplete = true;
				nextPage = result.getNextPage();
				
				append(extractListItems(result.getRelatedItems()));
				
				fetchReactor.dispose();
				fetchReactor = null;
			}
			
			@Override
			public void onError(@NonNull Throwable e) {
				
				isComplete = true;
				// notify changed
				append();
			}
		};
	}
	
	SingleObserver<ListExtractor.InfoItemsPage> getNextPageObserver() {
		
		return new SingleObserver<ListExtractor.InfoItemsPage>() {
			
			@Override
			public void onSubscribe(@NonNull Disposable disposable) {
				
				if (isComplete || isInitial || (fetchReactor != null && !fetchReactor.isDisposed())) {
					disposable.dispose();
				}
				else {
					fetchReactor = disposable;
				}
			}
			
			@Override
			public void onSuccess(@NonNull ListExtractor.InfoItemsPage result) {
				
				if (!result.hasNextPage()) isComplete = true;
				nextPage = result.getNextPage();
				
				append(extractListItems(result.getItems()));
				
				fetchReactor.dispose();
				fetchReactor = null;
			}
			
			@Override
			public void onError(@NonNull Throwable e) {
				
				isComplete = true;
				// notify changed
				append();
			}
		};
	}
	
	@Override
	public void dispose() {
		
		super.dispose();
		
		if (fetchReactor != null) fetchReactor.dispose();
		fetchReactor = null;
	}
	
	private static List<PlayQueueItem> extractListItems(final List<StreamInfoItem> streamInfoItems) {
		
		List<PlayQueueItem> result = new ArrayList<>();
		for (final InfoItem stream : streamInfoItems) {
			
			if (stream instanceof StreamInfoItem) {
				result.add(new PlayQueueItem((StreamInfoItem) stream));
			}
		}
		return result;
	}
}
