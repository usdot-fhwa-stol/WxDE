package wde.dao;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

public class WdeDbSessionManager {

    private static String resourcePath = "mybatis.xml";

    private static SqlSessionFactory sqlMapper;

    public static SqlSessionFactory getSqlSessionFactory() {
        if (sqlMapper == null) {

            try {
                Reader reader = Resources.getResourceAsReader(resourcePath);
                sqlMapper = new SqlSessionFactoryBuilder().build(reader, "standalone");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return sqlMapper;
    }

}
