package io.awesome.gagtube.fragments.discover.adapter;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.fragments.discover.holder.VideoHolder;
import io.awesome.gagtube.fragments.discover.model.VideoListResponse;
import io.awesome.gagtube.util.FallbackViewHolder;

public class VideoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	public interface Listener {
		
		void onVideoClicked(int position);
		void onMoreOption(int position, View view);
	}
	
	private static final int HEADER_TYPE = 0;
	private static final int VIDEO_TYPE = 1;
	private View header = null;
	
	private Listener listener;
	private List<VideoListResponse.Item> items = new ArrayList<>();
	
	public VideoListAdapter(Listener listener) {
		
		this.listener = listener;
	}
	
	public void setItems(List<VideoListResponse.Item> items) {
		
		this.items = items;
		notifyDataSetChanged();
	}
	
	public List<VideoListResponse.Item> getItems() {
		return items;
	}
	
	public VideoListResponse.Item getItem(int position) {
		
		if (header != null) position--;
		
		return items.get(position);
	}
	
	public void setHeader(View header) {
		
		boolean changed = header != this.header;
		this.header = header;
		if (changed) notifyDataSetChanged();
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		
		switch (viewType) {
			
			case HEADER_TYPE:
				return new HeaderViewHolder(header);
			
			case VIDEO_TYPE:
				return new VideoHolder(parent, listener);
			
			default:
				return new FallbackViewHolder(new View(parent.getContext()));
		}
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		
		if (holder instanceof HeaderViewHolder && position == 0 && header != null) {
			((HeaderViewHolder) holder).view = header;
		}
		else if (holder instanceof VideoHolder) {
			
			// If header isn't null, offset the items by -1
			if (header != null) position--;
			
			((VideoHolder) holder).set(items.get(position));
		}
	}
	
	@Override
	public int getItemViewType(int position) {
		
		if (header != null && position == 0) {
			return HEADER_TYPE;
		}
		return VIDEO_TYPE;
	}
	
	@Override
	public int getItemCount() {
		
		int count = items.size();
		if (header != null) count++;
		
		return count;
	}
	
	public static class HeaderViewHolder extends RecyclerView.ViewHolder {
		
		public View view;
		
		HeaderViewHolder(View v) {
			
			super(v);
			view = v;
		}
	}
}
