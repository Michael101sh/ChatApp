/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import android.location.LocationListener;
import android.widget.Toast;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MapActivity extends FragmentActivity implements
        View.OnTouchListener,View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener, LocationListener,
        com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks, SensorEventListener {

    private MyGestureDetector slideDetector;
    private GestureDetector gestureDetector;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationRequest mLocationRequest;
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private GoogleApiClient mGoogleApiClient;
    private SwipeRefreshLayout SwipeRefreshLayout;
    private SensorManager sensorManager;
    private float last_x, last_y, last_z;
    private long lastUpdate;
    private static final int SHAKE_THRESHOLD = 1000;
    private boolean visibilityFlag = false;

    private HashMap channelColor;
    private Double myLat = null;
    private Double myLongt = null;

    private GPSTracker tracker;

    private HttpClient holder;
    private SharedPreferences userServerSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        holder = HttpClientHolder.getHttpClient();

        // Hash map to store each channel his unique color
        channelColor = new HashMap();

        lastUpdate = System.currentTimeMillis();

        slideDetector = new MyGestureDetector();
        gestureDetector = new GestureDetector(this, slideDetector);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        RelativeLayout relativeLayout =
                (RelativeLayout) findViewById(R.id.channelsListFragment);
        if (relativeLayout != null) { // in landscape mode
            Fragment channelsFragment = new ChannelsFragment();
            ft.add(R.id.channelsListFragment, channelsFragment);
            ft.commit();
        } else { // in portrait case
            if (getSupportFragmentManager().findFragmentById(R.id.listView) != null) {
                getSupportFragmentManager().beginTransaction().
                        remove(getSupportFragmentManager().findFragmentById(R.id.listView)).
                        commit();
            }
            ImageView swipeFragment = (ImageView) findViewById(R.id.swipeFragment);
            swipeFragment.setOnTouchListener(this);
        }

        setUpMapIfNeeded();

        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        takeCareSwipeDown();
        takeCareMenu();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        // Starting load balancing
        this.userServerSharedPref = this.getSharedPreferences("userServer", Context.MODE_PRIVATE);
        // if the user is logged in activate load balancing
        if (userServerSharedPref.contains("userServer")) {
            try {
                // try to use load balancing
                new LoadBalancer().execute(this.getResources().getString(R.string.our_server_base_url) +"/loadBalancing");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private BroadcastReceiver reloadDone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(MapActivity.this, "Reload is done", Toast.LENGTH_SHORT).show();
        }
    };

    private void takeCareSwipeDown() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            SwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.map_swipeLayout);
            SwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    Intent intent = new Intent(MapActivity.this, ReloadService.class);
                    startService(intent);
                }
            });
            SwipeRefreshLayout.setColorScheme(R.color.holo_blue_bright, R.color.holo_green_light,
                    R.color.holo_orange_light, R.color.holo_red_light);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ReloadService.DONE);
        registerReceiver(reloadDone, intentFilter);
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        long curTime = System.currentTimeMillis();
        // only allow one update every 100ms.
        if ((curTime - lastUpdate) > 100) {
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
            if (speed > SHAKE_THRESHOLD) {
                SwipeRefreshLayout.setRefreshing(true);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(3000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SwipeRefreshLayout.setRefreshing(false);
                                }
                            });
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(ReloadService.DONE);
                            registerReceiver(reloadDone, intentFilter);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
                Toast.makeText(this, "Shake occurred", Toast.LENGTH_SHORT).show();
            }
            last_x = x;
            last_y = y;
            last_z = z;
        }
    }

    private void takeCareMenu() {
        //set menu click listener
        Button menuButton = (Button) findViewById(R.id.menuButton);
        menuButton.setBackgroundDrawable(getResources()
                .getDrawable(R.mipmap.menu_icon));
        menuButton.setOnClickListener(this);
        Button channelsListButton = (Button) findViewById(R.id.channelsListButton);
        channelsListButton.setOnClickListener(this);
        Button addChannelButton = (Button) findViewById(R.id.addChannelButton);
        addChannelButton.setOnClickListener(this);
        Button settingsButton = (Button) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(this);
    }


    private void showOrHideNavigation() {
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.menuFragment);
        if (!this.visibilityFlag) { // need to show
            this.visibilityFlag = !visibilityFlag;
            relativeLayout.setVisibility(View.VISIBLE);
        } else { // need to hide
            this.visibilityFlag = !visibilityFlag;
            relativeLayout.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()) {
            case R.id.menuButton:
                showOrHideNavigation();
                break;
            case R.id.channelsListButton:
                i = new Intent(MapActivity.this, ChannelsActivity.class);
                finish();
                startActivity(i);
                break;
            case R.id.addChannelButton:
                i = new Intent(MapActivity.this, AddChannelActivity.class);
                startActivity(i);
                break;
            case R.id.settingsButton:
                i = new Intent(MapActivity.this, SettingsActivity.class);
                startActivity(i);
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                getAccelerometer(event);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.

            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        // Get my friends shared preference
        SharedPreferences friends = getApplication().getSharedPreferences("friends", 0);

        // Store friends in a map
        Map<String,?> keys = friends.getAll();

        // Get friends name location and channel and store in array
        String friendsTemp[];
        for (Map.Entry<String,?> entry : keys.entrySet()){
            friendsTemp = entry.getValue().toString().split(":");

            String name = friendsTemp[0];
            Double longt = Double.parseDouble(friendsTemp[1]);
            Double lat = Double.parseDouble(friendsTemp[2]);
            String channelId = friendsTemp[3];


            int color = 12;
            // If channel isn't in color's hash map store a new color for it
            if (channelColor.containsKey(channelId)) {
                color = (int)channelColor.get(channelId);
            }
            else {
                Random rand = new Random();

                color = rand.nextInt(355)+1;
                channelColor.put(channelId,color);

            }

            // Add marker for the friend with his channel's color
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, longt))
                    .title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean result = gestureDetector.onTouchEvent(event);
        String slideDirection = slideDetector.getSlide();
        if (slideDirection.equals("left") || slideDirection.equals("right")) {
            Intent i = new Intent(MapActivity.this, ChannelsActivity.class);
            finish();
            startActivity(i);
        }
        return result;
    }

    @Override
    public void onLocationChanged(final Location location) {

        tracker = new GPSTracker(this);
        if (!tracker.canGetLocation()) {
            tracker.showSettingsAlert();
        } else {
            myLat = tracker.getLatitude();
            myLongt = tracker.getLongitude();
        }
        if (myLat != null && myLongt != null) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(myLat, myLongt)).title("Me"));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(myLat, myLongt))
                    .zoom(11)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        try {
            unregisterReceiver(reloadDone);
        } catch (Exception ex) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }

        setUpMapIfNeeded();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "Starting location updates", Toast.LENGTH_SHORT).show();
        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Failed to connect to location updates", Toast.LENGTH_SHORT).show();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,
                (com.google.android.gms.location.LocationListener) this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(MapActivity.this, ChannelsActivity.class);
        startActivity(intent);
    }

    private class LoadBalancer extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpGet httpGet = new HttpGet(params[0]);
            String text = null;
            try {
                HttpResponse response = holder.execute(httpGet);
                HttpEntity entity = response.getEntity();
                text = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            String url = "http://" + result;

            // result of load balancing is the server the user need to go to
            if (url.contains("appspot.com")) {
                Toast.makeText(MapActivity.this, "You were moved to server: " + url,Toast.LENGTH_LONG).show();
                // logoff from this server
                DefaultHttpClient userHttpClient = HttpClientHolder.getHttpClient();

                try {
                    new LoginOrLogoffCall(userHttpClient, MapActivity.this)
                            .execute(result, "logoff");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(MapActivity.this, GoogleLoginActivity.class);

                intent.putExtra("serverName", url);
                startActivity(intent);
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