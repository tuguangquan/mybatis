/*
 *    Copyright 2009-2013 the original author or authors.
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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 */
/**
 * XML映射构建器，建造者模式,继承BaseBuilder
 *
 */
public class XMLMapperBuilder extends BaseBuilder {

  private XPathParser parser;
  //映射器构建助手
  private MapperBuilderAssistant builderAssistant;
  //用来存放sql片段的哈希表
  private Map<String, XNode> sqlFragments;
  private String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  //解析
  public void parse() {
    //如果没有加载过再加载，防止重复加载
    if (!configuration.isResourceLoaded(resource)) {
      //配置mapper
      configurationElement(parser.evalNode("/mapper"));
      //标记一下，已经加载过了
      configuration.addLoadedResource(resource);
      //绑定映射器到namespace
      bindMapperForNamespace();
    }

    //还有没解析完的东东这里接着解析？  
    parsePendingResultMaps();
    parsePendingChacheRefs();
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

	//配置mapper元素
//	<mapper namespace="org.mybatis.example.BlogMapper">
//	  <select id="selectBlog" parameterType="int" resultType="Blog">
//	    select * from Blog where id = #{id}
//	  </select>
//	</mapper>
  private void configurationElement(XNode context) {
    try {
      //1.配置namespace
      String namespace = context.getStringAttribute("namespace");
      if (namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      builderAssistant.setCurrentNamespace(namespace);
      //2.配置cache-ref
      cacheRefElement(context.evalNode("cache-ref"));
      //3.配置cache
      cacheElement(context.evalNode("cache"));
      //4.配置parameterMap(已经废弃,老式风格的参数映射)
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      //5.配置resultMap(高级功能)
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      //6.配置sql(定义可重用的 SQL 代码段)
      sqlElement(context.evalNodes("/mapper/sql"));
      //7.配置select|insert|update|delete TODO
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. Cause: " + e, e);
    }
  }

  //7.配置select|insert|update|delete
  private void buildStatementFromContext(List<XNode> list) {
    //调用7.1构建语句
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  //7.1构建语句
  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      //构建所有语句,一个mapper下可以有很多select
      //语句比较复杂，核心都在这里面，所以调用XMLStatementBuilder
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
          //核心XMLStatementBuilder.parseStatementNode
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
          //如果出现SQL语句不完整，把它记下来，塞到configuration去
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  private void parsePendingChacheRefs() {
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolveCacheRef();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Cache ref is still missing a resource...
        }
      }
    }
  }

  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  //2.配置cache-ref,在这样的 情况下你可以使用 cache-ref 元素来引用另外一个缓存。 
//<cache-ref namespace="com.someone.application.data.SomeMapper"/>
  private void cacheRefElement(XNode context) {
    if (context != null) {
      //增加cache-ref
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  //3.配置cache
//  <cache
//  eviction="FIFO"
//  flushInterval="60000"
//  size="512"
//  readOnly="true"/>
  private void cacheElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type", "PERPETUAL");
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      String eviction = context.getStringAttribute("eviction", "LRU");
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      Long flushInterval = context.getLongAttribute("flushInterval");
      Integer size = context.getIntAttribute("size");
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      boolean blocking = context.getBooleanAttribute("blocking", false);
      //读入额外的配置信息，易于第三方的缓存扩展,例:
//    <cache type="com.domain.something.MyCustomCache">
//      <property name="cacheFile" value="/tmp/my-custom-cache.tmp"/>
//    </cache>
      Properties props = context.getChildrenAsProperties();
      //调用builderAssistant.useNewCache
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  //4.配置parameterMap
  //已经被废弃了!老式风格的参数映射。可以忽略
  private void parameterMapElement(List<XNode> list) throws Exception {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        @SuppressWarnings("unchecked")
        Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  //5.配置resultMap,高级功能
  private void resultMapElements(List<XNode> list) throws Exception {
      //基本上就是循环把resultMap加入到Configuration里去,保持2份，一份缩略，一分全名
    for (XNode resultMapNode : list) {
      try {
          //循环调resultMapElement
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  //5.1 配置resultMap
  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.<ResultMapping> emptyList());
  }

  //5.1 配置resultMap
  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
    //错误上下文
//取得标示符   ("resultMap[userResultMap]")
//    <resultMap id="userResultMap" type="User">
//      <id property="id" column="user_id" />
//      <result property="username" column="username"/>
//      <result property="password" column="password"/>
//    </resultMap>
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    String id = resultMapNode.getStringAttribute("id",
        resultMapNode.getValueBasedIdentifier());
    //一般拿type就可以了，后面3个难道是兼容老的代码？
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    //高级功能，还支持继承?
//  <resultMap id="carResult" type="Car" extends="vehicleResult">
//    <result property="doorCount" column="door_count" />
//  </resultMap>
    String extend = resultMapNode.getStringAttribute("extends");
    //autoMapping
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    Class<?> typeClass = resolveClass(type);
    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
    resultMappings.addAll(additionalResultMappings);
    List<XNode> resultChildren = resultMapNode.getChildren();
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        //解析result map的constructor
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        //解析result map的discriminator
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        List<ResultFlag> flags = new ArrayList<ResultFlag>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        //调5.1.1 buildResultMappingFromContext,得到ResultMapping
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    //最后再调ResultMapResolver得到ResultMap
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

//解析result map的constructor
//<constructor>
//  <idArg column="blog_id" javaType="int"/>
//</constructor>
  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<ResultFlag>();
      //结果标志加上ID和CONSTRUCTOR
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

//解析result map的discriminator
//<discriminator javaType="int" column="draft">
//  <case value="1" resultType="DraftPost"/>
//</discriminator>
  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    Class<?> javaTypeClass = resolveClass(javaType);
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<String, String>();
    for (XNode caseChild : context.getChildren()) {
      String value = caseChild.getStringAttribute("value");
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  //6 配置sql(定义可重用的 SQL 代码段)
  private void sqlElement(List<XNode> list) throws Exception {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  //6.1 配置sql
//<sql id="userColumns"> id,username,password </sql>
  private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      //比较简单，就是将sql片段放入hashmap,不过此时还没有解析sql片段
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        sqlFragments.put(id, context);
      }
    }
  }
  
  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      if (!requiredDatabaseId.equals(databaseId)) {
        return false;
      }
    } else {
      if (databaseId != null) {
        return false;
      }
      // skip this fragment if there is a previous one with a not null databaseId
      //如果有重名的id了
      //<sql id="userColumns"> id,username,password </sql>
      if (this.sqlFragments.containsKey(id)) {
        XNode context = this.sqlFragments.get(id);
        //如果之前那个重名的sql id有databaseId，则false，否则难道true？这样新的sql覆盖老的sql？？？
        if (context.getStringAttribute("databaseId") != null) {
          return false;
        }
      }
    }
    return true;
  }

  //5.1.1 构建resultMap
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
	//<id property="id" column="author_id"/>
	//<result property="username" column="author_username"/>
    String property = context.getStringAttribute("property");
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    //处理嵌套的result map
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.<ResultMapping> emptyList()));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resulSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    Class<?> javaTypeClass = resolveClass(javaType);
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    //又去调builderAssistant.buildResultMapping
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resulSet, foreignColumn, lazy);
  }
  
  //5.1.1.1 处理嵌套的result map
  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
	  //处理association|collection|case
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
    	
//    	<resultMap id="blogResult" type="Blog">
//    	  <association property="author" column="author_id" javaType="Author" select="selectAuthor"/>
//    	</resultMap>
//如果不是嵌套查询
      if (context.getStringAttribute("select") == null) {
    	//则递归调用5.1 resultMapElement
        ResultMap resultMap = resultMapElement(context, resultMappings);
        return resultMap.getId();
      }
    }
    return null;
  }

  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) {
        if (!configuration.hasMapper(boundType)) {
          // Spring may not know the real resource name so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          configuration.addLoadedResource("namespace:" + namespace);
          configuration.addMapper(boundType);
        }
      }
    }
  }

}
