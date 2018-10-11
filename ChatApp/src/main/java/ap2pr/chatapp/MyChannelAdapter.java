/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.List;

public class MyChannelAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<ChannelObject> channelObjectsList;
    private Context context = null;

    public MyChannelAdapter(List<ChannelObject> channelObjectsList, Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.channelObjectsList = channelObjectsList;
        this.context = context;
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
            holder.layoutView = (RelativeLayout)convertView.findViewById(R.id.layout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        System.out.println("get in");
        holder.nameView.setText(channelObjectsList.get(position).getName());
        try {
            holder.iconView.setBackgroundDrawable( (Drawable)new BitmapDrawable(decodeBase64(channelObjectsList.get(position).getIcon())));
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
}