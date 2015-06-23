package wde.dao.orm;

import wde.data.Platform;

import java.util.List;

public interface PlatformDao {

    public List<Platform> getPlatforms(Integer contributorId);

    public List<Platform> getPlatforms();
}
