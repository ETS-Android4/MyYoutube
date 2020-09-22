package io.awesome.gagtube.util.recyclerview;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.ButterKnife;

public abstract class AbstractViewHolder extends RecyclerView.ViewHolder {
	
	public AbstractViewHolder(ViewGroup parent, @LayoutRes int layoutResId) {
		
		super(LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false));
		ButterKnife.bind(this, itemView);
	}
}
