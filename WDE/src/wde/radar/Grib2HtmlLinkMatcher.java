package wde.radar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Grib2HtmlLinkMatcher {

    //private static final String HTML_ANCHOR_TAG_PATTERN = "<a href=\"([^\"]+)\"[^>]*>([^<]+?)\\.grib2(.gz)?</a>";
    private static final String HTML_ANCHOR_TAG_PATTERN = "<a href=\"([^\"]+\\.grib2(.gz)?)\"[^>]*>[^<]+?</a>";

    private InputStream inputStream;
    private Pattern pattern;

    public Grib2HtmlLinkMatcher(InputStream inputStream) {
        this.inputStream = inputStream;
        this.pattern = Pattern.compile(HTML_ANCHOR_TAG_PATTERN, Pattern.CASE_INSENSITIVE);
    }

//    public List<MatchResult> match() throws IOException {
//        Scanner scanner = new Scanner(inputStream);
//
//        ArrayList<MatchResult> matches = new ArrayList<MatchResult>();
//        while (scanner.findWithinHorizon(pattern, 0) != null) {
//            MatchResult result = scanner.match();
//
//            matches.add(result);
//        }
//
//        return matches;
//    }

    public List<String> match() throws IOException {
        Scanner scanner = new Scanner(inputStream);

        ArrayList<String> matches = new ArrayList<String>();
        while (scanner.findWithinHorizon(pattern, 0) != null) {
            MatchResult result = scanner.match();

            matches.add(result.group(1));
        }

        return matches;
    }

    public static Grib2HtmlLinkMatcher fromURL(URL url) throws IOException {
        return new Grib2HtmlLinkMatcher(url.openStream());
    }
}