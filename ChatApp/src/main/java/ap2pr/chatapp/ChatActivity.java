/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Display the chat screen
 */
public class ChatActivity extends BaseActivity{

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    protected EditText chatText;
    private Button buttonSend;
    private Button buttonLeave;
    private Double myLat = 0.0, myLongt = 0.0;

    private boolean side = false;

    public SharedPreferences counter;
    public SharedPreferences.Editor cEditor;
    private SharedPreferences userServerSharedPref;

    private String channelName = "";
    private String channelID = "";

    private HttpClient holder;

    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get sender current location
        GPSTracker tracker = new GPSTracker(this);
        if (!tracker.canGetLocation()) {
            tracker.showSettingsAlert();
        } else {
            myLat = tracker.getLatitude();
            myLongt = tracker.getLongitude();
        }

        // Create mediaPlayer for sending sound
        mediaPlayer = MediaPlayer.create(this, R.raw.send_message);

        // Get app's http client
        holder = HttpClientHolder.getHttpClient();

        // Get channel's name and id from channelsActivity
        channelName = getIntent().getExtras().getString("name");
        channelID = getIntent().getExtras().getString("id");

        // Create shared preference for counter used to order the messages
        counter = getSharedPreferences("counter" + channelID, 0);
        cEditor = counter.edit();
        if (counter.getInt("counter",-1) == -1) {
            cEditor.putInt("counter", 0);
            cEditor.commit();
        }

        userServerSharedPref = this.getSharedPreferences("userServer", Context.MODE_PRIVATE);
        // Create shared preference to save this channel's messages
        sharedPref = getSharedPreferences(channelID, 0);
        editor = sharedPref.edit();

        // Send toast with channel's name
        Toast.makeText(this,channelName,Toast.LENGTH_SHORT).show();

        // Set views buttons layouts and lists
        setContentView(R.layout.activity_chat);
        TextView title = (TextView)findViewById(R.id.chatTitle);
        title.setText(channelName + " Channel");
        buttonSend = (Button) findViewById(R.id.buttonSend);
        buttonLeave = (Button) findViewById(R.id.leave_btn);
        buttonLeave.setVisibility(View.VISIBLE);

        listView = (ListView) findViewById(R.id.listView);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.activity_chat_singlemessage);
        listView.setAdapter(chatArrayAdapter);

        //Set chat text
        chatText = (EditText) findViewById(R.id.chatText);
        chatText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage();
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mediaPlayer.start();
                sendChatMessage();
            }
        });


        buttonLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if (userServerSharedPref.contains("userServer")) {
                    String serverUrl = userServerSharedPref.getString("userServer", "none");
                    try {
                        // try to add the leave the channel
                        new LeaveChannel(getIntent().getExtras().getString("id"))
                                .execute(serverUrl.concat("/leaveChannel"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });

        //Get all messages from shared preference
        for (Integer h=0; h<sharedPref.getAll().size(); h++) {
            String opt1 = h.toString()+"/myMessage";
            String opt2 = h.toString()+"/otherMessage";

            if (sharedPref.contains(opt1)) {
                String r = sharedPref.getString(opt1,"none");
                chatArrayAdapter.add(new ChatMessage(side, r));
            }
            else if (sharedPref.contains(opt2)){
                String r = sharedPref.getString(opt2,"none");
                chatArrayAdapter.add(new ChatMessage(!side, r));
            }
        }
    }

    protected boolean sendChatMessage(){
        // Get current time and convert it to string
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String time = dateFormat.format(date);

        // Create message to present and message to store
        String message = time + System.getProperty("line.separator") + chatText.getText().toString();
        String noTimeMessage = chatText.getText().toString();

        chatArrayAdapter.add(new ChatMessage(side, message));
        chatText.setText("");

        // Store the message and increment the counter
        Integer c = counter.getInt("counter" + channelID, 0);
        editor.putString(c.toString() + "/myMessage", message);
        editor.commit();
        c++;
        cEditor.putInt("counter" + channelID, c);
        cEditor.commit();

        // Send the message to the server
        PostMessage post = new PostMessage(noTimeMessage,channelID,myLongt,myLat);
        if (userServerSharedPref.contains("userServer")) {
            String serverUrl = userServerSharedPref.getString("userServer", "none");
            try {
                // try to post a message
                post.execute(serverUrl.concat("/sendMessage"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private class PostMessage extends AsyncTask<String, String, String> {

        private String text;
        private String channel_id;
        private Double longtitude;
        private Double latitude;

        // Post message constructor
        public PostMessage(String text, String channel_id, Double longt, Double lat) {
            this.text = text;
            this.channel_id = channel_id;
            this.longtitude = longt;
            this.latitude = lat;
        }


        @Override
        protected String doInBackground(String... params) {
            // Getting method url
            HttpPost httpPost = new HttpPost(params[0]);

            // Data to post
            List<NameValuePair> valueList = new ArrayList<>(4);
            valueList.add(new BasicNameValuePair("channel_id", channel_id));
            valueList.add(new BasicNameValuePair("text", text));
            valueList.add(new BasicNameValuePair("longtitude", longtitude.toString()));
            valueList.add(new BasicNameValuePair("latitude", latitude.toString()));
            String result = null;
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(valueList, "UTF-8"));
            } catch (Exception e) {
            }

            // Execute method and get response
            try {
                HttpResponse response = holder.execute(httpPost);
                HttpEntity entity = response.getEntity();
                result = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
            }
            return result;
        }

        protected void onPostExecute(String result) {

        }
    }
    protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException,
            IOException{
        // Convert response to readable text
        InputStream in = entity.getContent();
        StringBuffer out = new StringBuffer();
        int n=1;

        while (n>0) {
            byte[] b = new byte[4096];
            n = in.read(b);
            if (n>0)
                out.append(new String(b, 0, n));
        }
        return  out.toString();
    }


    private class LeaveChannel extends AsyncTask<String, String, String> {
        private String channelId;

        public LeaveChannel(String channelId) {
            this.channelId = channelId;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpClient = HttpClientHolder.getHttpClient();
            HttpPost httpPost = new HttpPost(params[0]);
            // Request parameters and other properties.
            List<NameValuePair> methodParams = new ArrayList<NameValuePair>(1);
            methodParams.add(new BasicNameValuePair("id", channelId));
            String result = null;
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(methodParams, "UTF-8"));
                try {
                    HttpResponse response = httpClient.execute(httpPost);
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
            try {
                //get servers from the JSON result
                JSONObject obj = new JSONObject(result);
                //get the result message to the join request and display it on a toast
                Integer status = obj.getInt("status");
                String message = obj.getString("message");
                if (status == 1) {
                    Toast toast = Toast.makeText(getBaseContext(), "Leave succeed", Toast.LENGTH_LONG);
                    toast.show();
                    Intent i = new Intent(ChatActivity.this, ChannelsActivity.class);
                    finish();
                    startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getBaseContext(), "Leave failed:\n" + message, Toast.LENGTH_LONG);
                    toast.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        Intent i = new Intent(ChatActivity.this, ChannelsActivity.class);
        startActivity(i);
    }

}