package org.vimeoid.activity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.vimeoid.R;

import org.vimeoid.connection.VimeoApi;
import org.vimeoid.connection.VimeoApi.AdvancedApiCallError;
import org.vimeoid.connection.VimeoApi.ApiError;
import org.vimeoid.connection.advanced.Methods;
import org.vimeoid.util.Dialogs;
import org.vimeoid.util.Invoke;

/**
 * 
 * <dl>
 * <dt>Project:</dt> <dd>vimeoid</dd>
 * <dt>Package:</dt> <dd>org.vimeoid</dd>
 * </dl>
 *
 * <code>Videos</code>
 *
 * <p>Description</p>
 *
 * @author Ulric Wilfred <shaman.sir@gmail.com>
 * @date Sep 3, 2010 11:58:57 PM 
 *
 */
public class Welcome extends Activity {
    
    public static final String TAG = "Welcome";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        Log.d(TAG, "Running Vimeo App");
        
        setContentView(R.layout.welcome);
        
        // TODO: if credentials already saved, just start User's profile (tryToEnterAsUser)
        
        final Button enterButton = (Button) findViewById(R.id.enterButton);
        enterButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {                
                tryToEnterAsUser();
            }
        });
        
        final Button guestButton = (Button) findViewById(R.id.guestButton);
        guestButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                /* QuickAction qa = new QuickAction(v);
                qa.addActionItem("Play", getResources().getDrawable(R.drawable.play));
                qa.addActionItem("Face", getResources().getDrawable(R.drawable.contact));
                qa.addActionItem("Jagagaga", getResources().getDrawable(R.drawable.video));
                qa.addActionItem("Trace", getResources().getDrawable(R.drawable.album));
                qa.addActionItem("Author", getResources().getDrawable(R.drawable.channel));
                qa.addActionItem("Later", getResources().getDrawable(R.drawable.watchlater));*/
                /*qa.addActionItem("Video", getResources().getDrawable(R.drawable.video));
                qa.addActionItem("Info", getResources().getDrawable(R.drawable.info));
                qa.addActionItem("Later", getResources().getDrawable(R.drawable.watchlater));
                qa.addActionItem("Gegegegegege", getResources().getDrawable(R.drawable.channel));
                qa.addActionItem("Two words", getResources().getDrawable(R.drawable.channel)); */
                /*qa.show();*/
                enterAsGuest();
            }
            
        });
        
    }
    
    protected void tryToEnterAsUser() {
        
        if (!VimeoApi.connectedToWeb(this)) {
            Dialogs.makeToast(this, getString(R.string.no_iternet_connection));
            return;
        }
        
        if (!VimeoApi.vimeoSiteReachable(this)) {
            Dialogs.makeToast(this, getString(R.string.vimeo_not_reachable));
            return;
        }
        
        Log.d(TAG, "Starting OAuth login");
        if (!VimeoApi.ensureOAuth(this)) {
            try { 
                authenticate();
            } catch (OAuthException oae) {
                Log.e(TAG, oae.getLocalizedMessage());
                oae.printStackTrace();
                Dialogs.makeExceptionToast(this, "OAuth Exception", oae);  
            }                       
        } else {
            Log.d(TAG, "OAuth is ready, loading user name");
            try {
                login();
            } catch (AdvancedApiCallError aace) {
                Log.e(TAG, "Got API error " + aace.code);
                if ((aace.code == ApiError.INVALID_OR_EXPIRED_TOKEN) ||
                    (aace.code == ApiError.LOGIN_FAILED_OR_INVALID_TOKEN) ||
                    (aace.code == ApiError.INVALID_OAUTH_NONCE)) {                    
                    try {
                        Log.e(TAG, "Token exception, will try to re-authenticate");
                        authenticate();
                    } catch (OAuthException oae) {
                        Log.e(TAG, oae.getLocalizedMessage());
                        oae.printStackTrace();
                        Dialogs.makeExceptionToast(this, "Failed to excute authentication", oae);
                    }
                } else VimeoApi.handleApiError(this, aace);
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                e.printStackTrace();
                Dialogs.makeExceptionToast(this, "Getting user exception", e); 
            }
        }        
        
    }
    
    protected void authenticate() throws OAuthMessageSignerException, OAuthNotAuthorizedException, 
                                         OAuthExpectationFailedException, OAuthCommunicationException {
        Log.d(TAG, "Requesting OAuth Uri");
        Uri authUri = VimeoApi.requestForOAuthUri();
        Log.d(TAG, "Got OAuth Uri, staring Browser activity");
        Dialogs.makeToast(this, "Please wait while browser opens");
        Invoke.User_.authenticate(this, authUri);        
    }
    
    protected void login() throws ClientProtocolException, NoSuchAlgorithmException,
                                  JSONException, IOException, URISyntaxException, AdvancedApiCallError,
                                  OAuthMessageSignerException, OAuthExpectationFailedException, 
                                  OAuthCommunicationException {
        JSONObject user = VimeoApi.advancedApi(Methods.test.login).getJSONObject("user");
        Log.d(TAG, "got user " + user.getString("id") + " / " + user.get("username"));
        Invoke.User_.showPersonalPage(this, user.getLong("id"), user.getString("username"));        
    }     
    
    protected void enterAsGuest() {
        Invoke.Guest.selectChannelContent(this, "staffpicks");
    }
       
}