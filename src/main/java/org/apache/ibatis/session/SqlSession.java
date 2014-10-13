/*
 *    Copyright 2009-2011 the original author or authors.
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
package org.apache.ibatis.session;

import java.io.Closeable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.BatchResult;

/**
 * The primary Java interface for working with MyBatis.
 * Through this interface you can execute commands, get mappers and manage transactions.
 *
 * @author Clinton Begin
 */
/**
 * 这是MyBatis主要的一个类，用来执行SQL，获取映射器，管理事务
 *
 */
public interface SqlSession extends Closeable {

  //语句执行方法
  //这些方法被用来执行SELECT，INSERT，UPDATE和DELETE语句。
  /**
   * Retrieve a single row mapped from the statement key
   * 获取一条记录
   * @param <T> the returned object type
   * @param statement
   * @return Mapped object
   */
  <T> T selectOne(String statement);

  /**
   * Retrieve a single row mapped from the statement key and parameter.
   * 获取一条记录
   * @param <T> the returned object type
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @return Mapped object
   */
  <T> T selectOne(String statement, Object parameter);

  /**
   * Retrieve a list of mapped objects from the statement key and parameter.
   * 获取多条记录
   * @param <E> the returned list element type
   * @param statement Unique identifier matching the statement to use.
   * @return List of mapped object
   */
  <E> List<E> selectList(String statement);

  /**
   * Retrieve a list of mapped objects from the statement key and parameter.
   * 获取多条记录
   * @param <E> the returned list element type
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @return List of mapped object
   */
  <E> List<E> selectList(String statement, Object parameter);

  /**
   * Retrieve a list of mapped objects from the statement key and parameter,
   * within the specified row bounds.
   * 获取多条记录,加上分页
   * @param <E> the returned list element type
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @param rowBounds  Bounds to limit object retrieval
   * @return List of mapped object
   */
  <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);

  /**
   * The selectMap is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * Eg. Return a of Map[Integer,Author] for selectMap("selectAuthors","id")
   * 获取多条记录,并存入Map
   * @param <K> the returned Map keys type
   * @param <V> the returned Map values type
   * @param statement Unique identifier matching the statement to use.
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   */
  <K, V> Map<K, V> selectMap(String statement, String mapKey);

  /**
   * The selectMap is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * 获取多条记录,并存入Map
   * @param <K> the returned Map keys type
   * @param <V> the returned Map values type
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @param mapKey The property to use as key for each value in the list.
   * @return Map containing key pair data.
   */
  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);

  /**
   * The selectMap is a special case in that it is designed to convert a list
   * of results into a Map based on one of the properties in the resulting
   * objects.
   * 获取多条记录,加上分页,并存入Map
   * @param <K> the returned Map keys type
   * @param <V> the returned Map values type
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @param mapKey The property to use as key for each value in the list.
   * @param rowBounds  Bounds to limit object retrieval
   * @return Map containing key pair data.
   */
  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);

  /**
   * Retrieve a single row mapped from the statement key and parameter
   * using a {@code ResultHandler}.
   * 获取一条记录,并转交给ResultHandler处理
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @param handler ResultHandler that will handle each retrieved row
   * @return Mapped object
   */
  void select(String statement, Object parameter, ResultHandler handler);

  /**
   * Retrieve a single row mapped from the statement
   * using a {@code ResultHandler}.
   * 获取一条记录,并转交给ResultHandler处理
   * @param statement Unique identifier matching the statement to use.
   * @param handler ResultHandler that will handle each retrieved row
   * @return Mapped object
   */
  void select(String statement, ResultHandler handler);

  /**
   * Retrieve a single row mapped from the statement key and parameter
   * using a {@code ResultHandler} and {@code RowBounds}
   * 获取一条记录,加上分页,并转交给ResultHandler处理
   * @param statement Unique identifier matching the statement to use.
   * @param rowBounds RowBound instance to limit the query results
   * @param handler ResultHandler that will handle each retrieved row
   * @return Mapped object
   */
  void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

  /**
   * Execute an insert statement.
   * 插入记录
   * @param statement Unique identifier matching the statement to execute.
   * @return int The number of rows affected by the insert.
   */
  int insert(String statement);

  /**
   * Execute an insert statement with the given parameter object. Any generated
   * autoincrement values or selectKey entries will modify the given parameter
   * object properties. Only the number of rows affected will be returned.
   * 插入记录
   * @param statement Unique identifier matching the statement to execute.
   * @param parameter A parameter object to pass to the statement.
   * @return int The number of rows affected by the insert.
   */
  int insert(String statement, Object parameter);

  /**
   * Execute an update statement. The number of rows affected will be returned.
   * 更新记录
   * @param statement Unique identifier matching the statement to execute.
   * @return int The number of rows affected by the update.
   */
  int update(String statement);

  /**
   * Execute an update statement. The number of rows affected will be returned.
   * 更新记录
   * @param statement Unique identifier matching the statement to execute.
   * @param parameter A parameter object to pass to the statement.
   * @return int The number of rows affected by the update.
   */
  int update(String statement, Object parameter);

  /**
   * Execute a delete statement. The number of rows affected will be returned.
   * 删除记录
   * @param statement Unique identifier matching the statement to execute.
   * @return int The number of rows affected by the delete.
   */
  int delete(String statement);

  /**
   * Execute a delete statement. The number of rows affected will be returned.
   * 删除记录
   * @param statement Unique identifier matching the statement to execute.
   * @param parameter A parameter object to pass to the statement.
   * @return int The number of rows affected by the delete.
   */
  int delete(String statement, Object parameter);

  //以下是事务控制方法,commit,rollback
  /**
   * Flushes batch statements and commits database connection.
   * Note that database connection will not be committed if no updates/deletes/inserts were called.
   * To force the commit call {@link SqlSession#commit(boolean)}
   */
  void commit();

  /**
   * Flushes batch statements and commits database connection.
   * @param force forces connection commit
   */
  void commit(boolean force);

  /**
   * Discards pending batch statements and rolls database connection back.
   * Note that database connection will not be rolled back if no updates/deletes/inserts were called.
   * To force the rollback call {@link SqlSession#rollback(boolean)}
   */
  void rollback();

  /**
   * Discards pending batch statements and rolls database connection back.
   * Note that database connection will not be rolled back if no updates/deletes/inserts were called.
   * @param force forces connection rollback
   */
  void rollback(boolean force);

  /**
   * Flushes batch statements.
   * 刷新批处理语句,返回批处理结果
   * @return BatchResult list of updated records
   * @since 3.0.6
   */
  List<BatchResult> flushStatements();

  /**
   * Closes the session
   * 关闭Session
   */
  @Override
  void close();

  /**
   * Clears local session cache
   * 清理Session缓存
   */
  void clearCache();

  /**
   * Retrieves current configuration
   * 得到配置
   * @return Configuration
   */
  Configuration getConfiguration();

  /**
   * Retrieves a mapper.
   * 得到映射器
   * 这个巧妙的使用了泛型，使得类型安全
   * 到了MyBatis 3，还可以用注解,这样xml都不用写了
   * @param <T> the mapper type
   * @param type Mapper interface class
   * @return a mapper bound to this SqlSession
   */
  <T> T getMapper(Class<T> type);

  /**
   * Retrieves inner database connection
   * 得到数据库连接
   * @return Connection
   */
  Connection getConnection();
}
