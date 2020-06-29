import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.submitted.basetest.Mapper;

import java.io.IOException;

/**
 * @author: gaomz
 * @date: 2020/6/25
 * @description:
 */

public class SimpleExample {
    //获取流
    String resource = "org/mybatis/example/mybatis-config.xml";
    //在构造者内部调用xmlconfig构造者解析流，将流解析后的数据创建为Configuration对象,将configuration对象赋值给工厂的Configuration属性，工厂只有这一个属性。
    SqlSessionFactory sqlSessionFactory;

    {
        try {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream(resource));
            SqlSession sqlSession = sqlSessionFactory.openSession();
            sqlSession.selectOne(new String());
            sqlSession.getMapper(Object.class);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //

}
