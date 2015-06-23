package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.Contributor;

import java.util.List;

public class ContributorDaoImpl implements ContributorDao {

    @Override
    public List<Contributor> getContributors() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager
                .getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Contributor> retList = null;
        try {
            retList = session
                    .selectList("wde.MetadataMapper.selectDisplayContributers");
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public List<Contributor> getContributorsSubset() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager
                .getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Contributor> retList = null;
        try {
            retList = session
                    .selectList("wde.MetadataMapper.selectDisplayContributersSubset");
        } finally {
            session.close();
        }
        return retList;
    }

}
