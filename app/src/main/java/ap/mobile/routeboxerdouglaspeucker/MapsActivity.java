package ap.mobile.routeboxerdouglaspeucker;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;

import ap.mobile.douglaspeuckerlib.DouglasPeucker;
import ap.mobile.routeboxerlib.RouteBoxer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnInfoWindowClickListener, IMaps, RouteBoxerTask.IRouteBoxerTask {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private float defaultZoom = 13;
    private LatLng origin, destination;
    private Marker originMarker, destinationMarker;
    private Polyline routePolyline, simplifiedPolyline;
    private ArrayList<Polygon> boxPolygons;
    private MaterialDialog routeBoxProcessDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Create an instance of GoogleAPIClient.
        if (this.mGoogleApiClient == null) {
            this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    protected void onStart() {
        this.mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        this.mGoogleApiClient.disconnect();
        super.onStop();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            return;
        }
        this.mMap.setMyLocationEnabled(true);
        this.mMap.setOnMapLongClickListener(this);
        this.mMap.setOnInfoWindowClickListener(this);

        UiSettings uiSettings = this.mMap.getUiSettings();

        uiSettings.setCompassEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMapToolbarEnabled(true);

        if (this.origin != null)
            this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(this.origin, this.defaultZoom));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(this.mGoogleApiClient);
        this.origin = new LatLng(location.getLatitude(), location.getLongitude());
        //origin = new LatLng(38.595900, -89.985198);
        //origin = new LatLng(38.506380, -89.968063);

        if (this.origin == null)
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("Unable to obtain your last known location. Please enable Location on Settings.")
                    .show();
        if (this.mMap != null && this.origin != null) {
            this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(this.origin, this.defaultZoom));
            MarkerOptions originMarkerOptions = new MarkerOptions()
                    .title("Your location")
                    .snippet("Your last known location")
                    .position(this.origin)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            if (this.originMarker != null)
                this.originMarker.remove();
            this.originMarker = this.mMap.addMarker(originMarkerOptions);
        }
        this.mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        this.destination = latLng;

        //this.destination = new LatLng(-7.953037137608645,112.63877917081118);
        //this.origin = new LatLng(-7.9545055,112.6148412);

        //this.destination = new LatLng(-7.982594952681266,112.63102859258652);
        //this.origin = new LatLng(-7.9520931,112.6126944);

        MarkerOptions destinationMarkerOptions = new MarkerOptions()
                .title("Destination")
                .position(this.destination)
                .snippet("Click to route box from your location.");

        if(this.destinationMarker != null)
            this.destinationMarker.remove();
        this.destinationMarker = this.mMap.addMarker(destinationMarkerOptions);
        this.destinationMarker.showInfoWindow();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, marker.getPosition().latitude + ", " + marker.getPosition().longitude, Toast.LENGTH_SHORT).show();
        if(this.origin != null && this.destination != null) {
            //origin = new LatLng(38.595900, -89.985198);
            //destination = new LatLng(38.506360, -89.984318);
            //destination = new LatLng(38.506380, -89.968063);
            //origin = new LatLng(38.504700, -89.851810);

            //this.origin = new LatLng(-7.9544773,112.6148372);
            //this.destination = new LatLng(-7.953271897865304,112.63915132731199);

            RouteTask routeTask = new RouteTask(this, this.origin, this.destination);
            if (routeTask.getStatus() == AsyncTask.Status.PENDING)
                routeTask.execute();

            this.routeBoxProcessDialog = new MaterialDialog.Builder(this)
                    .cancelable(false)
                    .content("Obtaining boxes...")
                    .progress(true, 0)
                    .progressIndeterminateStyle(true)
                    .show();

        }
    }

    @Override
    public void onJSONRouteLoaded(ArrayList<LatLng> route) throws IOException {
        //RouteBoxerTask routeBoxerTask = new RouteBoxerTask(route, this.distance, this);
        //routeBoxerTask.execute();
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(8);
        for (LatLng point : route)
            polylineOptions.add(point);
        if (this.routePolyline != null)
            this.routePolyline.remove();
        this.routePolyline = this.mMap.addPolyline(polylineOptions);

        ArrayList<RouteBoxer.LatLng> points = new ArrayList<>();
        for(LatLng point: route)
            points.add(new RouteBoxer.LatLng(point.latitude, point.longitude));

        DouglasPeucker douglasPeucker = new DouglasPeucker();
        ArrayList<RouteBoxer.LatLng> simplifiedRoute = douglasPeucker.simplify(points);


        PolylineOptions simplifiedPolylineOptions = new PolylineOptions()
                .color(Color.BLUE)
                .width(8);
        for(RouteBoxer.LatLng latLng : simplifiedRoute)
            simplifiedPolylineOptions.add(new LatLng(latLng.latitude, latLng.longitude));

        if(this.simplifiedPolyline != null)
            this.simplifiedPolyline.remove();

        this.simplifiedPolyline = this.mMap.addPolyline(simplifiedPolylineOptions);

        if (this.boxPolygons == null)
            this.boxPolygons = new ArrayList<>();
        else {
            for (Polygon polygon : this.boxPolygons) {
                polygon.remove();
            }
        }

        ArrayList<LatLng> sRoute = new ArrayList<>();
        for(RouteBoxer.LatLng point: simplifiedRoute)
            sRoute.add(new LatLng(point.latitude, point.longitude));

        RouteBoxerTask routeBoxerTask = new RouteBoxerTask(sRoute, 200, this);
        routeBoxerTask.execute();


    }


    @Override
    public void onRouteBoxerTaskComplete(ArrayList<RouteBoxer.Box> boxes) {
        this.draw(boxes, Color.GRAY, Color.argb(15, 255, 0, 0));
        if(this.routeBoxProcessDialog != null && this.routeBoxProcessDialog.isShowing())
            this.routeBoxProcessDialog.dismiss();
    }

    @Override
    public void onRouteBoxerBoxPublished(ArrayList<RouteBoxer.Box> boxes, int step) {

    }

    @Override
    public void onMessage(String message) {

    }

    private void draw(ArrayList<RouteBoxer.Box> boxes, int color, int fillColor) {

        if(this.boxPolygons == null)
            this.boxPolygons = new ArrayList<>();
        else this.boxPolygons.clear();

        for (RouteBoxer.Box box : boxes) {
            LatLng nw = new LatLng(box.ne.latitude, box.sw.longitude);
            LatLng se = new LatLng(box.sw.latitude, box.ne.longitude);
            LatLng sw = new LatLng(box.sw.latitude, box.sw.longitude);
            LatLng ne = new LatLng(box.ne.latitude, box.ne.longitude);
            PolygonOptions polygonOptions = new PolygonOptions()
                    .add(sw, nw, ne, se, sw)
                    .strokeColor(color)
                    .strokeWidth(5);
            if (box.marked) {
                polygonOptions.strokeColor(Color.DKGRAY)
                        .fillColor(Color.argb(96, 0, 0, 0));
            } else if (box.expandMarked) {
                polygonOptions.strokeColor(Color.DKGRAY)
                        .fillColor(Color.argb(72, 0, 0, 0));
            } else
                polygonOptions.fillColor(fillColor);
            Polygon boxPolygon = mMap.addPolygon(polygonOptions);
            this.boxPolygons.add(boxPolygon);
        }

        return;
    }
}
