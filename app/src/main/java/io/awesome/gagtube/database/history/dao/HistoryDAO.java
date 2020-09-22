package io.awesome.gagtube.database.history.dao;

import io.awesome.gagtube.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
