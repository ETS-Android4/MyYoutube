package io.awesome.gagtube.fragments.list;

import io.awesome.gagtube.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
