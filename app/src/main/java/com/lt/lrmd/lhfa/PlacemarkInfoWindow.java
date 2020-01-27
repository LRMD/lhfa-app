package com.lt.lrmd.lhfa;

import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class PlacemarkInfoWindow extends InfoWindow implements Observer {

    private Context context;
    private MapView mapView;
    private Marker marker;
    private ArrayList<Polyline> routes;
    private ObservedLocation observedLocation;
    private Placemark placemark;

    public PlacemarkInfoWindow(int layoutResId, MapView mapView, Context context, Marker marker, ArrayList<Polyline> routes, ObservedLocation observedLocation, Placemark placemark) {
        super(layoutResId, mapView);
        this.context = context;
        this.marker = marker;
        this.mapView = mapView;
        this.routes = routes;
        this.observedLocation = observedLocation;
        this.placemark = placemark;
    }
    public void onClose() {

    }

    public void update(Observable obj, final Object arg)
    {
        TextView distance = mView.findViewById(R.id.distance);
        GPSTracker tracker = new GPSTracker(context, observedLocation);
        GeoPoint currentGeoPoint = new GeoPoint(tracker.getLatitude(), tracker.getLongitude());
        distance.setText(distance(currentGeoPoint, new GeoPoint(placemark.getLatitude(), placemark.getLongitude())));
    }

    private String distance(GeoPoint current, GeoPoint des)
    {
        float[] results = new float[1];
        Location.distanceBetween(
                current.getLatitude(),current.getLongitude(),
                des.getLatitude(), des.getLongitude(), results);
        return getView().getResources().getString(R.string.distance) + ": ~" + results[0] + " " + getView().getResources().getString(R.string.meters);
    }

    public void onOpen(Object arg0) {
        InfoWindow.closeAllInfoWindowsOn(mapView);
        TextView title = mView.findViewById(R.id.placemarkTitle);
        title.setText(placemark.name);
        TextView description = mView.findViewById(R.id.placemarkDescription);
        description.setText(placemark.description);
        Button showRouteButton = mView.findViewById(R.id.showRouteButton);
        observedLocation.addObserver(this);
        TextView distance = mView.findViewById(R.id.distance);
        GPSTracker tracker = new GPSTracker(context, observedLocation);
        GeoPoint currentGeoPoint = new GeoPoint(tracker.getLatitude(), tracker.getLongitude());
        distance.setText(distance(currentGeoPoint, new GeoPoint(placemark.getLatitude(), placemark.getLongitude())));
        showRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0 ; i < routes.size(); i++) mapView.getOverlays().remove(routes.get(i));
                mapView.invalidate();
                final GPSTracker tracker = new GPSTracker(context, observedLocation);
                GeoPoint currentGeoPoint = new GeoPoint(tracker.getLatitude(), tracker.getLongitude());
                RoadManager roadManager = new OSRMRoadManager(context);
                ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
                waypoints.add(currentGeoPoint);
                waypoints.add(new GeoPoint(marker.getPosition().getLatitude(), marker.getPosition().getLongitude()));
                closeAllInfoWindowsOn(mapView);
                new GetRouteTask(roadManager, waypoints, mapView, routes).execute();
            }
        });
    }
}
