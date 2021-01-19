package com.example.adhocnetwork.adhocnetwork;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ConnectionService {
    private static final ConnectionService ourInstance = new ConnectionService();

    public static ConnectionService getInstance()
    {
        return ourInstance;
    }

    private final Strategy strategy = Strategy.P2P_STAR;
    private String name="User";
    private static final String SERVICE_ID = "com.example.adhocnetwork.adhocnetwork";
    private State mState = State.UNKNOWN;
    private MainActivity mainActivity;
    public String Role;

    private ConnectionService() {
        start();
    }

    private ConnectionLifecycleCallback mConnectionLifecycleCallback;
    private EndpointDiscoveryCallback mEndpointDiscoveryCallback;


    public void setConnectionLifecycleCallback(ConnectionLifecycleCallback clc) {
        mConnectionLifecycleCallback = clc;
    }
    public void setEndpointDiscoveryCallback(EndpointDiscoveryCallback edc) {
        mEndpointDiscoveryCallback = edc;
    }
    /** Our handler to Nearby Connections. */
    private ConnectionsClient mConnectionsClient;

    /** The devices we've discovered near us. */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * #acceptConnection(Endpoint)} or {@link #rejectConnection(Endpoint)}.
     */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     */
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private boolean mIsConnecting = false;

    /** True if we are discovering. */
    private boolean mIsDiscovering = false;

    /** True if we are advertising. */
    private boolean mIsAdvertising = false;

    /** Callbacks for connections to other devices. */

    public void onConnectionInitiatedAlt(String endpointId, ConnectionInfo connectionInfo) {

        Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
        mPendingConnections.put(endpointId, endpoint);
        onConnectionInitiated(endpoint, connectionInfo);
    }
    public void onConnectionResultAlt(String endpointId, ConnectionResolution result) {
        // We're no longer connecting
        mIsConnecting = false;

        if (!result.getStatus().isSuccess()) {

            onConnectionFailed(mPendingConnections.remove(endpointId));
            return;
        }
        connectedToEndpoint(mPendingConnections.remove(endpointId));
        if (mainActivity != null)
            mainActivity.onConnectionResultAlt(endpointId,result);
    }
    public void onDisconnectedAlt(String endpointId) {
        if (!mEstablishedConnections.containsKey(endpointId)) {

            return;
        }
        disconnectedFromEndpoint(Objects.requireNonNull(mEstablishedConnections.get(endpointId)));
        if (mainActivity != null)
            mainActivity.onDisconnectedAlt(endpointId);
    }
    /** Callbacks for payloads (bytes of data) sent from another device to us. */
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    onReceive(mEstablishedConnections.get(endpointId), payload);
                    if (mainActivity != null)
                        mainActivity.onPayloadReceivedAlt(endpointId,payload);
                    if (Role.equals("Host") && mEstablishedConnections.size() > 1)
                        sendToOthers(payload, endpointId);

                }
                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (mainActivity != null)
                        mainActivity.onPayloadTransferUpdateAlt(endpointId,update);
                }
            };

    public void start() {
        mConnectionsClient = Nearby.getConnectionsClient( StartActivity.get() );
    }
    public void stop(){
        setState(State.UNKNOWN);
    }

    protected void startAdvertising() {
        mIsAdvertising = true;
        final String localEndpointName = getName();

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(getStrategy());

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        getServiceId(),
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                onAdvertisingStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                onAdvertisingFailed();
                            }
                        });
    }

    /** Stops advertising. */
    protected void stopAdvertising() {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    /** Returns {@code true} if currently advertising. */
    protected boolean isAdvertising() {
        return mIsAdvertising;
    }

    /** Called when advertising successfully starts. Override this method to act on the event. */
    protected void onAdvertisingStarted() {}

    /** Called when advertising fails to start. Override this method to act on the event. */
    protected void onAdvertisingFailed() {}

    /**
     * Called when a pending connection with a remote endpoint is created. Use {@link ConnectionInfo}
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call {@link #acceptConnection(Endpoint)}. Otherwise,
     * call {@link #rejectConnection(Endpoint)}.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        acceptConnection(endpoint);
    }

    /** Accepts a connection request. */
    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                            }
                        });
    }

    /** Rejects a connection request. */
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                            }
                        });
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
     * out if we successfully entered this mode.
     */
    protected void startDiscovering() {
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(getStrategy());
        mConnectionsClient
                .startDiscovery(
                        getServiceId(), mEndpointDiscoveryCallback,
                        discoveryOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                onDiscoveryStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                onDiscoveryFailed();
                            }
                        });
    }
    public void onEndpointFoundAlt(String endpointId, DiscoveredEndpointInfo info) {
        if (getServiceId().equals(info.getServiceId())) {
            Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
            mDiscoveredEndpoints.put(endpointId, endpoint);
            onEndpointDiscovered(endpoint);
        }
    }

    /** Stops discovery. */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }

    /** Returns {@code true} if currently discovering. */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /** Called when discovery successfully starts. Override this method to act on the event. */
    protected void onDiscoveryStarted() {}

    /** Called when discovery fails to start. Override this method to act on the event. */
    protected void onDiscoveryFailed() {}

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call {@link
     * #connectToEndpoint(Endpoint)}.
     */
    protected void onEndpointDiscovered(Endpoint endpoint) {
        connectToEndpoint(endpoint);
    }

    /** Disconnects from the given endpoint. */
    protected void disconnect(Endpoint endpoint) {
        mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    /** Disconnects from all currently connected endpoints. */
    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        }
        mEstablishedConnections.clear();
    }

    /** Resets and clears all state in Nearby Connections. */
    protected void stopAllEndpoints() {
        mConnectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    /**
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true;

        // Ask to connect
        mConnectionsClient
                .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsConnecting = false;
                                onConnectionFailed(endpoint);
                            }
                        });
    }

    /** Returns {@code true} if we're currently attempting to connect to another device. */
    protected final boolean isConnecting() {
        return mIsConnecting;
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected void onConnectionFailed(Endpoint endpoint) {}

    /** Called when someone has connected to us. Override this method to act on the event. */
    protected void onEndpointConnected(Endpoint endpoint) {}

    /** Called when someone has disconnected. Override this method to act on the event. */
    protected void onEndpointDisconnected(Endpoint endpoint) {}

    /** Returns a list of currently connected endpoints. */
    public Set<Endpoint> getDiscoveredEndpoints() {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    /** Returns a list of currently connected endpoints. */
    public Set<Endpoint> getConnectedEndpoints() {
        return new HashSet<>(mEstablishedConnections.values());
    }

    public void changeMainActivity(MainActivity main)
    {
        mainActivity = main;
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    public void send(Payload payload) {
        send(payload, mEstablishedConnections.keySet());
    }

    private void send(Payload payload, Set<String> endpoints) {
        mConnectionsClient
                .sendPayload(new ArrayList<>(endpoints), payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                            }
                        });
    }
    private void sendToOthers(Payload payload, String exceptEndpoint) {
        ArrayList<String> otherEndpoints = new ArrayList<>(mEstablishedConnections.keySet());
        otherEndpoints.remove(exceptEndpoint);
        mConnectionsClient
                .sendPayload(otherEndpoints, payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                            }
                        });
    }

    /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected void onReceive(Endpoint endpoint, Payload payload) {}

    /**
     * Returns the strategy we use to connect to other devices. Only devices using the same strategy
     * and service id will appear when discovering. Stragies determine how many incoming and outgoing
     * connections are possible at the same time, as well as how much bandwidth is available for use.
     */
    public void setName(String n)
    {
        name = n;
    }
    public String getName(){return name;}
    public String getServiceId(){return SERVICE_ID;}
    protected Strategy getStrategy(){return strategy;}

    public void setStateAdvertising()
    {
        setState(State.ADVERTISING);
    }
    public void setStateDiscovering()
    {
        setState(State.DISCOVERING);
    }

    private void setState(State state) {
        if (mState == state) {
            return;
        }
        mState = state;
        onStateChanged(state);
    }

    /** @return The current state. */
    private State getState() {
        return mState;
    }
    /**
     * State has changed.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private void onStateChanged(State newState) {

        // Update Nearby Connections to the new state.
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                disconnectFromAllEndpoints();
                startDiscovering();
                break;
            case ADVERTISING:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints();
                startAdvertising();
                break;
            case CONNECTED:
                if (isDiscovering()) {
                    stopDiscovering();
                } else if (isAdvertising()) {
                    // Continue to advertise, so others can still connect,
                    // but clear the discover runnable.
                    ////removeCallbacks(mDiscoverRunnable);
                }
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            default:
                // no-op
                break;
        }
    }
    public enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED
    }

    public static class Endpoint {
        @NonNull private final String id;
        @NonNull
        private final String name;

        public Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }


}

