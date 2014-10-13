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
package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * SPI for cache providers.
 * 
 * One instance of cache will be created for each namespace.
 * 
 * The cache implementation must have a constructor that receives the cache id as an String parameter.
 * 
 * MyBatis will pass the namespace as id to the constructor.
 * 
 * <pre>
 * public MyCache(final String id) {
 *  if (id == null) {
 *    throw new IllegalArgumentException("Cache instances require an ID");
 *  }
 *  this.id = id;
 *  initialize();
 * }
 * </pre>
 *
 * @author Clinton Begin
 */

/**
 * 缓存
 *
 */
public interface Cache {

  /**
   * @return The identifier of this cache
   */
    //取得ID
  String getId();

  /**
   * @param key Can be any object but usually it is a {@link CacheKey}
   * @param value The result of a select.
   */
  //存入值
  void putObject(Object key, Object value);

  /**
   * @param key The key
   * @return The object stored in the cache.
   */
  //获取值
  Object getObject(Object key);

  /**
   * Optional. It is not called by the core.
   * 
   * @param key The key
   * @return The object that was removed
   */
  //删除值
  Object removeObject(Object key);

  /**
   * Clears this cache instance
   */  
  //清空
  void clear();

  /**
   * Optional. This method is not called by the core.
   * 
   * @return The number of elements stored in the cache (not its capacity).
   */
  //取得大小
  int getSize();
  
  /** 
   * Optional. As of 3.2.6 this method is no longer called by the core.
   *  
   * Any locking needed by the cache must be provided internally by the cache provider.
   * 
   * @return A ReadWriteLock 
   */
  //取得读写锁, 从3.2.6开始没用了，要SPI自己实现锁
  ReadWriteLock getReadWriteLock();

}