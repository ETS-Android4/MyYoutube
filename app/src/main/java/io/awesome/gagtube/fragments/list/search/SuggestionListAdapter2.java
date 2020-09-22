package io.awesome.gagtube.fragments.list.search;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.R;

public class SuggestionListAdapter2 extends RecyclerView.Adapter<SuggestionListAdapter2.SuggestionItemHolder> {
	
	private List<SuggestionItem> items = new ArrayList<>();
	private OnSuggestionItemSelected listener;
	
	public interface OnSuggestionItemSelected {
		
		void onSuggestionItemRemoved(SuggestionItem item);
		void onSuggestionItemClicked(SuggestionItem item);
	}
	
	public SuggestionListAdapter2(OnSuggestionItemSelected listener) {
		
		this.listener = listener;
	}
	
	public void setItems(List<SuggestionItem> items) {
		
		this.items = items;
		notifyDataSetChanged();
	}
	
	public void clearItems() {
		
		items.clear();
		notifyDataSetChanged();
	}
	
	@NonNull
	@Override
	public SuggestionItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		
		return new SuggestionItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_suggestion2, parent, false));
	}
	
	@Override
	public void onBindViewHolder(@NonNull SuggestionItemHolder holder, int position) {
		
		final SuggestionItem currentItem = getItem(position);
		holder.updateFrom(currentItem);
		
		holder.itemView.setOnClickListener(v -> listener.onSuggestionItemClicked(currentItem));
		holder.removeView.setOnClickListener(v -> listener.onSuggestionItemRemoved(currentItem));
	}
	
	private SuggestionItem getItem(int position) {
		return items.get(position);
	}
	
	@Override
	public int getItemCount() {
		return items.size();
	}
	
	static class SuggestionItemHolder extends RecyclerView.ViewHolder {
		
		private final TextView itemSuggestionQuery;
		private final ImageView suggestionIcon;
		private final View removeView;
		
		private SuggestionItemHolder(View rootView) {
			
			super(rootView);
			
			suggestionIcon = rootView.findViewById(R.id.item_suggestion_icon);
			itemSuggestionQuery = rootView.findViewById(R.id.item_suggestion_query);
			removeView = rootView.findViewById(R.id.suggestion_remove);
		}
		
		private void updateFrom(SuggestionItem item) {
			
			suggestionIcon.setImageResource(R.drawable.ic_history_dark_24dp);
			itemSuggestionQuery.setText(item.query);
		}
		
		private static int resolveResourceIdFromAttr(Context context) {
			
			TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{R.attr.history});
			int attributeResourceId = typedArray.getResourceId(0, 0);
			typedArray.recycle();
			return attributeResourceId;
		}
	}
}
