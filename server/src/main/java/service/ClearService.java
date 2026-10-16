package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

/** Clears application data without requiring an authorization token. */
public class ClearService {
    private final DataAccess dataAccess;

    public ClearService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void clear() throws DataAccessException {
        dataAccess.clear();
    }
}
