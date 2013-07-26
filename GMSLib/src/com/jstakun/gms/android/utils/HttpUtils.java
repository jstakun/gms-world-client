/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author jstakun
 */
public class HttpUtils {

    private static HttpClient httpClient = null;
    private String errorMessage = null;
    private HttpPost postRequest;
    private HttpGet getRequest;
    private Map<String, String> headers = new HashMap<String, String>();
    private String postResponse = null;
    private static final String APP_HEADER = "X-GMS-AppId";
    private static final String USE_COUNT_HEADER = "X-GMS-UseCount";
    private static final String LAT_HEADER = "X-GMS-Lat";
    private static final String LNG_HEADER = "X-GMS-Lng";
    private static java.util.Locale locale;
    private static final int RETRY_COUNT = 1;

    private static HttpClient getHttpClient() {
        if (httpClient == null) {

            HttpParams params = new BasicHttpParams();
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, (int) DateTimeUtils.ONE_MINUTE);
            params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, (int) DateTimeUtils.ONE_MINUTE);

            ConnPerRouteBean connPerRoute = new ConnPerRouteBean();
            connPerRoute.setDefaultMaxPerRoute(12);
            HttpHost gmsHost = new HttpHost(ConfigurationManager.SERVER_HOST, 80);
            connPerRoute.setMaxForRoute(new HttpRoute(gmsHost), 32);
            ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

            if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.BUILD_INFO, String.class)) {
                String buildInfo = (String) ConfigurationManager.getInstance().getObject(ConfigurationManager.BUILD_INFO, String.class);
                HttpProtocolParams.setUserAgent(params, buildInfo);
            }

            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(
                    new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(
                    new Scheme("https", getSSLSocketFactory(), 443));

            httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, schemeRegistry), params);

            //httpClient = AndroidHttpClient.newInstance(buildInfo, ConfigurationManager.getInstance().getContext());
        }

        return httpClient;
    }

    public void sendPostRequest(String url, List<NameValuePair> params, boolean auth) {
        getThreadSafeClientConnManagerStats();
        sendPostRequest(url, params, auth, RETRY_COUNT);
    }

    private void sendPostRequest(String url, List<NameValuePair> params, boolean auth, int retryCount) {

        InputStream is = null;
        int responseCode = -1;
        boolean needRetry = false;

        if (locale == null) {
            locale = ConfigurationManager.getInstance().getCurrentLocale();
        }

        /*System.out.println("Calling: " + url);
        for (int i = 0; i < params.size(); i++) {
            NameValuePair nvp = params.get(i);
            System.out.println(nvp.getName() + " " + nvp.getValue());
        }*/

        try {
            // Open an HTTP Connection object
            if (postRequest == null) {
                postRequest = new HttpPost(url);

                //postRequest.addHeader("User-Agent", buildInfo);
                postRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
                postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
                postRequest.addHeader(APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
                postRequest.addHeader(USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

                if (auth) {
                    setBasicAuth(postRequest);
                }

                postRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            }

            HttpResponse httpResponse = getHttpClient().execute(postRequest);

            responseCode = httpResponse.getStatusLine().getStatusCode();

            HttpEntity entity = httpResponse.getEntity();
            is = entity.getContent();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                bos.write(buffer, 0, count);
            }

            byte[] byteBuffer = bos.toByteArray();

            postResponse = new String(byteBuffer, "UTF-8");

            LoggerUtils.debug(postResponse);

            if (params != null && byteBuffer != null) {
                increaseCounter(1024, byteBuffer.length);
            }
            if (responseCode == HttpStatus.SC_OK) {
                readHeaders(httpResponse, "key", "name", "hash");
            } else if (retryCount > 0) {
                needRetry = true;
            } else {
                errorMessage = handleHttpStatus(responseCode);
            }
        } catch (Exception e) {
            LoggerUtils.debug("HttpUtils.loadHttpFile exception: ", e);
            errorMessage = handleHttpException(e);
            if (responseCode != HttpStatus.SC_OK && retryCount > 0) {
                needRetry = true;
            } else {
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }
        }

        if (needRetry) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ie) {
            }
            sendPostRequest(url, params, auth, retryCount - 1);
        }
    }

    public void uploadFile(String url, boolean auth, double latitude, double longitude, byte[] file, String filename) {
        getThreadSafeClientConnManagerStats();
        InputStream is = null;
        int responseCode;

        //if (locale == null) {
        //    locale = ConfigurationManager.getInstance().getCurrentLocale();
        //}

        try {
            // Open an HTTP Connection object
            if (postRequest == null) {
                postRequest = new HttpPost(url);

                //postRequest.addHeader("User-Agent", buildInfo);
                //postRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
                //postRequest.addHeader("Content-Type", "multipart/mixed");
                postRequest.addHeader(APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
                postRequest.addHeader(USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));
                postRequest.addHeader(LAT_HEADER, StringUtil.formatCoordE6(latitude));
                postRequest.addHeader(LNG_HEADER, StringUtil.formatCoordE6(longitude));

                if (auth) {
                    setBasicAuth(postRequest);
                    String oauthUser = ConfigurationManager.getInstance().getOAuthLoggedInUsername();
                    if (StringUtils.isNotEmpty(oauthUser)) {
                        postRequest.addHeader("username", oauthUser);
                    }    
                    
                }

                ByteArrayBody bab = new ByteArrayBody(file, filename);
                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                entity.addPart("screenshot", bab);

                postRequest.setEntity(entity);
            }

            HttpResponse httpResponse = getHttpClient().execute(postRequest);

            responseCode = httpResponse.getStatusLine().getStatusCode();

            HttpEntity entity = httpResponse.getEntity();
            is = entity.getContent();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                bos.write(buffer, 0, count);
            }

            byte[] byteBuffer = bos.toByteArray();

            postResponse = new String(byteBuffer, "UTF-8");

            LoggerUtils.debug(postResponse);

            if (byteBuffer != null) {
                increaseCounter(1024 + file.length, byteBuffer.length);
            }
            if (responseCode == HttpStatus.SC_OK) {
            } else {
                errorMessage = handleHttpStatus(responseCode);
            }
        } catch (Exception e) {
            LoggerUtils.debug("HttpUtils.loadHttpFile exception: ", e);
            errorMessage = handleHttpException(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public byte[] loadHttpFile(String url, boolean auth, String format) {
        getThreadSafeClientConnManagerStats();
        return loadHttpFile(url, auth, format, RETRY_COUNT);
    }

    private byte[] loadHttpFile(String url, boolean auth, String format, int retryCount) {

        byte[] byteBuffer = null;
        boolean needRetry = false;

        try {
            LoggerUtils.debug("Loading file: " + url + ", retry count " + retryCount);
            //boolean compressed = false;
            InputStream is;

            if (locale == null) {
                locale = ConfigurationManager.getInstance().getCurrentLocale();
            }

            if (getRequest == null) {
                getRequest = new HttpGet(url);

                getRequest.addHeader("Accept-Encoding", "gzip, deflate");
                //getRequest.addHeader("User-Agent", buildInfo);
                getRequest.addHeader("Connection", "close");
                getRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
                getRequest.addHeader(APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
                getRequest.addHeader(USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

                // HTTP Response
                if (auth) {
                    setBasicAuth(getRequest);
                }
            } else {
                getRequest.setURI(new URI(url));
            }

            HttpResponse httpResponse = getHttpClient().execute(getRequest);

            int responseCode = httpResponse.getStatusLine().getStatusCode();

            if (responseCode == HttpStatus.SC_OK) {
                HttpEntity entity = httpResponse.getEntity();

                if (entity != null) {
                    Header contentEncoding = entity.getContentEncoding();

                    Header contentType = entity.getContentType();
                    if (contentType != null) {
                        String contentTypeValue = contentType.getValue();
                        if (!contentTypeValue.contains(format)) {
                            throw new IOException("Wrong content format! Expected: " + format + ", found: " + contentTypeValue + " at url: " + url);
                        }
                    } else {
                    //    throw new IOException("Missing content type! Expected: " + format + " at url: " + url);
                    	LoggerUtils.debug("Missing content type! Expected: " + format + " at url: " + url);
                    }

                    if (contentEncoding != null && contentEncoding.getValue().indexOf("gzip") != -1) {
                        is = new GZIPInputStream(entity.getContent());
                    } else {
                        is = entity.getContent();
                    }


                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[512];
                        int count;
                        while ((count = is.read(buffer)) >= 0) {
                            bos.write(buffer, 0, count);
                        }

                        byteBuffer = bos.toByteArray();
                        increaseCounter(0, byteBuffer.length);
                        LoggerUtils.debug("File " + url + " size: " + byteBuffer.length + " bytes");//, Compressed = " + compressed);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }

                    entity.consumeContent();
                }
            } else {
                LoggerUtils.error(url + " loading error: " + responseCode + " " + httpResponse.getStatusLine().getReasonPhrase());
                needRetry = true;
                errorMessage = handleHttpStatus(responseCode);
            }
        } catch (Exception e) {
            byteBuffer = null;
            LoggerUtils.error("HttpUtils.loadHttpFile Exception: ", e);
            needRetry = true;
            errorMessage = handleHttpException(e);
        }

        if (needRetry && retryCount > 0) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ie) {
            }
            return loadHttpFile(url, auth, format, retryCount - 1);
        } else {
            return byteBuffer;
        }
    }

    public void close() throws IOException {
        if (getRequest != null) {
            getRequest.abort();
        } else if (postRequest != null) {
            postRequest.abort();
        }
    }

    private void readHeaders(HttpResponse httpResponse, String... headerNames) throws UnsupportedEncodingException {
        for (int i = 0; i < headerNames.length; i++) {
            String headerName = headerNames[i];
            Header header = httpResponse.getFirstHeader(headerName);
            if (header != null) {
                headers.put(headerName, URLDecoder.decode(header.getValue(), "UTF-8"));
            }
        }
    }

    private static void increaseCounter(int sent, int received) {
        long allDataReceivedCounter = Long.parseLong(ConfigurationManager.getInstance().getString(ConfigurationManager.PACKET_DATA_RECEIVED));
        long allDataSentCounter = Long.parseLong(ConfigurationManager.getInstance().getString(ConfigurationManager.PACKET_DATA_SENT));
        allDataReceivedCounter += received;
        allDataSentCounter += sent;
        ConfigurationManager.getInstance().putString(ConfigurationManager.PACKET_DATA_RECEIVED, Long.toString(allDataReceivedCounter));
        ConfigurationManager.getInstance().putString(ConfigurationManager.PACKET_DATA_SENT, Long.toString(allDataSentCounter));
    }

    public static void clearCounter() {
        ConfigurationManager.getInstance().putString(ConfigurationManager.PACKET_DATA_RECEIVED, "0");
        ConfigurationManager.getInstance().putString(ConfigurationManager.PACKET_DATA_SENT, "0");
    }

    public static String[] formatCounter() {
        double mb = 1024.0 * 1024.0 * 8.0;

        long allDataReceivedCounter = Long.parseLong(ConfigurationManager.getInstance().getString(ConfigurationManager.PACKET_DATA_RECEIVED));
        long allDataSentCounter = Long.parseLong(ConfigurationManager.getInstance().getString(ConfigurationManager.PACKET_DATA_SENT));

        String ds = StringUtil.formatDouble((allDataSentCounter / mb), 2);
        String dr = StringUtil.formatDouble((allDataReceivedCounter / mb), 2);

        return new String[]{ds, dr};
    }

    private void setBasicAuth(HttpRequest request) throws IOException {
        if (ConfigurationManager.getInstance().isUserLoggedIn() || !ConfigurationManager.getInstance().isDefaultUser()) {
            String username = ConfigurationManager.getInstance().getString(ConfigurationManager.USERNAME);
            String password = ConfigurationManager.getInstance().getString(ConfigurationManager.PASSWORD);

            //System.out.println("Setting Basic Auth " + username + ":" + password);

            byte[] userpassword = concat((username + ":").getBytes(), Base64.decode(password));
            String encodedAuthorization = new String(Base64.encode(userpassword));
            request.addHeader("Authorization", "Basic " + encodedAuthorization);
        }
    }

    public static void closeConnManager() {
        if (httpClient != null) {
            try {
                ClientConnectionManager cm = httpClient.getConnectionManager();
                if (cm != null) {
                    cm.shutdown();
                }
            } catch (Exception e) {
                LoggerUtils.error("HttpUtils.closeConnManager", e);
            }
        }
        httpClient = null;
    }

    private static byte[] concat(byte[] b1, byte[] b2) {
        byte[] b3 = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, b3, 0, b1.length);
        System.arraycopy(b2, 0, b3, b1.length, b2.length);
        return b3;
    }

    public String getHeader(String key) {
        if (headers.containsKey(key)) {
            return headers.get(key);
        } else {
            return null;
        }
    }

    public String getPostResponse() {
        return postResponse;
    }

    public String getResponseCodeErrorMessage() {
        return errorMessage;
    }

    public static String handleHttpException(Exception e) {
        if (e instanceof java.net.UnknownHostException) {
            //unknown host exception
            return Locale.getMessage(R.string.Internet_connection_error);
        } else if (e instanceof java.io.InterruptedIOException) {
            //interrupted connection - ok
            return null;
        } else if (e instanceof java.net.SocketException) {
            //other socket exceptions - not presented to user
            return Locale.getMessage(R.string.Internet_connection_error);
            //return null;
        } else if (e instanceof java.lang.IllegalStateException) {
            return null;
        } else if (e.getMessage() != null && StringUtils.containsIgnoreCase(e.getMessage(), "IOException")) {
            return Locale.getMessage(R.string.Internet_connection_error);
        } else if (e instanceof java.io.IOException && StringUtils.containsIgnoreCase(e.getMessage(), "SSL shutdown failed")) {
            return Locale.getMessage(R.string.Internet_connection_error); //ssl connection error
        } else if (e.getMessage() != null && StringUtils.containsIgnoreCase(e.getMessage(), "Wrong content format")) {
            return Locale.getMessage(R.string.Internet_connection_error);
        } else if (e.getMessage() != null) {
            return e.getMessage();
        } else {
            return Locale.getMessage(R.string.Internet_connection_error);
        }
    }

    private String handleHttpStatus(int status) {
        if (status == HttpStatus.SC_UNAUTHORIZED) {
            return Locale.getMessage(R.string.Authz_error);
        } else if (status == HttpStatus.SC_CONFLICT) {
            return Locale.getMessage(R.string.Venue_exists_error);
        } else {
            return Locale.getMessage(R.string.Http_error, Integer.toString(status));
        }
    }

    private void getThreadSafeClientConnManagerStats() {
        if (httpClient != null) {
            ClientConnectionManager cm = httpClient.getConnectionManager();
            if (cm != null && cm instanceof ThreadSafeClientConnManager) {
                LoggerUtils.debug("No of connections in pool: " + ((ThreadSafeClientConnManager) cm).getConnectionsInPool());
            }
        }
    }

    private static SSLSocketFactory getSSLSocketFactory() {
    	SSLSocketFactory sslSocketFactory = null;
    	if (OsUtil.isGingerbreadOrHigher()) {
    		sslSocketFactory = SSLSocketFactoryHelper.getSSLSocketFactory();
        } else {
        	sslSocketFactory = SSLSocketFactory.getSocketFactory();
        }
    	sslSocketFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER); 
    	//SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    	return sslSocketFactory;
    }

    private static class SSLSocketFactoryHelper {

        private static SSLSocketFactory getSSLSocketFactory() {
            try {
                SSLSessionCache sessionCache = new SSLSessionCache(ConfigurationManager.getInstance().getContext());
                return SSLCertificateSocketFactory.getHttpSocketFactory((int) DateTimeUtils.ONE_MINUTE, sessionCache);
            } catch (Throwable e) {
            }
            return SSLSocketFactory.getSocketFactory();
        }
    }
}
