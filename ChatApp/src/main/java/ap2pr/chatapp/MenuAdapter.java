/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.List;

/*Override menu list view*/
public class MenuAdapter extends BaseAdapter {
    private Activity activity;
    private LayoutInflater inflater;
    private List<MenuItem> items;

    /*Constructor*/
    public MenuAdapter(Activity activity, List<MenuItem> items) {
        this.activity = activity;
        this.items = items;
    }

    @Override
    /*Get number of menu items*/
    public int getCount() {
        return items.size();
    }

    @Override
    /*Get a menu item*/
    public Object getItem(int location) {
        return items.get(location);
    }

    @Override
    /*Get menu item id*/
    public long getItemId(int position) {
        return position;
    }

    @Override
    /*Get menu item view*/
    public View getView(int position, View convertView, ViewGroup parent) {

        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.list_item_menu, null);

        TextView txtTitle = (TextView) convertView.findViewById(R.id.menu_title);
        RelativeLayout layout = (RelativeLayout) convertView.findViewById(R.id.menu_item);

        MenuItem item = items.get(position);

        txtTitle.setText(item.getTitle());

        layout.setOnClickListener(item.getListener());

        return convertView;
    }
}