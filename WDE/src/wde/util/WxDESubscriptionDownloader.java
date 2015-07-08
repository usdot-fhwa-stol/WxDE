/************************************************************************
 * Source filename: WxDESubscriptionDownloader.java
 * <p/>
 * Creation date: Mar 4, 2014
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
import java.lang.reflect.Field;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WxDESubscriptionDownloader extends TimerTask {

    private static WxDESubscriptionDownloader instance = null;
    private FileDownloader fd = null;
    private HttpURLConnection conn;
    private Properties prop = null;
    private String baseDataFolder = null;
    private String wxdeHostStr = null;
    private String wxdeAuthUrl = null;
    private String wxdeAuth2Url = null;
    private String loginPage = null;
    private String logoutPage = null;
    private String actionUrl = null;
    private String username = null;
    private String password = null;
    private String subId = null;
    private String ext = null;
    private long pollingInterval;
    private long dataDelay;

    private Timer myTimer = null;

    /**
     *
     */
    private WxDESubscriptionDownloader() {
        prop = new Properties();
        loadPropertiesFile();
        loginPage = wxdeAuthUrl + "/loginRedirect.jsp";
        logoutPage = wxdeAuthUrl + "/logout.jsp";
        actionUrl = wxdeAuthUrl + "/j_security_check";

        // make sure cookie is turn on
        CookieHandler.setDefault(new CookieManager());

        fd = new FileDownloader();
        myTimer = new Timer();

        run();

        long currentTime = Calendar.getInstance().getTimeInMillis();
        long delay = pollingInterval - currentTime % pollingInterval + dataDelay;

        myTimer.scheduleAtFixedRate(this, delay, pollingInterval);
    }

    /**
     * @return a reference to the WxDESubscriptionDownloader singleton.
     */
    public static WxDESubscriptionDownloader getIntance() {
        if (instance == null)
            instance = new WxDESubscriptionDownloader();

        return instance;
    }

    public static void main(String[] args) {
        WxDESubscriptionDownloader.getIntance();
    }

    /**
     *
     */
    public void run() {
        // login first.
        try {
            String loginPageContent = getPageContent(loginPage);
            String postParams = getFormParams(loginPageContent, username, password);
            sendPost(actionUrl, postParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String timeFormat = "yyyyMMdd";
        SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);
        timeFormatter.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));

        Calendar cal = (Calendar) Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTime = cal.getTimeInMillis() - dataDelay;
        long currentMark = currentTime / pollingInterval * pollingInterval;

        cal.setTimeInMillis(currentMark);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String suffix = "_" + String.format("%02d", hour) + String.format("%02d", minute) + "." + ext;
        String separator = System.getProperty("file.separator");

        String dateStr = timeFormatter.format(currentMark);
        String fileName = dateStr + suffix;
        String targetFilePath = baseDataFolder + separator + dateStr + separator + fileName;
        String contentUrl = wxdeAuth2Url + "/SubShowObs.jsp?subId=" + subId + "&file=" + fileName;

        fd.download(contentUrl, targetFilePath, false);

        // logout
        try {
            getPageContent(logoutPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        System.out.println("Loading properties file");

        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "subscription_downloader.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            wxdeHostStr = prop.getProperty("wxdehosturl");
            wxdeAuthUrl = "http://" + wxdeHostStr + "/auth";
            wxdeAuth2Url = "http://" + wxdeHostStr + "/auth2";
            username = prop.getProperty("username");
            password = prop.getProperty("password");
            subId = prop.getProperty("subid");
            ext = prop.getProperty("ext");
            baseDataFolder = prop.getProperty("basedatafolder");

            // default 2 minutes for testing
            pollingInterval = Integer.valueOf(prop.getProperty("pollingInterval", "2")).intValue() * 60000;

            // default 1 hour
            dataDelay = Integer.valueOf(prop.getProperty("delay", "60")).intValue() * 60000;
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

//        int responseCode = conn.getResponseCode();
//        System.out.println("\nSending 'POST' request to URL : " + url);
//        System.out.println("Post parameters : " + postParams);
//        System.out.println("Response Code : " + responseCode);
//
//        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = in.readLine()) != null) {
//            response.append(inputLine);
//        }
//        in.close();
//
//        System.out.println(response.toString());

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

    private Map<String, Object> getMap(Object o) throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        Field[] declaredFields = o.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            result.put(field.getName(), field.get(o));
        }
        return result;
    }

    private String getFormParams(String html, String username, String password)
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
