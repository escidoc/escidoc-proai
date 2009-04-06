package proai.cache;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Vector;

import org.apache.log4j.Logger;

import proai.CloseableIterator;
import proai.error.ServerException;

/**
 * An iterator around a database <code>ResultSet</code> that provides
 * a <code>String[]</code> for each row.
 *
 * Rows in the result set contain two values.  The first value is a 
 * <code>String</code> representing a relative filesystem path.  The second
 * value is a <code>long</code> representing a date.
 *
 * The returned <code>String[]</code> for each row will have two parts:
 * The first is the relative filesystem path and the second is an 
 * ISO8601-formatted date (second precision).
 */
public class StringResultIterator implements CloseableIterator<Vector<String>> {

    private static final Logger logger =
            Logger.getLogger(StringResultIterator.class.getName());

    private Connection m_conn;
    private Statement m_stmt;
    private Statement new_stmt;
    private ResultSet m_rs;

    private boolean m_closed;

    //private String[] m_nextStringArray;
    private Vector<String> m_nextStringVector;

    private boolean m_exhausted;

    public StringResultIterator(Connection conn,
                                Statement stmt,
                                ResultSet rs,
                                Statement newStmt) throws ServerException {
        logger.debug("Constructing");
        new_stmt = newStmt;
        m_conn = conn;
        m_stmt = stmt;
        m_rs = rs;
        m_closed = false;
        //m_nextStringArray = getNext();
        m_nextStringVector = getNext();
    }

    public boolean hasNext() {
        return m_nextStringVector != null;
    }

    public Vector<String> next() throws ServerException {
       Vector <String> next = m_nextStringVector;
        m_nextStringVector = getNext();
        return next;
    }

    private Vector<String> getNext() throws ServerException {
        if (m_exhausted) return null;
        try {
            if (m_rs.next()) {
                Vector<String> result = new Vector<String> ();
                Integer recordKey = m_rs.getInt(1);
                result.add(m_rs.getString(2));
                Date d = new Date(m_rs.getLong(3));
                try {
                    result.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(d));
                } catch (Exception e) { // won't happen
                    e.printStackTrace();
                }
                ResultSet rs = null;
                try {
                    String sql = "SELECT rcSet.setSpec from rcSet, rcMembership "
                        + "WHERE rcMembership.recordKey = " + recordKey
                        + " AND rcMembership.setKey = rcSet.setKey";
                    logger.debug("Executing query: " + sql);
                    rs = new_stmt.executeQuery(sql);
                    while (rs.next()) {
                        String setSpec = rs.getString(1);
                        result.add(setSpec);
                    } 
                } catch (SQLException e) {
                    throw new ServerException("Error determining set specs for record with a key : " + recordKey, e);
                } finally {
                    if (rs != null) try { rs.close(); } catch (Exception e) { }
                }
                return result;
            } else {
                m_exhausted = true;
                close(); // since we know it was exhausted
                return null;
            }
        } catch (SQLException e) {
            close(); // since we know there was an error
            throw new ServerException("Error pre-getting next string from db", e);
        }
    }

    public void close() {
        if (!m_closed) {
            if (m_rs != null) try { m_rs.close(); m_rs = null; } catch (Exception e) { }
            if (m_stmt != null) try { m_stmt.close(); m_stmt = null; } catch (Exception e) { }
            if (new_stmt != null) try { new_stmt.close(); new_stmt = null; } catch (Exception e) { }
            RecordCache.releaseConnection(m_conn);

            // gc and print memory stats when we're done with the
            // (potentially large) resultset.
            long startTime = System.currentTimeMillis();
            long startFreeBytes = Runtime.getRuntime().freeMemory();
            System.gc();
            long ms = System.currentTimeMillis() - startTime;
            long currentFreeBytes = Runtime.getRuntime().freeMemory();
            logger.info("GC ran in " + ms + "ms and free memory "
                    + "went from " + startFreeBytes + " to " 
                    + currentFreeBytes + " bytes.");

            m_closed = true;
            logger.info("Closed.");
        }
    }


    public void finalize() {
        close();
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("StringResultIterator does not support remove().");
    }

}