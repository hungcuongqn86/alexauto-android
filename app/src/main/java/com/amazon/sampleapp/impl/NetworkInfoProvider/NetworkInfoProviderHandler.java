/*
 * Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.sampleapp.impl.NetworkInfoProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.amazon.aace.core.Engine;
import com.amazon.aace.network.NetworkInfoProvider;
import com.amazon.aace.network.NetworkProperties;
import com.amazon.sampleapp.R;

import java.util.HashSet;
import java.util.Set;

public class NetworkInfoProviderHandler extends NetworkInfoProvider {

    private static final String sTag = "NetworkInfoProvider";

    private final Activity mActivity;
    private final Engine mEngine;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final NetworkChangeReceiver mReceiver;
    private TextView mNetworkStatusText;
    private View mInterfaceEntry;
    private EditText mInterfaceText;

    private NetworkStatus mStatus;

    // List of Network Connection observers
    private Set<NetworkConnectionObserver> mObservers;

    public NetworkInfoProviderHandler( Activity activity, Engine engine ) {
        mActivity = activity;
        mEngine = engine;
        mStatus = NetworkStatus.UNKNOWN;

        // Initialize GUI components
        setupGUI();

        mObservers = new HashSet<>(1);

        Context context = mActivity.getApplicationContext();
        // Note: >=API 24 should use NetworkCallback to receive network change updates
        // instead of CONNECTIVITY_ACTION
        mReceiver = new NetworkChangeReceiver();
        context.registerReceiver( mReceiver, new IntentFilter( ConnectivityManager.CONNECTIVITY_ACTION ) );

        mWifiManager = ( WifiManager ) context.getSystemService( Context.WIFI_SERVICE );
        mConnectivityManager = ( ConnectivityManager ) context.getSystemService( Context.CONNECTIVITY_SERVICE );

        updateNetworkStatus();
    }

    @Override
    public NetworkStatus getNetworkStatus() {
        return mStatus;
    }

    @Override
    public int getWifiSignalStrength() { return mWifiManager.getConnectionInfo().getRssi(); }

    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive( final Context context, final Intent intent )
        {
            if ( mConnectivityManager != null ) {
                updateNetworkStatus();
                int rssi = mWifiManager.getConnectionInfo().getRssi();
                updateGUI( mStatus );
                networkStatusChanged( mStatus, rssi );
                notifyConnectionStatusObservers( mStatus );
            }
        }
    }

    public void unregister() { mActivity.getApplicationContext().unregisterReceiver( mReceiver ); }

    private void showAlertDialog( String message ) {
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        alertDialog.setMessage( message );

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void setupGUI() {
        mNetworkStatusText = mActivity.findViewById( R.id.networkStatus );

        mInterfaceEntry = mActivity.findViewById( R.id.networkInterface);
        mInterfaceText = mActivity.findViewById( R.id.networkInterfaceText );

        // Switch to toggle network interface selection
        View interfaceToggle = mActivity.findViewById( R.id.toggleNetworkInterface );
        ( (TextView) interfaceToggle.findViewById( R.id.text ) ).setText( R.string.select_network_interface);
        SwitchCompat interfaceSwitch = interfaceToggle.findViewById( R.id.drawerSwitch );
        interfaceSwitch.setChecked( false );
    }

    private void updateGUI( final NetworkInfoProvider.NetworkStatus status ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mNetworkStatusText.setText( status != null ? status.toString() : "" );
            }
        } );
    }

    private void updateNetworkStatus() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        if ( activeNetwork != null ) {
            NetworkInfo.State state = activeNetwork.getState();
            switch ( state ) {
                case CONNECTED:
                    mStatus = NetworkStatus.CONNECTED;
                    break;
                case CONNECTING:
                    mStatus = NetworkStatus.CONNECTING;
                    break;
                case DISCONNECTING:
                    mStatus = NetworkStatus.DISCONNECTING;
                    break;
                case DISCONNECTED:
                case SUSPENDED:
                    mStatus = NetworkStatus.DISCONNECTED;
                    break;
                case UNKNOWN:
                    mStatus = NetworkStatus.UNKNOWN;
                    break;
            }
        } else {
            mStatus = NetworkStatus.UNKNOWN;
        }
    }

    // Connection State Observable methods
    public void registerNetworkConnectionObserver( NetworkConnectionObserver observer ) {
        if ( observer == null ) return;
        mObservers.add( observer );
        observer.onConnectionStatusChanged( mStatus );
    }

    private void notifyConnectionStatusObservers( NetworkStatus status ){
        for ( NetworkConnectionObserver observer : mObservers ) {
            observer.onConnectionStatusChanged( status );
        }
    }

    private boolean setNetworkInterface( String interfaceText ){
        return mEngine.setProperty(NetworkProperties.NETWORK_INTERFACE, interfaceText );
    }

    private String getNetworkInterface(){
        return mEngine.getProperty(NetworkProperties.NETWORK_INTERFACE );
    }

    private class setNetworkInterfaceAsyncTask extends AsyncTask<String, String, String> {

        private String result;

        @Override
        protected String doInBackground( String... params ) {
            if( !setNetworkInterface( params[0] ) ) {
                result = "Failed to change the network interface to '" + params[0] + "'";;
            } else {
                if( !params[0].isEmpty() ) {
                    result = "Network interface successfully changed to '" + params[0] + "'";
                } else {
                    result = "Network interface successfully reset to default!";
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute( String result ) {
            showAlertDialog( result );
        }

    }

}
