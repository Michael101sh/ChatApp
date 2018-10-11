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
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
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

public class ChannelsFragment extends Fragment {

    private Map<String,ChannelObject> allChannels = new HashMap<>();
    private SharedPreferences userServerSharedPref;
    private SharedPreferences friendsServerSharedPref;
    private SharedPreferences.Editor friendsEditor;
    private List<ChannelObject> channelObjectsList;
    private HttpClient holder;
    private MediaPlayer mediaPlayer;
    private FragmentActivity faActivity;
    private FrameLayout llLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        faActivity = (FragmentActivity) super.getActivity();
        llLayout = (FrameLayout) inflater
                .inflate(R.layout.fragment_channels, container, false);

        friendsServerSharedPref = getActivity().getSharedPreferences("friends", getActivity().MODE_PRIVATE);

        mediaPlayer = MediaPlayer.create(getActivity(), R.raw.get_updates);

        holder = HttpClientHolder.getHttpClient();

        this.userServerSharedPref = getActivity().getSharedPreferences("userServer", Context.MODE_PRIVATE);
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
        return llLayout;
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

                    SharedPreferences shared = getActivity().getSharedPreferences(c_id, 0);
                    SharedPreferences.Editor sEditor = shared.edit();

                    SharedPreferences sharedCounter = getActivity().getSharedPreferences("counter" + c_id,0);
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
                ListView listView = (ListView) getActivity().findViewById(R.id.listView);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Intent intent;
                        if (channelObjectsList.get(position).getID().equals("0")) {
                            intent = new Intent(getActivity(), ReviewChatActivity.class);
                            intent.putExtra("name", channelObjectsList.get(position).getName());
                            intent.putExtra("id", channelObjectsList.get(position).getID());
                        } else {
                            intent = new Intent(getActivity(), ChatActivity.class);
                            intent.putExtra("name", channelObjectsList.get(position).getName());
                            intent.putExtra("id", channelObjectsList.get(position).getID());
                            getActivity().finish();
                        }
                        startActivity(intent);
                    }
                });
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
                MyChannelAdapter channelsAdapter = new MyChannelAdapter(channelObjectsList, getActivity());
                listView.setAdapter(channelsAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}