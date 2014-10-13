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
package org.apache.ibatis.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A parameter handler sets the parameters of the {@code PreparedStatement}
 *
 * @author Clinton Begin
 */
/**
 * 参数处理器
 * 
 */
public interface ParameterHandler {

  //得到参数
  Object getParameterObject();

  //设置参数
  void setParameters(PreparedStatement ps)
      throws SQLException;

}
