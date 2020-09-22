package io.awesome.gultube.retrofit;

import io.awesome.gultube.fragments.discover.model.VideoListResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public interface RestApi {
	
	@GET("videos?part=snippet,contentDetails,statistics&chart=mostPopular")
	Observable<VideoListResponse> getVideosByCategory(@Query("key") String key,
													  @Query("regionCode") String regionCode,
													  @Query("hl") String language,
													  @Query("videoCategoryId") int videoCategoryId,
													  @Query("maxResults") int maxResults);
}
