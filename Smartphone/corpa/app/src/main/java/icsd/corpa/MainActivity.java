package icsd.corpa;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    ClusterManager<Defibrillator> clusterManager;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        tb.setSubtitle("MapView");

        FloatingActionButton ref = findViewById(R.id.ref);
        ref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDefs();
            }
        });

        Button vid = findViewById(R.id.vid);
        vid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=fb29LCjX4-E&t=81s"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setPackage("com.google.android.youtube");
                startActivity(intent);
            }
        });

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                LatLng point = marker.getPosition();
                Snackbar snack = Snackbar.make(findViewById(R.id.main), marker.getTitle(), Snackbar.LENGTH_LONG);
                snack.setAction("Επεξεργασία Απινιδωτή", v -> {
                    Intent i = new Intent(MainActivity.this, EdDefActivity.class);
                    i.putExtra("latlng", point);
                    i.putExtra("name", marker.getTitle());
                    i.putExtra("desc", marker.getSnippet());
                    startActivity(i);
                }).show();
            }
        });

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                Snackbar snack = Snackbar.make(findViewById(R.id.main), point.toString(), Snackbar.LENGTH_LONG);
                snack.setAction("Προσθήκη Απινιδωτή", v -> {
                    Intent i = new Intent(MainActivity.this, RegDefActivity.class);
                    i.putExtra("latlng", point);
                    startActivity(i);
                }).show();
            }
        });
        getDefs();
        googleMap.setIndoorEnabled(true);

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                // mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
            }
        }
    };


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void setUpClusterManager(GoogleMap googleMap, List<Defibrillator> items){
        clusterManager = new ClusterManager(this, googleMap);  // 3
        googleMap.setOnCameraIdleListener(clusterManager);
        clusterManager.addItems(items);  // 4
        clusterManager.cluster();
    }

    private ArrayList<Defibrillator> jsonStringToArrayList(String json) {
        ArrayList<Defibrillator> items = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray defs = jsonObject.getJSONArray("defibrillatorsList");
            for(int i=0; i<defs.length(); i++){
                items.add(new Defibrillator(
                        defs.getJSONObject(i).getString("name"),
                        defs.getJSONObject(i).getString("description"),
                        new LatLng(
                                defs.getJSONObject(i).getDouble("lat"),
                                defs.getJSONObject(i).getDouble("long")
                        )
                ));
            }
        }  catch (JSONException e) {
            Log.i("json", e.toString());
        }

        return items;
    }

    private void getDefs() {
       new Thread(new Runnable() {
           @Override
           public void run() {
               StringBuilder json = new StringBuilder();
               try {
                   Connection.Response defList = Jsoup
                           .connect("https://kostas109.pythonanywhere.com/defibrillators")
                           .ignoreContentType(true)
                           .execute();

                   if(defList.statusCode() == 200) {
                       json.append(defList.body());
                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               setUpClusterManager(mGoogleMap, jsonStringToArrayList(json.toString()));
                               Toast.makeText(MainActivity.this, "Οι τοποθεσίες ανανεώθηκαν", Toast.LENGTH_LONG).show();
                           }
                       });
                   }

               } catch (Exception e) {
                   Log.v("error", e.toString());
               }
           }
       }).start();
    }

    private void updateDefs() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder json = new StringBuilder();
                try {
                    Connection.Response defList = Jsoup
                            .connect("https://kostas109.pythonanywhere.com/defibrillators")
                            .ignoreContentType(true)
                            .execute();

                    if(defList.statusCode() == 200) {
                        json.append(defList.body());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clusterManager.clearItems();
                                clusterManager.addItems(jsonStringToArrayList(json.toString()));
                                Toast.makeText(MainActivity.this, "Οι τοποθεσίες ανανεώθηκαν", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.v("error", e.toString());
                }
            }
        }).start();
    }

}
