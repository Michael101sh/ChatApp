/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SettingsActivity extends BaseActivity implements View.OnClickListener
        , AdapterView.OnItemClickListener{
    private String currentServerName = null;
    private List<String> serversNamesFull = null;
    private DefaultHttpClient userHttpClient = null;
    private List<ChannelObject> channelObjectList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences userServerSharedPref = this.getSharedPreferences("userServer", Context.MODE_PRIVATE);
       // if the user connected to any server
        if (userServerSharedPref.contains("userServer")) {
            this.currentServerName = userServerSharedPref.getString("userServer", "none");
            try {
                //get the servers list from the server and display it
                new GetServers().execute(currentServerName.concat("/getServers"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                //get the channels list from the server and display it
                new GetChannels(channelObjectList).execute(currentServerName.concat("/getChannels"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        findViewById(R.id.goToAppButton).setOnClickListener(this);
        userHttpClient = HttpClientHolder.getHttpClient();
    }

    @Override
    public void onClick(View v) {
        // go to app button was clicked
        Intent intent = new Intent(this, ChannelsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            String clickedServerName = serversNamesFull.get(position);
            // Is the user logged in to any server?
            if (this.currentServerName != null) {
                String currentServerNameWithoutNeedless = currentServerName.replace("http://", "");
                if (clickedServerName.equals(currentServerNameWithoutNeedless)) {
                    String message = getResources().getString(R.string.already_login_to_this_server);
                    Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    return;
                }
                // logoff from this server
                if (this.currentServerName != null) {
                    try {
                        new LoginOrLogoffCall(this.userHttpClient, SettingsActivity.this)
                                .execute(currentServerName, "logoff");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            // make login to the new server
            Intent intent = new Intent(SettingsActivity.this, GoogleLoginActivity.class);
            clickedServerName = "http://".concat(clickedServerName);
            intent.putExtra("serverName", clickedServerName);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GetServers extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(params[0]);
            String text = null;
            try {
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                text = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            try {
                //get servers from the JSON result
                JSONObject obj = new JSONObject(result);
                //set signs to remove
                String pat = "[" + Pattern.quote("[]\\//:\"") + "]";
                //remove unwanted signs and words from the JSON and split it to several server urls
                List<String> serversNames = Arrays.asList(obj.getString("server").replaceAll(pat, "")
                        .replace("http", "").split(","));
                serversNamesFull = new ArrayList<String>(serversNames);
                String ourServerName = getResources().getString(R.string.our_server_base_url).replace("http://", "");
                if (!serversNamesFull.contains(ourServerName)) {
                    serversNamesFull.add(ourServerName);
                }
                String backupServer1 = getResources().getString(R.string.our_first_backup_server_url).replace("http://", "");
                if (!serversNamesFull.contains(backupServer1)) {
                    serversNamesFull.add(backupServer1);
                }
                String backupServer2 = getResources().getString(R.string.our_second_backup_server_url).replace("http://", "");

                if (!serversNamesFull.contains(backupServer2)) {
                    serversNamesFull.add(backupServer2);
                }
                String backupServer3 = getResources().getString(R.string.our_third_backup_server_url).replace("http://", "");
                if (!serversNamesFull.contains(backupServer3)) {
                    serversNamesFull.add(backupServer3);
                }
                ListView serversList = (ListView) findViewById(R.id.servers_list);
                serversList.setOnItemClickListener(SettingsActivity.this);
                //set adapter for the servers view
                ArrayAdapter<String> serversAdapter = new ArrayAdapter<String>(SettingsActivity.this,
                        android.R.layout.simple_list_item_1, serversNamesFull);
                serversList.setAdapter(serversAdapter);
            } catch (Exception e) {
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

    private class GetChannels extends AsyncTask<String, String, String> {
        private List<ChannelObject> channelObjectList;

        public GetChannels(List<ChannelObject> channelObjectList) {
            this.channelObjectList = channelObjectList;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(params[0]);
            String text = null;
            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                text = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            try {
                ListView channelsList = (ListView) findViewById(R.id.channels_list);
                channelObjectList = new ArrayList<>();
                //get channels from the JSON result
                JSONObject obj = new JSONObject(result);
                //get channels array
                JSONArray channels = obj.getJSONArray("channels");
                //for each channel collect all parameters create and add a channel object to the list
                for (int i = 0; i < channels.length(); i++) {
                    String id = channels.getJSONObject(i).getString("id");
                    String icon = channels.getJSONObject(i).getString("icon");
                    String name = channels.getJSONObject(i).getString("name");
                    channelObjectList.add(new ChannelObject(id, name, icon));
                }
                //set adapter for the channels view
                ChannelAdapter channelsAdapter = new ChannelAdapter(channelObjectList, SettingsActivity.this);
                channelsList.setAdapter(channelsAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(this, ChannelsActivity.class);
        startActivity(intent);
    }
}