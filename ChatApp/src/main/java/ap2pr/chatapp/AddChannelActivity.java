/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package ap2pr.chatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class AddChannelActivity extends BaseActivity {
    private int iconPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_channel);
        Button b = (Button) findViewById(R.id.addButton);
        final SharedPreferences userServerSharedPref =
                this.getSharedPreferences("userServer", Context.MODE_PRIVATE);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userServerSharedPref.contains("userServer")) {
                    String currentServerName = userServerSharedPref.getString("userServer", "none");
                    EditText channelId = (EditText) findViewById(R.id.channelId);
                    String id = channelId.getText().toString();
                    if (id.equals("")) {
                        String toastMessage = getResources().getString(R.string.please_enter_id);
                        Toast toast = Toast.makeText(AddChannelActivity.this,
                                toastMessage, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        return;
                    }
                    EditText channelName = (EditText) findViewById(R.id.channelName);
                    String name = channelName.getText().toString();
                    if (name.equals("")) {
                        String toastMessage = getResources().getString(R.string.please_enter_name);
                        Toast toast = Toast.makeText(AddChannelActivity.this,
                                toastMessage, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        return;
                    }
                    if (iconPosition == 0) {
                        String toastMessage = getResources().getString(R.string.please_choose_icon);
                        Toast toast = Toast.makeText(AddChannelActivity.this,
                                toastMessage, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        return;
                    }
                    String stringResName = "icon".concat(String.valueOf(iconPosition)).concat("_base64");
                    int resIdentifier = getStringIdentifier(AddChannelActivity.this, stringResName);
                    String icon = getResources().getString(resIdentifier);
                    ChannelObject channelObject = new ChannelObject(id, name, icon);
                    try {
                        // try to add the chosen channel
                        new AddChannelCall(AddChannelActivity.this, channelObject).execute(
                                currentServerName.concat("/addChannel"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        ImageView icon1 = (ImageView) findViewById(R.id.icon1);
        ImageView icon2 = (ImageView) findViewById(R.id.icon2);
        ImageView icon3 = (ImageView) findViewById(R.id.icon3);
        ImageView icon4 = (ImageView) findViewById(R.id.icon4);
        ImageView icon5 = (ImageView) findViewById(R.id.icon5);
        icon1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconPosition = 1;
            }
        });
        icon2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconPosition = 2;
            }
        });
        icon3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconPosition = 3;
            }
        });
        icon4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconPosition = 4;
            }
        });
        icon5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconPosition = 5;
            }
        });

    }
    private static int getStringIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }
}
