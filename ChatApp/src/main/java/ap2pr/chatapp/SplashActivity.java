/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends BaseActivity {
    private final int[] textArr = new int[]{R.string.hello,
            R.string.welcome_to_chatapp};

    private TextView textView = null;
    private ProgressBar pbLoader;
    private Integer i = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        textView = (TextView) findViewById(R.id.SwitchedText);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 2; i++) {
                        final String currentText = getResources().
                                getString(textArr[i]);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(currentText);
                            }
                        });
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };
        Thread t = new Thread(r);
        t.start();
        pbLoader = (ProgressBar) findViewById(R.id.logo_pbLoader);
        Thread tLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (i <= 100) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbLoader.setProgress(i);
                            }
                        });
                        Thread.sleep(20);
                        i++;
                    }
                    Intent i = new Intent(SplashActivity.this, ChannelsActivity.class);
                    startActivity(i);
                } catch (Exception ex) {

                }
            }
        });
        tLoader.start();
    }
}