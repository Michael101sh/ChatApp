/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;

public class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
    private ArrayList<String> slideDirection = new ArrayList<String>();
    private final static int SWIPE_MIN_DISTANCE = 60;
    private final static int SWIPE_MAX_OFF_PATH = 250; // range of deviation
    private final static int SWIPE_THRESHOLD_VELOCITY = 100; // speed

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                return false;
            }
            if ((e1.getX() - e2.getX()) >
                    SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                slideDirection.add("left");
                System.out.println("left");
            } else if ((e2.getX() - e1.getX()) >
                    SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                slideDirection.add("right");
                System.out.println("right");
            }
        } catch (Exception e) {

        }
        return false;
    }

    public String getSlide() {
        return (slideDirection.size() > 0) ? slideDirection.remove(0) : "";
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }
}