package ap.mobile.routeboxerdouglaspeucker;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import ap.mobile.routeboxerlib.RouteBoxer;

/**
 * Created by aryo on 30/1/16.
 */
public class RouteBoxerTask extends AsyncTask<Void, Void, ArrayList<RouteBoxer.Box>> {

    private final ArrayList<RouteBoxer.LatLng> route = new ArrayList<>();
    private final int distance;
    private final IRouteBoxerTask iRouteBoxerTask;
    private int step;

    public RouteBoxerTask(ArrayList<LatLng> route, int distance, IRouteBoxerTask iRouteBoxerTask) {
        for (LatLng point:
                route) {
            RouteBoxer.LatLng latLng = new RouteBoxer.LatLng(point.latitude, point.longitude);
            this.route.add(latLng);
        }
        this.distance = distance;
        this.iRouteBoxerTask = iRouteBoxerTask;
    }

    @Override
    protected ArrayList<RouteBoxer.Box> doInBackground(Void... params) {
        RouteBoxer routeBoxer = new RouteBoxer(route, distance);
        ArrayList<RouteBoxer.Box> boxes = routeBoxer.box();
        return boxes;
    }

    @Override
    protected void onPostExecute(ArrayList<RouteBoxer.Box> boxes) {
        if(this.iRouteBoxerTask != null)
            this.iRouteBoxerTask.onRouteBoxerTaskComplete(boxes);
    }

    public interface IRouteBoxerTask {

        void onRouteBoxerTaskComplete(ArrayList<RouteBoxer.Box> boxes);
        void onRouteBoxerBoxPublished(ArrayList<RouteBoxer.Box> boxes, int step);
        void onMessage(String message);
    }
}
