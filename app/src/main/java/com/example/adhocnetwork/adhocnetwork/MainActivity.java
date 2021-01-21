package com.example.adhocnetwork.adhocnetwork;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;

import com.example.adhocnetwork.adhocnetwork.ui.main.CreateList;
import com.example.adhocnetwork.adhocnetwork.ui.main.MapFragment;
import com.example.adhocnetwork.adhocnetwork.ui.main.UsersFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.material.tabs.TabLayout;

import androidx.collection.SimpleArrayMap;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.viewpager.widget.ViewPager;

import android.os.ParcelFileDescriptor;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;

import com.example.adhocnetwork.adhocnetwork.ui.main.SectionsPagerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
/*import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.LocationDataSource;
import com.here.android.mpa.common.LocationDataSourceGoogleServices;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.MapCircle;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;*/

public class MainActivity extends FragmentActivity implements SensorEventListener {

    private double startingLatitude = 43.34224905;
    private double startingLongitude = 21.896666;//start in nis
    private MapFragment mapFragment;
    private UsersFragment usersFragment;

    private ConnectionService mConnectionService;
    private MyData mMyData;
    private Context context;

    private SensorManager sensorManager;
    private Sensor mLight;
    private Sensor mTemperature;
    private Sensor mPressure;

    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();

    private final SimpleArrayMap<Long, NotificationCompat.Builder> incomingPayloads =
            new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, NotificationCompat.Builder> outgoingPayloads =
            new SimpleArrayMap<>();
    NotificationManagerCompat notificationManager;

    private MediaPlayer mediaPlayer;



    public void onConnectionResultAlt(String endpointId, ConnectionResolution result) {
        //Toast.makeText(getApplicationContext(), "ConRes| " + result.getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
        if (result.getStatus().isSuccess())
        {
            for (ConnectionService.Endpoint e : mConnectionService.getConnectedEndpoints()) {
                if (e.getId().equals(endpointId)){
                   InsertUser(e.getName(),e.getId());
                }
            }
        }
    }
    public void onDisconnectedAlt(String endpointId) {
        Toast.makeText(getApplicationContext(), "Disconnected from endpoint! Left:"+mConnectionService.getConnectedEndpoints().size(), Toast.LENGTH_LONG).show();
        if(mConnectionService.getConnectedEndpoints().size() == 0)
            finish();
        else
        {
            String endpointName = "";
            for (int i = 0; i< mMyData.getUsers().size();i++)
            {
                if (mMyData.getUsers().get(i).EndpointId.equals(endpointId))
                    endpointName = mMyData.getUsers().get(i).Name;
            }

            int remIndex = mMyData.removeUser(endpointName);
            if (mapFragment != null)
                mapFragment.DeleteUserMarker(endpointName);

            if (usersFragment != null)
                usersFragment.removeSingleItem(remIndex);

            String removalMessage = "cmd_remove=" + endpointName;
            Payload removalPayload = Payload.fromBytes(removalMessage.getBytes(StandardCharsets.UTF_8));
            mConnectionService.send(removalPayload);
        }
    }
    public void InsertUser(String name,String eId)
    {
        UserData ud = new UserData(name, eId, startingLatitude, startingLongitude);
        mMyData.addUser(ud);
        if (mapFragment == null)
            mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapFragment);
        if (mapFragment != null)
            mapFragment.AddUserMarker(ud.Name, ud.ShowName);

        if (usersFragment == null)
            usersFragment = (UsersFragment) getSupportFragmentManager().findFragmentById(R.id.myUsersFragment);
        if (usersFragment != null)
            usersFragment.insertSingleItem(ud);
    }
    public void RemoveUser(String fullname)
    {
        if (!mMyData.containsUserByName(fullname))
            return;

        int remIndex = mMyData.removeUser(fullname);
        if (mapFragment != null)
            mapFragment.DeleteUserMarker(fullname);

        if (usersFragment != null)
            usersFragment.removeSingleItem(remIndex);
    }

    public void onPayloadReceivedAlt(String endpointId, Payload payload) {
        //onReceive(mEstablishedConnections.get(endpointId), payload);
        if (payload.getType() == Payload.Type.BYTES) {
            if (payload.asBytes().length < 70) {
                String payloadString = new String(payload.asBytes(), StandardCharsets.UTF_8);
                if (payloadString.startsWith("cmd_remove=") )
                {
                    String userNameForRemove =payloadString.split("=",2)[1];
                    RemoveUser(userNameForRemove);
                }
                else if (payloadString.startsWith("cmd_settings="))
                {
                     String[] params =  payloadString.split("=");
                     int heartrate_time = Integer.parseInt(params[1]);
                     int heartrate_value = Integer.parseInt(params[2]);
                     int pressure_time = Integer.parseInt(params[3]);
                     int pressure_value = Integer.parseInt(params[4]);
                     mMyData.updateFakeSensorSettings(heartrate_time,heartrate_value,pressure_time,pressure_value);
                }
                else if (payloadString.startsWith("flag="))
                {
                    String[] params =  payloadString.split("=");
                    Double flagLat = Double.parseDouble(params[2]);
                    Double flagLon = Double.parseDouble(params[3]);
                    mMyData.setMyTargetFlag(false,flagLon,flagLat);
                    if (mapFragment != null)
                        mapFragment.placeMyMarker(new LatLng((double)flagLat,(double)flagLon),params[1]);
                    Toast.makeText(getApplicationContext(), "Map flag has been placed", Toast.LENGTH_LONG).show();
                }
                else {
                    long payloadId = addPayloadFilename(payloadString);
                    processFilePayload(payloadId);
                    System.out.println(payloadString);
                    Toast.makeText(getApplicationContext(), "Filename:"+payloadString, Toast.LENGTH_LONG).show();
                }
            }
            else
            {
                try {
                    UserDataTransfer udt = (UserDataTransfer)MyData.deserialize( payload.asBytes() );
                    String userNameToShow = udt.Name.split("=")[0];
                    if (mConnectionService.Role.equals("Client"))
                    {
                        if (!mMyData.containsUserByName(udt.Name))
                        {
                            InsertUser(udt.Name,"");
                        }
                    }
                    int index = mMyData.refreshUser(/*endpointId,*/udt);
                    if (mapFragment != null)
                        mapFragment.refreshMarkerForUser(/*endpointId,*/udt);

                    if (usersFragment != null)
                        usersFragment.updateSingleItem(index);

                    if (udt.healthAlarm)
                    {
                        if (mediaPlayer.isPlaying())
                            mediaPlayer.stop();
                        mediaPlayer.start();
                        Toast.makeText(getApplicationContext(), userNameToShow +" might be in trouble!", Toast.LENGTH_LONG).show();
                        displayNotification(userNameToShow +" might be in trouble!","Check to see if he is alright",2);
                    }

                    System.out.println(mMyData.getUsers().size() + " " + udt.Name);
                    //Toast.makeText(getApplicationContext(), "Got his data location!", Toast.LENGTH_LONG).show();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } else if (payload.getType() == Payload.Type.FILE) {
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingFilePayloads.put(payload.getId(), payload);

            //progress
            // Build and start showing the notification.
            NotificationCompat.Builder notification = buildNotification(payload, true /*isIncoming*/);
            notificationManager.notify((int) payload.getId(), notification.build());

            // Add it to the tracking list so we can update it.
            incomingPayloads.put(payload.getId(), notification);

        }
    }

    public void onPayloadTransferUpdateAlt(String endpointId, PayloadTransferUpdate update) {
        if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
            long payloadId = update.getPayloadId();
            Payload payload = incomingFilePayloads.remove(payloadId);
            if (payload != null) {
                completedFilePayloads.put(payloadId, payload);
                if (payload.getType() == Payload.Type.FILE) {
                    processFilePayload(payloadId);
                }
            }
        }
        //progress
        long payloadId = update.getPayloadId();
        NotificationCompat.Builder notification = null;
        if (incomingPayloads.containsKey(payloadId)) {
            notification = incomingPayloads.get(payloadId);
            if (update.getStatus() != PayloadTransferUpdate.Status.IN_PROGRESS) {
                // This is the last update, so we no longer need to keep track of this notification.
                incomingPayloads.remove(payloadId);
            }
        } else if (outgoingPayloads.containsKey(payloadId)) {
            notification = outgoingPayloads.get(payloadId);
            if (update.getStatus() != PayloadTransferUpdate.Status.IN_PROGRESS) {
                // This is the last update, so we no longer need to keep track of this notification.
                outgoingPayloads.remove(payloadId);
            }
        }

        if (notification == null) {
            return;
        }

        switch (update.getStatus()) {
            case PayloadTransferUpdate.Status.IN_PROGRESS:
                long size = update.getTotalBytes();
                if (size == -1) {
                    // This is a stream payload, so we don't need to update anything at this point.
                    return;
                }
                int percentTransferred =
                        (int) (100.0 * (update.getBytesTransferred() / (double) update.getTotalBytes()));
                notification.setProgress(100, percentTransferred, /* indeterminate= */ false);
                break;
            case PayloadTransferUpdate.Status.SUCCESS:
                // SUCCESS always means that we transferred 100%.
                notification
                        .setProgress(100, 100, /* indeterminate= */ false)
                        .setContentText("Transfer complete!");
                break;
            case PayloadTransferUpdate.Status.FAILURE:
            case PayloadTransferUpdate.Status.CANCELED:
                notification.setProgress(0, 0, false).setContentText("Transfer failed");
                break;
            default:
                // Unknown status.
        }
        notificationManager.notify((int) payloadId, notification.build());

}

    private long addPayloadFilename(String payloadFilenameMessage) {
        String[] parts = payloadFilenameMessage.split(":");
        long payloadId = Long.parseLong(parts[0]);
        String filename = parts[1];
        filePayloadFilenames.put(payloadId, filename);
        return payloadId;
    }
    private void processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        String filename = filePayloadFilenames.get(payloadId);
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId);
            filePayloadFilenames.remove(payloadId);
            // Get the received file (which will be in the Downloads folder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                ParcelFileDescriptor p = filePayload.asFile().asParcelFileDescriptor();
                //Uri uri = filePayload.asFile().asUri();
                try {
                    // Copy the file to a new location.
                    //InputStream in = context.getContentResolver().openInputStream(uri);
                    InputStream in = new FileInputStream(p.getFileDescriptor());//
                    File f = new File(context.getCacheDir(), filename);
                    copyStream(in, new FileOutputStream( f ));

                    if (filename.contains(".png") || filename.contains(".jpg"))
                    {
                        CreateList cl = new CreateList();
                        cl.setImage_ID(BitmapFactory.decodeFile(f.getAbsolutePath()));
                        cl.setImage_title(filename);
                        mMyData.addImage(cl);
                    }
                    displayNotification("You have received a file","All downloaded files can be found in downloads folder.",1);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                File payloadFile = filePayload.asFile().asJavaFile();
                // Rename the file.
                if (payloadFile != null) {
                    System.out.println(payloadFile.getAbsolutePath());

                    File f = new File(payloadFile.getParentFile(), filename);
                    payloadFile.renameTo(f);

                    if (filename.contains(".png") || filename.contains(".jpg"))
                    {
                        CreateList cl = new CreateList();
                        cl.setImage_ID(BitmapFactory.decodeFile(f.getAbsolutePath()));
                        cl.setImage_title(filename);
                        mMyData.addImage(cl);
                    }
                    displayNotification("You have received a file","All downloaded files can be found in downloads folder.",1);
                    Toast.makeText(getApplicationContext(), "Downloaded file: "+filename +"!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            in.close();
            out.close();
        }
    }
    private NotificationCompat.Builder buildNotification(Payload payload, boolean isIncoming) {
        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context,"adhocnetwork_id_1")//"M_CH_ID_7890"
                        .setContentTitle(isIncoming ? "Receiving..." : "Sending...").setSmallIcon(R.drawable.ic_launcher_background);
        boolean indeterminate = false;
        if (payload.getType() == Payload.Type.STREAM) {
            // We can only show indeterminate progress for stream payloads.
            indeterminate = true;
        }
        notification.setProgress(100, 0, indeterminate);
        return notification;
    }
    public void displayNotification(String title,String text, int id)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "adhocnetwork_id_1")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(id, builder.build());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();*/
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        context = this;
        mConnectionService = ConnectionService.getInstance();

        MainActivity mActivity = this;
        mConnectionService.changeMainActivity(mActivity);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        sectionsPagerAdapter.startUpdate(viewPager);
        mapFragment = (MapFragment) sectionsPagerAdapter.instantiateItem(viewPager, 0);
        usersFragment = (UsersFragment) sectionsPagerAdapter.instantiateItem(viewPager, 1);
        sectionsPagerAdapter.finishUpdate(viewPager);

        mMyData = MyData.getInstance();
        mMyData.setMyInitialValues(mConnectionService.getName(),startingLatitude,startingLongitude);
        for (ConnectionService.Endpoint tab : mConnectionService.getConnectedEndpoints()) {
            UserData ud = new UserData(tab.getName(), tab.getId(), startingLatitude, startingLongitude);
            mMyData.addUser(ud);
        }
        //notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager = NotificationManagerCompat.from(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        checkMySensors();

        Intent intent = new Intent(this, SharingService.class);
        startService(intent);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
        //initialize();
    }

    private void checkMySensors() {
        mLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (mLight != null){
            mMyData.setHasLightSensor(true);
        } else {
            mMyData.setHasLightSensor(false);
        }
        mTemperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (mTemperature != null){
            mMyData.setHasTemperatureSensor(true);
        } else {
            mMyData.setHasTemperatureSensor(false);
        }
        mPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mPressure != null){
            mMyData.setHasPressureSensor(true);
        } else {
            mMyData.setHasPressureSensor(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, SharingService.class);
        stopService(intent);
        mConnectionService.changeMainActivity(null);
        mConnectionService.stopAllEndpoints();
        mMyData.clearUsers();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mMyData.getHasLightSensor())
            sensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        if (mMyData.getHasTemperatureSensor())
            sensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        if (mMyData.getHasPressureSensor())
            sensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mMyData.setBatteryLevel(getBatteryPercentage());
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mMyData.getHasLightSensor() || mMyData.getHasTemperatureSensor() || mMyData.getHasPressureSensor())
        sensorManager.unregisterListener(this);
    }
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT)

        switch (event.sensor.getType())
        {
            case Sensor.TYPE_LIGHT:
                float lux = event.values[0];
                mMyData.setLight(lux);

                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                float temperatureC = event.values[0];
                mMyData.setTemperature(temperatureC);
                break;
            case Sensor.TYPE_PRESSURE:
                float milibars = event.values[0];
                mMyData.setPressure(milibars);
                break;
            default:
                break;
        }
        if (usersFragment != null)
            usersFragment.updateMySensors();

    }

    public int getBatteryPercentage() {
        BatteryManager bm = (BatteryManager) this.getApplicationContext().getSystemService(BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "AdHoc";
            String description = "Adhoc network notifications for health alarm and downloaded files";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("adhocnetwork_id_1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}





/** Callbacks for connections to other devices. */
    /*private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    mConnectionService.onConnectionInitiatedAlt(endpointId, connectionInfo);
                }
                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    mConnectionService.onConnectionResultAlt(endpointId, result);
                    Toast.makeText(getApplicationContext(), "ConRes| " + result.getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
                    //NEED TO ADD NEW USER HERE
                    if (result.getStatus().isSuccess())
                    {
                        for (ConnectionService.Endpoint e : mConnectionService.getConnectedEndpoints()) {
                            if (e.getId().equals(endpointId)){
                                UserData ud = new UserData(e.getName(), e.getId(), startingLatitude, startingLongitude, null, null);
                                mMyData.addUser(ud);
                                if (mapFragment == null)
                                mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapFragment);
                                if (mapFragment != null)
                                    mapFragment.AddUserMarker(ud);

                                if (usersFragment == null)
                                    usersFragment = (UsersFragment) getSupportFragmentManager().findFragmentById(R.id.myUsersFragment);
                                if (usersFragment != null)
                                    usersFragment.insertSingleItem(ud);
                            }
                        }
                    }
                }
                @Override
                public void onDisconnected(String endpointId) {
                    mConnectionService.onDisconnectedAlt(endpointId);
                    Toast.makeText(getApplicationContext(), "Disconnected from endpoint! Left:"+mConnectionService.getConnectedEndpoints().size(), Toast.LENGTH_LONG).show();
                    if(mConnectionService.getConnectedEndpoints().size() == 0)
                        finish();//MainActivity.this.finish();//this is useless?
                    else
                    {
                        int remIndex = mMyData.removeUser(endpointId);
                        if (mapFragment == null)
                            mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapFragment);
                        if (mapFragment != null)
                            mapFragment.DeleteUserMarker(endpointId);

                        if (usersFragment == null)
                            usersFragment = (UsersFragment) getSupportFragmentManager().findFragmentById(R.id.myUsersFragment);
                        if (usersFragment != null)
                            usersFragment.removeSingleItem(remIndex);
                    }
                }
            };*/

/** Callbacks for payloads (bytes of data) (files) sent from another device to us. */
    /*private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    //onReceive(mEstablishedConnections.get(endpointId), payload);
                    //Toast.makeText(getApplicationContext(), "im getting payload", Toast.LENGTH_LONG).show();
                    if (payload.getType() == Payload.Type.BYTES) {//MORA SE PROVERITI KOJA VRSTA JE:FILENAME OR LOCATION_SHARE
                        if (payload.asBytes().length < 45) {
                            String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);
                            long payloadId = addPayloadFilename(payloadFilenameMessage);
                            processFilePayload(payloadId); //COMMENTED FOR TESTING;   ENEBLE IT
                            Toast.makeText(getApplicationContext(), "Didnt expect this bytes!", Toast.LENGTH_LONG).show();

                        }
                        else
                        {
                            try {
                                UserDataTransfer udt = (UserDataTransfer)MyData.deserialize( payload.asBytes() );
                                int index = mMyData.refreshUser(endpointId,udt);
                                if (mapFragment == null)
                                mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapFragment);
                                if (mapFragment != null)
                                mapFragment.refreshMarkerForUser(endpointId,udt);

                                if (usersFragment == null)
                                    usersFragment = (UsersFragment) getSupportFragmentManager().findFragmentById(R.id.myUsersFragment);
                                if (usersFragment != null)
                                    usersFragment.updateSingleItem(index);

                                Toast.makeText(getApplicationContext(), "Got his data location!", Toast.LENGTH_LONG).show();
                            } catch (IOException | ClassNotFoundException e) {//ILI MOZDA PROVERITI DAL JE OVAJ EXCEPTION
                                e.printStackTrace();
                            }
                        }

                    } else if (payload.getType() == Payload.Type.FILE) {
                        // Add this to our tracking map, so that we can retrieve the payload later.
                        incomingFilePayloads.put(payload.getId(), payload);
                    }
                }
                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        long payloadId = update.getPayloadId();
                        Payload payload = incomingFilePayloads.remove(payloadId);
                        completedFilePayloads.put(payloadId, payload);
                        if (payload.getType() == Payload.Type.FILE) {
                            processFilePayload(payloadId);
                        }
                    }
                }

            };*/

/*
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {

            if (mConnectionService.getServiceId().equals(info.getServiceId())) {
                Toast.makeText(getApplicationContext(), endpointId + "| "+ info.getEndpointName(), Toast.LENGTH_LONG).show();
                mConnectionService.onEndpointFoundAlt(endpointId, info);
            }
        }

        @Override
        public void onEndpointLost(String endpointId) {
            Toast.makeText(getApplicationContext(), "Endpoint lost!", Toast.LENGTH_LONG).show();
        }
    };*/