package Main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class StatusOfApplication {
    private List<String> cookies;
    private HttpURLConnection conn;

    private final String USER_AGENT="Mozilla/5.0";

        public static void main(String[] args){
         String url = "https://frs.gov.cz/en/ioff/application-status";
         StatusOfApplication http = new StatusOfApplication();
         CookieHandler.setDefault(new CookieManager());
            try {
                String page =http.getPageContent(url);
                String postParams = http.getFormParams(page,"21880","DP","2020");
                http.sendPost("https://frs.gov.cz/en/ioff/application-status",postParams);
                String result = http.getPageContent(url);
                System.out.println(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    /**
     * got html for next parsing
     * @param url
     * @return
     * @throws Exception
     */
        private String getPageContent(String url) throws Exception{
            URL obj = new URL(url);
            conn = (HttpsURLConnection) obj.openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Host","frs.gov.cz");
            conn.setRequestProperty("User-Agent",USER_AGENT);
            conn.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9," +
                    "image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            conn.setRequestProperty("Accept-Language","en-US,en;q=0.9,ru;q=0.8,cs;q=0.7");
            if (cookies!=null){
                for(String cookie: cookies){
                    conn.addRequestProperty("Cookie",cookie.split(";",1)[0]);
                }
            }
            conn.setRequestProperty("Referer", "https://frs.gov.cz/cs/ioff/application-status");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            int reponseCode = conn.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : "+url);
            System.out.println("Response code : "+reponseCode);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine())!= null){
                response.append(inputLine);
            }
            in.close();
            setCookies(conn.getHeaderFields().get("Set-Cookie"));
            return response.toString();
        }

    /**
     * manualy make params for post request
     * @param html
     * @param applicationNumber
     * @param applicationCode
     * @param applicationYear
     * @return
     * @throws UnsupportedEncodingException
     */
        public String getFormParams(String html,String applicationNumber,String applicationCode,String applicationYear)
            throws UnsupportedEncodingException {
        Document doc = Jsoup.parse(html);
        Element applicationStatus = doc.getElementById("edit-oam");
        Elements inputElements = applicationStatus.getElementsByTag("input");
        Elements selectElements = applicationStatus.getElementsByTag("select");
        //***********************************************
        Elements placeholder=doc.getElementsByClass("placeholder");
        Elements alert = doc.getElementsByClass("alert alert-success");
        List<String> list = new ArrayList<String>();
        for (Element element:placeholder){
            String key = element.text();
            list.add(key);
        }
        for(Element element:alert){
            String key = element.text();
            list.add(key);
        }
        for (String s:list){
            System.out.println(s);
        }
        //**********************************************
        List<String> paramsList = new ArrayList<String>();
        for(Element inputElement: inputElements ){
            String key=inputElement.attr("name");
            String value=inputElement.attr("value");
            if(key.equals("ioff_application_number")){
                value = applicationNumber;
            }
            paramsList.add(key + "="+ URLEncoder.encode(value,"UTF-8"));
        }
        for (Element select : selectElements){
            String key=select.attr("name");
            String value=select.attr("value");
            if(key.equals("ioff_application_code")){
                value=applicationCode;
            }else if(key.equals("ioff_application_year")){
                value=applicationYear;
            }
            paramsList.add(key+"="+URLEncoder.encode(value,"UTF-8"));
        }
        paramsList.add("op="+URLEncoder.encode("Verify","UTF-8"));
        paramsList.add(URLEncoder.encode("form_build_id=form-MheaOKUJmbbNZKrrT_GoEA519BZ72iDI0yyUy_5MDyE","UTF-8"));
        paramsList.add (URLEncoder.encode("form_id=ioff_application_status_form","UTF-8"));
        paramsList.add(URLEncoder.encode("honeypot_time: 1604421311|lckeYDK3kdgpjtuE0dVcRTkddTY7kHGAt-9k29YJUAY","UTF-8"));
        paramsList.add(URLEncoder.encode("surname:=","UTF-8"));
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
     * got response cod 200, but i need 302
     * @param url
     * @param postParams
     * @throws Exception
     */
    public void sendPost(String url, String postParams) throws Exception {
        URL obj = new URL(url);
        conn =(HttpURLConnection)obj.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setUseCaches(false);
       // conn.setRequestMethod("POST");
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Host","frs.gov.cz");
        conn.setRequestProperty("User-Agent",USER_AGENT);
        conn.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9," +
                "image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        conn.setRequestProperty("Accept-Language","en-US,en;q=0.9,ru;q=0.8,cs;q=0.7");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Referer", "https://frs.gov.cz/cs/ioff/application-status");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length",Integer.toString(postParams.length()));
        conn.setDoInput(true);
        conn.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes(postParams);
        out.flush();
        out.close();boolean redirect = false;
        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inLine;
        StringBuffer response = new StringBuffer();
        while((inLine = in.readLine())!=null){
            response.append(inLine);
        }
        in.close();
        int responseCode = conn.getResponseCode();
        System.out.println("\nSending POST request to url "+url);
        System.out.println("Post parameters :"+postParams);
        System.out.println("Response Code: "+responseCode);
        System.out.println(response.toString());

        }
        void setCookies(List<String> cookies){
            this.cookies = cookies;
        }
}