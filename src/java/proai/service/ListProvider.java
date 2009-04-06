package proai.service;

import java.util.Vector;

import proai.CloseableIterator;
import proai.cache.RecordCache;
import proai.error.ServerException;

public interface ListProvider<T> {

    public CloseableIterator<T> getList() throws ServerException;

    // abbreviated form of above -- gets String[]s of ("cachePath" [, "dateString"])
    public CloseableIterator<Vector<String>> getPathList() throws ServerException;

    public RecordCache getRecordCache();

    public int getIncompleteListSize();

    public String getVerb();

}