package wde.radar;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class MrmsRadarFileRetriever {

    private String webpageUrl;

//    public MrmsRadarFileRetriever() throws MalformedURLException {
//        this("http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/");
//    }

    public MrmsRadarFileRetriever(String webpageUrl) {
        this.webpageUrl = webpageUrl;
    }

    public String getNewestRadarFileUrl() throws IOException {
        URL url = new URL(this.webpageUrl);
        wde.radar.Grib2HtmlLinkMatcher grib2HtmlLinkMatcher = wde.radar.Grib2HtmlLinkMatcher.fromURL(url);
        List<String> grib2Matches = grib2HtmlLinkMatcher.match();
        Collections.sort(grib2Matches, new wde.radar.AlphanumComparator());
        Collections.reverse(grib2Matches);

        if (grib2Matches.size() == 0) {
            throw new IOException("No radar files were found at the address: " + webpageUrl);
        }

        return String.format("%s%s", this.webpageUrl, grib2Matches.get(0));
    }
}
