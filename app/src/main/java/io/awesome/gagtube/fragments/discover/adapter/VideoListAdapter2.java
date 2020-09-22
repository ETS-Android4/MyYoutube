package io.awesome.gagtube.fragments.discover.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.fragments.discover.holder.VideoHolder2;
import io.awesome.gagtube.fragments.discover.model.VideoListResponse;

public class VideoListAdapter2 extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	public interface Listener {
		
		void onVideoClicked(int position);
		void onMoreOption(int position, View view);
	}
	
	private static final int VIDEO_TYPE = 0;
	private static final int AD_TYPE = 1;
	
	private Listener listener;
	private List<Object> recyclerViewItems;
	
	public VideoListAdapter2(Listener listener, List<Object> objects) {
		this.listener = listener;
		this.recyclerViewItems = objects;
	}
	
	public void setRecyclerViewItems(List<Object> recyclerViewItems) {
		this.recyclerViewItems = recyclerViewItems;
		notifyDataSetChanged();
	}
	
	public List<Object> getRecyclerViewItems() {
		return recyclerViewItems;
	}
	
	public Object getRecyclerViewItem(int position) {
		return recyclerViewItems.get(position);
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case VIDEO_TYPE:
				// fall through
			default:
				return new VideoHolder2(parent, listener);
		}
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		int viewType = getItemViewType(position);
		switch (viewType) {
			case VIDEO_TYPE:
				// fall through
			default:
				VideoListResponse.Item item = (VideoListResponse.Item) recyclerViewItems.get(position);
				((VideoHolder2) holder).set(item);
				break;
		}
	}
	
	@Override
	public int getItemViewType(int position) {
		Object recyclerViewItem = recyclerViewItems.get(position);
		return VIDEO_TYPE;
	}
	
	@Override
	public int getItemCount() {
		return recyclerViewItems.size();
	}
	

	

}
