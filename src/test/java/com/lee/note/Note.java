package com.lee.note;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.Test;

import java.util.Properties;


public class Note {


    /**
     * mybatis框架里的占位符替换实现
     */
    @Test
    public void testPropertieParser() {
        Properties p = new Properties();
        p.put("result", "成功");
        String out = PropertyParser.parse("测试占位符替换${result}", p);
        System.out.println(out);

        ////////////字的英译//////////
        class VariableTokenHandler implements TokenHandler {
            private Properties variables;

            public VariableTokenHandler(Properties variables) {
                this.variables = variables;
            }

            public String handleToken(String content) {
                if (variables != null && variables.containsKey(content)) {
                    return variables.getProperty(content);
                }
                return "#{" + content + "}";
            }
        }

        VariableTokenHandler handler = new VariableTokenHandler(p);
        GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
        System.out.println(parser.parse("这样也可以？#{result}"));
    }


    /**
     * MetaObject对象可以用来通过反射操作对象，
     * 比如通过getValue("xx.ss.yy")的方式取到实例的属性值
     */
    @Test
    public void testMetaObject() {
        BeanForMetaObjTest bean = new BeanForMetaObjTest();
        BeanForMetaObjTest.SmallFish smallFish = new BeanForMetaObjTest.SmallFish();
        smallFish.setName("我是一条小鱼");
        bean.setBigFish(new BeanForMetaObjTest.BigFish(smallFish));
        MetaObject metaObject = SystemMetaObject.forObject(bean);
        System.out.println(metaObject.getValue("bigFish.smallFish.name"));
    }


}
