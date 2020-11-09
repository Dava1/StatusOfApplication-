package Main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class StatusOfApplication {

    private HttpURLConnection conn;
    private static String url ="https://frs.gov.cz/en/ioff/application-status";
    private final String USER_AGENT=
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36";
    private CookieStore cookieStore;

    public StatusOfApplication() throws IOException {
    }
        public static void main(String[] args){
           try {
               StatusOfApplication http = new StatusOfApplication();
               http.getCookieUsingHandler();
                //send POST
                String postParams =http.getPostParams(http.getRequest(),
                        "21880","DP","2020");
                 http.postRequest(url, postParams);
                 String response = http.getRequest();
                    System.out.println(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    /**
     * this method provaide HTTPs GET request and retrieve html for next handling
     * @return String file which contains html code
     * @throws Exception
     */
        public String getRequest() throws Exception{
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",USER_AGENT);
            conn.setRequestProperty("Set-Cookie","SSESSbf8afec40cd9c9d3bf5d990116b04c88=deleted; expires=Thu," +
                    " 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; domain=.frs.gov.cz; secure; HttpOnly");
            if(!cookieStore.getCookies().isEmpty()){
                conn.setRequestProperty("Cookie",cookieStore.getCookies().get(0).toString());
            }
            conn.getContent();
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine())!= null){
                response.append(inputLine);
            }
            in.close();
            String headerName = null;
            setCookieStore(conn.getURL().toString(),conn);
            System.out.println("\nProviding GET request on adress "+conn.getURL());
            System.out.println("Response code "+conn.getResponseCode());
            System.out.println("Cookie " + cookieStore.getCookies());

            return response.toString();
        }

    /**
     * through this is method. i send POST request
     * @implNote  got response cod 200, but i need 302
     * @param url
     * @param postParams
     * @throws Exception
     */
    public void postRequest(String url, String postParams) throws Exception {
        conn =(HttpURLConnection)new URL(url).openConnection();
        //conn.setFollowRedirects(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent",USER_AGENT);
        conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept-Language","en-US,en;q=0.9,ru;q=0.8,cs;q=0.7");
       //this is just test
        conn.setRequestProperty("Set-Cookie",
        "SSESSbf8afec40cd9c9d3bf5d990116b04c88=VEZj40XttOhi8xdOnxyFWBMJZmoNFsX0aNH_y5tyfM0; expires=Tue, " +
                "01-Dec-2020 23:33:00 GMT; Max-Age=2000000; path=/; domain=.frs.gov.cz; secure; HttpOnly");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        //sending post request
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();
        //send request
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes(postParams);
        out.flush();
        out.close();
        setCookieStore(conn.getURL().toString(),conn);
        int responseCode = conn.getResponseCode();
        System.out.println("\nSending POST request to url "+url);
        System.out.println("Response Code: "+responseCode);
        System.out.println("Cookie handler retrieve cookies "+cookieStore.getCookies());
    }
    /**
     * retrieve all parameters which we should fill on the FCR
     * @param html
     * @param applicationNumber this number you could get on the migrace police when you bring all document
     * @param applicationCode this is code which could find to on you paper,for instance DP means
     * longtime permission etc
     * @param applicationYear current year on your application request for visa
     * @return data which you should fill on the form for got status of application, in this case you
     * we use OAM application.
     * @throws UnsupportedEncodingException
     */
    public String getPostParams(String html, String applicationNumber, String applicationCode, String applicationYear)
            throws UnsupportedEncodingException {
       // String formBuild ="",formId="",honeypotTime="";
        Document doc = Jsoup.parse(html);
        Element applicationStatus = doc.getElementById("ioff-application-status-form");
        Elements inputElements = applicationStatus.getElementsByTag("input");
        Elements selectElements = applicationStatus.getElementsByTag("select");
        List<String> paramsList = new ArrayList<String>();
        for(Element inputElement: inputElements ){
            String key=inputElement.attr("name");
            String value=inputElement.attr("value");
            if(key.equals("ioff_application_aom")){
                continue;
            }if(key.equals("ioff_application_number")){
                value = applicationNumber;
            }if (key.equals("ioff_application_number_fake")){
                paramsList.add(key+"="+URLEncoder.encode(value,"UTF-8"));
                for (Element select : selectElements){
                    key=select.attr("name");
                    value=select.attr("value");
                    if(key.equals("ioff_application_code")){
                        value=applicationCode;
                    }else if(key.equals("ioff_application_year")){
                        value=applicationYear;continue;
                    }
                    paramsList.add(key+"="+URLEncoder.encode(value,"UTF-8"));
                }
            }
            if(key.equals("ioff_zov")){
                paramsList.add(key + "="+ URLEncoder.encode(value,"UTF-8"));
                paramsList.add("op" + "="+ URLEncoder.encode("Verify","UTF-8"));continue;
            }
            paramsList.add(key + "="+ URLEncoder.encode(value,"UTF-8"));
        }
        StringBuilder res = new StringBuilder();
        for (String param:paramsList){
            if(res.length()==0){
                res.append(param);
            }else{
                res.append("&"+param);
            }
        }

        return res.toString();
    }

    /**
     *
     * @param html
     * @return
     */
    public void setCookieStore(String html ,HttpURLConnection conn) throws URISyntaxException {
        String headerName = null;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")){
                String cookie = conn.getHeaderField(i);
                cookie = cookie.substring(0,cookie.indexOf(";"));
                String cookieNames = cookie.substring(0,cookie.indexOf("="));
                String cookieValue = cookie.substring(cookie.indexOf("=")+1,cookie.length());
                HttpCookie httpCookie = new HttpCookie(cookieNames, cookieValue);
                httpCookie.setDomain("frs.gov.cz");
                httpCookie.setPath("/");
                httpCookie.setVersion(0);
                cookieStore.add(new URI("frs.gov.cz/en/ioff/application-status"),httpCookie);
            }
        }
    }
    public void getCookieUsingHandler(){
        try{
           CookieManager manager = new CookieManager();
           manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
           CookieHandler.setDefault(manager);
           //get content from url
            URL obj = new URL(url);
            conn =(HttpURLConnection)obj.openConnection();
            conn.getContent();
            cookieStore = manager.getCookieStore();
            List<HttpCookie> cookies = cookieStore.getCookies();
            for(HttpCookie cook:cookies) {
                System.out.println(cook);
            }
        }catch (Exception e){
            System.out.println("Unable to get cookie using cookie handler");
            e.printStackTrace();
        }
    }
//    /**
//     *
//     * @return
//     */
//         List<String> getCookies(){
//        String header = null;
//        cookies = new ArrayList<String>();
//            for (int i = 0; (header = conn.getHeaderField(i))!=null; i++) {
//                cookies.add(conn.getHeaderField(i));
//            }
//        return null;
//        }
/*
    /**
     *
     * @param cookies
     */
   /*
            if(key.equals("form_build_id")){
                formBuild = value;
            }if(key.equals("form_id")){
                formId=value;
            }if(key.equals("honeypot_time")){
                honeypotTime =value;
            }

    try {
            Document post = Jsoup.connect("https://frs.gov.cz/cs/ioff/application-status").
            data("ioff_application_number",applicationNumber).data("ioff_application_number_fake","").
            data("ioff_application_code",applicationCode).data("ioff_application_year",applicationYear)
            .data("ioff_zov","").data("op","Verify").data("form_build_id",formBuild).
            data("form_id",formId).data("honeypot_time",honeypotTime).data("surname","")
            .userAgent
    ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36")
                    .post();
            String out =post.outerHtml();
            System.out.println(out);
            }catch(IOexption e){e.printace;}
    void setCookies(List<String> cookies){
            this.cookies = cookies;
        }*/
}