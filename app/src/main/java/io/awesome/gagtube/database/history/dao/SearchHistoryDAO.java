package io.awesome.gagtube.database.history.dao;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;
import io.awesome.gagtube.database.history.model.SearchHistoryEntry;
import io.reactivex.Flowable;

@Dao
public interface SearchHistoryDAO extends HistoryDAO<SearchHistoryEntry> {

    String ORDER_BY_CREATION_DATE = " ORDER BY " + SearchHistoryEntry.CREATION_DATE + " DESC";

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME +
            " WHERE " + SearchHistoryEntry.ID + " = (SELECT MAX(" + SearchHistoryEntry.ID + ") FROM " + SearchHistoryEntry.TABLE_NAME + ")")
    @Nullable
	SearchHistoryEntry getLatestEntry();

    @Query("DELETE FROM " + SearchHistoryEntry.TABLE_NAME)
    @Override
    int deleteAll();

    @Query("DELETE FROM " + SearchHistoryEntry.TABLE_NAME + " WHERE " + SearchHistoryEntry.SEARCH + " = :query")
    int deleteAllWhereQuery(String query);

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + ORDER_BY_CREATION_DATE)
    @Override
    Flowable<List<SearchHistoryEntry>> getAll();

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + " GROUP BY " + SearchHistoryEntry.SEARCH + ORDER_BY_CREATION_DATE + " LIMIT :limit")
    Flowable<List<SearchHistoryEntry>> getUniqueEntries(int limit);

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + " WHERE " + SearchHistoryEntry.SERVICE_ID + " = :serviceId" + ORDER_BY_CREATION_DATE)
    @Override
    Flowable<List<SearchHistoryEntry>> listByService(int serviceId);

    @Query("SELECT * FROM " + SearchHistoryEntry.TABLE_NAME + " WHERE " + SearchHistoryEntry.SEARCH + " LIKE :query || '%' GROUP BY " + SearchHistoryEntry.SEARCH + " LIMIT :limit")
    Flowable<List<SearchHistoryEntry>> getSimilarEntries(String query, int limit);
}
