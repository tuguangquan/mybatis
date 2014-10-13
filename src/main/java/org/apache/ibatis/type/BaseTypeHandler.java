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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 * @author Simone Tripodi
 */
/**
 * 类型处理器的基类
 * 
 */
public abstract class BaseTypeHandler<T> extends TypeReference<T> implements TypeHandler<T> {

  protected Configuration configuration;

  public void setConfiguration(Configuration c) {
    this.configuration = c;
  }

  @Override
  public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
    //特殊情况，设置NULL
    if (parameter == null) {
      if (jdbcType == null) {
        //如果没设置jdbcType，报错啦
        throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
      }
      try {
        //设成NULL
        ps.setNull(i, jdbcType.TYPE_CODE);
      } catch (SQLException e) {
        throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . " +
                "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. " +
                "Cause: " + e, e);
      }
    } else {
      //非NULL情况，怎么设还得交给不同的子类完成, setNonNullParameter是一个抽象方法
      setNonNullParameter(ps, i, parameter, jdbcType);
    }
  }

  @Override
  public T getResult(ResultSet rs, String columnName) throws SQLException {
    T result = getNullableResult(rs, columnName);
    //通过ResultSet.wasNull判断是否为NULL
    if (rs.wasNull()) {
      return null;
    } else {
      return result;
    }
  }

  @Override
  public T getResult(ResultSet rs, int columnIndex) throws SQLException {
    T result = getNullableResult(rs, columnIndex);
    if (rs.wasNull()) {
      return null;
    } else {
      return result;
    }
  }

  @Override
  public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
    T result = getNullableResult(cs, columnIndex);
	//通过CallableStatement.wasNull判断是否为NULL
    if (cs.wasNull()) {
      return null;
    } else {
      return result;
    }
  }

	//非NULL情况，怎么设参数还得交给不同的子类完成
  public abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

	//以下3个方法是取得可能为null的结果，具体交给子类完成
  public abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

  public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;

  public abstract T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException;

}
