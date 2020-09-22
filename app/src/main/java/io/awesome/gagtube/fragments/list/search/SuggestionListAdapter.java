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

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.awesome.gagtube.R;

public class SuggestionListAdapter extends RecyclerView.Adapter<SuggestionListAdapter.SuggestionItemHolder> {
    private final ArrayList<SuggestionItem> items = new ArrayList<>();
    private final Context context;
    private OnSuggestionItemSelected listener;
    private boolean showSuggestionHistory = true;

    public interface OnSuggestionItemSelected {
        void onSuggestionItemSelected(SuggestionItem item);

        void onSuggestionItemInserted(SuggestionItem item);

        void onSuggestionItemLongClick(SuggestionItem item);
    }

    SuggestionListAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<SuggestionItem> items) {
        this.items.clear();
        if (showSuggestionHistory) {
            this.items.addAll(items);
        } else {
            // remove history items if history is disabled
            for (SuggestionItem item : items) {
                if (!item.fromHistory) {
                    this.items.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setListener(OnSuggestionItemSelected listener) {
        this.listener = listener;
    }

    public void setShowSuggestionHistory(boolean v) {
        showSuggestionHistory = v;
    }

    @NonNull
    @Override
    public SuggestionItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SuggestionItemHolder(LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionItemHolder holder, int position) {
        final SuggestionItem currentItem = getItem(position);
        holder.updateFrom(currentItem);
        holder.queryView.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionItemSelected(currentItem);
        });
        holder.queryView.setOnLongClickListener(v -> {
            if (listener != null) listener.onSuggestionItemLongClick(currentItem);
            return true;
        });
        holder.insertView.setOnClickListener(v -> {
            if (listener != null) listener.onSuggestionItemInserted(currentItem);
        });
    }

    private SuggestionItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    static class SuggestionItemHolder extends RecyclerView.ViewHolder {
        private final TextView itemSuggestionQuery;
        private final ImageView suggestionIcon;
        private final View queryView;
        private final View insertView;

        private SuggestionItemHolder(View rootView) {
            super(rootView);
            suggestionIcon = rootView.findViewById(R.id.item_suggestion_icon);
            itemSuggestionQuery = rootView.findViewById(R.id.item_suggestion_query);

            queryView = rootView.findViewById(R.id.suggestion_search);
            insertView = rootView.findViewById(R.id.suggestion_insert);
        }

        private void updateFrom(SuggestionItem item) {
            suggestionIcon.setImageResource(item.fromHistory ? R.drawable.ic_history_dark_24dp : R.drawable.ic_search_dark_24dp);
            itemSuggestionQuery.setText(item.query);
        }

        private static int resolveResourceIdFromAttr(Context context, @AttrRes int attr) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
            int attributeResourceId = a.getResourceId(0, 0);
            a.recycle();
            return attributeResourceId;
        }
    }
}
