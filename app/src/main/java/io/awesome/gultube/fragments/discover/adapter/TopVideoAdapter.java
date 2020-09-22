package io.awesome.gultube.fragments.discover.adapter;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gultube.fragments.discover.holder.TopVideoHolder;
import io.awesome.gultube.fragments.discover.model.VideoListResponse;

public class TopVideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	public interface Listener {
		
		void onVideoClicked(int position);
	}
	
	private Listener listener;
	private List<VideoListResponse.Item> items = new ArrayList<>();
	
	public TopVideoAdapter(Listener listener) {
		
		this.listener = listener;
	}
	
	public void setItems(List<VideoListResponse.Item> items) {
		
		this.items = items;
		notifyDataSetChanged();
	}
	
	public VideoListResponse.Item getItem(int position) {
		
		return items.get(position);
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		
		return new TopVideoHolder(parent, listener);
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		
		((TopVideoHolder) holder).set(getItem(position));
	}
	
	@Override
	public int getItemCount() {
		
		return items.size();
	}
}
