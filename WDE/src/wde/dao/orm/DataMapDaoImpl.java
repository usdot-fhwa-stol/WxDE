package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.DataMapState;

import java.util.List;

public class DataMapDaoImpl implements DataMapDao {

    @Override
    public DataMapState getDataMapState(String code) {
        return null;
    }

    @Override
    public List<DataMapState> getStates() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<DataMapState> retList = null;
        try {
            retList = session.selectList("wde.UtilMapper.selectDataMapStates");
        } finally {
            session.close();
        }
        return retList;
    }

}
