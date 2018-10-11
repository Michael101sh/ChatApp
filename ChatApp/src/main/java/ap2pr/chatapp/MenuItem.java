/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.view.View;

/*An item in the menu*/
public class MenuItem {
    private String title;
    View.OnClickListener listener;

    /*Constructor*/
    public MenuItem(String title, View.OnClickListener listener){
        this.title = title;
        this.listener = listener;
    }

    /*Get the item title*/
    public String getTitle(){
        return this.title;
    }

    /*Get the item listener for a click*/
    public View.OnClickListener getListener(){
        return this.listener;
    }
}