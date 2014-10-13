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
package org.apache.ibatis.reflection.factory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
/**
 * 默认对象工厂，所有对象都要由工厂来产生
 * 
 */
public class DefaultObjectFactory implements ObjectFactory, Serializable {

  private static final long serialVersionUID = -8855120656740914948L;

  @Override
  public <T> T create(Class<T> type) {
    return create(type, null, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    //根据接口创建具体的类
    //1.解析接口
    Class<?> classToCreate = resolveInterface(type);
    // we know types are assignable
    //2.实例化类
    return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
  }

  //默认没有属性可以设置
  @Override
  public void setProperties(Properties properties) {
    // no props for default
  }

  //2.实例化类
  private <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    try {
      Constructor<T> constructor;
      //如果没有传入constructor，调用空构造函数，核心是调用Constructor.newInstance
      if (constructorArgTypes == null || constructorArgs == null) {
        constructor = type.getDeclaredConstructor();
        if (!constructor.isAccessible()) {
          constructor.setAccessible(true);
        }
        return constructor.newInstance();
      }
      //如果传入constructor，调用传入的构造函数，核心是调用Constructor.newInstance
      constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
      if (!constructor.isAccessible()) {
        constructor.setAccessible(true);
      }
      return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
    } catch (Exception e) {
        //如果出错，包装一下，重新抛出自己的异常
      StringBuilder argTypes = new StringBuilder();
      if (constructorArgTypes != null) {
        for (Class<?> argType : constructorArgTypes) {
          argTypes.append(argType.getSimpleName());
          argTypes.append(",");
        }
      }
      StringBuilder argValues = new StringBuilder();
      if (constructorArgs != null) {
        for (Object argValue : constructorArgs) {
          argValues.append(String.valueOf(argValue));
          argValues.append(",");
        }
      }
      throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
    }
  }

  //1.解析接口,将interface转为实际class
  protected Class<?> resolveInterface(Class<?> type) {
    Class<?> classToCreate;
    if (type == List.class || type == Collection.class || type == Iterable.class) {
        //List|Collection|Iterable-->ArrayList
      classToCreate = ArrayList.class;
    } else if (type == Map.class) {
        //Map->HashMap
      classToCreate = HashMap.class;
    } else if (type == SortedSet.class) { // issue #510 Collections Support
        //SortedSet->TreeSet
      classToCreate = TreeSet.class;
    } else if (type == Set.class) {
        //Set->HashSet
      classToCreate = HashSet.class;
    } else {
        //除此以外，就用原来的类型
      classToCreate = type;
    }
    return classToCreate;
  }

  @Override
  public <T> boolean isCollection(Class<T> type) {
      //是否是Collection的子类
    return Collection.class.isAssignableFrom(type);
  }

}
