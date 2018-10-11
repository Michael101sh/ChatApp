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
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class AddChannelCall extends AsyncTask<String, String, String>  {
    private Context context;
    private ChannelObject channelObject;
    private DefaultHttpClient userHttpclient;

    public AddChannelCall(Context context, ChannelObject channelObject) {
        this.context = context;
        this.channelObject = channelObject;
        this.userHttpclient = HttpClientHolder.getHttpClient();
    }

    protected String doInBackground(String... params) {
        HttpPost httpPost = new HttpPost(params[0]);
        String id = channelObject.getID();
        String name = channelObject.getName();
        String icon = channelObject.getIcon();
        // Request parameters and other properties.
        List<NameValuePair> methodParams = new ArrayList<NameValuePair>(3);
        methodParams.add(new BasicNameValuePair("id", id));
        methodParams.add(new BasicNameValuePair("name", name));
        methodParams.add(new BasicNameValuePair("icon", icon));
        String result = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(methodParams, "UTF-8"));
            try {
                HttpResponse response = userHttpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                result = getASCIIContentFromEntity(entity);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    protected void onPostExecute(String result) {
        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);
                int returnedStatus = 0;
                returnedStatus = obj.getInt("status");
                if (returnedStatus == 1) {
                    String toastMessage = context.getResources().getString(R.string.channel_added);
                    Toast toast = Toast.makeText(context,
                            toastMessage, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
