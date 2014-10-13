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
package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * Creates an {@link SqlSesion} out of a connection or a DataSource
 * 
 * @author Clinton Begin
 */
/**
 * 构建SqlSession的工厂.工厂模式
 *
 */
public interface SqlSessionFactory {

  //8个方法可以用来创建SqlSession实例
  SqlSession openSession();

  //自动提交
  SqlSession openSession(boolean autoCommit);
  //连接
  SqlSession openSession(Connection connection);
  //事务隔离级别
  SqlSession openSession(TransactionIsolationLevel level);

  //执行器的类型
  SqlSession openSession(ExecutorType execType);
  SqlSession openSession(ExecutorType execType, boolean autoCommit);
  SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);
  SqlSession openSession(ExecutorType execType, Connection connection);

  Configuration getConfiguration();

}
