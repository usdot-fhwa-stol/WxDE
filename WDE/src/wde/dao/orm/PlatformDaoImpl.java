package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.Platform;

import java.util.List;

public class PlatformDaoImpl implements PlatformDao {

    @Override
    public List<Platform> getPlatforms(Integer contributorId) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Platform> retList = null;
        try {
            retList = session.selectList("wde.MetadataMapper.selectPlatformsByContributor", contributorId);
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public List<Platform> getPlatforms() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Platform> retList = null;
        try {
            retList = session.selectList("wde.MetadataMapper.selectAllPlatforms");
        } finally {
            session.close();
        }
        return retList;
    }

}
