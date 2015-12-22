package co.carlosjimenez.android.googlesigninsample;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.bt_signin)
    public Button mBtSignIn;
    @Bind(R.id.bt_signout)
    public Button mBtSignout;
    @Bind(R.id.bt_revoke)
    public Button mBtRevoke;
    @Bind(R.id.tv_status)
    public TextView mTvStatus;

    private GoogleApiClient mGoogleApiClient;

    private final int STATE_SIGNED_IN = 0;
    private final int STATE_SIGN_IN = 1;
    private final int STATE_IN_PROGRESS = 2;

    private int mSignInProgress = 1;
    private int mSignInError = 0;

    private int RC_SIGN_IN = 0;

    private PendingIntent mSignInIntent;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(new Scope(Scopes.EMAIL))
                //.addScope(new Scope(Scopes.PROFILE))
                .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, ">>>>>>>> onConnectionFailed " + connectionResult.getErrorCode());

        if (mSignInProgress != STATE_IN_PROGRESS) {
            mSignInIntent = connectionResult.getResolution();
            mSignInError = connectionResult.getErrorCode();

            if (mSignInProgress == STATE_SIGN_IN)
                resolveSignInError();
        }

        onSignOut();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, ">>>>>>>> onConnected ");

        mBtSignIn.setEnabled(false);
        mBtSignout.setEnabled(true);
        mBtRevoke.setEnabled(true);

        mSignInProgress = STATE_SIGNED_IN;

        //Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
        mTvStatus.setText("User " + accountName + " has signed in");
    }

    @Override
    public void onConnectionSuspended(int i) {

        String cause;

        switch (i) {
            case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST:
                cause = "Network lost";
                break;
            case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
                cause = "Service disconnected";
                break;
            default:
                cause = "Unknown";
        }

        Log.i(LOG_TAG, ">>>>>>>> onConnectionSuspended " + cause);

        mGoogleApiClient.connect();
    }

    @OnClick(R.id.bt_signin)
    public void onSignInClicked() {
        Log.i(LOG_TAG, ">>>>>>>> onSignInClicked ");

        if (mGoogleApiClient.isConnecting())
            return;

        mTvStatus.setText("Connecting...");
        resolveSignInError();
    }

    @OnClick(R.id.bt_signout)
    public void onSignOutClicked() {
        Log.i(LOG_TAG, ">>>>>>>> onSignOutClicked ");

        if (mGoogleApiClient.isConnecting())
            return;

        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        mGoogleApiClient.disconnect();
        mGoogleApiClient.connect();

    }

    @OnClick(R.id.bt_revoke)
    public void onRevokeClicked() {
        Log.i(LOG_TAG, ">>>>>>>> onRevokeClicked ");

        if (mGoogleApiClient.isConnecting())
            return;

        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
        buildGoogleApiClient();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK)
                mSignInProgress = STATE_SIGN_IN;
            else
                mSignInProgress = STATE_SIGNED_IN;

            if (!mGoogleApiClient.isConnecting())
                mGoogleApiClient.connect();
        }
    }

    public void resolveSignInError() {
        if (mSignInIntent != null) {

            try {
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                mSignInProgress = STATE_SIGN_IN;

                Log.i(LOG_TAG, "Sign in intent could not be sent: " + e.getLocalizedMessage());
                mGoogleApiClient.connect();
            }

        } else {

            Log.i(LOG_TAG, "Sign in intent null");

            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    public void onSignOut() {
        mBtSignIn.setEnabled(true);
        mBtSignout.setEnabled(false);
        mBtRevoke.setEnabled(false);

        mTvStatus.setText("Signed out...");
    }
}
