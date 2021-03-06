// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm_server.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.HttpURLConnection;
//import java.net.Authenticator.RequestorType;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
//import java.util.logging.Logger;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm_server.data.oauth.OAuthParameters;
import org.openstreetmap.josm_server.gui.preferences.server.OAuthAccessTokenHolder;
//import org.openstreetmap.josm_server.io.auth.CredentialsManagerException;
//import org.openstreetmap.josm_server.io.auth.CredentialsManagerFactory;
import org.openstreetmap.josm_server.io.auth.CredentialsManagerResponse;
import org.openstreetmap.josm_server.tools.Base64;

/**
 * GWT
 * 
 * TODO
 *  addOAuthAuthorizationHeader not really supported
 * 
 * changelog
 *  addAuth requires explicit credentials
 */

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 *
 * @author imi
 */
public class OsmConnection {
    @SuppressWarnings("unused")
    //private static final Logger logger = Logger.getLogger(OsmConnection.class.getName());

    protected boolean cancel = false;
    protected HttpURLConnection activeConnection;
    protected OAuthParameters oauthParameters;

    /**
     * Initialize the http defaults and the authenticator.
     */
    static {
        try {
            HttpURLConnection.setFollowRedirects(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        cancel = true;
        synchronized (this) {
            if (activeConnection != null) {
                activeConnection.setConnectTimeout(100);
                activeConnection.setReadTimeout(100);
            }
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }

        synchronized (this) {
            if (activeConnection != null) {
                activeConnection.disconnect();
            }
        }
    }

    /**
     * Adds an authentication header for basic authentication
     *
     * @param con the connection
     * @throws OsmTransferException thrown if something went wrong. Check for nested exceptions
     */
    protected void addBasicAuthorizationHeader(HttpURLConnection con, CredentialsManagerResponse credentials) throws OsmTransferException {
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        String token;
        String username= credentials.getUsername() == null ? "" : credentials.getUsername();
        String password = credentials.getPassword() == null ? "" : String.valueOf(credentials.getPassword());
        token = username + ":" + password;
        try {
            ByteBuffer bytes = encoder.encode(CharBuffer.wrap(token));
            con.addRequestProperty("Authorization", "Basic "+Base64.encode(bytes));
        } catch(CharacterCodingException e) {
            throw new OsmTransferException(e);
        }
    }

    /**
     * Signs the connection with an OAuth authentication header
     *
     * @param connection the connection
     *
     * @throws OsmTransferException thrown if there is currently no OAuth Access Token configured
     * @throws OsmTransferException thrown if signing fails
     */
    protected void addOAuthAuthorizationHeader(HttpURLConnection connection) throws OsmTransferException {
        if (oauthParameters == null) {
            oauthParameters = OAuthParameters.createFromPreferences(Main.pref);
        }
        OAuthConsumer consumer = oauthParameters.buildConsumer();
        OAuthAccessTokenHolder holder = OAuthAccessTokenHolder.getInstance();
        if (! holder.containsAccessToken())
            throw new MissingOAuthAccessTokenException();
        consumer.setTokenWithSecret(holder.getAccessTokenKey(), holder.getAccessTokenSecret());
        try {
            consumer.sign(connection);
        } catch(OAuthException e) {
            throw new OsmTransferException(tr("Failed to sign a HTTP connection with an OAuth Authentication header"), e);
        }
    }

    protected void addAuth(HttpURLConnection connection, CredentialsManagerResponse credentials) throws OsmTransferException {
        String authMethod = Main.pref.get("osm-server.auth-method", "basic");
        if (authMethod.equals("basic")) {
            addBasicAuthorizationHeader(connection, credentials);
        } else if (authMethod.equals("oauth")) {
            addOAuthAuthorizationHeader(connection);
        } else {
            String msg = tr("Warning: unexpected value for preference ''{0}''. Got ''{1}''.", "osm-server.auth-method", authMethod);
            System.err.println(msg);
            throw new OsmTransferException(msg);
        }
    }

    /**
     * Replies true if this connection is canceled
     *
     * @return true if this connection is canceled
     * @return
     */
    public boolean isCanceled() {
        return cancel;
    }
}
