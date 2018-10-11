/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class GoogleLoginActivity extends BaseActivity implements View.OnClickListener {
    private AccountManager accountManager;
    private Account[] accounts;
    private Spinner spinner;
    private DefaultHttpClient userHttpClient;
    private Account account;
    private boolean isLoginClicked = false;
    private Boolean isLoggedIn = false;
    private SharedPreferences isLoggedInSharedPref;
    private SharedPreferences.Editor isLoggedInEditor;
    private SharedPreferences isJoinedToReviewChannelShPr;
    private String baseServerUrl;
    private View signOut;
    private View goToApp;
    private Context context = GoogleLoginActivity.this;
    List<String> myChannelsIds;
    List<String> allChannelIds;
    final GoogleLoginActivity self = this;
    private int registersServersCounter = 0;
    private List<String> serversNames;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        /* announce the the user wait for successful login announcement after
            s/he press on the login button */
        String waitForMessage = getResources().getString(R.string.please_wait_for_announcement);
        Toast waitForToast = Toast.makeText(this, waitForMessage, Toast.LENGTH_SHORT);
        waitForToast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        waitForToast.show();
        userHttpClient = new DefaultHttpClient();
        accountManager = AccountManager.get(getApplicationContext());
        accounts = accountManager.getAccountsByType("com.google");

        ArrayList<String> accountList = new ArrayList<String>();
        for (Account account : accounts) {
            accountList.add(account.name);
        }

        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, accountList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        isLoggedInSharedPref = this.getSharedPreferences("isLoggedIn", Context.MODE_PRIVATE);
        Intent intent = getIntent();
        if (intent.hasExtra("serverName")) {
            baseServerUrl = intent.getStringExtra("serverName");
        } else {
            baseServerUrl = getResources().getString(R.string.our_server_base_url);
        }
        String firstServerForRegister = getResources()
                .getString(R.string.first_server_for_register);
        try {
            //get the channels list from the server and display it
            new GetChannels().execute(baseServerUrl.concat("/getChannels"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // register to first "hardcoded" server for getting data
            new Register(firstServerForRegister).execute(baseServerUrl.concat("/register"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // get servers will collect the servers from the above server which we register to.
            new GetServers().execute(baseServerUrl.concat("/getServers"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        goToApp = findViewById(R.id.continue_button);
        goToApp.setOnClickListener(this);
        signOut = findViewById(R.id.sign_out_button);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        signOut.setOnClickListener(this);
        signOut.setVisibility(View.GONE);
    }

    private class Register extends AsyncTask<String, String, String> {
        private String url;

        public Register(String url) {
            this.url = url;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(params[0]);
            // Request parameters and other properties.
            List<NameValuePair> methodParams = new ArrayList<NameValuePair>(1);
            methodParams.add(new BasicNameValuePair("link", url));
            String text = null;
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(methodParams, "UTF-8"));
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    HttpEntity entity = response.getEntity();
                    text = getASCIIContentFromEntity(entity);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            JSONObject obj;
            int returnedStatus = 0;
            if (result != null) {
                try {
                    obj = new JSONObject(result);
                    returnedStatus = obj.getInt("status");
                    if (returnedStatus == 1) {
                        registersServersCounter++;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
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
            } catch (IOException e) {
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
                serversNames = Arrays.asList(obj.getString("server").replaceAll(pat, "")
                        .replace("http", "").split(","));
                // call callback
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        self.geServersCallback();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void geServersCallback() {
        try {
            int size = serversNames.size();
            for (int i = 0; i < size; i++) {
                if (registersServersCounter == 2) {
                    break;
                }
                String server = serversNames.get(i);
                String firstServerForRegister = getResources()
                        .getString(R.string.first_server_for_register);
                if (!server.equals(firstServerForRegister)) {
                    try {
                        new Register(server).execute(baseServerUrl.concat("/register"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                // try to make login
                accountManager.getAuthToken(account, "ah", null, false,
                        new OnTokenAcquired(userHttpClient, baseServerUrl, GoogleLoginActivity.this), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_in_button) {
            onSignInClicked();
            signOut.setVisibility(View.VISIBLE);
            goToApp.setVisibility(View.VISIBLE);
        }
        if (view.getId() == R.id.continue_button) {
            onContinueClicked();
        }
        if (view.getId() == R.id.sign_out_button) {
            onSignOutClicked();
            signOut.setVisibility(View.GONE);
            goToApp.setVisibility(View.GONE);

        }
    }

    private void onSignInClicked() {
        if (isLoginClicked) {
            // announce the the user that he already logged in.
            String message = getResources().getString(R.string.already_login);
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        } else {
            /* before we know that the login succeeded on the first time we report that
             the user is not logged in */
            isLoggedInEditor = isLoggedInSharedPref.edit();
            isLoggedIn = false;
            isLoggedInEditor.putBoolean("isLoggedIn", isLoggedIn);
            isLoggedInEditor.commit();
            spinner = (Spinner) findViewById(R.id.spinner);
            account = accounts[spinner.getSelectedItemPosition()];
            new UserHolder(account.name);
            try {
                // try to make login
                accountManager.getAuthToken(account, "ah", null, false,
                        new OnTokenAcquired(userHttpClient, baseServerUrl, GoogleLoginActivity.this), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isLoginClicked = true;
        }
    }

    private void onContinueClicked() {
        // load the information which will tell us whether the user logged in or not
        isLoggedIn = isLoggedInSharedPref.getBoolean("isLoggedIn", false);
        // is the user logged in?
        if (isLoggedIn) {
            SharedPreferences userServerSharedPref;
            SharedPreferences.Editor userServerShPrEditor;
            userServerSharedPref = this.getSharedPreferences("userServer", Context.MODE_PRIVATE);
            userServerShPrEditor = userServerSharedPref.edit();
            userServerShPrEditor.putString("userServer", baseServerUrl);
            userServerShPrEditor.commit();
            HttpClientHolder.setHttpClient(this.userHttpClient);
            isJoinedToReviewChannelShPr = this.getSharedPreferences("isJoinedToReviewChannel",
                    Context.MODE_PRIVATE);
            boolean isJoinedToReviewChannel = false;
            if (isJoinedToReviewChannelShPr.contains("isJoinedToReviewChannel")) {
                isJoinedToReviewChannel = isJoinedToReviewChannelShPr.getBoolean("isJoinedToReviewChannel", false);
            }
            /* if the user didn't join to review channel, so either he should join - in case the channel exist,
                or he should to create this channel - in case the channel don't exist */
            if (!isJoinedToReviewChannel) {
                // for check whether the review channel already created by another user which use our app.
                try {
                    //get all channels list from the default server and save it
                    new GetMyChannels().execute(baseServerUrl.concat("/getMyChannels"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            /* the login server method failed, so we let the user to choose another server
           from the setting activity's servers list. */
            String toastMessage = getResources().getString(R.string.delivering_to_setting_screen);
            Toast toast = Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            Intent intent = new Intent(GoogleLoginActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    private void onSignOutClicked() {
        this.isLoginClicked = false;
        this.isLoggedIn = false;
        isLoggedInEditor = isLoggedInSharedPref.edit();
        isLoggedInEditor.putBoolean("isLoggedIn", isLoggedIn);
        isLoggedInEditor.commit();
        try {
            // try to logoff
            new LoginOrLogoffCall(this.userHttpClient, GoogleLoginActivity.this)
                    .execute(baseServerUrl, "logoff");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getMyChannelsCallback() {
        // add /join to the review channel
        String id = "0";
        String name = this.getString(R.string.review);
        String icon = getResources().getString(R.string.review_channel_icon_base64);
        ChannelObject reviewChannel = new ChannelObject(id, name, icon);
        // the review channel exist and the user on it.
        if (myChannelsIds.contains(id)) {
            Intent intent = new Intent(GoogleLoginActivity.this, SplashActivity.class);
            finish();
            startActivity(intent);
        }

        /* the review channel don't exist in user channel, so either the channel exist but the user
            not on it - we need to join him, or the channel don't exist. */
        if (allChannelIds.contains(id)) {
            // the channel exist but the usernot on it - we need to join him
            try {
                new JoinChannel(id).execute(baseServerUrl.concat("/joinChannel"));
                SharedPreferences.Editor isJoinedToReviewChannelEditor = isJoinedToReviewChannelShPr.edit();
                isJoinedToReviewChannelEditor.putBoolean("isReviewChannelAdded", true);
                isJoinedToReviewChannelEditor.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // the review channel isn't added already, so add it
        else {
            try {
                // try to add review channel
                new AddChannelCall(GoogleLoginActivity.this, reviewChannel).execute(
                        baseServerUrl.concat("/addChannel"));
                Intent intent = new Intent(GoogleLoginActivity.this, SplashActivity.class);
                finish();
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private class GetChannels extends AsyncTask<String, String, String> {
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
                allChannelIds = new ArrayList<String>();
                //get channels from the JSON result
                JSONObject obj = new JSONObject(result);
                //get channels array
                JSONArray channels = obj.getJSONArray("channels");
                //for each channel collect all parameters create and add a channel object to the list
                for (int i = 0; i < channels.length(); i++) {
                    String id = channels.getJSONObject(i).getString("id");
                    allChannelIds.add(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class GetMyChannels extends AsyncTask<String, String, String> {

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
                myChannelsIds = new ArrayList<String>();
                //get channels from the JSON result
                JSONObject obj = new JSONObject(result);
                JSONArray jsonArray = obj.getJSONArray("channels");
                for(int i = 0; i < jsonArray.length(); i++){
                    myChannelsIds.add(jsonArray.getString(i)); // iterate the JSONArray and extract the ids
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // call callback
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    self.getMyChannelsCallback();
                }
            });
        }
    }

    private class JoinChannel extends AsyncTask<String, String, String> {
        private String channelId;

        public JoinChannel(String channelId) {
            this.channelId = channelId;
        }

        @Override
        protected String doInBackground(String... params) {
            DefaultHttpClient httpClient = HttpClientHolder.getHttpClient();
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
                String status = obj.getString("status");
                String message = obj.getString("message");
                if (status.equals("1")) {
                    Toast toast = Toast.makeText(context, "Join succeed", Toast.LENGTH_LONG);
                    toast.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected String getASCIIContentFromEntity(HttpEntity entity)
            throws IllegalStateException, IOException {
        InputStream in = entity.getContent();
        StringBuilder out = new StringBuilder();
        int n = 1;
        while (n > 0) {
            byte[] b = new byte[4096];
            n = in.read(b);
            if (n > 0)
                out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

}
