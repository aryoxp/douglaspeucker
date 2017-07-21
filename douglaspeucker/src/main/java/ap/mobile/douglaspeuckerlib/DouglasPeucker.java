package ap.mobile.douglaspeuckerlib;

import java.util.ArrayList;
import ap.mobile.routeboxerlib.RouteBoxer;

/**
 * Created by Aryo on 21/07/2017.
 */

public class DouglasPeucker {

    private ArrayList<LatLng> points = new ArrayList<>();
    private double toleranceDistance = 200; // meter
    private double deg = 111319.9;

    public ArrayList<RouteBoxer.LatLng> simplify(ArrayList<RouteBoxer.LatLng> points) {

        for (RouteBoxer.LatLng point:
                points
             ) {
            this.points.add(new LatLng(point.latitude, point.longitude));
        }

        this.points = this.douglasPeucker(this.points);

        ArrayList<RouteBoxer.LatLng> simplifiedPoints = new ArrayList<>();
        simplifiedPoints.add(this.points.get(0));
        for (LatLng point:
                this.points) {
            if(point.isKeep())
                simplifiedPoints.add(point);
        }
        simplifiedPoints.add(this.points.get(this.points.size()-1));
        return simplifiedPoints;

    }


    public ArrayList<LatLng> douglasPeucker(ArrayList<LatLng> points) {


        int length = points.size();

        double maxDistance = 0;
        int maxIndex = 0;
        double distanceDeg = toleranceDistance / deg;

        for(int i = 1; i < length - 2; i++) {

            LatLng origin = points.get(0);
            LatLng destination = points.get(length-1);

            double pDistance = perpendicularDistance(points.get(i), origin, destination);
            if(pDistance > maxDistance) {
                maxDistance = pDistance;
                maxIndex = i;
            }

        }

        if(maxDistance > distanceDeg) {
            points.get(maxIndex).keep();
            douglasPeucker(new ArrayList<LatLng>(points.subList(0, maxIndex)));
            douglasPeucker(new ArrayList<LatLng>(points.subList(maxIndex, length-1)));
        }

        return points;

    }

    public double perpendicularDistance(LatLng point, LatLng origin, LatLng destination) {

        // (py – qy)x + (qx – px)y + (pxqy – qxpy) = 0

        double a = origin.latitude - destination.latitude;
        double b = destination.longitude - origin.longitude;
        double c = (origin.longitude * destination.latitude)
                - (destination.longitude * origin.latitude);

        //d = |Am + Bn + C| / sqrt (A^2 + B^2);

        double pDistance = Math.abs(a * point.longitude + b * point.latitude + c) /
                (Math.sqrt(a * a + b * b));
        return pDistance;

    }


    public class LatLng extends RouteBoxer.LatLng {

        private boolean keep = false;

        public LatLng(double latitude, double longitude) {
            super(latitude, longitude);
        }

        private void keep() {
            this.keep = true;
        }

        private boolean isKeep() {
            return this.keep;
        }

    }


}
