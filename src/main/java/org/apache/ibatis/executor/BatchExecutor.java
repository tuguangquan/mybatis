/*
 *    Copyright 2009-2014 the original author or authors.
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
package org.apache.ibatis.executor;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Jeff Butler
 */
public class BatchExecutor extends BaseExecutor {

  public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE + 1002;

  //JDBC Statement接口:其实现类有PreparedStatement,PreparedStatement
  private final List<Statement> statementList = new ArrayList<Statement>();
  //BatchResult主体是封装好的sql，这儿声明一个List主要是为了sql的复用
  private final List<BatchResult> batchResultList = new ArrayList<BatchResult>();
  //executor当前的sql
  private String currentSql;
  private MappedStatement currentStatement;

  //BatchExecutor也支持事务
  public BatchExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  //为父类BaseExecutor中的doUpdate()方法的批量实现
  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    final Configuration configuration = ms.getConfiguration();
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    final BoundSql boundSql = handler.getBoundSql();
    final String sql = boundSql.getSql();
    final Statement stmt;
    //获取当前sql对应的Statement
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      int last = statementList.size() - 1;
      stmt = statementList.get(last);
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    } else {
      //如果没有当前sql对应的Statement,创建一个Statement并添加到batchResultList,方便下次复用
      Connection connection = getConnection(ms.getStatementLog());
      stmt = handler.prepare(connection);
      currentSql = sql;
      currentStatement = ms;
      statementList.add(stmt);
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    //给每个Statement设置参数
    handler.parameterize(stmt);
    //调用Statement.batch()方法,批量更新
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }

  //查询方法
  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
          throws SQLException {
    Statement stmt = null;
    try {
      //调用父类BaseExcutor的flushStatements()的方法执行BatchExecutor中doFlushStatements()方法
      flushStatements();
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameterObject, rowBounds, resultHandler, boundSql);
      Connection connection = getConnection(ms.getStatementLog());
      stmt = handler.prepare(connection);
      //设置参数
      handler.parameterize(stmt);
      //查询方法，并将查询到的结果交给resultHandler封装
      return handler.<E>query(stmt, resultHandler);
    } finally {
      closeStatement(stmt);
    }
  }

  //实现父类BaseExcutor的静态方法
  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    try {
      List<BatchResult> results = new ArrayList<BatchResult>();
      if (isRollback) {
        //如果调用了父类BaseExecutor中回滚方法,会清空要执行的sql语句
        return Collections.emptyList();
      }
      for (int i = 0, n = statementList.size(); i < n; i++) {
        Statement stmt = statementList.get(i);
        BatchResult batchResult = batchResultList.get(i);
        try {
          //对多个Statement的查询结果进行拦截,并设置返回的主键
          //注:BatchExecutor的doQuery()不会执行此逻辑 查询时不存在sql语句复用 此时statementList为空
          //反而是对doUpdate()方法进行了拦截,如果是对数据库进行批量添加,这里能够往bean中自动注入主键
          batchResult.setUpdateCounts(stmt.executeBatch());
          MappedStatement ms = batchResult.getMappedStatement();
          List<Object> parameterObjects = batchResult.getParameterObjects();
          KeyGenerator keyGenerator = ms.getKeyGenerator();
          if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
            Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
            jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
          } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) { //issue #141
            for (Object parameter : parameterObjects) {
              keyGenerator.processAfter(this, ms, stmt, parameter);
            }
          }
        } catch (BatchUpdateException e) {
          StringBuilder message = new StringBuilder();
          message.append(batchResult.getMappedStatement().getId())
                  .append(" (batch index #")
                  .append(i + 1)
                  .append(")")
                  .append(" failed.");
          if (i > 0) {
            message.append(" ")
                    .append(i)
                    .append(" prior sub executor(s) completed successfully, but will be rolled back.");
          }
          throw new BatchExecutorException(message.toString(), e, results, batchResult);
        }
        results.add(batchResult);
      }
      return results;
    } finally {
      for (Statement stmt : statementList) {
        closeStatement(stmt);
      }
      currentSql = null;
      statementList.clear();
      batchResultList.clear();
    }
  }

}
