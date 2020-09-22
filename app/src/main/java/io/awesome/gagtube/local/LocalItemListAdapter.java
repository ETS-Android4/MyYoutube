package io.awesome.gagtube.local;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.database.LocalItem;
import io.awesome.gagtube.local.holder.LocalItemHolder;
import io.awesome.gagtube.local.holder.LocalPlaylistItemHolder;
import io.awesome.gagtube.local.holder.LocalPlaylistStreamItemHolder;
import io.awesome.gagtube.local.holder.LocalStatisticStreamItemHolder;
import io.awesome.gagtube.local.holder.RemotePlaylistItemHolder;
import io.awesome.gagtube.util.FallbackViewHolder;
import io.awesome.gagtube.util.Localization;
import io.awesome.gagtube.util.OnClickGesture;

public class LocalItemListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	private static final int HEADER_TYPE = 0;
	private static final int FOOTER_TYPE = 1;
	
	private static final int STREAM_STATISTICS_HOLDER_TYPE = 0x1000;
	private static final int STREAM_PLAYLIST_HOLDER_TYPE = 0x1001;
	private static final int LOCAL_PLAYLIST_HOLDER_TYPE = 0x2000;
	private static final int REMOTE_PLAYLIST_HOLDER_TYPE = 0x2001;
	
	private final LocalItemBuilder localItemBuilder;
	private final ArrayList<Object> localItems;
	private final DateFormat dateFormat;
	
	private boolean showFooter = false;
	private View header = null;
	private View footer = null;
	
	public LocalItemListAdapter(Activity activity, boolean showOptionMenu) {
		
		localItemBuilder = new LocalItemBuilder(activity, showOptionMenu);
		localItems = new ArrayList<>();
		dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Localization.getPreferredLocale(activity));
	}
	
	public void setSelectedListener(OnClickGesture<LocalItem> listener) {
		localItemBuilder.setOnItemSelectedListener(listener);
	}
	
	public void unsetSelectedListener() {
		localItemBuilder.setOnItemSelectedListener(null);
	}
	
	public void addItems(List<? extends LocalItem> data) {
		
		if (data != null) {
			
			int offsetStart = sizeConsideringHeader();
			localItems.addAll(data);
			
			notifyItemRangeInserted(offsetStart, data.size());
			
			if (footer != null && showFooter) {
				int footerNow = sizeConsideringHeader();
				notifyItemMoved(offsetStart, footerNow);
			}
		}
	}
	
	public void removeItem(final LocalItem data) {
		
		final int index = localItems.indexOf(data);
		
		localItems.remove(index);
		notifyItemRemoved(index + (header != null ? 1 : 0));
	}
	
	public boolean swapItems(int fromAdapterPosition, int toAdapterPosition) {
		
		final int actualFrom = adapterOffsetWithoutHeader(fromAdapterPosition);
		final int actualTo = adapterOffsetWithoutHeader(toAdapterPosition);
		
		if (actualFrom < 0 || actualTo < 0) return false;
		if (actualFrom >= localItems.size() || actualTo >= localItems.size()) return false;
		
		localItems.add(actualTo, localItems.remove(actualFrom));
		notifyItemMoved(fromAdapterPosition, toAdapterPosition);
		return true;
	}
	
	public void clearStreamItemList() {
		
		if (localItems.isEmpty()) {
			return;
		}
		localItems.clear();
		notifyDataSetChanged();
	}
	
	public void setHeader(View header) {
		
		boolean changed = header != this.header;
		this.header = header;
		if (changed) notifyDataSetChanged();
	}
	
	public void setFooter(View view) {
		this.footer = view;
	}
	
	public void showFooter(boolean show) {
		
		if (show == showFooter) return;
		
		showFooter = show;
		if (show) notifyItemInserted(sizeConsideringHeader());
		else notifyItemRemoved(sizeConsideringHeader());
	}
	
	private int adapterOffsetWithoutHeader(final int offset) {
		return offset - (header != null ? 1 : 0);
	}
	
	private int sizeConsideringHeader() {
		return localItems.size() + (header != null ? 1 : 0);
	}
	
	public ArrayList<Object> getItemsList() {
		return localItems;
	}
	
	@Override
	public int getItemCount() {
		
		int count = localItems.size();
		if (header != null) count++;
		if (footer != null && showFooter) count++;
		
		return count;
	}
	
	@Override
	public int getItemViewType(int position) {
		
		if (header != null && position == 0) {
			return HEADER_TYPE;
		}
		else if (header != null) {
			position--;
		}
		
		if (footer != null && position == localItems.size() && showFooter) {
			return FOOTER_TYPE;
		}
		
		final LocalItem item = (LocalItem) localItems.get(position);
		
		switch (item.getLocalItemType()) {
			case PLAYLIST_LOCAL_ITEM:
				return LOCAL_PLAYLIST_HOLDER_TYPE;
			case PLAYLIST_REMOTE_ITEM:
				return REMOTE_PLAYLIST_HOLDER_TYPE;
			case PLAYLIST_STREAM_ITEM:
				return STREAM_PLAYLIST_HOLDER_TYPE;
			case STATISTIC_STREAM_ITEM:
				return STREAM_STATISTICS_HOLDER_TYPE;
			default:
				return -1;
		}
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
		
		switch (type) {
			case HEADER_TYPE:
				return new HeaderFooterHolder(header);
			case FOOTER_TYPE:
				return new HeaderFooterHolder(footer);
			case LOCAL_PLAYLIST_HOLDER_TYPE:
				return new LocalPlaylistItemHolder(localItemBuilder, parent);
			case REMOTE_PLAYLIST_HOLDER_TYPE:
				return new RemotePlaylistItemHolder(localItemBuilder, parent);
			case STREAM_PLAYLIST_HOLDER_TYPE:
				return new LocalPlaylistStreamItemHolder(localItemBuilder, parent);
			case STREAM_STATISTICS_HOLDER_TYPE:
				return new LocalStatisticStreamItemHolder(localItemBuilder, parent);
			default:
				return new FallbackViewHolder(new View(parent.getContext()));
		}
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		
		if (holder instanceof LocalItemHolder) {
			// If header isn't null, offset the items by -1
			if (header != null) position--;
			
			((LocalItemHolder) holder).updateFromItem((LocalItem) localItems.get(position), dateFormat);
		}
		else if (holder instanceof HeaderFooterHolder && position == 0 && header != null) {
			((HeaderFooterHolder) holder).view = header;
		}
		else if (holder instanceof HeaderFooterHolder && position == sizeConsideringHeader()
				&& footer != null && showFooter) {
			((HeaderFooterHolder) holder).view = footer;
		}
	}
	
}
