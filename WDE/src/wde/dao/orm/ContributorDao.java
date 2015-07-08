package wde.dao.orm;

import wde.data.Contributor;

import java.util.List;


public interface ContributorDao {

    public List<Contributor> getContributors();

    public List<Contributor> getContributorsSubset();

}
