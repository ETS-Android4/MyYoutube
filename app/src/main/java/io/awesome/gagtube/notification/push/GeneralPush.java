package io.awesome.gagtube.notification.push;

public class GeneralPush implements Push {
	
	private String title;
	private String alert;
	private String imageUrl;
	private String packageName;
	private long sentTime;
	
	public GeneralPush(String title, String alert, String imageUrl, String packageName, long sentTime) {
		
		this.title = title;
		this.alert = alert;
		this.imageUrl = imageUrl;
		this.packageName = packageName;
		this.sentTime = sentTime;
	}
	
	public String getTitle() {
		return title;
	}
	
	@Override
	public String getAlert() {
		return alert;
	}
	
	public String getImageUrl() {
		return imageUrl;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	@Override
	public long getSentTime() {
		return sentTime;
	}
}
