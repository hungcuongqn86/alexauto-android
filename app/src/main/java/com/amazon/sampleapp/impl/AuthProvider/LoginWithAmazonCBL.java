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

package com.amazon.sampleapp.impl.AuthProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import com.amazon.aace.alexa.AuthProvider;
import com.amazon.aace.network.NetworkInfoProvider;
import com.amazon.sampleapp.R;
import com.amazon.sampleapp.impl.NetworkInfoProvider.NetworkConnectionObserver;
import com.amazon.sampleapp.logView.LogRecyclerViewAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/*
* Login with Amazon Code Based Linking implementation.
* See https://developer.amazon.com/docs/alexa-voice-service/code-based-linking-other-platforms.html for additional reference.
*/
public class LoginWithAmazonCBL implements AuthHandler, NetworkConnectionObserver {

    private static final String sTag = "CBL";

    private static final int sResponseOk = 200;

    // Refresh access token 2 minutes before it expires
    private static final int sRefreshAccessTokenTime = 120000;

    // Poll every 10 seconds when requesting device token
    private static final int sPollInterval = 10;

    // CBL auth endpoint URLs
    private static final String sBaseEndpointUrl = "https://api.amazon.com/auth/O2/";
    private static final String sAuthRequestUrl = LoginWithAmazonCBL.sBaseEndpointUrl + "create/codepair";
    private static final String sTokenRequestUrl = LoginWithAmazonCBL.sBaseEndpointUrl + "token";
    private static final String sTokenVerificationRequestUrl = LoginWithAmazonCBL.sBaseEndpointUrl + "tokeninfo?access_token=";
    private static final String sProfileRequestUrl = "https://api.amazon.com/user/profile";

    //    To fetch User Profile data, set the sUserProfileEnabled to true
    //    You will need additional parameters in your Security Profile for the profile scope request to succeed,
    //    please see the README CBL section for more.
    private static final boolean sUserProfileEnabled = false;
    private static final String sScopeValue = sUserProfileEnabled ? "alexa:all+profile" : "alexa:all";

    // default client id regexpr <>
    private static String sDefaultRegExpr = "^<[^>]*>$";

    private final SharedPreferences mPreferences;
    private final Activity mActivity;

    // List of Authentication observers
    private Set<AuthStateObserver> mObservers;

    private AuthProvider.AuthState mCurrentAuthState;
    private AuthProvider.AuthError mCurrentAuthError;
    private String mCurrentAuthToken;

    // assume connected in case of no network info provider
    private boolean mConnected = true;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private String mClientId;
    private String mProductID;
    private String mProductDSN;

    private Timer mTimer = new Timer();
    private TimerTask mAuthorizationTimerTask;
    private TimerTask mRefreshTimerTask;

    public LoginWithAmazonCBL( Activity activity) {
        mActivity = activity;
        mPreferences = activity.getSharedPreferences(
                activity.getString( R.string.preference_file_key ), Context.MODE_PRIVATE );

        mClientId = mPreferences.getString( mActivity.getString( R.string.preference_client_id ), "" );
        mProductID = mPreferences.getString( mActivity.getString( R.string.preference_product_id ), "" );
        mProductDSN = mPreferences.getString( mActivity.getString( R.string.preference_product_dsn ), "" );
        mObservers = new HashSet<>(1);

        mCurrentAuthState = AuthProvider.AuthState.UNINITIALIZED;
        mCurrentAuthError = AuthProvider.AuthError.NO_ERROR;
        mCurrentAuthToken = "";
    }

    private void requestDeviceAuthorization() {
        mExecutor.submit( new requestDeviceAuthorizationTask() );
    }

    private class requestDeviceAuthorizationTask implements Runnable {
        @Override
        public void run() {
            try {
                if ( !Pattern.matches( sDefaultRegExpr, mClientId ) ) {
                    final JSONObject scopeData = new JSONObject();
                    final JSONObject data = new JSONObject();
                    final JSONObject productInstanceAttributes = new JSONObject();

                    productInstanceAttributes.put( "deviceSerialNumber", mProductDSN );
                    data.put( "productInstanceAttributes", productInstanceAttributes );
                    data.put( "productID", mProductID );
                    scopeData.put( "alexa:all", data );

                    final String urlParameters = "response_type=device_code"
                            + "&client_id=" + mClientId
                            + "&scope=" + sScopeValue
                            + "&scope_data=" + scopeData.toString();

                    HttpsURLConnection con = null;
                    DataOutputStream os = null;
                    InputStream response = null;

                    try {
                        URL obj = new URL( sAuthRequestUrl );
                        con = ( HttpsURLConnection ) obj.openConnection();
                        con.setRequestMethod( "POST" );

                        con.setDoOutput( true );
                        os = new DataOutputStream( con.getOutputStream() );
                        os.writeBytes( urlParameters );

                        int responseCode = con.getResponseCode();
                        if ( responseCode == sResponseOk ) response = con.getInputStream();

                    } catch ( IOException e ) {
                    } finally {
                        if ( con != null ) con.disconnect();
                        if ( os != null ) {
                            try {
                                os.flush();
                                os.close();
                            } catch ( IOException e ) {
                            }
                        }
                    }

                    JSONObject responseJSON = getResponseJSON( response );
                    if ( responseJSON != null ) {
                        String uri = responseJSON.getString( "verification_uri" );
                        String code = responseJSON.getString( "user_code" );

                        // Log card
                        JSONObject renderJSON = new JSONObject();
                        renderJSON.put( "verification_uri", uri );
                        renderJSON.put( "user_code", code );
                        requestDeviceToken( responseJSON );

                    }

                }

            } catch ( Exception e ) {}
        }
    }

    private void requestDeviceToken( JSONObject response ) {
        try {
            final String deviceCode = response.getString( "device_code" );
            final String userCode = response.getString( "user_code" );
            final String expirySeconds = response.getString( "expires_in" );
            final String urlParameters = "grant_type=device_code"
                    + "&device_code=" + deviceCode
                    + "&user_code=" + userCode;

            mTimer.schedule( mAuthorizationTimerTask = new TimerTask() {
                int i = ( Integer.parseInt( expirySeconds ) ) / sPollInterval;
                public void run() {
                    if ( i > 0 ) {
                        HttpsURLConnection con = null;
                        DataOutputStream os = null;
                        BufferedReader in = null;
                        try {
                            URL obj = new URL( sTokenRequestUrl );
                            con = ( HttpsURLConnection ) obj.openConnection();

                            con.setRequestMethod( "POST" );
                            con.setRequestProperty( "Host", "api.amazon.com" );
                            con.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );

                            con.setDoOutput( true );

                            os = new DataOutputStream( con.getOutputStream() );
                            os.writeBytes( urlParameters );

                            int responseCode = con.getResponseCode();
                            if ( responseCode == sResponseOk ) {
                                this.cancel();
                                in = new BufferedReader(
                                        new InputStreamReader( con.getInputStream() ) );
                                String inputLine;
                                StringBuilder response = new StringBuilder();

                                while ( ( inputLine = in.readLine() ) != null ) {
                                    response.append( inputLine );
                                }

                                JSONObject responseJSON = new JSONObject( response.toString() );
                                mCurrentAuthToken = responseJSON.getString( "access_token" );
                                String refreshToken = responseJSON.getString( "refresh_token" );
                                String expiresInSeconds = responseJSON.getString( "expires_in" );

                                // Write refresh token to shared preferences
                                SharedPreferences.Editor editor = mPreferences.edit();
                                editor.putString( mActivity.getString( R.string.preference_refresh_token ), refreshToken );
                                editor.apply();

                                // Refresh access token automatically before expiry
                                startRefreshTimer( Long.parseLong( expiresInSeconds ), refreshToken );

                                mCurrentAuthState = AuthProvider.AuthState.REFRESHED;
                                mCurrentAuthError = AuthProvider.AuthError.NO_ERROR;
                                notifyAuthObservers();

                                // Fetch User Profile if profile scope was authorized
                                if ( sScopeValue.contains( "profile" ) ) {
                                    requestUserProfile( mCurrentAuthToken );
                                }
                            }

                        } catch ( Exception e ) {
                            this.cancel();
                            return;
                        } finally {
                            if ( con != null ) con.disconnect();
                            if ( os != null ) {
                                try {
                                    os.flush();
                                    os.close();
                                } catch ( IOException e ) {
                                }
                            }
                            if ( in != null ) {
                                try {
                                    in.close();
                                } catch ( IOException e ) {
                                }
                            }
                        }
                        i--;

                    } else { // User didn't authorize with code before it expired
                        this.cancel();
                        // Prompt to attempt authorization again
                        String expiredMessage = "The code has expired. Retry to generate a new code.";
                        try {
                            // Log code expired card
                            JSONObject renderJSON = new JSONObject();
                            renderJSON.put( "message", expiredMessage );
                        } catch ( JSONException e ) {
                            return;
                        }
                    }
                }
            }, 0, sPollInterval * 1000 );
        } catch ( Exception e ) {
        }
    }

    private void requestUserProfile( final String accessToken ) {
        HttpsURLConnection urlConnection = null;
        try {
            // token authenticity verification
            URL requestUrl = new URL( sTokenVerificationRequestUrl + URLEncoder.encode(accessToken, "UTF-8" ) );

            urlConnection = ( HttpsURLConnection ) requestUrl.openConnection();
            urlConnection.setRequestMethod( "GET" );
            urlConnection.setRequestProperty( "Host", "api.amazon.com" );
            urlConnection.setRequestProperty( "access_token", URLEncoder.encode( accessToken, "UTF-8" ) );

            int responseCode = urlConnection.getResponseCode();

            if( responseCode == HttpURLConnection.HTTP_OK ) {
                JSONObject responseJSON = getResponseJSON( urlConnection.getInputStream() );
                urlConnection.disconnect();
                if ( responseJSON == null ) {
                } else {
                    if (!mClientId.equals( responseJSON.getString("aud") ) ) {
                    } else {
                        try {
                            requestUrl = new URL(sProfileRequestUrl);
                            urlConnection = (HttpsURLConnection) requestUrl.openConnection();
                            urlConnection.setRequestMethod("GET");
                            urlConnection.setRequestProperty("Host", "api.amazon.com");
                            urlConnection.setRequestProperty("Authorization", "bearer " + accessToken);
                            urlConnection.getResponseCode();
                            if (urlConnection != null) {
                                urlConnection.disconnect();
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }
        } catch( Exception e ) {
        }
    }

    private void refreshAuthToken( final String refreshToken ) {
        mExecutor.submit( new refreshAuthTokenTask( refreshToken ) );
    }

    private class refreshAuthTokenTask implements Runnable {
        String mRefreshToken = "";
        refreshAuthTokenTask( String refreshToken ) { mRefreshToken = refreshToken; }

        @Override
        public void run() {
            if ( !mRefreshToken.equals( "" )
                    && !mClientId.equals( "" ) ) {

                final String urlParameters = "grant_type=refresh_token"
                        + "&refresh_token=" + mRefreshToken
                        + "&client_id=" + mClientId;
                    HttpsURLConnection con = null;
                    DataOutputStream os = null;
                    InputStream response = null;

                try {
                    URL obj = new URL( sTokenRequestUrl );
                    con = ( HttpsURLConnection ) obj.openConnection();
                    con.setRequestMethod( "POST" );

                    con.setDoOutput( true );
                    os = new DataOutputStream( con.getOutputStream() );
                    os.writeBytes( urlParameters );

                    int responseCode = con.getResponseCode();
                    if ( responseCode == sResponseOk ) response = con.getInputStream();

                } catch ( IOException e ) {

                } finally {
                    if ( con != null ) con.disconnect();
                    if ( os != null ) {
                        try {
                            os.flush();
                            os.close();
                        } catch ( IOException e ) {

                        }
                    }
                }

                JSONObject responseJSON = getResponseJSON( response );

                if ( responseJSON != null ) {
                    try {

                        String expiresInSeconds = responseJSON.getString( "expires_in" );
                        mCurrentAuthToken = responseJSON.getString( "access_token" );

                        // Refresh access token automatically before expiry
                        startRefreshTimer( Long.parseLong( expiresInSeconds ), mRefreshToken );


                        mCurrentAuthState = AuthProvider.AuthState.REFRESHED;
                        mCurrentAuthError = AuthProvider.AuthError.NO_ERROR;
                        notifyAuthObservers();

                    } catch ( JSONException e ) {
                    }

                } else {
                    mCurrentAuthState = AuthProvider.AuthState.UNINITIALIZED;
                    mCurrentAuthError = AuthProvider.AuthError.AUTHORIZATION_FAILED;
                    mCurrentAuthToken = "";
                    notifyAuthObservers();
                }

            }
        }
    }

    private void startRefreshTimer( Long delaySeconds, final String refreshToken ) {
        mTimer.schedule( mRefreshTimerTask = new TimerTask() {
            public void run() {
                if ( !mConnected ) {
                    mCurrentAuthState = AuthProvider.AuthState.EXPIRED;
                    mCurrentAuthError = AuthProvider.AuthError.AUTHORIZATION_EXPIRED;
                    mCurrentAuthToken = "";
                    notifyAuthObservers();
                } else refreshAuthToken( refreshToken );

            }
        }, delaySeconds * 1000 - sRefreshAccessTokenTime );
    }

    public void authorize() {
        if ( mConnected ) {
            if ( mAuthorizationTimerTask != null ) {
                mAuthorizationTimerTask.cancel();
            }
            requestDeviceAuthorization();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder( mActivity ) ;
            builder.setTitle( "Internet not available" );
            builder.setIcon( android.R.drawable.ic_dialog_alert );
            builder.setMessage( "Please verify your network settings." );
            builder.setCancelable( false );
            builder.setPositiveButton( "OK", null );
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void deauthorize() {
        // stop refresh timer task
        if ( mRefreshTimerTask != null ) mRefreshTimerTask.cancel();

        // Clear refresh token in preferences
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString( mActivity.getString( R.string.preference_refresh_token ), "" );
        editor.apply();

        mCurrentAuthState = AuthProvider.AuthState.UNINITIALIZED;
        mCurrentAuthError = AuthProvider.AuthError.NO_ERROR;
        mCurrentAuthToken = "";
        notifyAuthObservers();
    }

    private JSONObject getResponseJSON( InputStream inStream ) {
        if ( inStream != null ) {

            String inputLine;
            StringBuilder response = new StringBuilder();

            try ( BufferedReader in = new BufferedReader( new InputStreamReader( inStream ) ) ) {
                while ( ( inputLine = in.readLine() ) != null ) response.append( inputLine );
                return new JSONObject( response.toString() );
            } catch ( Exception e ) {
            } finally {
                try {
                    inStream.close();
                } catch ( IOException e ) { }
            }
        }
        return null;
    }

    // Auth State Observable methods
    public void registerAuthStateObserver( AuthStateObserver observer ) {
        if (observer == null) return;
        mObservers.add(observer);
        observer.onAuthStateChanged( mCurrentAuthState, mCurrentAuthError, mCurrentAuthToken);
    }

    private void notifyAuthObservers(){
        if (mObservers == null) return;
        for (AuthStateObserver observer : mObservers) {
            observer.onAuthStateChanged( mCurrentAuthState, mCurrentAuthError, mCurrentAuthToken );
        }
    }

    // Network Connection Observer methods
    public void onConnectionStatusChanged( NetworkInfoProvider.NetworkStatus status ){
        if ( status == NetworkInfoProvider.NetworkStatus.CONNECTED ) {
            mConnected = true;
        } else mConnected = false;
        mExecutor.execute( new ConnectionStateChangedRunnable( mConnected ) );
    }

    private class ConnectionStateChangedRunnable implements Runnable {
        private boolean mConnectionStatus;
        ConnectionStateChangedRunnable( boolean connected ){
            mConnectionStatus = connected;
        }
        public void run() {
            String refreshToken = mPreferences.getString( mActivity.getString( R.string.preference_refresh_token ), "" );
            // call refresh on connect if auth state is not refreshed, and have a saved refresh token
            if ( mCurrentAuthState != AuthProvider.AuthState.REFRESHED && !"".equals( refreshToken ) ) {
                if ( mConnectionStatus ) {
                    refreshAuthToken( refreshToken );
                }
            }
        }
    }
}