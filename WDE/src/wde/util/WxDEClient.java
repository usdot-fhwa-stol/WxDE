/************************************************************************
 * Source filename: WxDEClient.java
 * <p/>
 * Creation date: May 8, 2014
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WxDEClient {

    private static WxDEClient instance = null;
    private HttpURLConnection conn;
    private Properties prop = null;
    private String wxdeHostStr = null;
    private String wxdeUrl = null;
    private String wxdeAuthUrl = null;
    private String loginPage = null;
    private String logoutPage = null;
    private String actionUrl = null;
    private String username = null;
    private String password = null;
    private CookieManager cm = null;

    /**
     *
     */
    private WxDEClient() {
        prop = new Properties();
        loadPropertiesFile();
        loginPage = wxdeAuthUrl + "/loginRedirect.jsp";
        logoutPage = wxdeAuthUrl + "/logout.jsp";
        actionUrl = wxdeAuthUrl + "/j_security_check";

        cm = new CookieManager();

        // make sure cookie is turn on
        CookieHandler.setDefault(cm);
    }

    /**
     * @return a reference to the WxDEClient singleton.
     */
    public static WxDEClient getIntance() {
        if (instance == null)
            instance = new WxDEClient();

        return instance;
    }

    public static void main(String[] args) {
        WxDEClient wc = WxDEClient.getIntance();
        long now = System.currentTimeMillis();
        if (args[0].equals("1"))
            wc.getDataSummary1(44.5, -94, 45.5, -92, now - 86400000, now);
        else
            wc.getDataSummary2(45, -93, 100, now - 86400000, now);
    }

    /**
     * @param lat1
     * @param long1
     * @param lat2
     * @param long2
     * @param beginTime
     * @param endTime
     */
    public void getDataSummary1(double lat1, double long1, double lat2, double long2, long beginTime, long endTime) {
        // login first.
        try {
            String loginPageContent = getPageContent(loginPage);
            String postParams = getFormParams(loginPageContent);
            sendPost(actionUrl, postParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Issue query
        String contentUrl = wxdeAuthUrl + "/GetDataSummary.jsp?"
                + "lat1=" + lat1 + "&long1=" + long1
                + "&lat2=" + lat2 + "&long2=" + long2
                + "&beginTime=" + beginTime + "&endTime=" + endTime;

        test(contentUrl, "data\\result.txt");

        // logout
        try {
            getPageContent(logoutPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getDataSummary2(double lat, double lng, double radius, long beginTime, long endTime) {
        // login first.
        try {
            String loginPageContent = getPageContent(loginPage);
            String postParams = getFormParams(loginPageContent);
            sendPost(actionUrl, postParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Issue query
        String contentUrl = wxdeAuthUrl + "/GetDataSummary.jsp?"
                + "lat=" + lat + "&long=" + lng
                + "&radius=" + radius
                + "&beginTime=" + beginTime + "&endTime=" + endTime;

        test(contentUrl, "data\\result.txt");

        // logout
        try {
            getPageContent(logoutPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void test(String urlStr, String targetFilePath) {
        try {
           /*
            * Get a connection to the URL and start up a buffered reader.
            */

            URL url = new URL(urlStr);
            url.openConnection();
            InputStream reader = url.openStream();

           /*
            * Setup a buffered file writer to write out what we read from the website.
            */
            File targetFile = new File(targetFilePath);
            targetFile.getParentFile().mkdirs();
            FileOutputStream writer = new FileOutputStream(targetFile);
            byte[] buffer = new byte[153600];
            int bytesRead = 0;

            while ((bytesRead = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
            }

            writer.close();
            reader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        System.out.println("Loading properties file");

        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "wxdeclient_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            wxdeHostStr = prop.getProperty("wxdehosturl");
            wxdeUrl = "http://" + wxdeHostStr;
            wxdeAuthUrl = wxdeUrl + "/auth";
            username = prop.getProperty("username");
            password = prop.getProperty("password");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void sendPost(String url, String postParams) throws Exception {

        URL obj = new URL(url);
        conn = (HttpURLConnection) obj.openConnection();

        // Acts like a browser
        conn.setRequestMethod("POST");
        setRequestProperties();
        String lenStr = Integer.toString(postParams.length());
        conn.setRequestProperty("Content-Length", lenStr);
        conn.setDoOutput(true);
        conn.setDoInput(true);

        // Send post request
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

        wr.writeBytes(postParams);
        wr.flush();
        wr.close();

        conn.getResponseCode();
    }

    private String getPageContent(String url) throws Exception {

        URL obj = new URL(url);
        conn = (HttpURLConnection) obj.openConnection();

        // act like a browser
        conn.setRequestMethod("GET");
        setRequestProperties();

        int responseCode = conn.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            response.append("\n");
        }
        in.close();

        return response.toString();
    }

    private void setRequestProperties() {
        conn.setRequestProperty("Host", wxdeHostStr);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:20.0) Gecko/20100101 Firefox/20.0");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("Referer", wxdeAuthUrl + "/loginRedirect.jsp");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    }

    private String getFormParams(String html)
            throws UnsupportedEncodingException {

        System.out.println("Extracting form's data...");

        Document doc = Jsoup.parse(html);

        Element loginform = doc.getElementById("loginForm");
        Elements inputElements = loginform.getElementsByTag("input");
        List<String> paramList = new ArrayList<String>();
        for (Element inputElement : inputElements) {
            String key = inputElement.attr("name");
            String value = inputElement.attr("value");

            if (key.equals("j_username"))
                value = username;
            else if (key.equals("j_password"))
                value = password;
            paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
        }

        // build parameters list
        StringBuilder result = new StringBuilder();
        for (String param : paramList) {
            if (result.length() == 0) {
                result.append(param);
            } else {
                result.append("&" + param);
            }
        }
        return result.toString();
    }
}