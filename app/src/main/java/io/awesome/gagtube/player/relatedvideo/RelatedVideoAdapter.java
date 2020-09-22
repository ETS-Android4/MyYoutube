package io.awesome.gagtube.player.relatedvideo;

import android.view.ViewGroup;

import org.schabi.newpipe.extractor.InfoItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RelatedVideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	
	public interface Listener {
		void onVideoClicked(int position);
	}
	
	private Listener listener;
	private List<InfoItem> items = new ArrayList<>();
	
	public RelatedVideoAdapter(Listener listener) {
		this.listener = listener;
	}
	
	public void setItems(List<InfoItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}
	
	public InfoItem getItem(int position) {
		return items.get(position);
	}
	
	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new RelatedVideoViewHolder(parent, listener);
	}
	
	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		((RelatedVideoViewHolder) holder).set(getItem(position));
	}
	
	@Override
	public int getItemCount() {
		return items.size();
	}
}