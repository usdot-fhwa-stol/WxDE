package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.Country;

import java.util.List;

public class CountriesDaoImpl implements CountriesDao {

    @Override
    public List<Country> getCountries() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Country> retList = null;
        try {
            retList = session.selectList("wde.UtilMapper.selectAllCountries");
        } finally {
            session.close();
        }
        return retList;
    }

}
