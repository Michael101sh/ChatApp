/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.app.IntentService;
import android.content.Intent;

public class ReloadService extends IntentService
{
    public static final String DONE = "ap2pr.chatapp.Services.ReloadService.DONE";

    public ReloadService() {
        super(ReloadService.class.getName());
    }

    public ReloadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Intent i = new Intent(DONE);
        this.sendBroadcast(i);
    }
}
