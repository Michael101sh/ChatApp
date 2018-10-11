/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<ChannelObject> channelObjectsList;
    private Context context = null;
    private SharedPreferences userServerSharedPref;

    public ChannelAdapter(List<ChannelObject> channelObjectsList, Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.channelObjectsList = channelObjectsList;
        this.context = context;
        this. userServerSharedPref = context.getSharedPreferences("userServer", Context.MODE_PRIVATE);

    }

    public int getCount() {
        return channelObjectsList.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    /*private view holder class*/
    private class ViewHolder {
        public TextView nameView;
        public ImageView iconView;
        public RelativeLayout layoutView;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final int p = position;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.channel_adapter, null);
            holder.nameView = (TextView) convertView.findViewById(R.id.name);
            holder.iconView = (ImageView) convertView.findViewById(R.id.icon);
            holder.layoutView = (RelativeLayout) convertView.findViewById(R.id.layout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        holder.layoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);

                // set title
                alertDialogBuilder.setTitle("JOIN CHANNEL");

                // set dialog message
                alertDialogBuilder
                        .setMessage("Do you want to join ''" + channelObjectsList.get(p).getName() + "''?")
                        .setCancelable(false)
                        .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // if this button is clicked join the chosen channel
                                if (userServerSharedPref.contains("userServer")) {
                                    String serverUrl = userServerSharedPref.getString("userServer", "none");
                                    new JoinChannel(channelObjectsList.get(p).getID())
                                                    .execute(serverUrl.concat("/joinChannel"));
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, just close the dialog box and do nothing
                                dialog.cancel();
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });
        System.out.println("get in");
        holder.nameView.setText(channelObjectsList.get(position).getName());
        //set the icon from the input string
        try {

            holder.iconView.setBackgroundDrawable(
                    (Drawable)new BitmapDrawable(decodeBase64(channelObjectsList.get(position).getIcon())));
        } catch (Exception e) {
            //if the string not base64 show a default icon
            holder.iconView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.icon1));
        }
        return convertView;
    }

    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
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
                if (status.equals("1"))
                {
                    Toast toast = Toast.makeText(context, "Join succeed", Toast.LENGTH_LONG);
                    toast.show();
                }
                else
                {
                    Toast toast = Toast.makeText(context, "Join failed:\n" + message, Toast.LENGTH_LONG);
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
}