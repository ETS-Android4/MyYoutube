package io.awesome.gultube.fragments.detail;

import java.io.Serializable;

import io.awesome.gultube.player.playqueue.PlayQueue;

class StackItem implements Serializable {
	
	private int serviceId;
	private String title;
	private String url;
	private PlayQueue playQueue;
	
	StackItem(int serviceId, String url, String title, PlayQueue playQueue) {
		
		this.serviceId = serviceId;
		this.url = url;
		this.title = title;
		this.playQueue = playQueue;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public int getServiceId() {
		return serviceId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getUrl() {
		return url;
	}
	
	public PlayQueue getPlayQueue() {
		return playQueue;
	}
	
	@Override
	public String toString() {
		return getServiceId() + ":" + getUrl() + " > " + getTitle();
	}
}