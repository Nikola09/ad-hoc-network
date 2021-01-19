package com.example.adhocnetwork.adhocnetwork;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import java.util.Random;

/** A class that connects to Nearby Connections and provides convenience methods and callbacks. */
public class StartActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ConnectionService mConnectionService;

    private static Context context;
    public static Context get() {return context;}
    private Context contxt;
    boolean inThisActivity;

    /**
     * These permissions are required before connecting to Nearby Connections. Only {@link
     * Manifest.permission#ACCESS_COARSE_LOCATION} is considered dangerous, so the others should be
     * granted just by having them in our AndroidManfiest.xml
     */
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private EditText editText;
    private Button button;
    private Spinner spinner;
    private static final String[] paths = {"Client", "Host"};
    private String mRole = "Client";
    private Boolean isSearching;

    /** Callbacks for connections to other devices. */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    mConnectionService.onConnectionInitiatedAlt(endpointId, connectionInfo);
                }
                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    mConnectionService.onConnectionResultAlt(endpointId, result);
                    Toast.makeText(context, "Connection | " + result.getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
                    if (result.getStatus().isSuccess() && inThisActivity){
                        button.setText(R.string.connect);
                        inThisActivity = false;
                        Intent intent = new Intent(contxt, MainActivity.class);
                        startActivity(intent);
                    }
                }
                @Override
                public void onDisconnected(String endpointId) {
                    mConnectionService.onDisconnectedAlt(endpointId);
                    //Toast.makeText(context, "Disconnected from endpoint!", Toast.LENGTH_LONG).show();
                }
            };

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {

            if (mConnectionService.getServiceId().equals(info.getServiceId())) {
                //Toast.makeText(context, endpointId + " | "+ info.getEndpointName(), Toast.LENGTH_LONG).show();
                mConnectionService.onEndpointFoundAlt(endpointId, info);
            }
        }

        @Override
        public void onEndpointLost(String endpointId) {
            //Toast.makeText(context, "Endpoint lost!", Toast.LENGTH_LONG).show();
        }
    };

    /** Called when our Activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        context = getApplicationContext();
        contxt = getApplicationContext();
        isSearching = false;
        mConnectionService = ConnectionService.getInstance();
        mConnectionService.setConnectionLifecycleCallback(mConnectionLifecycleCallback);
        mConnectionService.setEndpointDiscoveryCallback(mEndpointDiscoveryCallback);

        editText = (EditText)findViewById(R.id.txtName);
        button = (Button)findViewById(R.id.btnConnect);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasPermissions(view.getContext(), getRequiredPermissions())) {
                    if (!isSearching) {
                        Random r = new Random();
                        int randomId =  r.nextInt(9999);
                        //DEPENDING ON ROLE DO ADVERTISE OR DISCOVER
                        if (editText.getText().length() < 2) { //if not entered or only 1 letter
                            mConnectionService.setName("User" + "=" + randomId);
                        } else {
                            mConnectionService.setName(editText.getText().toString() + "=" + randomId);
                        }
                        mConnectionService.Role = mRole;
                        if (mRole.equals("Client"))
                        {
                            mConnectionService.setStateDiscovering();
                        }
                        else
                        {
                            mConnectionService.setStateAdvertising();
                        }
                        button.setText(R.string.searching);
                    }
                    else
                    {
                        mConnectionService.stop();
                        button.setText(R.string.connect);
                    }
                    isSearching = !isSearching;
                }
                else
                    Toast.makeText(view.getContext(), "Application doesn't have permissions", Toast.LENGTH_LONG).show();

                //FOR TESTING ONLY
//                mConnectionService.setName("User");
//                mConnectionService.Role = "Host";
//                Intent intent = new Intent(view.getContext(), MainActivity.class);
//                startActivity(intent);
            }

        });
        spinner = (Spinner)findViewById(R.id.spinner_role);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(StartActivity.this,
                android.R.layout.simple_spinner_item,paths);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

        switch (position) {
            case 0:
                mRole = "Client";

                break;
            case 1:
                mRole = "Host";

                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //KEEP ROLE CLIENT
    }

    @Override
    protected void onResume() {
        super.onResume();
        inThisActivity = true;
    }


    /** Called when our Activity has been made visible to the user. */
    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, getRequiredPermissions())) {
            if (!hasPermissions(this, getRequiredPermissions())) {
                if (Build.VERSION.SDK_INT < 23) {
                    ActivityCompat.requestPermissions(
                            this, getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                } else {
                    requestPermissions(getRequiredPermissions(), REQUEST_CODE_REQUIRED_PERMISSIONS);
                }
            }
        }
    }
    /** Called when the user has accepted (or denied) our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            recreate();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * An optional hook to pool any permissions the app needs with the permissions ConnectionsActivity
     * will request.
     *
     * @return All permissions required for the app to properly function.
     */
    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    /**
     * Returns the service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to them.
     */
    //protected abstract String getServiceId();

    /**
     * Returns {@code true} if the app was granted all the permissions. Otherwise, returns {@code
     * false}.
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}