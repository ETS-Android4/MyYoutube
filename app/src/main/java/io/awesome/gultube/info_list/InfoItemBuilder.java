package io.awesome.gultube.info_list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import androidx.annotation.NonNull;
import io.awesome.gultube.info_list.holder.ChannelInfoItemHolder;
import io.awesome.gultube.info_list.holder.ChannelMiniInfoItemHolder;
import io.awesome.gultube.info_list.holder.InfoItemHolder;
import io.awesome.gultube.info_list.holder.PlaylistInfoItemHolder;
import io.awesome.gultube.info_list.holder.PlaylistMiniInfoItemHolder;
import io.awesome.gultube.info_list.holder.StreamInfoItemHolder;
import io.awesome.gultube.info_list.holder.StreamMiniInfoItemHolder;
import io.awesome.gultube.util.OnClickGesture;

public class InfoItemBuilder {
	
	private final Context context;
	
	private OnClickGesture<StreamInfoItem> onStreamSelectedListener;
	private OnClickGesture<ChannelInfoItem> onChannelSelectedListener;
	private OnClickGesture<PlaylistInfoItem> onPlaylistSelectedListener;
	
	public InfoItemBuilder(Context context) {
		this.context = context;
	}
	
	public View buildView(@NonNull ViewGroup parent, @NonNull final InfoItem infoItem) {
		
		return buildView(parent, infoItem, false);
	}
	
	public View buildView(@NonNull ViewGroup parent, @NonNull final InfoItem infoItem, boolean useMiniVariant) {
		
		InfoItemHolder holder = holderFromInfoType(parent, infoItem.getInfoType(), useMiniVariant);
		holder.updateFromItem(infoItem);
		return holder.itemView;
	}
	
	private InfoItemHolder holderFromInfoType(@NonNull ViewGroup parent, @NonNull InfoItem.InfoType infoType, boolean useMiniVariant) {
		
		switch (infoType) {
			
			case STREAM:
				return useMiniVariant ? new StreamMiniInfoItemHolder(this, parent) : new StreamInfoItemHolder(this, parent);
			case CHANNEL:
				return useMiniVariant ? new ChannelMiniInfoItemHolder(this, parent) : new ChannelInfoItemHolder(this, parent);
			case PLAYLIST:
				return useMiniVariant ? new PlaylistMiniInfoItemHolder(this, parent) : new PlaylistInfoItemHolder(this, parent);
			default:
				throw new RuntimeException("InfoType not expected = " + infoType.name());
		}
	}
	
	public Context getContext() {
		return context;
	}
	
	public OnClickGesture<StreamInfoItem> getOnStreamSelectedListener() {
		return onStreamSelectedListener;
	}
	
	public void setOnStreamSelectedListener(OnClickGesture<StreamInfoItem> listener) {
		this.onStreamSelectedListener = listener;
	}
	
	public OnClickGesture<ChannelInfoItem> getOnChannelSelectedListener() {
		return onChannelSelectedListener;
	}
	
	public void setOnChannelSelectedListener(OnClickGesture<ChannelInfoItem> listener) {
		this.onChannelSelectedListener = listener;
	}
	
	public OnClickGesture<PlaylistInfoItem> getOnPlaylistSelectedListener() {
		return onPlaylistSelectedListener;
	}
	
	public void setOnPlaylistSelectedListener(OnClickGesture<PlaylistInfoItem> listener) {
		this.onPlaylistSelectedListener = listener;
	}
	
}
