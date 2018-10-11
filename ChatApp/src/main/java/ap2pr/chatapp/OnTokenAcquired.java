/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.apache.http.impl.client.DefaultHttpClient;

//the result for the auth token request is returned to your application
//via the Account Manager Callback you specified when making the request.
//check the returned bundle if an Intent is stored against the AccountManager.KEY_INTENT key.
//if there is an Intent then start the activity using that intent to ask for user permission
//otherwise you can retrieve the auth token from the bundle.
public class OnTokenAcquired implements AccountManagerCallback<Bundle> {

    private static final int USER_PERMISSION = 989;
    private String APP_ID;
    private DefaultHttpClient httpclient;
    private Activity activity;
    private String baseServerUrl;

    public OnTokenAcquired(DefaultHttpClient httpclient, String baseServerUrl,
                           Activity activity)
    {
        this.httpclient = httpclient;
        this.activity = activity;
        this.baseServerUrl = baseServerUrl;
        this.APP_ID = (this.baseServerUrl.replace("http://", "")).replace(".appspot.com", "");
    }

    public void run(AccountManagerFuture<Bundle> result) {

        Bundle bundle;

        try {
            bundle = (Bundle) result.getResult();
            // there is not token
            if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
                intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivityForResult(intent, USER_PERMISSION);
            // there is token, so we use it.
            } else {
                setAuthToken(bundle);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    //using the auth token and ask for a auth cookie
    protected void setAuthToken(Bundle bundle) {
        String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);

        new GetCookie(httpclient, baseServerUrl, APP_ID, activity.getBaseContext())
                .execute(authToken);
    }
};