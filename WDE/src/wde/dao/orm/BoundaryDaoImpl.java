package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.Boundary;

import java.util.List;

public class BoundaryDaoImpl implements BoundaryDao {

    @Override
    public Boundary getBoundary(String code) {
        Boundary boundary = null;
        return boundary;
    }

    @Override
    public List<Boundary> getBoundaries() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager
                .getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Boundary> retList = null;
        try {
            retList = session
                    .selectList("wde.UtilMapper.selectBoundariesKml");
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public List<Boundary> getBoundariesAndStatuses() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager
                .getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Boundary> retList = null;
        try {
            retList = session
                    .selectList("wde.UtilMapper.selectBoundariesAndStatuses");
        } finally {
            session.close();
        }
        return retList;
    }
}