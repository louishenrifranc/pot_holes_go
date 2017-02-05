package com.example.lh.niddepoule;

import android.content.IntentSender;
import android.location.Location;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final double RADIUS_SEARCH_QUERY = 1.2;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMapOptions mGoogleMapOptions;

    private Button mReportButton;
    private RadioButton mRadioButton;

    private List<LatLng> mHeatMapPoints;
    private DatabaseReference ref;

    private LatLng currentPosition;

    private GeoFire geoFire;
    private GeoQuery geoQuery;

    Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleMapOptions = new GoogleMapOptions();
        mGoogleMapOptions.mapType(GoogleMap.MAP_TYPE_HYBRID)
                .compassEnabled(false)
                .rotateGesturesEnabled(false)
                .tiltGesturesEnabled(false);

        mReportButton = (Button) findViewById(R.id.button);
        mRadioButton = (RadioButton) findViewById(R.id.radioButton);

        mHeatMapPoints = new ArrayList<>();


        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("POSITION", currentPosition.toString());
                String key = Double.toString((double) currentPosition.latitude) + "," + Double.toString((double) currentPosition.longitude);
                key = key.replaceAll("\\.", "_");
                geoFire.setLocation(key, new GeoLocation(currentPosition.latitude, currentPosition.longitude), new GeoFire.CompletionListener() {

                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error == null) {
                            Snackbar
                                    .make(findViewById(R.id.map), "You are the " +
                                            "best, thank you for making Montreal an even better place", Snackbar.LENGTH_LONG).show();
                        } else {
                            Snackbar
                                    .make(findViewById(R.id.map), "Couldn't upload the information, but you are still the best", Snackbar.LENGTH_LONG).show();

                        }
                    }
                });
            }
        });

        ref = FirebaseDatabase.getInstance().getReference("locations/");
        geoFire = new GeoFire(ref);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public static double distance(LatLng l1, LatLng l2) {
        double lat1 = l1.latitude;
        double lat2 = l2.latitude;
        double lon1 = l1.longitude;
        double lon2 = l2.longitude;
        double el1 = 0;
        double el2 = 0;
        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraMoveCanceledListener(new GoogleMap.OnCameraMoveCanceledListener() {
            @Override
            public void onCameraMoveCanceled() {
                Log.d("MOVE", "HAS CANCELLED");
            }
        });
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                currentPosition = mMap.getCameraPosition().target;
                Log.d("MOVE",
                        Double.toString(distance(currentPosition, new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))));
            }
        });
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                Log.d("IDLE", "HAS IDLE");
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("CLICK", "HAS CLICKED");
            }
        });
    }

    private Location getLastLocation() {
        return LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLastLocation = getLastLocation();
        if (mLastLocation != null) {
            LatLng newPosition = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 20.0f));
        }
        uploadHeatMap();
    }

    public void uploadHeatMap() {
        if (mRadioButton.isChecked()) {
            mLastLocation = getLastLocation();
            geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),
                    mLastLocation.getLongitude()), RADIUS_SEARCH_QUERY);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    key = key.replaceAll("_", "\\.");
                    Log.d("RETRIEVE", key);
                    List<String> list = new ArrayList<String>(Arrays.asList(key.split("\\s*,\\s*")));
                    try {
                        Double latitude = Double.parseDouble(list.get(0));
                        Double longitude = Double.parseDouble(list.get(1));
                        mHeatMapPoints.add(new LatLng(latitude, longitude));
                    } catch (Exception e) {
                        // for beta
                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                }

                @Override
                public void onGeoQueryReady() {
                    buildHeatMap();
                    geoQuery.removeAllListeners();
                    System.out.println("All initial data has been loaded and events have been fired!");
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    System.err.println("There was an error with this query: " + error);
                }
            });
        }
    }

    public void buildHeatMap() {
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(mHeatMapPoints)
                .build();
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        // number of milliseconds before new receiving new updates
        mLocationRequest.setInterval(10000);
        // set the fastest interval at which the app can handle new updates
        mLocationRequest.setFastestInterval(5000);
        // PRIORITY_BALANCED_POWER_ACCURACY: approximately 100 meters
        // PRIORITY_HIGH_ACCURACY: GPS localisation (very precise)
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.

                        break;
                }
            }
        });
    }
}
