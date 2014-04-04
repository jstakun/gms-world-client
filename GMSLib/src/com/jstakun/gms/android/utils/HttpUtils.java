/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
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
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class HttpUtils {

	private static final int REQUEST_RETRY_COUNT = 2;
    private static final int SOCKET_TIMEOUT = (int) DateTimeUtils.ONE_MINUTE; //DateTimeUtils.THIRTY_SECONDS;
    private static DefaultHttpClient httpClient = null;
    private static HttpContext httpContext = null;
    private static java.util.Locale locale;
    private static boolean closeConnManager = false;
	private String errorMessage = null;
    private HttpPost postRequest;
    private HttpGet getRequest;
    private Map<String, String> headers = new HashMap<String, String>();
    private String postResponse = null;
    private int responseCode;
    
    private static HttpClient getHttpClient() {
        if (httpClient == null) {
        	closeConnManager = false;
            HttpParams params = new BasicHttpParams();
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
            params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, SOCKET_TIMEOUT);

            ConnPerRouteBean connPerRoute = new ConnPerRouteBean();
            connPerRoute.setDefaultMaxPerRoute(12);
            HttpHost gmsHost1 = new HttpHost(ConfigurationManager.SERVER_HOST, 443);
            connPerRoute.setMaxForRoute(new HttpRoute(gmsHost1), 32);
            HttpHost gmsHost2 = new HttpHost("www.gms-world.net", 80);
            connPerRoute.setMaxForRoute(new HttpRoute(gmsHost2), 32);
            ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

            String userAgent = System.getProperty("http.agent");
            if (StringUtils.isEmpty(userAgent)) {
            	userAgent = ConfigurationManager.getAppUtils().collectSystemInformation();
            }	
            
            HttpProtocolParams.setUserAgent(params, userAgent);          
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setUseExpectContinue(params, false);
            HttpConnectionParams.setStaleCheckingEnabled(params, false);

            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(
                    new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(
                    new Scheme("https", getSSLSocketFactory(), 443));
            
            httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, schemeRegistry), params);
            httpClient.setHttpRequestRetryHandler(new SocketTimeOutRetryHandler(params, REQUEST_RETRY_COUNT, true));
            
            httpContext = new BasicHttpContext(); 
            httpContext.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
        }

        return httpClient;
    }

    public void sendPostRequest(String url, List<NameValuePair> params, boolean auth) {
        getThreadSafeClientConnManagerStats();
        InputStream is = null;
        
        if (locale == null) {
            locale = ConfigurationManager.getInstance().getCurrentLocale();
        }

        /*System.out.println("Calling: " + url);
        for (int i = 0; i < params.size(); i++) {
            NameValuePair nvp = params.get(i);
            System.out.println(nvp.getName() + " " + nvp.getValue());
        }*/

        try {
        	URI uri = new URI(url);
        	
            postRequest = new HttpPost(uri);

            postRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            postRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

            if (auth) {
               setAuthHeader(postRequest, url.contains(ConfigurationManager.SERVICES_SUFFIX));
            }
           
            postRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse httpResponse = getHttpClient().execute(postRequest, httpContext);

            responseCode = httpResponse.getStatusLine().getStatusCode();

            HttpEntity entity = httpResponse.getEntity();
            /*is = entity.getContent();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                bos.write(buffer, 0, count);
            }*/

            byte[] byteBuffer = EntityUtils.toByteArray(entity); //bos.toByteArray();

            postResponse = new String(byteBuffer, "UTF-8");

            //LoggerUtils.debug(postResponse);

            if (params != null && byteBuffer != null) {
                increaseCounter(1024, byteBuffer.length);
            }
            if (responseCode == HttpStatus.SC_OK) {
                readHeaders(httpResponse, "key", "name", "hash");
            } else {
                errorMessage = handleHttpStatus(responseCode);
            }
            
            entity.consumeContent();
        } catch (Exception e) {
            LoggerUtils.debug("HttpUtils.loadHttpFile exception: " + e.getMessage(), e);
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

    public void uploadFile(String url, boolean auth, double latitude, double longitude, byte[] file, String filename) {
        getThreadSafeClientConnManagerStats();
        InputStream is = null;
        
        try {
        	URI uri = new URI(url);
        	
            postRequest = new HttpPost(uri);

            postRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            postRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));
            postRequest.addHeader(Commons.LAT_HEADER, StringUtil.formatCoordE6(latitude));
            postRequest.addHeader(Commons.LNG_HEADER, StringUtil.formatCoordE6(longitude));

            if (auth) {
                setAuthHeader(postRequest, url.contains(ConfigurationManager.SERVICES_SUFFIX));
                String username = ConfigurationManager.getUserManager().getLoggedInUsername();
                if (StringUtils.isNotEmpty(username)) {
                   postRequest.addHeader("username", username);
                }                      
            }
    
            ByteArrayBody bab = new ByteArrayBody(file, filename);
            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            entity.addPart("screenshot", bab);

            postRequest.setEntity(entity);        

            HttpResponse httpResponse = getHttpClient().execute(postRequest, httpContext);

            responseCode = httpResponse.getStatusLine().getStatusCode();

            HttpEntity respEntity = httpResponse.getEntity();
            is = respEntity.getContent();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int count;
            while ((count = is.read(buffer)) >= 0) {
                bos.write(buffer, 0, count);
            }

            byte[] byteBuffer = bos.toByteArray(); //EntityUtils.toByteArray(entity); 

            postResponse = new String(byteBuffer, "UTF-8");

            LoggerUtils.debug(postResponse);

            if (byteBuffer != null) {
                increaseCounter(1024 + file.length, byteBuffer.length);
            }
            if (responseCode == HttpStatus.SC_OK) {
            } else {
                errorMessage = handleHttpStatus(responseCode);
            }
            respEntity.consumeContent();
        } catch (Exception e) {
            LoggerUtils.debug("HttpUtils.loadHttpFile() exception: " + e.getMessage(), e);
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

    public synchronized byte[] loadHttpFile(String url, boolean auth, String format) {
        getThreadSafeClientConnManagerStats();     
        byte[] byteBuffer = null;
        
        try {
            LoggerUtils.debug("Loading file: " + url);
            //boolean compressed = false;
            InputStream is;

            if (locale == null) {
                locale = ConfigurationManager.getInstance().getCurrentLocale();
            }

            URI uri = new URI(url);
            
            getRequest = new HttpGet(uri);

            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            getRequest.addHeader("Connection", "close");
            getRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
            getRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            getRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

            // HTTP Response
            if (auth) {
               setAuthHeader(getRequest, url.contains(ConfigurationManager.SERVICES_SUFFIX));
            }
            
            if (!getRequest.isAborted() && !closeConnManager) {
            	HttpResponse httpResponse = getHttpClient().execute(getRequest, httpContext);

            	responseCode = httpResponse.getStatusLine().getStatusCode();

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
            				//throw new IOException("Missing content type! Expected: " + format + " at url: " + url);
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
                	errorMessage = handleHttpStatus(responseCode);
            	}
            }
        } catch (Exception e) {
            byteBuffer = null;
            LoggerUtils.error("HttpUtils.loadHttpFile() exception: " + e.getMessage(), e);
            errorMessage = handleHttpException(e);
        }

        return byteBuffer;
    }
    
    public synchronized List<ExtendedLandmark> loadLandmarkList(URI uri, List<NameValuePair> params, boolean auth, String format) {
        getThreadSafeClientConnManagerStats();
        
        List<ExtendedLandmark> reply = new ArrayList<ExtendedLandmark>();
        
        /*System.out.println("Calling: " + uri.toString());
        for (int i = 0; i < params.size(); i++) {
            NameValuePair nvp = params.get(i);
            System.out.println(nvp.getName() + " " + nvp.getValue());
        }*/
        
        try {
        	if (ServicesUtils.isNetworkActive()) { 	
        		LoggerUtils.debug("Loading file: " + uri.toString());
            
        		if (locale == null) {
        			locale = ConfigurationManager.getInstance().getCurrentLocale();
        		}

        		postRequest = new HttpPost(uri);

        		postRequest.addHeader("Accept-Encoding", "gzip, deflate");
        		postRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
        		postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        		postRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
        		postRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

        		if (auth) {
        			setAuthHeader(postRequest, uri.getPath().contains(ConfigurationManager.SERVICES_SUFFIX));
        		}
           
        		postRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            
        		if (!postRequest.isAborted() && !closeConnManager) {
        			HttpResponse httpResponse = getHttpClient().execute(postRequest, httpContext);

        			responseCode = httpResponse.getStatusLine().getStatusCode();

        			if (responseCode == HttpStatus.SC_OK) {
        				HttpEntity entity = httpResponse.getEntity();

        				if (entity != null) {
        					Header contentType = entity.getContentType();
        					if (contentType != null) {
        						String contentTypeValue = contentType.getValue();
        						if (!contentTypeValue.contains(format)) {
        							throw new IOException("Wrong content format! Expected: " + format + ", found: " + contentTypeValue + " at url: " + uri.toString());
        						}
        					} else {
        						//throw new IOException("Missing content type! Expected: " + format + " at url: " + uri.toString());
        						LoggerUtils.debug("Missing content type! Expected: " + format + " at url: " + uri.toString());
        					}

        					//1. read bytes stream
                    	
        					/*InputStream is = entity.getContent();
                    		ByteArrayOutputStream baos = new ByteArrayOutputStream();				
            				byte[] buffer = new byte[1024];
            				int read = 0;
            				while ((read = is.read(buffer, 0, buffer.length)) != -1) {
            					baos.write(buffer, 0, read);
            				}		
            				baos.flush();
            				is.close();
            			
            				int bufferSize = baos.size();
        					 */
                    	
        					byte[] buffer = EntityUtils.toByteArray(entity);
        					int bufferSize = buffer.length;
                    
        					LoggerUtils.debug("File " + uri.toString() + " size: " + bufferSize + " bytes");
            		
        					if (bufferSize > 0) {
        						increaseCounter(0, bufferSize);
        						ObjectInputStream ois = null;
        						ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        						if (contentType != null && contentType.getValue().indexOf("deflate") != -1) {
        							ois = new ObjectInputStream(new InflaterInputStream(bais, new Inflater(false)));
        						} else {
        							ois = new ObjectInputStream(bais);
        						}
                   
        						if (ois.available() > 0) {
        							int size = ois.readInt();
        							LoggerUtils.debug("Reading " + size + " landmarks...");
        							if (size > 0) {
        								for(int i = 0;i < size;i++) {
        									try {
        										ExtendedLandmark landmark = new ExtendedLandmark(); 
        										landmark.readExternal(ois);
        										reply.add(landmark);
        									} catch (IOException e) {
        										LoggerUtils.error("HttpUtils.loadLandmarkList() exception: ", e);
        									}
        								}
        							}
        						}
                            
        						bais.close();
        						ois.close();
        					} 
                    
        					entity.consumeContent();
        				}
        			} else {
        				LoggerUtils.error(uri.toString() + " loading error: " + responseCode + " " + httpResponse.getStatusLine().getReasonPhrase());
        				errorMessage = handleHttpStatus(responseCode);
        			}
        		}
        	} else {
        		errorMessage = Locale.getMessage(R.string.Network_connection_error_title);
        		LoggerUtils.error("HttpUtils.loadLandmarkList() network exception: " + errorMessage);
        	}
        } catch (Exception e) {
            LoggerUtils.error("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
            errorMessage = handleHttpException(e);
        }

        //comment
        //System.out.println("Cookies list: ------------------------------------------------------------------------");
        //BasicCookieStore bcs = (BasicCookieStore)httpContext.getAttribute(ClientContext.COOKIE_STORE);
        //for (Cookie c : bcs.getCookies()) {
        //	System.out.println(c.getName() + ": " + c.getValue());
        //}
        
        return reply;
    }

    public synchronized void close() throws IOException {
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
        ConfigurationManager.getInstance().putString(ConfigurationManager.PACKET_DATA_DATE, Long.toString(System.currentTimeMillis()));
    }

    public static String[] formatCounter() {
        double mb = 1024.0 * 1024.0 * 8.0;

        long allDataReceivedCounter = ConfigurationManager.getInstance().getLong(ConfigurationManager.PACKET_DATA_RECEIVED);
        long allDataSentCounter = ConfigurationManager.getInstance().getLong(ConfigurationManager.PACKET_DATA_SENT);
        long packetDateDate = ConfigurationManager.getInstance().getLong(ConfigurationManager.PACKET_DATA_DATE);
        String sd = null;
        if (packetDateDate == -1) {
        	sd = "unknown";
        } else {
        	sd = DateTimeUtils.getDefaultDateTimeString(packetDateDate, ConfigurationManager.getInstance().getCurrentLocale());
        }
        
        String ds = StringUtil.formatDouble((allDataSentCounter / mb), 2);
        String dr = StringUtil.formatDouble((allDataReceivedCounter / mb), 2);
        
        return new String[]{ds, dr, sd};
    }

    private static void setAuthHeader(HttpRequest request, boolean throwIfEmpty) {
    	/*String username = null, password = null;
    	boolean decodePassword = true, decodeUsername = true;
    	
    	if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.GMS_USERNAME, String.class) && //OK
    			ConfigurationManager.getInstance().containsObject(ConfigurationManager.GMS_PASSWORD, String.class)) {
        	//user is in process of login to gms world
    		username = (String) ConfigurationManager.getInstance().removeObject(ConfigurationManager.GMS_USERNAME, String.class);
    		password = (String) ConfigurationManager.getInstance().removeObject(ConfigurationManager.GMS_PASSWORD, String.class);
    		decodeUsername = false;
    		decodePassword = false;
        } else if (ConfigurationManager.getUserManager().isUserLoggedIn()) { //OK
    		//user is logged in
        	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GMS_AUTH_STATUS)) {
        		username = ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_USERNAME);
        		password = ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_PASSWORD);
        		decodeUsername = false;
        	} else {
        		username = Commons.GMS_APP_USER;
                password = Commons.APP_USER_PWD;
        	}
    	} else if (ConfigurationManager.getInstance().containsObject(Commons.MY_POS_CODE, String.class)) { //OK
    		//my pos request
    		username = Commons.MY_POS_USER;
            password = Commons.APP_USER_PWD;
            ConfigurationManager.getInstance().removeObject(Commons.MY_POS_CODE, String.class);
        } else if (ConfigurationManager.getInstance().getInt(ConfigurationManager.APP_ID) == 1) { //OK
    		//da app request
    		username = Commons.DA_APP_USER;
            password = Commons.APP_USER_PWD;
    	}
        
    	if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
    		request.addHeader("Authorization", getAuthHeader(username, decodeUsername, password, decodePassword));
    	} else if (throwIfEmpty) {
    		LoggerUtils.error("Authorization header is empty for user " + username);
    		throw new SecurityException("Authorization header is empty for user " + username);
    	}*/
    	
    	if (ConfigurationManager.getUserManager().isTokenPresent()) {
    		request.addHeader(Commons.TOKEN_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_TOKEN));
    		request.addHeader(Commons.SCOPE_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_SCOPE));
    	} else if (throwIfEmpty) {
    		LoggerUtils.error("Missing authorization token");
    		throw new SecurityException("Missing authorization token");
    	}
    }
    
    /*public static String getAuthHeader(String username, boolean decodeUsername, String password, boolean decodePassword) {
    	byte[] usr, pwd;
    	
    	if (decodeUsername) {
    	   usr = Base64.decode(username);
    	} else {
    	   usr = username.getBytes();	
    	}
    	
    	if (decodePassword) {
    		pwd = Base64.decode(password);
    	} else {
    		pwd = password.getBytes();
    	}
    		
    	byte[] userpassword = StringUtil.concat(StringUtil.concat(usr, ":".getBytes()), pwd);
		String encodedAuthorization = new String(Base64.encode(userpassword));
		return "Basic " + encodedAuthorization;
    }*/

    public static void closeConnManager() {
    	closeConnManager = true;
        new HttpClientClosingTask(1).execute();
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
    
    public int getResponseCode() {
    	return responseCode;
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
        } else if (status == HttpStatus.SC_FORBIDDEN) {
        	return Locale.getMessage(R.string.Forbidden_connecion_error);
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
    	/*SSLSocketFactory sslSocketFactory = null;
    	if (OsUtil.isGingerbreadOrHigher()) {
    		sslSocketFactory = SSLSocketFactoryHelper.getSSLSocketFactory();
        } else {
        	sslSocketFactory = SSLSocketFactory.getSocketFactory();
        }
    	sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);*/
        //SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER); 
    	try {
    		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    		trustStore.load(null, null);

    		SSLSocketFactory sslSocketFactory = TrustAllSSLSocketFactory.getSSLSocketFactory(trustStore);
    		sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    		return sslSocketFactory;
    	} catch (Exception e) {
    		LoggerUtils.error("HttpUtils.getSSLSocketFactory() exception", e);
    		return null;
    	}
    }
    
    /*private static void setUserCredentials(URI uri) throws UnsupportedEncodingException { 
    	String username = null, password = null;
    	boolean decodePassword = true, decodeUsername = true;
    	
    	if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.GMS_USERNAME, String.class) && 
    			ConfigurationManager.getInstance().containsObject(ConfigurationManager.GMS_PASSWORD, String.class)) {
        	//user in process of login to gms world
    		username = (String) ConfigurationManager.getInstance().removeObject(ConfigurationManager.GMS_USERNAME, String.class);
    		password = (String) ConfigurationManager.getInstance().removeObject(ConfigurationManager.GMS_PASSWORD, String.class);
    		decodeUsername = false;
    		decodePassword = false;
        } else if (ConfigurationManager.getUserManager().isUserLoggedIn()) {
    		//user is logged in
        	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GMS_AUTH_STATUS)) {
        		username = ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_USERNAME);
        		password = ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_PASSWORD);
        		decodeUsername = false;
        	} else {
        		username = Commons.GMS_APP_USER;
                password = Commons.APP_USER_PWD;
        	}
    	} else if (ConfigurationManager.getInstance().containsObject(Commons.MY_POS_CODE, String.class)) {
    		//mypos request
    		username = Commons.MY_POS_USER;
            password = Commons.APP_USER_PWD;
            ConfigurationManager.getInstance().removeObject(Commons.MY_POS_CODE, String.class);
        } else if (ConfigurationManager.getInstance().getInt(ConfigurationManager.APP_ID) == 1) {
    		//da app request
    		username = Commons.DA_APP_USER;
            password = Commons.APP_USER_PWD;
    	}
    	
        byte[] usr, pwd;
    	
    	if (decodeUsername) {
    	   usr = Base64.decode(username);
    	} else {
    	   usr = username.getBytes();	
    	}
    	
    	if (decodePassword) {
    		pwd = Base64.decode(password);
    	} else {
    		pwd = password.getBytes();
    	}
    	
    	Credentials creds = new UsernamePasswordCredentials(new String(usr, "US-ASCII"), new String(pwd, "US-ASCII"));
    	
    	httpClient.getCredentialsProvider().setCredentials(new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_REALM), creds);
    }*/

    /*private static class SSLSocketFactoryHelper {

        private static SSLSocketFactory getSSLSocketFactory() {
            try {
                SSLSessionCache sessionCache = new SSLSessionCache(ConfigurationManager.getInstance().getContext());
                return SSLCertificateSocketFactory.getHttpSocketFactory(SOCKET_TIMEOUT, sessionCache);
            } catch (Throwable e) {
            }
            return SSLSocketFactory.getSocketFactory();
        }
    }*/
    
    private static class HttpClientClosingTask extends GMSAsyncTask<Void, Void, Void> {

		public HttpClientClosingTask(int priority) {
			super(priority);
		}

		@Override
		protected Void doInBackground(Void... params) {
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
	        return null;
		}   	
    }
    
    private static class SocketTimeOutRetryHandler extends DefaultHttpRequestRetryHandler {
    	
    	 private HttpParams httpParams;
    	
    	 public SocketTimeOutRetryHandler(HttpParams httpParams, int retryCount, boolean requestSentRetryEnabled) {
    		 super(retryCount, requestSentRetryEnabled);
    		 this.httpParams = httpParams;
    	 }
    	 
    	 @Override
         public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
             if (exception instanceof SocketTimeoutException) {
                 if (executionCount <= getRetryCount()) {
                	 if (httpParams != null) {
                         final int newSocketTimeOut = HttpConnectionParams.getSoTimeout(httpParams) * 2;
                         HttpConnectionParams.setSoTimeout(httpParams, newSocketTimeOut);
                         LoggerUtils.debug("SocketTimeOut - increasing time out to " + newSocketTimeOut
                                 + " millis and trying again");
                     } else {
                    	 LoggerUtils.debug("SocketTimeOut - no HttpParams, cannot increase time out. Trying again with current settings");
                     }
                     return true;
                 } else {
                	 return false;
                 }
             } else {
            	 return super.retryRequest(exception, executionCount, context);
             }
    	 }   
    }
    
    private static class TrustAllSSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        private static TrustAllSSLSocketFactory instance; 
        
        public static SSLSocketFactory getSSLSocketFactory(KeyStore truststore) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        	instance = new TrustAllSSLSocketFactory(truststore);
        	return instance;
        }
        
        private TrustAllSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
                
            };

            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }
}
