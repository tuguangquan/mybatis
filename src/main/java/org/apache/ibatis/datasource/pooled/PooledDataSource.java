/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.datasource.pooled;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * This is a simple, synchronous, thread-safe database connection pool.
 *
 * @author Clinton Begin
 */
/**
 * 有连接池的数据源
 * 这个是mybatis自己实行了一个连接池
 * 其他还有诸如C3P0的com.mchange.v2.c3p0.ComboPooledDataSource
 * DBCP 等等
 * 还有一些人喜欢自己用apache commons pool写一个连接池
 */
public class PooledDataSource implements DataSource {

  private static final Log log = LogFactory.getLog(PooledDataSource.class);

  //有一个池状态
  private final PoolState state = new PoolState(this);

  //里面有一个UnpooledDataSource
  private final UnpooledDataSource dataSource;

  // OPTIONAL CONFIGURATION FIELDS
  //正在使用连接的数量
  protected int poolMaximumActiveConnections = 10;
  //空闲连接数
  protected int poolMaximumIdleConnections = 5;
  //在被强制返回之前,池中连接被检查的时间
  protected int poolMaximumCheckoutTime = 20000;
  //这是给连接池一个打印日志状态机会的低层次设置,还有重新 尝试获得连接, 这些情况下往往需要很长时间 为了避免连接池没有配置时静默失 败)。
  protected int poolTimeToWait = 20000;
  //发送到数据的侦测查询,用来验证连接是否正常工作,并且准备 接受请求。默认是“NO PING QUERY SET” ,这会引起许多数据库驱动连接由一 个错误信息而导致失败
  protected String poolPingQuery = "NO PING QUERY SET";
  //开启或禁用侦测查询
  protected boolean poolPingEnabled = false;
  //用来配置 poolPingQuery 多次时间被用一次
  protected int poolPingConnectionsNotUsedFor = 0;

  private int expectedConnectionTypeCode;

  public PooledDataSource() {
    dataSource = new UnpooledDataSource();
  }

  public PooledDataSource(String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  @Override
  public Connection getConnection() throws SQLException {
	  //覆盖了DataSource.getConnection方法，每次都是pop一个Connection，即从池中取出一个来
    return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return popConnection(username, password).getProxyConnection();
  }

  @Override
  public void setLoginTimeout(int loginTimeout) throws SQLException {
    DriverManager.setLoginTimeout(loginTimeout);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    DriverManager.setLogWriter(logWriter);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return DriverManager.getLogWriter();
  }

  public void setDriver(String driver) {
    dataSource.setDriver(driver);
    forceCloseAll();
  }

  public void setUrl(String url) {
    dataSource.setUrl(url);
    forceCloseAll();
  }

  public void setUsername(String username) {
    dataSource.setUsername(username);
    forceCloseAll();
  }

  public void setPassword(String password) {
    dataSource.setPassword(password);
    forceCloseAll();
  }

  public void setDefaultAutoCommit(boolean defaultAutoCommit) {
    dataSource.setAutoCommit(defaultAutoCommit);
    forceCloseAll();
  }

  public void setDefaultTransactionIsolationLevel(Integer defaultTransactionIsolationLevel) {
    dataSource.setDefaultTransactionIsolationLevel(defaultTransactionIsolationLevel);
    forceCloseAll();
  }

  public void setDriverProperties(Properties driverProps) {
    dataSource.setDriverProperties(driverProps);
    forceCloseAll();
  }

  /*
   * The maximum number of active connections
   *
   * @param poolMaximumActiveConnections The maximum number of active connections
   */
  public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
    this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    forceCloseAll();
  }

  /*
   * The maximum number of idle connections
   *
   * @param poolMaximumIdleConnections The maximum number of idle connections
   */
  public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
    this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    forceCloseAll();
  }

  /*
   * The maximum time a connection can be used before it *may* be
   * given away again.
   *
   * @param poolMaximumCheckoutTime The maximum time
   */
  public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
    this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
    forceCloseAll();
  }

  /*
   * The time to wait before retrying to get a connection
   *
   * @param poolTimeToWait The time to wait
   */
  public void setPoolTimeToWait(int poolTimeToWait) {
    this.poolTimeToWait = poolTimeToWait;
    forceCloseAll();
  }

  /*
   * The query to be used to check a connection
   *
   * @param poolPingQuery The query
   */
  public void setPoolPingQuery(String poolPingQuery) {
    this.poolPingQuery = poolPingQuery;
    forceCloseAll();
  }

  /*
   * Determines if the ping query should be used.
   *
   * @param poolPingEnabled True if we need to check a connection before using it
   */
  public void setPoolPingEnabled(boolean poolPingEnabled) {
    this.poolPingEnabled = poolPingEnabled;
    forceCloseAll();
  }

  /*
   * If a connection has not been used in this many milliseconds, ping the
   * database to make sure the connection is still good.
   *
   * @param milliseconds the number of milliseconds of inactivity that will trigger a ping
   */
  public void setPoolPingConnectionsNotUsedFor(int milliseconds) {
    this.poolPingConnectionsNotUsedFor = milliseconds;
    forceCloseAll();
  }

  public String getDriver() {
    return dataSource.getDriver();
  }

  public String getUrl() {
    return dataSource.getUrl();
  }

  public String getUsername() {
    return dataSource.getUsername();
  }

  public String getPassword() {
    return dataSource.getPassword();
  }

  public boolean isAutoCommit() {
    return dataSource.isAutoCommit();
  }

  public Integer getDefaultTransactionIsolationLevel() {
    return dataSource.getDefaultTransactionIsolationLevel();
  }

  public Properties getDriverProperties() {
    return dataSource.getDriverProperties();
  }

  public int getPoolMaximumActiveConnections() {
    return poolMaximumActiveConnections;
  }

  public int getPoolMaximumIdleConnections() {
    return poolMaximumIdleConnections;
  }

  public int getPoolMaximumCheckoutTime() {
    return poolMaximumCheckoutTime;
  }

  public int getPoolTimeToWait() {
    return poolTimeToWait;
  }

  public String getPoolPingQuery() {
    return poolPingQuery;
  }

  public boolean isPoolPingEnabled() {
    return poolPingEnabled;
  }

  public int getPoolPingConnectionsNotUsedFor() {
    return poolPingConnectionsNotUsedFor;
  }

  /*
   * Closes all active and idle connections in the pool
   */
  public void forceCloseAll() {
    synchronized (state) {
      expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
      //关闭所有的activeConnections和idleConnections
      for (int i = state.activeConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.activeConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // ignore
        }
      }
      for (int i = state.idleConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.idleConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("PooledDataSource forcefully closed/removed all connections.");
    }
  }

  public PoolState getPoolState() {
    return state;
  }

  private int assembleConnectionTypeCode(String url, String username, String password) {
    return ("" + url + username + password).hashCode();
  }

  protected void pushConnection(PooledConnection conn) throws SQLException {

    synchronized (state) {
      //先从activeConnections中删除此connection
      state.activeConnections.remove(conn);
      if (conn.isValid()) {
        if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
      	  //如果空闲的连接太少，
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          //new一个新的Connection，加入到idle列表
          PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
          state.idleConnections.add(newConn);
          newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
          newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
          conn.invalidate();
          if (log.isDebugEnabled()) {
            log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
          }
          //通知其他线程可以来抢connection了
          state.notifyAll();
        } else {
        	//否则，即空闲的连接已经足够了
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          //那就将connection关闭就可以了
          conn.getRealConnection().close();
          if (log.isDebugEnabled()) {
            log.debug("Closed connection " + conn.getRealHashCode() + ".");
          }
          conn.invalidate();
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
        }
        state.badConnectionCount++;
      }
    }
  }

  private PooledConnection popConnection(String username, String password) throws SQLException {
    boolean countedWait = false;
    PooledConnection conn = null;
    long t = System.currentTimeMillis();
    int localBadConnectionCount = 0;

    //最外面是while死循环，如果一直拿不到connection，则不断尝试
    while (conn == null) {
      synchronized (state) {
        if (!state.idleConnections.isEmpty()) {
          //如果有空闲的连接的话
          // Pool has available connection
          //删除空闲列表里第一个，返回
          conn = state.idleConnections.remove(0);
          if (log.isDebugEnabled()) {
            log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
          }
        } else {
        	//如果没有空闲的连接
          // Pool does not have available connection
          if (state.activeConnections.size() < poolMaximumActiveConnections) {
        	  //如果activeConnections太少,那就new一个PooledConnection
            // Can create new connection
            conn = new PooledConnection(dataSource.getConnection(), this);
            if (log.isDebugEnabled()) {
              log.debug("Created connection " + conn.getRealHashCode() + ".");
            }
          } else {
        	  //如果activeConnections已经很多了，那不能再new了
            // Cannot create new connection
        	  //取得activeConnections列表的第一个（最老的）
            PooledConnection oldestActiveConnection = state.activeConnections.get(0);
            long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
            if (longestCheckoutTime > poolMaximumCheckoutTime) {
            	//如果checkout时间过长，则这个connection标记为overdue（过期）
              // Can claim overdue connection
              state.claimedOverdueConnectionCount++;
              state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
              state.accumulatedCheckoutTime += longestCheckoutTime;
              state.activeConnections.remove(oldestActiveConnection);
              if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                oldestActiveConnection.getRealConnection().rollback();
              }
              //删掉最老的连接，然后再new一个新连接
              conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
              oldestActiveConnection.invalidate();
              if (log.isDebugEnabled()) {
                log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
              }
            } else {
            	//如果checkout时间不够长，等待吧
              // Must wait
              try {
                if (!countedWait) {
                	//统计信息：等待+1
                  state.hadToWaitCount++;
                  countedWait = true;
                }
                if (log.isDebugEnabled()) {
                  log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                }
                long wt = System.currentTimeMillis();
                //睡一会儿吧
                state.wait(poolTimeToWait);
                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
              } catch (InterruptedException e) {
                break;
              }
            }
          }
        }
        if (conn != null) {
        	//如果已经拿到connection，则返回
          if (conn.isValid()) {
            if (!conn.getRealConnection().getAutoCommit()) {
              conn.getRealConnection().rollback();
            }
            conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
            //记录checkout时间
            conn.setCheckoutTimestamp(System.currentTimeMillis());
            conn.setLastUsedTimestamp(System.currentTimeMillis());
            state.activeConnections.add(conn);
            state.requestCount++;
            state.accumulatedRequestTime += System.currentTimeMillis() - t;
          } else {
            if (log.isDebugEnabled()) {
              log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
            }
            //如果没拿到，统计信息：坏连接+1
            state.badConnectionCount++;
            localBadConnectionCount++;
            conn = null;
            if (localBadConnectionCount > (poolMaximumIdleConnections + 3)) {
            	//如果好几次都拿不到，就放弃了，抛出异常
              if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Could not get a good connection to the database.");
              }
              throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
            }
          }
        }
      }

    }

    if (conn == null) {
      if (log.isDebugEnabled()) {
        log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
      }
      throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
    }

    return conn;
  }

  /*
   * Method to check to see if a connection is still usable
   *
   * @param conn - the connection to check
   * @return True if the connection is still usable
   */
  protected boolean pingConnection(PooledConnection conn) {
    boolean result = true;

    try {
      result = !conn.getRealConnection().isClosed();
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
      }
      result = false;
    }

    if (result) {
      if (poolPingEnabled) {
        if (poolPingConnectionsNotUsedFor >= 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
          try {
            if (log.isDebugEnabled()) {
              log.debug("Testing connection " + conn.getRealHashCode() + " ...");
            }
            Connection realConn = conn.getRealConnection();
            Statement statement = realConn.createStatement();
            ResultSet rs = statement.executeQuery(poolPingQuery);
            rs.close();
            statement.close();
            if (!realConn.getAutoCommit()) {
              realConn.rollback();
            }
            result = true;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
            }
          } catch (Exception e) {
            log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
            try {
              conn.getRealConnection().close();
            } catch (Exception e2) {
              //ignore
            }
            result = false;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            }
          }
        }
      }
    }
    return result;
  }

  /*
   * Unwraps a pooled connection to get to the 'real' connection
   *
   * @param conn - the pooled connection to unwrap
   * @return The 'real' connection
   */
  public static Connection unwrapConnection(Connection conn) {
    if (Proxy.isProxyClass(conn.getClass())) {
      InvocationHandler handler = Proxy.getInvocationHandler(conn);
      if (handler instanceof PooledConnection) {
        return ((PooledConnection) handler).getRealConnection();
      }
    }
    return conn;
  }

  protected void finalize() throws Throwable {
    forceCloseAll();
    super.finalize();
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException(getClass().getName() + " is not a wrapper.");
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  public Logger getParentLogger() {
    return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME); // requires JDK version 1.6
  }

}
