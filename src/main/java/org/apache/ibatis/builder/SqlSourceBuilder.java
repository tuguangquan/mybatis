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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 */
/**
 * SQL源码构建器
 * 
 */
public class SqlSourceBuilder extends BaseBuilder {

  private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  public SqlSourceBuilder(Configuration configuration) {
    super(configuration);
  }

  public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    //替换#{}中间的部分,如何替换，逻辑在ParameterMappingTokenHandler
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    String sql = parser.parse(originalSql);
    //返回静态SQL源码
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
  }

  //参数映射记号处理器，静态内部类
  private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

    private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
    private Class<?> parameterType;
    private MetaObject metaParameters;

    public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
      super(configuration);
      this.parameterType = parameterType;
      this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public List<ParameterMapping> getParameterMappings() {
      return parameterMappings;
    }

    @Override
    public String handleToken(String content) {
      //先构建参数映射
      parameterMappings.add(buildParameterMapping(content));
      //如何替换很简单，永远是一个问号，但是参数的信息要记录在parameterMappings里面供后续使用
      return "?";
    }

    //构建参数映射
    private ParameterMapping buildParameterMapping(String content) {
        //#{favouriteSection,jdbcType=VARCHAR}
        //先解析参数映射,就是转化成一个hashmap
      Map<String, String> propertiesMap = parseParameterMapping(content);
      String property = propertiesMap.get("property");
      Class<?> propertyType;
      //这里分支比较多，需要逐个理解
      if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
        propertyType = metaParameters.getGetterType(property);
      } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
        propertyType = parameterType;
      } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
        propertyType = java.sql.ResultSet.class;
      } else if (property != null) {
        MetaClass metaClass = MetaClass.forClass(parameterType);
        if (metaClass.hasGetter(property)) {
          propertyType = metaClass.getGetterType(property);
        } else {
          propertyType = Object.class;
        }
      } else {
        propertyType = Object.class;
      }
      ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
      Class<?> javaType = propertyType;
      String typeHandlerAlias = null;
      for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
        String name = entry.getKey();
        String value = entry.getValue();
        if ("javaType".equals(name)) {
          javaType = resolveClass(value);
          builder.javaType(javaType);
        } else if ("jdbcType".equals(name)) {
          builder.jdbcType(resolveJdbcType(value));
        } else if ("mode".equals(name)) {
          builder.mode(resolveParameterMode(value));
        } else if ("numericScale".equals(name)) {
          builder.numericScale(Integer.valueOf(value));
        } else if ("resultMap".equals(name)) {
          builder.resultMapId(value);
        } else if ("typeHandler".equals(name)) {
          typeHandlerAlias = value;
        } else if ("jdbcTypeName".equals(name)) {
          builder.jdbcTypeName(value);
        } else if ("property".equals(name)) {
          // Do Nothing
        } else if ("expression".equals(name)) {
          throw new BuilderException("Expression based parameters are not supported yet");
        } else {
          throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + parameterProperties);
        }
      }
      //#{age,javaType=int,jdbcType=NUMERIC,typeHandler=MyTypeHandler}
      if (typeHandlerAlias != null) {
        builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
      }
      return builder.build();
    }

    private Map<String, String> parseParameterMapping(String content) {
      try {
        return new ParameterExpression(content);
      } catch (BuilderException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
      }
    }
  }
  
}
