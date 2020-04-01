package cn.lamb.utils;

import java.sql.SQLException;

/**
 * @Description 有关闭自动提交、手动提交、事务回滚方法
 * @Date 2020/3/31 22:56
 * @Creator Lambert
 */
public class TransactionManager {

    private ConnectionUtils connectionUtils;

    public void setConnectionUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    /**
     * 关闭自动提交
     *
     * @throws SQLException
     */
    public void disableAutoCommit() throws SQLException {
        connectionUtils.getCurrentThreadConn().setAutoCommit(false);
    }

    /**
     * 提交
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        connectionUtils.getCurrentThreadConn().commit();
    }

    /**
     * 回滚
     *
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        connectionUtils.getCurrentThreadConn().rollback();
    }

}
