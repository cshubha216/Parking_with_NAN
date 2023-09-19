package net.mobilewebprint.nan;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Note: as it stands, to run, do the following:
 *
 * 1. On Pixel #1, run the NAN app, and click the PUBLISH button.
 * 2. On Pixel #2, run the NAN app, and click the SUBSCRIBE button.
 *   -- There is a slight delay. Wait until both have 2 MAC addresses.
 *
 * Unfortunately, requestNetwork does not get any callbacks.
 *
 */

public class ParkAssistActivity extends AppCompatActivity {

    private final int MAC_ADDRESS_MESSAGE = 55;
    private final int UPDATE_SLOT = 100;
    private static final int MY_PERMISSION_COARSE_LOCATION_REQUEST_CODE = 88;
    private final String THE_MAC = "THEMAC";

    private BroadcastReceiver broadcastReceiver;
    private WifiAwareManager wifiAwareManager;
    private ConnectivityManager connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private NetworkSpecifier networkSpecifier;
    private PublishDiscoverySession publishDiscoverySession;
    private SubscribeDiscoverySession subscribeDiscoverySession;
    private PeerHandle peerHandle;
    private byte[] myMac;
    private byte[] otherMac;

    private final int IP_ADDRESS_MESSAGE = 33;
    private final int MESSAGE = 7;
    private static final int MY_PERMISSION_EXTERNAL_REQUEST_CODE = 99;
    private Inet6Address ipv6;
    private ServerSocket serverSocket;
    private final byte[] serviceInfo = "android".getBytes();
    private byte[] portOnSystem;
    private int portToUse;
    private byte[] myIP;
    private byte[] otherIP;
    private byte[] msgtosend;
    private String actionData;
    private int bitString;

    /**
     * Handles initialization (creation) of the activity.
     *
     * @param savedInstanceState
     */
    @Override
    @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiAwareManager = null;
        wifiAwareSession = null;
        connectivityManager = null;
        networkSpecifier = null;
        publishDiscoverySession = null;
        subscribeDiscoverySession = null;
        peerHandle = null;
        final Bundle extras = getIntent().getExtras();
        actionData = extras.get("action").toString();

        wifiAwareManager = (WifiAwareManager) getSystemService(Context.WIFI_AWARE_SERVICE);
        if (wifiAwareManager != null) {
            attachToNanSession();
        }
        if (actionData.equals("manager")) {
            getSupportActionBar().setTitle("Parking Assist for Manager");
            setContentView(R.layout.activity_publish_nan);
            Button centerButton = findViewById(R.id.publishButton1);
            centerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        setContentView(R.layout.loader);
                        publishService();
                    } catch (Exception e) {
                    }
                }
            });

        } else {
            getSupportActionBar().setTitle("Parking Assist for User");
            setContentView(R.layout.loader);
        }
    }

    @TargetApi(26)
    private void publishService() {
        if (wifiAwareSession == null) {
            Toast.makeText(ParkAssistActivity.this, "wifiAwareSession is null", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        PublishConfig config = new PublishConfig.Builder().setServiceName(THE_MAC).setServiceSpecificInfo(serviceInfo).build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        wifiAwareSession.publish(config, new DiscoverySessionCallback() {
            @Override
            public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                setContentView(R.layout.activity_grid_view_users);
                setSlot(null);
                Toast.makeText(ParkAssistActivity.this, "Publish Nan Started", Toast.LENGTH_SHORT).show();
                super.onPublishStarted(session);
                publishDiscoverySession = session;
                if (publishDiscoverySession != null && peerHandle != null) {
                    publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, Constant.getSlotsDataAsBytes());
                }
            }

            @Override
            public void onMessageSendSucceeded(int messageId) {
                Toast.makeText(ParkAssistActivity.this, "Publisher Message sent success", Toast.LENGTH_LONG).show();
                super.onMessageSendSucceeded(messageId);
            }

            @Override
            public void onMessageSendFailed(int messageId) {
                super.onMessageSendFailed(messageId);
                Toast.makeText(ParkAssistActivity.this, "Publisher Message sent Failed", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle_, byte[] message) {
                String data = new String(message, StandardCharsets.UTF_8);

                super.onMessageReceived(peerHandle, message);
                peerHandle = peerHandle_;
                // Why checking getSlots here .... 
                if (data.equals("getSlots")) {
                    publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, Constant.getSlotsDataAsBytes());
                } else {
                    setSlot(message);
                    publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE,message);
                }
                if (message.length == 2) {
                    portToUse = byteToPortInt(message);
                    Log.d("received", "will use port number " + portToUse);
                } else if (message.length == 6) {
                    setOtherMacAddress(message);
                } else if (message.length == 16) {
                    setOtherIPAddress(message);
                }

                peerHandle = peerHandle_;

                if (publishDiscoverySession != null && peerHandle != null) {
                    publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, myMac);
                }
            }
        }, null);
        // --------------------------------------------------------------------------------------------
    }

    @TargetApi(26)
    private void subscribeToService() {
        if (wifiAwareSession == null) {
            Toast.makeText(ParkAssistActivity.this, "wifiAwareSession is null", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Log.d("nanSUBSCRIBE", "building subscribe session");
        SubscribeConfig config = new SubscribeConfig.Builder().setServiceName(THE_MAC).setServiceSpecificInfo(serviceInfo).build();
        Log.d("nanSUBSCRIBE", "build finish");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        wifiAwareSession.subscribe(config, new DiscoverySessionCallback() {
            @Override
            public void onServiceDiscovered(PeerHandle peerHandle_, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
                peerHandle = peerHandle_;
                if (peerHandle_ != null) {
                    setContentView(R.layout.parking_detection_layout);
                    setSlot(null);
                }
                if (subscribeDiscoverySession != null && peerHandle != null) {
                }
            }

            @Override
            public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                super.onSubscribeStarted(session);
                Toast.makeText(ParkAssistActivity.this, "onSubscribeStarted", Toast.LENGTH_LONG).show();
                subscribeDiscoverySession = session;

                if (subscribeDiscoverySession != null && peerHandle != null) {
                    final byte[] slotKey = "getSlots".getBytes();
                    subscribeDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, slotKey);
                    Log.d("nanSUBSCRIBE", "onServiceStarted send mac");
                }
            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                if (message.length == 2) {
                    portToUse = byteToPortInt(message);
                } else if (message.length == 6) {
                    setOtherMacAddress(message);
                } else if (message.length == 16) {
                    setOtherIPAddress(message);
                } else if (message.length > 16) {
                    setSlot(message);
                } else {
                    setSlot(message);
                }

            }

            @Override
            public void onMessageSendSucceeded(int messageId) {
                super.onMessageSendSucceeded(messageId);
                Toast.makeText(ParkAssistActivity.this, "message sent success", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onMessageSendFailed(int messageId) {
                super.onMessageSendFailed(messageId);
                Toast.makeText(ParkAssistActivity.this, "message sent failed", Toast.LENGTH_LONG).show();
            }
        }, null);
    }


    void setSlot(byte[] message) {
        if(message!=null)
        Constant.updateSlot(message);
        final ArrayList<SlotData> slots = Constant.getStaticSlots();
        for (int i = 0; i < 9; i++) {
            final int index = i;
            int buttonId = getResources().getIdentifier("button" + i, "id", getPackageName());
            Button button = findViewById(buttonId);
            if (button != null) {
                button.setText(slots.get(i).slotId);
                if (slots.get(i).subId.length() == 0)
                    button.setBackgroundResource(R.color.greenColor);// Change the text of the button
                else {
                    button.setBackgroundResource(R.color.colorPrimary);
                }
            }

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionData.equals("manager")) {
                        Toast.makeText(ParkAssistActivity.this, "Manager wont allow to select slot ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showConsentDialog( slots.get(index).slotId.getBytes());

                }
            });
        }
    }
    private void showConsentDialog(byte[] slotId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.consent_dialog, null);
        builder.setView(dialogView);

        Button acceptButton = dialogView.findViewById(R.id.acceptButton);
        Button declineButton = dialogView.findViewById(R.id.declineButton);

        final AlertDialog consentDialog = builder.create();
        consentDialog.setCancelable(false);

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ArrayList<SlotData> temp = new ArrayList<SlotData>();
                    if (subscribeDiscoverySession != null){
                        subscribeDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, slotId);

                    }
                    else {
                        Toast.makeText(ParkAssistActivity.this, "subscribeDiscoverySession is null", Toast.LENGTH_LONG).show();
                    }
                }

                consentDialog.dismiss();
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consentDialog.dismiss();
            }
        });

        consentDialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_COARSE_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;

                } else {
                    finish();
                }
            }
            // --------------------------------------------------------------------------------------------
            case MY_PERMISSION_EXTERNAL_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;

                } else {
                    Toast.makeText(this, "no sd card access", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    @TargetApi(26)
    private void requestNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (networkSpecifier == null) {
            Log.d("myTag", "No NetworkSpecifier Created ");
            return;
        }
        Log.d("myTag", "building network interface");
        Log.d("myTag", "using networkspecifier: " + networkSpecifier.toString());
        NetworkRequest networkRequest = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE).setNetworkSpecifier(networkSpecifier).build();

        Log.d("myTag", "finish building network interface");
        connectivityManager.requestNetwork(networkRequest, new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d("myTag", "Network Available: " + network.toString());
            }

            @Override
            public void onLosing(Network network, int maxMsToLive) {
                super.onLosing(network, maxMsToLive);
                Log.d("myTag", "losing Network");
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Toast.makeText(ParkAssistActivity.this, "lost network", Toast.LENGTH_LONG).show();
                Log.d("myTag", "Lost Network");
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Toast.makeText(ParkAssistActivity.this, "onUnavailable", Toast.LENGTH_LONG).show();
                Log.d("myTag", "entering onUnavailable ");
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                Toast.makeText(ParkAssistActivity.this, "onCapabilitiesChanged", Toast.LENGTH_LONG).show();
                Log.d("myTag", "entering onCapabilitiesChanged ");
            }

            // --------------------------------------------------------------------------------------------
            // +++++
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                // TODO: create socketServer on different thread to transfer files
                Toast.makeText(ParkAssistActivity.this, "onLinkPropertiesChanged", Toast.LENGTH_LONG).show();
                Log.d("myTag", "entering linkPropertiesChanged ");
                try {
                    // Log.d("myTag", "iface name: " + linkProperties.getInterfaceName());
                    // Log.d("myTag", "iface link addr: " + linkProperties.getLinkAddresses());

                    NetworkInterface awareNi = NetworkInterface.getByName(linkProperties.getInterfaceName());
                    /*
                     * Inet6Address ipv6 = null;
                     * Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
                     * while (ifcs.hasMoreElements()) {
                     * NetworkInterface iface = ifcs.nextElement();
                     * Log.d("myTag", "iface: " + iface.toString());
                     * }
                     */

                    Enumeration<InetAddress> Addresses = awareNi.getInetAddresses();
                    while (Addresses.hasMoreElements()) {
                        InetAddress addr = Addresses.nextElement();
                        if (addr instanceof Inet6Address) {
                            Log.d("myTag", "netinterface ipv6 address: " + addr.toString());
                            if (((Inet6Address) addr).isLinkLocalAddress()) {
                                ipv6 = Inet6Address.getByAddress("WifiAware", addr.getAddress(), awareNi);
                                myIP = addr.getAddress();
                                if (publishDiscoverySession != null && peerHandle != null) {
                                    publishDiscoverySession.sendMessage(peerHandle, IP_ADDRESS_MESSAGE, myIP);
                                } else if (subscribeDiscoverySession != null && peerHandle != null) {
                                    subscribeDiscoverySession.sendMessage(peerHandle, IP_ADDRESS_MESSAGE, myIP);
                                }
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    Log.d("myTag", "socket exception " + e.toString());
                } catch (Exception e) {
                    // EXCEPTION!!! java.lang.NullPointerException: Attempt to invoke virtual method
                    // 'java.util.Enumeration java.net.NetworkInterface.getInetAddresses()' on a
                    // null object reference
                    Log.d("myTag", "EXCEPTION!!! " + e.toString());
                }
                startServer(0, 3, ipv6);
                // should be done in a separate thread
                /*
                 * startServer
                 * ServerSocket ss = new ServerSocket(0, 5, ipv6);
                 * int port = ss.getLocalPort();
                 */
                // TODO: need to send this port via messages to other device to finish client
                // conn info

                // should be done in a separate thread
                // obtain server IPv6 and port number out-of-band
                // TODO: Retrieve address:port IPv6 before this client thread can be created
                /*
                 * Socket cs = network.getSocketFactory().createSocket(serverIpv6, serverPort);
                 */
            }
            // --------------------------------------------------------------------------------------------
            // -----

        });
    }

    /**
     * Resuming activity
     */

    /**
     * Handles attaching to NAN session.
     */
    @TargetApi(26)
    private void attachToNanSession() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Only once
        if (wifiAwareSession != null) {
            return;
        }


        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            wifiAwareManager.attach(new AttachCallback() {
                @Override
                public void onAttached(WifiAwareSession session) {
                    super.onAttached(session);
                    closeSession();
                    wifiAwareSession = session;
                    setHaveSession(true);
                    if(!actionData.equals("manager")){
                        subscribeToService();

                    }
                }

                @Override
                public void onAttachFailed() {
                    super.onAttachFailed();
                    setHaveSession(false);
                }

            }, new IdentityChangedListener() {
                @Override
                public void onIdentityChanged(byte[] mac) {
                    super.onIdentityChanged(mac);
                    setMacAddress(mac);
                }
            }, null);
        }
        catch (Exception e){
            Toast.makeText(ParkAssistActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    @TargetApi(26)
    protected void onResume() {
        super.onResume();
        String status = null;
        Log.d("myTag", "Current phone build" + Build.VERSION.SDK_INT + "\tMinimum:" + Build.VERSION_CODES.O);
        Log.d("myTag", "Supported Aware: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("myTag", "Entering OnResume is executed");
            IntentFilter filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String status = "";
                    wifiAwareManager.getCharacteristics();
                    boolean nanAvailable = false;
                    if (wifiAwareManager != null) nanAvailable = wifiAwareManager.isAvailable();
                    Log.d("myTag", "NAN is available");
                    if (nanAvailable) {
                        attachToNanSession();
                        status = "NAN has become Available";
                        Log.d("myTag", "NAN attached");
                    } else {
                        status = "NAN has become Unavailable";
                        Log.d("myTag", "NAN unavailable");
                    }
                }
            };

            getApplicationContext().registerReceiver(broadcastReceiver, filter);
            boolean nanAvailable = false;
            if (wifiAwareManager != null) nanAvailable = wifiAwareManager.isAvailable();
            if (nanAvailable) {
                attachToNanSession();
                status = "NAN is Available";
            } else {
                status = "NAN is Unavailable";
            }
        } else {
            status = "NAN is only supported in O+";
        }
    }

    /**
     * Handles cleanup of the activity.
     */
    @Override
    protected void onPause() {
        super.onPause();
        getApplicationContext().unregisterReceiver(broadcastReceiver);
        closeSession();
    }

    private void closeSession() {
        if (publishDiscoverySession != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                publishDiscoverySession.close();
            }
            publishDiscoverySession = null;
        }

        if (subscribeDiscoverySession != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                subscribeDiscoverySession.close();
            }
            subscribeDiscoverySession = null;
        }

        if (wifiAwareSession != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wifiAwareSession.close();
            }
            wifiAwareSession = null;
        }
    }

    /**
     * Handles creating the options menu.
     *
     * @param menu
     * @return
     */

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    /**
     * Handles when an option is selected from the menu.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // --------------------------------------------------------------------------------------------
        // +++++

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.close) {
            closeSession();
            finish();
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    private void setHaveSession(boolean haveSession) {
        // CheckBox cbHaveSession = (CheckBox)findViewById(R.id.haveSession);
        // cbHaveSession.setChecked(haveSession);
    }

    private void setMacAddress(byte[] mac) {
        myMac = mac;
        String macAddress = String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

    }

    private void setOtherMacAddress(byte[] mac) {
        otherMac = mac;
        String macAddress = String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    }

    // --------------------------------------------------------------------------------------------
    // +++++
    private void setOtherIPAddress(byte[] ip) {
        otherIP = ip;
        try {
            String ipAddr = Inet6Address.getByAddress(otherIP).toString();
            // EditText editText = (EditText) findViewById(R.id.IPv6text);
            // editText.setText(ipAddr);
        } catch (UnknownHostException e) {
            Log.d("myTag", "socket exception " + e.toString());
        }
    }


    public int byteToPortInt(byte[] bytes) {
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }

    public byte[] portToBytes(int port) {
        byte[] data = new byte[2];
        data[0] = (byte) (port & 0xFF);
        data[1] = (byte) ((port >> 8) & 0xFF);
        return data;
    }

    @TargetApi(26)
    public void startServer(final int port, final int backlog, final InetAddress bindAddr) {
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("serverThread", "thread running");
                    serverSocket = new ServerSocket(port, backlog, bindAddr);
                    // ServerSocket serverSocket = new ServerSocket();
                    while (true) {
                        portOnSystem = portToBytes(serverSocket.getLocalPort());
                        if (publishDiscoverySession != null && peerHandle != null) {
                            publishDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, portOnSystem);
                        } else if (subscribeDiscoverySession != null && peerHandle != null) {
                            subscribeDiscoverySession.sendMessage(peerHandle, MAC_ADDRESS_MESSAGE, portOnSystem);
                        }
                        Log.d("serverThread", "server waiting to accept on " + serverSocket.toString());
                        Socket clientSocket = serverSocket.accept();
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                        DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                        byte[] buffer = new byte[4096];
                        int read;
                        int totalRead = 0;
                        FileOutputStream fos = new FileOutputStream("/sdcard/Download/newfile");
                        Log.d("serverThread", "Socket being written to begin... ");
                        while ((read = in.read(buffer)) > 0) {
                            fos.write(buffer, 0, read);
                            totalRead += read;
                            if (totalRead % (4096 * 2500) == 0) {// every 10MB update status
                                Log.d("clientThread", "total bytes retrieved:" + totalRead);
                            }
                        }
                        Log.d("serverThread", "finished file transfer: " + totalRead);

                    }
                } catch (IOException e) {
                    Log.d("serverThread", "socket exception " + e.toString());
                }
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();

    }


    public void clientSendFile(final Inet6Address serverIP, final int serverPort) {
        Runnable clientTask = new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[4096];
                int bytesRead;
                Socket clientSocket = null;
                InputStream is = null;
                OutputStream outs = null;
                Log.d("clientThread", "thread running socket info " + serverIP.getHostAddress() + "\t" + serverPort);
                try {
                    clientSocket = new Socket(serverIP, serverPort);
                    is = clientSocket.getInputStream();
                    outs = clientSocket.getOutputStream();
                    Log.d("clientThread", "socket created ");
                } catch (IOException ex) {
                    Log.d("clientThread", "socket could not be created " + ex.toString());
                }
                try {
                    InputStream in = new FileInputStream("/sdcard/Download/IEEEspecJun2018.pdf");
                    int count;
                    int totalSent = 0;
                    DataOutputStream dos = new DataOutputStream(outs);
                    Log.d("clientThread", "beginning to send file");
                    while ((count = in.read(buffer)) > 0) {
                        totalSent += count;
                        dos.write(buffer, 0, count);
                        if (totalSent % (4096 * 2500) == 0) {// every 10MB update status
                            Log.d("clientThread", "total bytes sent:" + totalSent);
                        }
                    }
                    in.close();
                    dos.close();
                    Log.d("clientThread", "finished sending file!!! " + totalSent);
                } catch (FileNotFoundException e) {
                    Log.d("clientThread", "file not found exception " + e.toString());
                } catch (IOException e) {
                    Log.d("clientThread", e.toString());
                }

            }
        };
        Thread clientThread = new Thread(clientTask);
        clientThread.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, Integer.toString(data.getIntExtra("bit_string", 0)), Toast.LENGTH_LONG).show();
            bitString = data.getIntExtra("bit_string", -1);
        }
    }

    void showPopUp() {

        // Button showPopupButton = findViewById(R.id.showPopupButton);

        // showPopupButton.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // // Create an AlertDialog.Builder
        // AlertDialog.Builder builder = new
        // AlertDialog.Builder(ParkAssistActivity.this);
        //
        // // Set the dialog title and message
        // builder.setTitle("Popup Dialog").setMessage("This is a dialog with Cancel and
        // OK buttons.");
        //
        // // Add a Cancel button with a click listener
        // builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dialog, int id) {
        // // Handle the Cancel button click (if needed)
        // dialog.dismiss(); // Close the dialog
        // Toast.makeText(ParkAssistActivity.this, "Canceled",
        // Toast.LENGTH_SHORT).show();
        // }
        // });
        //
        // // Add an OK button with a click listener
        // builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dialog, int id) {
        // // Handle the OK button click (if needed)
        // dialog.dismiss(); // Close the dialog
        // Toast.makeText(ParkAssistActivity.this, "OK Clicked",
        // Toast.LENGTH_SHORT).show();
        // }
        // });
        //
        // // Create and show the AlertDialog
        // AlertDialog dialog = builder.create();
        // dialog.show();
        // }
        // });
    }
    // --------------------------------------------------------------------------------------------
    // -----

}
