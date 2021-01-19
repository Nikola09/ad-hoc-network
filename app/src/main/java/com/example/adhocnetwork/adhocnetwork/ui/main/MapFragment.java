package com.example.adhocnetwork.adhocnetwork.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.adhocnetwork.adhocnetwork.ConnectionService;
import com.example.adhocnetwork.adhocnetwork.MyData;
import com.example.adhocnetwork.adhocnetwork.R;
import com.example.adhocnetwork.adhocnetwork.UserData;
import com.example.adhocnetwork.adhocnetwork.UserDataTransfer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * A placeholder fragment containing a simple view.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final float DEFAULT_ZOOM = 16;

    private PageViewModel pageViewModel;
    private ImageButton btnFollow;
    private ImageButton btnFlag;
    private boolean isFollowPressed;
    private boolean isFlagPressed;
    private MyData mMyData;
    private ConnectionService mConnectionService;
    private Map<String, Marker> userMarkers; // USED FOR ALL PLAYER MARKERS by player ID

    private GoogleMap mMap;
    private UiSettings mUiSettings;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location lastKnownLocation;
    private boolean isLocationKnown;
    private Circle circle;
    private Polyline polyline;
    private Marker flag;
    private UserData followingUser;

    public MapFragment() {
        // Required empty public constructor
    }
    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
        mMyData = MyData.getInstance();
        mConnectionService = ConnectionService.getInstance();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        try {
            View root = inflater.inflate(R.layout.fragment_map, container, false);


            isLocationKnown = false;
            isFlagPressed = false;
            isFollowPressed = false;
            btnFlag = root.findViewById(R.id.btnFlag);
            btnFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isFlagPressed) {

                        btnFlag.setBackgroundColor(Color.WHITE);
                        isFlagPressed = false;
                    } else {
                        btnFlag.setBackgroundColor(Color.rgb(250, 244, 195));
                        Toast.makeText(getContext(), "Select flag location on map", Toast.LENGTH_LONG).show();
                        isFlagPressed = true;
                    }
                }
            });
            btnFollow = root.findViewById(R.id.btnFollow);
            btnFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isLocationKnown)
                    {
                        if (isFollowPressed) {
                            btnFollow.setBackgroundColor(Color.WHITE);
                            isFollowPressed = false;
                        } else {
                            if (mMyData.getFollowing() == null) {
                                btnFollow.setBackgroundColor(Color.rgb(250, 244, 195));
                                Toast.makeText(getContext(), "Select user to follow", Toast.LENGTH_LONG).show();
                                isFollowPressed = true;
                            } else {
                                followingUser = null;
                                mMyData.setFollowing(null);

                                if (polyline != null)
                                    if (polyline.isVisible())
                                        polyline.setVisible(false);
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(getContext(), "Your location is not known", Toast.LENGTH_LONG).show();
                    }

                }
            });
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }

            return root;
        } catch (Exception e){
            System.out.print(e);}
            return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(mMap == null)
            return;
        if (Build.MANUFACTURER.equals("LENOVO"))
            mMap.setMaxZoomPreference(16);

        mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);

        LatLng nis = new LatLng(43.34224905, 21.896666);
        if (circle == null)
        {
            circle = mMap.addCircle(new CircleOptions()
                        .center(nis)
                        .radius(100)
                        .visible(false));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nis,DEFAULT_ZOOM));
        mMap.setOnMapClickListener(this);// WHEN FLAG BUTTON PRESSED
        mMap.setOnMarkerClickListener(this);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
                getDeviceLocation();
                mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(getContext(), "Permission not given!", Toast.LENGTH_LONG).show();
        }
        if (userMarkers != null)
            addInitialUsers();
    }
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                LatLng myLoc= new LatLng( lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                circle.setCenter(myLoc);
                                if (!isLocationKnown)
                                {
                                    circle.setVisible(true);
                                    isLocationKnown = true;
                                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                                }
                                mMyData.setMyLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        myLoc, DEFAULT_ZOOM));
                            }
                        } else {
                            if (isLocationKnown)
                            {
                                isLocationKnown = false;
                                circle.setVisible(false);
                            }
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
        } catch (SecurityException e)  {

        }
    }
    public void addInitialUsers()
    {
        userMarkers= new HashMap<>();
        for (UserData user: mMyData.getUsers()) {
            LatLng userLoc = user.getLatLng();
            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(userLoc)
                            .title(user.ShowName)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_person_black_18)));
            userMarkers.put(user.Name, marker);//endpointid
        }
    }
    public void AddUserMarker(String name,String showName)
    {
        LatLng userLoc = new LatLng(mMyData.getLatitude(), mMyData.getLongitude());//spawnujem ga kod mene jer mozda nema lokaciju
        if(mMap != null)
        {
            if(userMarkers == null)
                userMarkers= new HashMap<>();

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(userLoc)
                    .title(showName)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.baseline_person_black_18)));
            userMarkers.put(name, marker);//endpointid
        }
    }
    public void DeleteUserMarker(String fullName)//String endpointID
    {
        if (userMarkers != null)
            Objects.requireNonNull(userMarkers.remove(fullName)).setVisible(false);
    }
    public void refreshMarkerForUser(/*String endpoint,*/ UserDataTransfer data)
    {
        if (userMarkers == null) {
            AddUserMarker(data.Name, data.Name.split("=")[0]);
        }
        else if (userMarkers.get(data.Name) == null){
            AddUserMarker(data.Name, data.Name.split("=")[0]);
        }

        Objects.requireNonNull(userMarkers.get(data.Name)).setPosition(new LatLng(data.Lat,data.Lon));

        if (followingUser != null)
        {
            if (followingUser.Name.equals(data.Name))//endpointid
            {
                followingUser.SetUserData(data);
                refreshPolyline(followingUser.getLatLng());

            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (isFlagPressed)
        {
            placeMyMarker(latLng,mMyData.getShowName());
            isFlagPressed = false;
            btnFlag.setBackgroundColor(Color.WHITE);
            mMyData.setMyTargetFlag(true, latLng.latitude, latLng.longitude);

            String limitedName = mMyData.getShowName();
            if (mMyData.getShowName().length() > 10) limitedName = mMyData.getShowName().substring(0,9);

            String flagMessage = "flag="+ limitedName +"=" + latLng.latitude +"="+ latLng.longitude;
            Payload flagPayload = Payload.fromBytes(flagMessage.getBytes(StandardCharsets.UTF_8));
            mConnectionService.send(flagPayload);

        }
    }
    public void placeMyMarker(LatLng pos,String userName)
    {
        if (flag == null)
        {
            flag = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Flag")
                    .snippet(userName)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.beachflag)));
        }
        else
        {
            flag.setPosition(pos);
            flag.setSnippet(userName);
        }
    }
    @Override
    public boolean onMarkerClick(Marker marker) {
        if(isFollowPressed)
        {
            if (isLocationKnown)
            placePolyline(marker);
            isFollowPressed = false;
            btnFollow.setBackgroundColor(Color.WHITE);
        }
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }
    public void placePolyline(Marker marker)
    {
        boolean isUser = false;
        //NEED TO CHECK IF IS IT USER OR FLAG
        for (UserData user:mMyData.getUsers()) {
            if (user.Name.contains(marker.getTitle())) {
                isUser = true;
                followingUser = user;
            }

        }
        if (isUser)
        {
            mMyData.setFollowing(followingUser);
            refreshPolyline(marker.getPosition());

        }
        else
        {
            Toast.makeText(getContext(), "You must select a user. Canceled", Toast.LENGTH_LONG).show();
        }
    }
    public void refreshPolyline(LatLng followed)
    {
        LatLng myLoc = new LatLng(lastKnownLocation.getLatitude(),lastKnownLocation.getLongitude());
        ArrayList<LatLng> points = new ArrayList<LatLng>();
        points.add(myLoc);
        points.add(followed);
        if (polyline == null) {
            polyline = mMap.addPolyline(new PolylineOptions().addAll(points).clickable(false));
        }
        else {
            if (!polyline.isVisible())
                polyline.setVisible(true);
            polyline.setPoints(points);
        }
    }

}
