package io.awesome.gagtube.fragments.discover.model;

import java.util.ArrayList;
import java.util.List;

public class VideoListResponse {
	
	List<Item> items = new ArrayList<>();
	
	public List<Item> getItems() {
		return items;
	}
	
	public static class Item {
		
		String id;
		Snippet snippet;
		ContentDetails contentDetails;
		Statistics statistics;
		
		public String getId() {
			return id;
		}
		
		public Snippet getSnippet() {
			return snippet;
		}
		
		public ContentDetails getContentDetails() {
			return contentDetails;
		}
		
		public Statistics getStatistics() {
			return statistics;
		}
		
		public static class Snippet {
			
			String publishedAt;
			String title;
			String description;
			Thumbnails thumbnails;
			String channelId;
			String channelTitle;
			String liveBroadcastContent;
			
			public String getPublishedAt() {
				return publishedAt;
			}
			
			public String getTitle() {
				return title;
			}
			
			public String getChannelId() {
				return channelId;
			}
			
			public String getDescription() {
				return description;
			}
			
			public Thumbnails getThumbnails() {
				return thumbnails;
			}
			
			public String getChannelTitle() {
				return channelTitle;
			}
			
			public String getLiveBroadcastContent() {
				return liveBroadcastContent;
			}
			
			public static class Thumbnails {
				
				High high;
				Standard standard;
				Maxres maxres;
				
				public High getHigh() {
					return high;
				}
				
				public Standard getStandard() {
					return standard;
				}
				
				public Maxres getMaxres() {
					return maxres;
				}
				
				public static class Standard {
					
					String url;
					
					public String getUrl() {
						return url;
					}
				}
				
				public static class High {
					
					String url;
					
					public String getUrl() {
						return url;
					}
				}
				
				public static class Maxres {
					
					String url;
					
					public String getUrl() {
						return url;
					}
				}
				
				public String getThumbnailUrl() {
					
					if (getMaxres() != null) {
						return getMaxres().getUrl();
					}
					else if (getStandard() != null) {
						return getStandard().getUrl();
					}
					else {
						return getHigh().getUrl();
					}
				}
			}
		}
		
		public static class ContentDetails {
			
			String duration;
			
			public String getDuration() {
				return duration;
			}
		}
		
		public static class Statistics {
			
			String viewCount;
			
			public String getViewCount() {
				return viewCount;
			}
		}
	}
}
