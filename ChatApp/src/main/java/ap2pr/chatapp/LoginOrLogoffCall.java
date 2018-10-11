/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Context;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import android.content.SharedPreferences;

public class LoginOrLogoffCall extends AsyncTask<String, String, String> {
    private DefaultHttpClient httpclient;
    private Context context;
    private String requestName = null;
    private boolean isLoggedIn;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    public LoginOrLogoffCall(DefaultHttpClient httpclient, Context context) {
        this.httpclient = httpclient;
        this.context = context;
        this.isLoggedIn = false;
    }
    @Override
    protected String doInBackground(String... params) {
        String baseServerUrl = params[0];
        this.requestName = params[1];
        String url = baseServerUrl.concat("/").concat(this.requestName);
        String result = null;
        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            result = getASCIIContentFromEntity(entity);
        } catch (Exception e) {
            e.printStackTrace();
            cancel(true);
        }
        return result;
    }

    protected void onPostExecute(String result) {
        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);
                int returnedStatus = 0;
                returnedStatus = obj.getInt("status");
                String returnedMessage = obj.getString("message");
                sharedPref = context.getSharedPreferences("isLoggedIn", Context.MODE_PRIVATE);
                if (returnedStatus == 1) {
                    String toastMessage;
                    if (requestName.equals("login")) {
                        toastMessage = context.getResources()
                                .getString(R.string.login_done_successfully);
                        editor = sharedPref.edit();
                        isLoggedIn = true;
                        editor.putBoolean("isLoggedIn", isLoggedIn);
                        editor.commit();
                    } else {
                        toastMessage = context.getResources()
                                .getString(R.string.logout_done_successfully);
                        editor = sharedPref.edit();
                        isLoggedIn = false;
                        editor.putBoolean("isLoggedIn", isLoggedIn);
                        editor.commit();
                    }
                    Toast toast = Toast.makeText(context,
                            toastMessage, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                } else {
                    String toastMessage;
                    if (returnedMessage.contains("already")) {
                        toastMessage = context.getResources()
                                .getString(R.string.login_done_successfully);
                        editor = sharedPref.edit();
                        isLoggedIn = true;
                        editor.putBoolean("isLoggedIn", isLoggedIn);
                        editor.commit();
                    } else {
                        if (requestName.equals("login")) {
                            toastMessage = (context.getResources()
                                    .getString(R.string.login_failed)).concat("because of: \n");
                            toastMessage = toastMessage.concat(returnedMessage);
                        } else {
                            toastMessage = context.getResources()
                                    .getString(R.string.logout_failed).concat("because of: \n");
                            toastMessage = toastMessage.concat(returnedMessage);
                        }
                    }
                    Toast toast = Toast.makeText(context, toastMessage , Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(context, requestName + " failed", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }
    }

    protected String getASCIIContentFromEntity(HttpEntity entity)
            throws IllegalStateException, IOException {
        InputStream in = entity.getContent();
        StringBuffer out = new StringBuffer();
        int n = 1;
        while (n > 0) {
            byte[] b = new byte[4096];
            n = in.read(b);
            if (n > 0)
                out.append(new String(b, 0, n));
        }
        return out.toString();
    }

}