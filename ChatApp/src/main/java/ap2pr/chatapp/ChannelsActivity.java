/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.io.BufferedReader;

public class ChannelsActivity extends BaseActivity
        implements AdapterView.OnItemClickListener, View.OnTouchListener {

    private Map<String,ChannelObject> allChannels = new HashMap<>();
    private SharedPreferences userServerSharedPref;
    private SharedPreferences friendsServerSharedPref;
    private SharedPreferences.Editor friendsEditor;
    private MyGestureDetector slideDetector;
    private GestureDetector gestureDetector;
    private List<ChannelObject> channelObjectsList;
    private HttpClient holder;
    private MediaPlayer mediaPlayer;
    private int count2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        friendsServerSharedPref = getSharedPreferences("friends",MODE_PRIVATE);

        mediaPlayer = MediaPlayer.create(this, R.raw.get_updates);

        setContentView(R.layout.activity_channels);
        slideDetector = new MyGestureDetector();
        gestureDetector = new GestureDetector(this, slideDetector);
        RelativeLayout swipeFragment = (RelativeLayout) findViewById(R.id.swipeFragment);
        swipeFragment.setOnTouchListener(this);
        holder = HttpClientHolder.getHttpClient();

        this.userServerSharedPref = this.getSharedPreferences("userServer", Context.MODE_PRIVATE);
        // is the user logged in?
        if (userServerSharedPref.contains("userServer")) {
            String serverUrl = userServerSharedPref.getString("userServer", "none");
            try {
                //get all channels list from the current server and save it
                new GetChannels().execute(serverUrl.concat("/getChannels"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                //get the my channels list from the current server and display it
                new GetNetwork().execute(serverUrl.concat("/getNetwork"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                //get updates from the current server and display it
                new GetUpdates().execute(serverUrl.concat("/getUpdates"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        takeCareMenu();
    }

    private void takeCareMenu() {
        //set menu click listener
        Button menuButton = (Button) findViewById(R.id.menu_button);
        menuButton.setBackgroundDrawable(getResources()
                .getDrawable(R.mipmap.menu_icon));
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = ChannelsActivity.this.getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                count2++;

                if (count2 % 2 == 1) {
                    Fragment menu = new MenuFragment();
                    ft.add(R.id.menuFragment, menu);
                    ft.addToBackStack("menu");
                } else {
                    fm.popBackStack("menu", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                ft.commit();
            }
        });
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent;
        if (channelObjectsList.get(position).getID().equals("0")) {
            intent = new Intent(this, ReviewChatActivity.class);
            intent.putExtra("name", channelObjectsList.get(position).getName());
            intent.putExtra("id", channelObjectsList.get(position).getID());
        } else {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("name", channelObjectsList.get(position).getName());
            intent.putExtra("id", channelObjectsList.get(position).getID());
            finish();
        }
        startActivity(intent);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean result = gestureDetector.onTouchEvent(event);
        String slideDirection = slideDetector.getSlide();
        if (slideDirection.equals("left") || slideDirection.equals("right")) {
            Intent i = new Intent(ChannelsActivity.this, MapActivity.class);
            finish();
            startActivity(i);
        }
        return result;
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


    private class GetUpdates extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(params[0]);
            String text = "";
            String inputLine;
            try {
                HttpResponse response = holder.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                try {
                    while ((inputLine = br.readLine()) != null) {
                        text += inputLine + "\n";
                    }
                    br.close();

                } catch (Exception e){
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            //get channels from the JSON result
            JSONObject obj = null;
            try {
                int i =0;
                obj = new JSONObject(result);
                //get channels array
                JSONArray channels = obj.getJSONArray("messages");
                //for each channel collect all parameters create and add a channel object to the list
                for (i = 0; i < channels.length(); i++) {
                    String c_id = channels.getJSONObject(i).getString("channel_id");
                    String u_id = channels.getJSONObject(i).getString("user_id");
                    String text = channels.getJSONObject(i).getString("text");
                    String time = channels.getJSONObject(i).getString("date_time");

                    Double longtitude = channels.getJSONObject(i).getDouble("longtitude");
                    Double latitude = channels.getJSONObject(i).getDouble("latitude");

                    String friendLocation = u_id + ":" + longtitude.toString() + ":" + latitude.toString() + ":" + c_id;
                    friendsEditor = friendsServerSharedPref.edit();
                    friendsEditor.putString(u_id, friendLocation);
                    friendsEditor.commit();


                    String newMessage = time + System.getProperty("line.separator") + u_id + System.getProperty("line.separator") + text;

                    SharedPreferences shared = getApplication().getSharedPreferences(c_id, 0);
                    SharedPreferences.Editor sEditor = shared.edit();

                    SharedPreferences sharedCounter = getApplication().getSharedPreferences("counter" + c_id,0);
                    SharedPreferences.Editor counterEditor = sharedCounter.edit();

                    Integer c = sharedCounter.getInt("counter" + c_id, 0);
                    sEditor.putString(c.toString()+"/otherMessage", newMessage);
                    sEditor.commit();
                    c++;

                    counterEditor.putInt("counter" + c_id, c);
                    counterEditor.commit();
                }
                if (i>0) {
                    mediaPlayer.start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private class GetChannels extends AsyncTask<String, String, String> {

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
                //reset the list
                allChannels = new HashMap<String, ChannelObject>();
                //get channels from the JSON result
                JSONObject obj = new JSONObject(result);
                //get channels array
                JSONArray channels = obj.getJSONArray("channels");
                //for each channel collect all parameters create and add a channel object to the list
                for (int i = 0; i < channels.length(); i++) {
                    String id = channels.getJSONObject(i).getString("id");
                    String icon = channels.getJSONObject(i).getString("icon");
                    String name = channels.getJSONObject(i).getString("name");
                    allChannels.put(id, new ChannelObject(id, name, icon)) ;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class GetNetwork extends AsyncTask<String, String, String> {

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
                ListView listView = (ListView) findViewById(R.id.listView);
                listView.setOnItemClickListener(ChannelsActivity.this);
                channelObjectsList = new ArrayList<ChannelObject>();
                Map<String, List<String>> channelsList = new HashMap<>();
                //get channels from the JSON result
                JSONObject obj = new JSONObject(result);
                //get channels array
                JSONArray channels = obj.getJSONArray("channels");
                //for each channel collect all parameters create and add a channel object to the list
                for (int i = 0; i < channels.length(); i++) {
                    String id = channels.getJSONObject(i).getString("id");
                    //set signs to remove
                    String pat = "[" + Pattern.quote("[]\\//:\"") + "]";
                    //remove unwanted signs and words from the JSON and split it to several server urls
                    String mems[] = channels.getJSONObject(i).getString("members").replaceAll(pat, "").split(",");
                    List<String> members = new ArrayList<String>(Arrays.asList(mems));
                    channelsList.put(id, members);
                }
                String myID = UserHolder.getUserNickname();
                for (Map.Entry<String, List<String>> entry : channelsList.entrySet()) {
                    String key = entry.getKey();//key = channel_id
                    List<String> value = entry.getValue();//value = members
                    if (value.contains(myID))//myID = app_user_id
                        channelObjectsList.add(allChannels.get(key));
                }

                //set adapter for the channels view
                MyChannelAdapter channelsAdapter = new MyChannelAdapter(channelObjectsList, ChannelsActivity.this);
                listView.setAdapter(channelsAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }

}