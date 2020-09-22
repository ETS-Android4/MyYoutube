package io.awesome.gagtube.database.stream.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import io.awesome.gagtube.database.BasicDAO;
import io.awesome.gagtube.database.stream.model.StreamStateEntity;
import io.reactivex.Flowable;

@Dao
public abstract class StreamStateDAO implements BasicDAO<StreamStateEntity> {
    @Override
    @Query("SELECT * FROM " + StreamStateEntity.STREAM_STATE_TABLE)
    public abstract Flowable<List<StreamStateEntity>> getAll();

    @Override
    @Query("DELETE FROM " + StreamStateEntity.STREAM_STATE_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<StreamStateEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + StreamStateEntity.STREAM_STATE_TABLE + " WHERE " + StreamStateEntity.JOIN_STREAM_ID + " = :streamId")
    public abstract Flowable<List<StreamStateEntity>> getState(final long streamId);

    @Query("DELETE FROM " + StreamStateEntity.STREAM_STATE_TABLE + " WHERE " + StreamStateEntity.JOIN_STREAM_ID + " = :streamId")
    public abstract int deleteState(final long streamId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract void silentInsertInternal(final StreamStateEntity streamState);

    @Transaction
    public long upsert(StreamStateEntity stream) {
        silentInsertInternal(stream);
        return update(stream);
    }
}
