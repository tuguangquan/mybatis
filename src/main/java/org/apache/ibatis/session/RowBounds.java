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

/**
 * @author Clinton Begin
 */
/**
 * 分页用，记录限制
 *
 */
public class RowBounds {

  public static final int NO_ROW_OFFSET = 0;
  public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;
  public static final RowBounds DEFAULT = new RowBounds();

  //offset,limit就等于一般分页的start,limit,
  private int offset;
  private int limit;

  //默认是一页Integer.MAX_VALUE条
  public RowBounds() {
    this.offset = NO_ROW_OFFSET;
    this.limit = NO_ROW_LIMIT;
  }

  public RowBounds(int offset, int limit) {
    this.offset = offset;
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public int getLimit() {
    return limit;
  }

}
