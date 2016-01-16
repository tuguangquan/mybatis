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
package org.apache.ibatis.session.defaults;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

/**
 * @author Clinton Begin
 */
/**
 * 默认的SqlSessionFactory
 * 
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

  private final Configuration configuration;

  public DefaultSqlSessionFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  //最终都会调用2种方法：openSessionFromDataSource,openSessionFromConnection
  //以下6个方法都会调用openSessionFromDataSource
  @Override
  public SqlSession openSession() {
    //默认的executor类型是simpleExecutor
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }
  //autoCommit  true为不支持事务，false为支持事务
  @Override
  public SqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
  }
  //执行类型，有4种，BatchExecutor、ReuseExecutor、SimpleExecutor和CachingExecutor
  @Override
  public SqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }

  // TransactionIsolationLevel 事务隔离等级，有5种，详见下面说明
  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return openSessionFromDataSource(execType, level, false);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return openSessionFromDataSource(execType, null, autoCommit);
  }

  //以下2个方法都会调用openSessionFromConnection
  @Override
  public SqlSession openSession(Connection connection) {
    return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return openSessionFromConnection(execType, connection);
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      final Environment environment = configuration.getEnvironment();
      //事务工厂，通过xml配置文件中的transactionManager 元素配置的type来选择事务
      //JDBC是直接全部使用JDBC的提交和回滚功能。它依靠使用链接的数据源来管理事务的作用域
      //MANAGED这个类型什么都不做，它从不提交、回滚和关闭连接，而是让窗口来管理事务的全部生命周期
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      //TransactionIsolationLevel事务的隔离级别有5个
      //Connection.TRANSACTION_NONE表示不支持事务的常量
      //Connection.TRANSACTION_READ_COMMITTED不可重复读和虚读可以发生
      //Connection.TRANSACTION_READ_UNCOMMITTED表示可以发生脏读 (dirty read)、不可重复读和虚读 (phantom read) 的常量
      //Connection.TRANSACTION_REPEATABLE_READ虚读可以发生
      //Connection.TRANSACTION_SERIALIZABLE指示不可以发生脏读、不可重复读和虚读的常量
      //脏读：如果一个事务对数据进行了更新，但事务还没有提交，另一个事务就可以“看到”该事务没有提交的更新结果。这样造成的问题是，
      //如果第一个事务回滚，那么第二个事务在此之前所“看到”的数据就是一笔脏数据。
      //不可重复读：指同个事务在整个事务过程中对同一笔数据进行读取，每次读取结果都不同。如果事务1在事务2的更新操作之前读取一次数据，
      //在事务2的更新操作之后再读取同一笔数据一次，两次结果是不同的。所以TRANSACTION_READ_COMMITTED是无法避免不可重复读和虚读。 
      //幻读：指同样一个查询在整个事务过程中多次执行后，查询所得的结果集是不一样的。幻读针对的是多笔记录。
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      //execType是sql操作类型
      //BatchExecutor用于执行批量sql操作
      //ReuseExecutor会重用statement执行sql操作
      //SimpleExecutor简单执行sql操作
      //CachingExecutor 在查找数据库前先查找缓存，若没有找到的话调用delegate从数据库查询，并将查询结果存入缓存中。
      final Executor executor = configuration.newExecutor(tx, execType);
      //然后产生一个SqlSession
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      //如果打开事务出错，则关闭它
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      //最后清空错误上下文
      ErrorContext.instance().reset();
    }
  }

  private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
    try {
      boolean autoCommit;
      try {
        autoCommit = connection.getAutoCommit();
      } catch (SQLException e) {
        // Failover to true, as most poor drivers
        // or databases won't support transactions
        autoCommit = true;
      }      
      final Environment environment = configuration.getEnvironment();
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      final Transaction tx = transactionFactory.newTransaction(connection);
      final Executor executor = configuration.newExecutor(tx, execType);
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
  //事务工厂，mybatis对于事务的管理有两种形式,
  //(1)使用JDBC的事务管理机制,就是利用java.sql.Connection对象完成对事务的提交
  //(2)使用MANAGED的事务管理机制，这种机制mybatis自身不会去实现事务管理，而是让程序的容器（JBOSS,WebLogic）来实现对事务的管理
  private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
    //如果没有配置事务工厂，则返回托管事务工厂
    if (environment == null || environment.getTransactionFactory() == null) {
      //没有配置的话就使用manage形式
      return new ManagedTransactionFactory();
    }
    return environment.getTransactionFactory();
  }

  private void closeTransaction(Transaction tx) {
    if (tx != null) {
      try {
        tx.close();
      } catch (SQLException ignore) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

}
