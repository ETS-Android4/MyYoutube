package io.awesome.gultube.info_list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gultube.info_list.holder.ChannelInfoItemHolder;
import io.awesome.gultube.info_list.holder.ChannelMiniInfoItemHolder;
import io.awesome.gultube.info_list.holder.InfoItemHolder;
import io.awesome.gultube.info_list.holder.PlaylistInfoItemHolder;
import io.awesome.gultube.info_list.holder.PlaylistMiniInfoItemHolder;
import io.awesome.gultube.info_list.holder.StreamInfoItemHolder;
import io.awesome.gultube.info_list.holder.StreamMiniInfoItemHolder;
import io.awesome.gultube.util.FallbackViewHolder;
import io.awesome.gultube.util.OnClickGesture;
import io.awesome.gultube.util.recyclerview.ViewBinderHelper;

public class InfoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	private static final int HEADER_TYPE = 0;
	private static final int FOOTER_TYPE = 1;
	
	private static final int MINI_STREAM_HOLDER_TYPE = 0x100;
	private static final int STREAM_HOLDER_TYPE = 0x101;
	private static final int MINI_CHANNEL_HOLDER_TYPE = 0x200;
	private static final int CHANNEL_HOLDER_TYPE = 0x201;
	private static final int MINI_PLAYLIST_HOLDER_TYPE = 0x300;
	private static final int PLAYLIST_HOLDER_TYPE = 0x301;
	
	private final InfoItemBuilder infoItemBuilder;
	private final ArrayList<InfoItem> infoItemList;
	private boolean useMiniVariant = false;
	private boolean showFooter = false;
	private View header = null;
	private View footer = null;
	
	// for swipe menu to unsubscribe channel
	public final ViewBinderHelper binderHelper = new ViewBinderHelper();
	private boolean unsubscribe;
	
	public class HFHolder extends RecyclerView.ViewHolder {
		public View view;
		
		HFHolder(View v) {
			super(v);
			view = v;
		}
	}
	
	public InfoListAdapter(Activity activity, boolean unsubscribe) {
		
		infoItemBuilder = new InfoItemBuilder(activity);
		infoItemList = new ArrayList<>();
		
		this.unsubscribe = unsubscribe;
		
		// open only one row at a time
		binderHelper.setOpenOnlyOne(true);
	}
	
	public void setOnStreamSelectedListener(OnClickGesture<StreamInfoItem> listener) {
		
		infoItemBuilder.setOnStreamSelectedListener(listener);
	}
	
	public void setOnChannelSelectedListener(OnClickGesture<ChannelInfoItem> listener) {
		
		infoItemBuilder.setOnChannelSelectedListener(listener);
	}
	
	public void setOnPlaylistSelectedListener(OnClickGesture<PlaylistInfoItem> listener) {
		
		infoItemBuilder.setOnPlaylistSelectedListener(listener);
	}
	
	public void useMiniItemVariants(boolean useMiniVariant) {
		
		this.useMiniVariant = useMiniVariant;
	}
	
	public void addInfoItemList(List<InfoItem> data) {
		
		if (data != null) {
			
			int offsetStart = sizeConsideringHeaderOffset();
			infoItemList.addAll(data);
			
			notifyItemRangeInserted(offsetStart, data.size());
			
			if (footer != null && showFooter) {
				int footerNow = sizeConsideringHeaderOffset();
				notifyItemMoved(offsetStart, footerNow);
			}
		}
	}
	
	public void addInfoItem(InfoItem data) {
		
		if (data != null) {
			
			int positionInserted = sizeConsideringHeaderOffset();
			infoItemList.add(data);
			
			notifyItemInserted(positionInserted);
			
			if (footer != null && showFooter) {
				int footerNow = sizeConsideringHeaderOffset();
				notifyItemMoved(positionInserted, footerNow);
			}
		}
	}
	
	public void clearStreamItemList() {
		
		if (infoItemList.isEmpty()) {
			return;
		}
		
		infoItemList.clear();
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
		if (show) notifyItemInserted(sizeConsideringHeaderOffset());
		else notifyItemRemoved(sizeConsideringHeaderOffset());
	}
	
	private int sizeConsideringHeaderOffset() {
		
		return infoItemList.size() + (header != null ? 1 : 0);
	}
	
	public ArrayList<InfoItem> getItemsList() {
		return infoItemList;
	}
	
	@Override
	public int getItemCount() {
		
		int count = infoItemList.size();
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
		if (footer != null && position == infoItemList.size() && showFooter) {
			return FOOTER_TYPE;
		}
		final InfoItem item = infoItemList.get(position);
		switch (item.getInfoType()) {
			case STREAM:
				return useMiniVariant ? MINI_STREAM_HOLDER_TYPE : STREAM_HOLDER_TYPE;
			case CHANNEL:
				return useMiniVariant ? MINI_CHANNEL_HOLDER_TYPE : CHANNEL_HOLDER_TYPE;
			case PLAYLIST:
				return useMiniVariant ? MINI_PLAYLIST_HOLDER_TYPE : PLAYLIST_HOLDER_TYPE;
			default:
				return -1;
		}
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
		
		switch (type) {
			case HEADER_TYPE:
				return new HFHolder(header);
			case FOOTER_TYPE:
				return new HFHolder(footer);
			case MINI_STREAM_HOLDER_TYPE:
				return new StreamMiniInfoItemHolder(infoItemBuilder, parent);
			case STREAM_HOLDER_TYPE:
				return new StreamInfoItemHolder(infoItemBuilder, parent);
			case MINI_CHANNEL_HOLDER_TYPE:
				return new ChannelMiniInfoItemHolder(infoItemBuilder, parent);
			case CHANNEL_HOLDER_TYPE:
				return new ChannelInfoItemHolder(infoItemBuilder, parent);
			case MINI_PLAYLIST_HOLDER_TYPE:
				return new PlaylistMiniInfoItemHolder(infoItemBuilder, parent);
			case PLAYLIST_HOLDER_TYPE:
				return new PlaylistInfoItemHolder(infoItemBuilder, parent);
			default:
				return new FallbackViewHolder(new View(parent.getContext()));
		}
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		
		if (holder instanceof InfoItemHolder) {
			// If header isn't null, offset the items by -1
			if (header != null) position--;
			
			((InfoItemHolder) holder).updateFromItem(infoItemList.get(position));
			// Use ViewBindHelper to restore and save the open/close state of the SwipeRevealView
			// put an unique string id as value, can be any string which uniquely define the data
			if (holder instanceof ChannelMiniInfoItemHolder) {
				// able to unsubscribe
				if (unsubscribe) {
					binderHelper.bind(((ChannelMiniInfoItemHolder) holder).swipeLayout, infoItemList.get(position).getUrl());
				}
				else {
					((ChannelMiniInfoItemHolder) holder).swipeLayout.setLockDrag(true);
				}
			}
		}
		else if (holder instanceof HFHolder && position == 0 && header != null) {
			((HFHolder) holder).view = header;
		}
		else if (holder instanceof HFHolder && position == sizeConsideringHeaderOffset() && footer != null && showFooter) {
			((HFHolder) holder).view = footer;
		}
	}
}
