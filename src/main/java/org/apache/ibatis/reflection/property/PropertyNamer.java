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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
/**
 * 属性命名器
 * 
 */
public final class PropertyNamer {

  private PropertyNamer() {
    // Prevent Instantiation of Static Class
  }

    //方法转为属性
  public static String methodToProperty(String name) {
      //去掉get|set|is
    if (name.startsWith("is")) {
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      name = name.substring(3);
    } else {
      throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }

    //如果只有1个字母-->转为小写
    //如果大于1个字母，第二个字母非大写-->转为小写
    //String uRL -->String getuRL() {
    if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    return name;
  }

  //是否是属性
  public static boolean isProperty(String name) {
      //必须以get|set|is开头
    return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
  }

  //是否是getter
  public static boolean isGetter(String name) {
    return name.startsWith("get") || name.startsWith("is");
  }

  //是否是setter
  public static boolean isSetter(String name) {
    return name.startsWith("set");
  }

}
