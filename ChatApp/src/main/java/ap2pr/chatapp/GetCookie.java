/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Context;
import android.os.AsyncTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import java.io.ByteArrayOutputStream;

//using the token to get an auth cookie
public class GetCookie extends AsyncTask<String, Void, Boolean> {

    private String appId;
    private HttpParams params;
    private HttpResponse response;
    private Context context;
    private DefaultHttpClient httpclient;
    private String baseServerUrl;

    public GetCookie(DefaultHttpClient httpclient, String baseServerUrl,
                     String appId, Context context) {
        this.httpclient = httpclient;
        params = httpclient.getParams();
        this.appId = appId;
        this.context = context;
        this.baseServerUrl = baseServerUrl;
    }

    protected Boolean doInBackground(String... tokens) {
        try {
            // Don't follow redirects
            params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);

            HttpGet httpGet = new HttpGet("http://" + appId +
                    ".appspot.com/_ah/login?continue=http://" + appId + ".appspot.com/&auth=" + tokens[0]);
            response = httpclient.execute(httpGet);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();

            if(response.getStatusLine().getStatusCode() != 302){
                // Response should be a redirect
                return false;
            }

            //check if we received the ACSID or the SACSID cookie, depends on http or https request
            for(Cookie cookie : httpclient.getCookieStore().getCookies()) {
                if(cookie.getName().equals("ACSID") || cookie.getName().equals("SACSID")){
                    return true;
                }
            }

        }  catch (Exception e) {
            e.printStackTrace();
            cancel(true);
        } finally {
            params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        }
        return false;
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            new LoginOrLogoffCall(httpclient, context)
                    .execute(baseServerUrl, "login");
        }
    }
}
