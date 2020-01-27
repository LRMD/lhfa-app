package com.lt.lrmd.lhfa;

import android.os.AsyncTask;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class GetRouteTask extends AsyncTask<Void, Void, Void> {

    private ArrayList<GeoPoint> waypoints;
    private RoadManager roadManager;
    private MapView mapView;
    private Polyline roadOverlay;
    private ArrayList<Polyline> routes;

    public GetRouteTask(RoadManager roadManager, ArrayList<GeoPoint> waypoints, MapView mapView, ArrayList<Polyline> routes)
    {
        this.roadManager = roadManager;
        this.waypoints = waypoints;
        this.mapView = mapView;
        this.routes = routes ;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Road road = roadManager.getRoad(waypoints);
        roadOverlay = RoadManager.buildRoadOverlay(road);
        routes.add(roadOverlay);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mapView.getOverlays().add(roadOverlay);
        mapView.invalidate();
    }
}
