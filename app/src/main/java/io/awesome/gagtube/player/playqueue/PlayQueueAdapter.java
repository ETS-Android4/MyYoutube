package io.awesome.gagtube.player.playqueue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.R;
import io.awesome.gagtube.player.playqueue.events.AppendEvent;
import io.awesome.gagtube.player.playqueue.events.ErrorEvent;
import io.awesome.gagtube.player.playqueue.events.MoveEvent;
import io.awesome.gagtube.player.playqueue.events.PlayQueueEvent;
import io.awesome.gagtube.player.playqueue.events.RemoveEvent;
import io.awesome.gagtube.player.playqueue.events.SelectEvent;
import io.awesome.gagtube.util.FallbackViewHolder;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class PlayQueueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	private static final int ITEM_VIEW_TYPE_ID = 0;
	private static final int FOOTER_VIEW_TYPE_ID = 1;
	
	private final PlayQueueItemBuilder playQueueItemBuilder;
	private final PlayQueue playQueue;
	private boolean showFooter = false;
	private View footer = null;
	
	private Disposable playQueueReactor;
	
	public static class HFHolder extends RecyclerView.ViewHolder {
		
		HFHolder(View view) {
			
			super(view);
			this.view = view;
		}
		
		public View view;
	}
	
	public PlayQueueAdapter(final PlayQueue playQueue) {
		
		if (playQueue.getBroadcastReceiver() == null) {
			throw new IllegalStateException("Play Queue has not been initialized.");
		}
		
		this.playQueueItemBuilder = new PlayQueueItemBuilder();
		this.playQueue = playQueue;
		
		playQueue.getBroadcastReceiver().toObservable().subscribe(getReactor());
	}
	
	private Observer<PlayQueueEvent> getReactor() {
		
		return new Observer<PlayQueueEvent>() {
			
			@Override
			public void onSubscribe(@NonNull Disposable disposable) {
				
				if (playQueueReactor != null) playQueueReactor.dispose();
				playQueueReactor = disposable;
			}
			
			@Override
			public void onNext(@NonNull PlayQueueEvent playQueueMessage) {
				if (playQueueReactor != null) onPlayQueueChanged(playQueueMessage);
			}
			
			@Override
			public void onError(@NonNull Throwable e) {
				// unimplemented
			}
			
			@Override
			public void onComplete() {
				dispose();
			}
		};
		
	}
	
	private void onPlayQueueChanged(final PlayQueueEvent message) {
		
		switch (message.type()) {
			case RECOVERY:
				// unimplemented
				break;
			case SELECT:
				final SelectEvent selectEvent = (SelectEvent) message;
				notifyItemChanged(selectEvent.getOldIndex());
				notifyItemChanged(selectEvent.getNewIndex());
				break;
			case APPEND:
				final AppendEvent appendEvent = (AppendEvent) message;
				notifyItemRangeInserted(playQueue.size(), appendEvent.getAmount());
				break;
			case ERROR:
				final ErrorEvent errorEvent = (ErrorEvent) message;
				notifyItemChanged(errorEvent.getErrorIndex());
				notifyItemChanged(errorEvent.getQueueIndex());
				break;
			case REMOVE:
				final RemoveEvent removeEvent = (RemoveEvent) message;
				notifyItemRemoved(removeEvent.getRemoveIndex());
				notifyItemChanged(removeEvent.getQueueIndex());
				break;
			case MOVE:
				final MoveEvent moveEvent = (MoveEvent) message;
				notifyItemMoved(moveEvent.getFromIndex(), moveEvent.getToIndex());
				break;
			case INIT:
			case REORDER:
			default:
				notifyDataSetChanged();
				break;
		}
	}
	
	public void dispose() {
		
		if (playQueueReactor != null) playQueueReactor.dispose();
		playQueueReactor = null;
	}
	
	public void setSelectedListener(final PlayQueueItemBuilder.OnSelectedListener listener) {
		playQueueItemBuilder.setOnSelectedListener(listener);
	}
	
	public void unsetSelectedListener() {
		playQueueItemBuilder.setOnSelectedListener(null);
	}
	
	public void setFooter(View footer) {
		
		this.footer = footer;
		notifyItemChanged(playQueue.size());
	}
	
	public void showFooter(final boolean show) {
		
		showFooter = show;
		notifyItemChanged(playQueue.size());
	}
	
	public List<PlayQueueItem> getItems() {
		return playQueue.getStreams();
	}
	
	@Override
	public int getItemCount() {
		
		int count = playQueue.getStreams().size();
		if (footer != null && showFooter) count++;
		return count;
	}
	
	@Override
	public int getItemViewType(int position) {
		
		if (footer != null && position == playQueue.getStreams().size() && showFooter) {
			return FOOTER_VIEW_TYPE_ID;
		}
		return ITEM_VIEW_TYPE_ID;
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
		
		switch (type) {
			case FOOTER_VIEW_TYPE_ID:
				return new HFHolder(footer);
			
			case ITEM_VIEW_TYPE_ID:
				return new PlayQueueItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.play_queue_item, parent, false));
			
			default:
				return new FallbackViewHolder(new View(parent.getContext()));
		}
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		
		if (holder instanceof PlayQueueItemHolder) {
			
			final PlayQueueItemHolder itemHolder = (PlayQueueItemHolder) holder;
			playQueueItemBuilder.buildStreamInfoItem(itemHolder, playQueue.getStreams().get(position));
			
			// Check if the current item should be selected/highlighted
			final boolean isSelected = playQueue.getIndex() == position;
			itemHolder.itemSelected.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
			itemHolder.itemView.setSelected(isSelected);
		}
		else if (holder instanceof HFHolder && position == playQueue.getStreams().size() && footer != null && showFooter) {
			((HFHolder) holder).view = footer;
		}
	}
}
