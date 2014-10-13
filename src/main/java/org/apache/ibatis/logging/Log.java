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
package org.apache.ibatis.logging;

/**
 * @author Clinton Begin
 */
/**
 * 日志接口
 * 
 */
public interface Log {

  //和一般的log4j很像，提供日志接口的一些方法,error, debug, warn。
  //用自己的日志类恐怕是为了通用，不绑死在某个特定的日志框架中
  //但不是也有类似的slf4j吗？为何还要自己写？可能是不想引入额外的jar包
  boolean isDebugEnabled();

  boolean isTraceEnabled();

  void error(String s, Throwable e);

  void error(String s);

  void debug(String s);

  void trace(String s);

  void warn(String s);

}
