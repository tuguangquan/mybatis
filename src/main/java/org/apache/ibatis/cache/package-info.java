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

/**
 * Base package for caching stuff
 */
//缓存基类、异常、缓存中的key对象。
// 缓存框架按照 Key-Value方式存储，Key的生成采取规则为：[hashcode:checksum:mappedStementId:offset:limit:executeSql:queryParams]。
package org.apache.ibatis.cache;
