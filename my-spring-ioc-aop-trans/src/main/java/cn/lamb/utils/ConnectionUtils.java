package cn.lamb.utils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @Description TODO
 * @Date 2020/3/31 22:29
 * @Creator Lambert
 */
public class ConnectionUtils {

    ThreadLocal<Connection> threadLocal = new ThreadLocal<>();//将当前连接和线程挂钩

    /**
     * 对外提供获取与当前线程挂钩的数据库连接
     */
    public Connection getCurrentThreadConn() throws SQLException {
        Connection connection = threadLocal.get();//通过调用ThreadLocal实例的get方法可以获取与其挂钩的Connection
        if (connection == null) {//如果当前的Connection为空，说明是当前线程第一次来获取连接
            connection = DruidUtils.getInstance().getConnection();//第一次可以从线程池拿
            threadLocal.set(connection);//将connection设入threadLocal
        }
        return connection;//否则就是第一次以后来获取连接，那么直接返回
    }


}
