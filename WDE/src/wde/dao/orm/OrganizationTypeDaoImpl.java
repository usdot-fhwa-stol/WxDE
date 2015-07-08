package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.OrganizationType;

import java.util.List;

public class OrganizationTypeDaoImpl implements OrganizationTypeDao {

    @Override
    public List<OrganizationType> getOrganizationTypes() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<OrganizationType> retList = null;
        try {
            retList = session.selectList("wde.UtilMapper.selectAllOrganizationTypes");
        } finally {
            session.close();
        }
        return retList;
    }

}
