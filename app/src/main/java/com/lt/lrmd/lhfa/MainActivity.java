package com.lt.lrmd.lhfa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import android.graphics.drawable.Drawable;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AsyncResponse, Observer, MapEventsReceiver {
    /**
     * See https://g.co/AppIndexing/AndroidStudio
     **/
    private GoogleApiClient client;
    private static String kmz_url = "http://rk.vdu.lt/phocadownload/Google_Earth/lhfa-updated.kmz";
    public static KmlDocument mKmlDocument; //made static to pass between activities
    protected FolderOverlay mKmlOverlay; //root container of overlays from KML reading
    protected MapView mapView;
    protected Marker startMarker;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private KmlDocument mKmlDoc;
    private ArrayList<Placemark> mPlacemarks = null;
    private SearchView mSearchView;
    private ObservedLocation observedLocation;
    private ArrayList<Polyline> routes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermissions();
        }
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(Environment.getExternalStorageDirectory(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(Environment.getExternalStorageDirectory(), "osmdroid/tiles"));
        observedLocation = new ObservedLocation();
        observedLocation.addObserver(this);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        createMapView();
        mSearchView = findViewById(R.id.searchView);
        mSearchView.setFocusable(false);
        mSearchView.clearFocus();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Koordinatės patikslintos!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                IMapController mapController = mapView.getController();
                mapController.setZoom(9);
                startMarker.setPosition(new GeoPoint(
                        new GPSTracker(getApplicationContext(), observedLocation).getLatitude(),
                        new GPSTracker(getApplicationContext(), observedLocation).getLongitude()));

                mapView.getController().setCenter(
                        new GeoPoint(
                                new GPSTracker(getApplicationContext(), observedLocation).getLatitude(),
                                new GPSTracker(getApplicationContext(), observedLocation).getLongitude())
                );
            }
        });
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        KMZDataHelper kmzDataHelper = new KMZDataHelper(this);
        mPlacemarks = kmzDataHelper.getAllPlacemarks();

    }

    public void createMapView() {

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        mapView.getOverlays().add(0, mapEventsOverlay);


        final GPSTracker tracker = new GPSTracker(getApplicationContext(), observedLocation);
        GeoPoint startPoint = new GeoPoint(tracker.getLatitude(), tracker.getLongitude());
        IMapController mapController;
        mapController = mapView.getController();
        mapController.setZoom(9);
        mapController.setCenter(startPoint);

        startMarker = new Marker(mapView);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(startMarker);

        startMarker.setIcon(getResources().getDrawable(R.drawable.ic_menu_mylocation));
        startMarker.setTitle("Jūsų koordinatės");
        mKmlDocument = new KmlDocument();
        mKmlOverlay = null;

        //putKMLAsync(kmz_url);

        ScaleBarOverlay scalebar = new ScaleBarOverlay(mapView);
        mapView.getOverlays().add(scalebar);
        mapView.setTilesScaledToDpi(true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * https://g.co/AppIndexing/AndroidStudio
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SearchView searchView = findViewById(R.id.searchView);
        searchView.clearFocus();
    }

    @Override
    public void processFinished() {

        if (mPlacemarks != null)
        {
            for (int i = 0; i < mPlacemarks.size(); i++)
            {
                Marker marker = new Marker(mapView);
                marker.setIcon(getResources().getDrawable(R.drawable.marker));
                marker.setPosition(new GeoPoint(mPlacemarks.get(i).getLatitude(), mPlacemarks.get(i).getLongitude()));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                final PlacemarkInfoWindow infoWindow = new PlacemarkInfoWindow(R.layout.info_window_layout, mapView, getApplicationContext(), marker, routes, observedLocation, mPlacemarks.get(i));
                infoWindow.getView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        infoWindow.close();
                    }
                });
                marker.setInfoWindow(infoWindow);
                mapView.getOverlays().add(marker);
            }
            mapView.invalidate();
            SearchSuggestionHelper searchSuggestionHelper = new SearchSuggestionHelper();
            final SearchView searchView = findViewById(R.id.searchView);
            final SimpleCursorAdapter suggestionAdapter = searchSuggestionHelper.buildSuggestionAdapter(getApplicationContext(), mPlacemarks, null);
            searchView.setSuggestionsAdapter(suggestionAdapter);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    showPlaceMarkOnMap(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    SearchSuggestionHelper searchSuggestionHelper = new SearchSuggestionHelper();
                    suggestionAdapter.swapCursor(searchSuggestionHelper.buildSuggestionAdapter(getApplicationContext(), mPlacemarks, newText).getCursor());
                    return true;
                }
            });
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    String query = ((MatrixCursor)suggestionAdapter.getItem(position)).getString(1);
                    showPlaceMarkOnMap(query);
                    return true;
                }
            });
        }
    }

    private void showPlaceMarkOnMap(String query)
    {
        for (int k = 0; k < mPlacemarks.size(); k++)
        {
            String placMarkName = mPlacemarks.get(k).getName() + ", " + mPlacemarks.get(k).getDescription();
            if (query.toUpperCase().contains(placMarkName.toUpperCase()))
            {
                IMapController mapController = mapView.getController();
                GeoPoint centerPoint = new GeoPoint(mPlacemarks.get(k).getLatitude(), mPlacemarks.get(k).getLongitude());
                mapView.getController().setZoom(14);
                mapController.setCenter(centerPoint);
                mSearchView.clearFocus();
            }
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        InfoWindow.closeAllInfoWindowsOn(mapView);
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    private class putKML extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... url) {
            boolean ok = false;
            URL urlobj;
            try {
                urlobj = new URL(url[0]);
                URLConnection connection = urlobj.openConnection();
                InputStream in = connection.getInputStream();
                File kmzfile = new File(getApplicationContext().getFilesDir().getPath() + "/lhfa.kmz");
                FileOutputStream fos = new FileOutputStream(kmzfile);
                byte[] buf = new byte[512];
                while (true) {
                    int len = in.read(buf);
                    if (len == -1) {
                        break;
                    }
                    fos.write(buf, 0, len);
                }
                in.close();
                fos.flush();
                fos.close();

                ok = mKmlDocument.parseKMZFile(kmzfile);

            }
            catch(MalformedURLException e){
                System.out.println("The url is not well formed: " + e.getMessage());
            }
            catch (IOException f){
                System.out.println("IO error:" + f.getMessage());
            }

            return ok;
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            super.onPostExecute(ok);
            updateKML();
        }
    }
    private void putKMLAsync(String url) {
        new putKML().execute(url);
    }


    private void updateKML(){
        if (mKmlOverlay != null){
            mKmlOverlay.closeAllInfoWindows();
            mapView.getOverlays().remove(mKmlOverlay);
        }
        Drawable defaultMarker = getResources().getDrawable(R.drawable.marker_default);
        Bitmap defaultBitmap = ((BitmapDrawable)defaultMarker).getBitmap();
        Style defaultStyle = new Style(defaultBitmap, 0x901010AA, 3.0f, 0x20AA1010);

        mKmlOverlay = (FolderOverlay) mKmlDocument.mKmlRoot.buildOverlay(mapView, defaultStyle, null, mKmlDocument);
        mapView.getOverlays().add(0,mKmlOverlay);
        mapView.invalidate();
    }

    @Override
    public void update(Observable obj, final Object arg)
    {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Location currentLocation = (Location) arg;
                startMarker.setPosition(new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
                mapView.invalidate();
            }
        });
    }

    // START PERMISSION CHECK

    private void checkPermissions() {
        List<String> permissions = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissions.isEmpty()) {
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (location && storage) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this, "All permissions granted", Toast.LENGTH_SHORT).show();
                } else if (location) {
                    Toast.makeText(this, "Storage permission is required to store map tiles to reduce data usage and for offline usage.", Toast.LENGTH_LONG).show();
                } else if (storage) {
                    Toast.makeText(this, "Location permission is required to show the user's location on map.", Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Storage permission is required to store map tiles to reduce data usage and for offline usage." +
                            "\nLocation permission is required to show the user's location on map.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}