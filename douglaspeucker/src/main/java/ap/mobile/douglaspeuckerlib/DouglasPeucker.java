package ap.mobile.douglaspeuckerlib;

import java.util.ArrayList;
import ap.mobile.routeboxerlib.RouteBoxer;

/**
 * Created by Aryo on 21/07/2017.
 */

public class DouglasPeucker {

    private ArrayList<LatLng> points = new ArrayList<>();

    public ArrayList<LatLng> simplify(ArrayList<RouteBoxer.LatLng> points) {

        

        return this.points;

    }

    public class LatLng extends RouteBoxer.LatLng {

        private boolean isRemoved = false;

        public LatLng(double latitude, double longitude) {
            super(latitude, longitude);
        }

        private void setRemoved() {
            this.isRemoved = true;
        }

        public boolean isRemoved() {
            return this.isRemoved;
        }

    }


}
