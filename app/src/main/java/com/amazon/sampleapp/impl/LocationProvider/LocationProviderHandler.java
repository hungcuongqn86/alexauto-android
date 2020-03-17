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

package com.amazon.sampleapp.impl.LocationProvider;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.amazon.aace.location.LocationProvider;
import com.amazon.aace.location.Location;
import com.amazon.sampleapp.R;

import java.io.IOException;
import java.util.List;

/**
 * A {@link LocationProvider} implementation that retrieves location updates from system GPS and
 * network providers. It includes a means to provide a user-entered mock location to the Engine
 * instead of the current physical location.
 */
public class LocationProviderHandler extends LocationProvider implements LocationListener {
    /// A string to identify log entries originating from this file
    private static final String TAG = "LocationProvider";
    /// The minimum time interval in milliseconds between updates from the location provider
    private static final int MIN_REFRESH_TIME = 60000; // 1 minute
    /// The minimum distance in meters between updates from the location provider
    private static final int MIN_REFRESH_DISTANCE = 0; // 0 meters
    /// The time interval in milliseconds for which a new location update will always be accepted
    /// over the current estimate
    private static final int LOCATION_UPDATE_TIMEOUT = 120000; // 2 minutes
    /// The view containing the mock location input UI elements
    private View mAddressEntry;
    /// The mock location text entry field
    private EditText mAddressText;
    /// The latitude/longitude location display
    private TextView mLatLongText;
    /// A reference to the containing activity
    private final Activity mActivity;
    /// The object providing access to system location services
    private final LocationManager mLocationManager;
    /// The object handling geocoding for mock location
    private final Geocoder mGeocoder;
    /// The current physical location best estimate
    private android.location.Location mCurrentLocation;
    /// The most recently set mock location
    private android.location.Location mMockLocation;
    /// Whether mock location is in use
    private boolean mMockLocationEnabled = false;

    public LocationProviderHandler( Activity activity ) {
        mActivity = activity;
        // Initialize GUI components
        setupGUI();

        // Initialize the mock and physical location providers
        mGeocoder = new Geocoder( mActivity );
        mLocationManager = ( LocationManager )
                activity.getApplicationContext().getSystemService( Context.LOCATION_SERVICE );
        requestLocationUpdates( LocationManager.NETWORK_PROVIDER );
        requestLocationUpdates( LocationManager.GPS_PROVIDER );

        // Retrieve an initial location estimate cached by the location providers
        getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
        getLastKnownLocation( LocationManager.GPS_PROVIDER );

        // Set an initial default mock location
        mMockLocation = new android.location.Location( "" );
        mMockLocation.setLatitude(0);
        mMockLocation.setLongitude(0);
        mMockLocation.setAltitude(0);
    }

    @Override
    public Location getLocation() {
        if ( mMockLocationEnabled ) {
            return new Location(
                    mMockLocation.getLatitude(),
                    mMockLocation.getLongitude(),
                    mMockLocation.getAltitude());
        }
        if ( mCurrentLocation == null ) {
            // null indicates no current available location
            return null;
        }
        return new Location(
                mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(),
                mCurrentLocation.getAltitude());
    }

    @Override
    public String getCountry(){
        // Get device country from a platform specific method/service.
        // As an example "US" is set here by default.
        return "US";
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        if ( !mMockLocationEnabled ) setLocation( location );
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        requestLocationUpdates( provider );
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * Register for location updates from the named provider
     *
     * @param provider The name of the provider with which to register
     */
    private void requestLocationUpdates( String provider ) {
        try {
            // Request updates from the provider only if it's enabled
            if ( !mLocationManager.isProviderEnabled( provider ) ) {

            } else {
                mLocationManager.requestLocationUpdates(
                        provider,
                        MIN_REFRESH_TIME,
                        MIN_REFRESH_DISTANCE,
                        this );
            }
        } catch ( SecurityException e ) {
        }
    }

    /**
     * Updates the current location estimate with the last known location fix obtained from the
     * given provider. Note the location may be out-of-date.
     *
     * @param provider The provider from which to get the last known location
     */
    private void getLastKnownLocation( String provider ){
        try {
            // Request location from the provider only if it's enabled
            if ( !mLocationManager.isProviderEnabled( provider ) ) {

            } else {
                // Get the last known fix from the provider and update the current location estimate
                android.location.Location lastKnownLocation =
                        mLocationManager.getLastKnownLocation( provider );
                if ( lastKnownLocation != null ) setLocation(lastKnownLocation);
            }
        }
        catch ( SecurityException e ) {

        }
    }

    /**
     * Update the current location best estimate using the provided location. If the provided
     * location is not better than the current estimate, the current estimate will not be updated
     * unless it has expired.
     *
     * @param location The location to set as the current estimate
     */
    private void setLocation( android.location.Location location ) {
        if ( mCurrentLocation != null) {
            // Only update if accuracy is equivalent or better or 2 mins since last update
            if ( location.getAccuracy() <= mCurrentLocation.getAccuracy() ||
                    System.currentTimeMillis() - mCurrentLocation.getTime() > LOCATION_UPDATE_TIMEOUT ) {
                mCurrentLocation = location;
                setGUILocation( mCurrentLocation );
            }
        } else {
            mCurrentLocation = location;
            setGUILocation(mCurrentLocation);
        }
    }

    /**
     * Enables or disables use of mock location. When enabled, the most recently set mock location
     * will be sent to the Engine until mock location is disabled.
     *
     * @param enable Whether mock location should be enabled
     */
    private void enableMockLocation( boolean enable ) {
        mMockLocationEnabled = enable;
        if ( enable ) {
            setGUILocation( mMockLocation );
        } else {
            if( mCurrentLocation != null) {
                setGUILocation( mCurrentLocation );
            }
        }
    }

    /**
     * Sets the current mock location. Uses geocoding to construct a location from the
     * provided string descriptor to send to the Engine. The provided location descriptor may
     * represent a place name, an address, an airport code, etc.
     *
     * @param location A string description of the location to set
     */
    private void setMockLocation( String location ) {
        if ( mMockLocationEnabled ) {
            try {
                List<Address> addressList = mGeocoder.getFromLocationName( location, 1 );
                if ( addressList == null || addressList.size() == 0 ) {

                } else {
                    Address address = addressList.get( 0 );
                    mMockLocation = new android.location.Location( "" );
                    mMockLocation.setLatitude( address.getLatitude() );
                    mMockLocation.setLongitude( address.getLongitude() );
                    mMockLocation.setAltitude( 0 );
                    mMockLocation.setAccuracy( 0 );
                    mMockLocation.setTime( System.currentTimeMillis() );
                    setGUILocation( mMockLocation );
                }
            } catch ( IOException e ) {

            }
        }
    }

    /**
     * Initializes GUI components associated with viewing the current location and setting a mock
     * location
     */
    private void setupGUI() {
        // Current location
        mLatLongText = mActivity.findViewById( R.id.latLong );
    }

    /**
     * Updates the current location display to the provided location
     */
    private void setGUILocation( final android.location.Location location ) {
        mActivity.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if ( location != null ) {
                    mLatLongText.setText(String.format("( %.3f, %.3f )",
                            location.getLatitude(), location.getLongitude()));
                } else {
                    mLatLongText.setText( R.string.loc_unavailable );
                }
            }
        });
    }
}
