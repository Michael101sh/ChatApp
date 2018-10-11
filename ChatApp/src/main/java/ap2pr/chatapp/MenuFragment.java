/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

/*Adapter for the menu view*/
public class MenuFragment extends Fragment {
    /*Constructor*/
    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    /*Initialize members when fragment created*/
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //get views
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        ListView lstMenu = (ListView) view.findViewById(R.id.lstMenu);
        List<MenuItem> menuItems = new ArrayList<>();
        String addChannelTitle = getActivity().getResources().getString(R.string.add_channel);
        String settingTitle = getActivity().getResources().getString(R.string.action_settings);

        //set menu items and they click listeners
        menuItems.add(new MenuItem(addChannelTitle, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddChannelActivity.class);
                startActivity(intent);
            }
        }));
        menuItems.add(new MenuItem(settingTitle, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        }));

        MenuAdapter menuAdapter = new MenuAdapter(getActivity(), menuItems);
        lstMenu.setAdapter(menuAdapter);

        return view;
    }
}