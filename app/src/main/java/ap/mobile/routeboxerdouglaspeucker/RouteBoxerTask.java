package ap.mobile.routeboxerdouglaspeucker;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import ap.mobile.routeboxerlib.RouteBoxer;

/**
 * Created by aryo on 30/1/16.
 */
public class RouteBoxerTask extends AsyncTask<Void, RouteBoxerTask.RouterBoxerData, ArrayList<RouteBoxer.Box>>
        implements RouteBoxer.IRouteBoxer {

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
        ArrayList<RouteBoxer.Box> boxes = RouteBoxer.box(route, distance, this);
        return boxes;
    }

    @Override
    protected void onPostExecute(ArrayList<RouteBoxer.Box> boxes) {
        if(this.iRouteBoxerTask != null)
            this.iRouteBoxerTask.onRouteBoxerTaskComplete(boxes);
    }

    @Override
    protected void onProgressUpdate(RouteBoxerTask.RouterBoxerData... values) {
        if(this.iRouteBoxerTask != null) {
            RouterBoxerData data = values[0];

            if(data.type == DataType.Line) {
                this.iRouteBoxerTask.drawLine(data.origin, data.destination, data.color);
                return;
            }

            if(data.type == DataType.Box) {
                this.iRouteBoxerTask.drawBox(data.origin, data.destination, data.color);
                return;
            }

            if(data.type == DataType.ClearPolygon) {
                this.iRouteBoxerTask.clearPolygon();
                return;
            }

            if(data.type == DataType.Message) {
                this.iRouteBoxerTask.onMessage(data.message);
            }

            if(data.type == DataType.Result) {

                String message = data.message;
                ArrayList<RouteBoxer.Box> boxes = data.boxes;
                if (message != null)
                    this.iRouteBoxerTask.onMessage(message);
                switch (this.step) {
                    case 1:
                        break;
                }
            }
        }
    }

    @Override
    public void onBoxesObtained(ArrayList<RouteBoxer.Box> boxes) {

    }

    @Override
    public void onBoundsObtained(RouteBoxer.LatLngBounds bounds) {}

    @Override
    public void onGridOverlaid(ArrayList<RouteBoxer.Box> boxes) {

    }

    @Override
    public void onGridObtained(RouteBoxer.Box[][] boxArray) {

    }

    @Override
    public void onGridMarked(ArrayList<RouteBoxer.Box> boxes) {
        //this.step = 2;
        //RouterBoxerData data = new RouterBoxerData(null, boxes);
        //publishProgress(data);
    }

    @Override
    public void onGridMarksExpanded(RouteBoxer.Box[][] boxArray) {}

    @Override
    public void onMergedAdjointVertically(ArrayList<RouteBoxer.Box> boxes) {}

    @Override
    public void onMergedAdjointHorizontally(ArrayList<RouteBoxer.Box> boxes) {}

    @Override
    public void onMergedVertically(ArrayList<RouteBoxer.Box> mergedBoxes) {}

    @Override
    public void onMergedHorizontally(ArrayList<RouteBoxer.Box> mergedBoxes) {}

    @Override
    public void onProcess(String processInfo) {
        RouterBoxerData data = new RouterBoxerData(processInfo);
        this.publishProgress(data);
    }

    @Override
    public void drawLine(RouteBoxer.LatLng origin, RouteBoxer.LatLng destination, int color) {
        RouterBoxerData data = new RouterBoxerData(origin, destination, color);
        publishProgress(data);
    }

    @Override
    public void drawBox(RouteBoxer.LatLng origin, RouteBoxer.LatLng destination, int color) {
        RouterBoxerData data = new RouterBoxerData(origin, destination, color, DataType.Box);
        publishProgress(data);
    }

    @Override
    public void clearPolygon() {
        RouterBoxerData data = new RouterBoxerData(DataType.ClearPolygon);
        publishProgress(data);
    }

    public interface IRouteBoxerTask {

        void onRouteBoxerTaskComplete(ArrayList<RouteBoxer.Box> boxes);
        void onMessage(String message);
        void drawLine(LatLng origin, LatLng destination, int color);
        void drawBox(LatLng origin, LatLng destination, int color);
        void clearPolygon();
    }

    public class RouterBoxerData {

        public int color;
        public LatLng destination;
        public LatLng origin;
        public DataType type;

        public ArrayList<RouteBoxer.Box> boxes;
        public String message;

        public RouterBoxerData(String message, ArrayList<RouteBoxer.Box> boxes) {
            this.message = message;
            this.boxes = boxes;
        }

        public RouterBoxerData(RouteBoxer.LatLng origin, RouteBoxer.LatLng destination, int color) {
            this.type = DataType.Line;
            this.origin = new LatLng(origin.latitude, origin.longitude);
            this.destination = new LatLng(destination.latitude, destination.longitude);
            this.color = color;
        }

        public RouterBoxerData(RouteBoxer.LatLng origin, RouteBoxer.LatLng destination, int color, DataType type) {
            this.type = type;
            this.origin = new LatLng(origin.latitude, origin.longitude);
            this.destination = new LatLng(destination.latitude, destination.longitude);
            this.color = color;
        }

        public RouterBoxerData(DataType clearPolygon) {
            this.type = clearPolygon;
        }

        public RouterBoxerData(String processInfo) {
            this.type = DataType.Message;
            this.message = processInfo;
        }
    }

    public enum DataType {
        Message, Box, Line, Result,
        ClearPolygon;
    }
}
