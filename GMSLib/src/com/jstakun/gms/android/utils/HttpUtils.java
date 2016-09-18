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
import java.net.URL;
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
import org.apache.http.client.params.ClientPNames;
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
import org.apache.http.message.BasicNameValuePair;
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
    //private String postResponse = null;
    private int responseCode;
    
    private static HttpClient getHttpClient() {
        if (httpClient == null) {
        	closeConnManager = false;
            HttpParams params = new BasicHttpParams();
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
            params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, SOCKET_TIMEOUT);
            params.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
            
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

    public String sendPostRequest(String url, Map<String, String> postParams, boolean auth) {
        /*getThreadSafeClientConnManagerStats();
        InputStream is = null;
        String postResponse = null;
        
        if (locale == null) {
            locale = ConfigurationManager.getInstance().getCurrentLocale();
        }

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> param : postParams.entrySet()) {
        	params.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }

        try {
        	URI uri = new URI(url);
        	
            postRequest = new HttpPost(uri);

            postRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            postRequest.addHeader(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
            postRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

            if (auth) {
               setAuthHeader(postRequest, url.contains(ConfigurationManager.SERVICES_SUFFIX));
            }
           
            postRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse httpResponse = getHttpClient().execute(postRequest, httpContext);

            responseCode = httpResponse.getStatusLine().getStatusCode();
            errorMessage = null;

            HttpEntity entity = httpResponse.getEntity();
        	
            if (!postRequest.isAborted()) {
            	byte[] byteBuffer = EntityUtils.toByteArray(entity);

            	postResponse = new String(byteBuffer, "UTF-8");

              	if (params != null && byteBuffer != null) {
            		ConfigurationManager.getAppUtils().increaseCounter(1024, byteBuffer.length);
            	}
            	if (responseCode == HttpStatus.SC_OK) {
            		readHeaders(httpResponse, "key", "name", "hash");
            	} else {
            		errorMessage = handleHttpStatus(responseCode);
            	}
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
        return postResponse;*/
    	return new HttpUtils2().sendPostRequest(url, postParams, auth);
    }

    public String uploadScreenshot(String url, boolean auth, double latitude, double longitude, byte[] file, String filename) {
        getThreadSafeClientConnManagerStats();
        InputStream is = null;
        String postResponse = null;
        
        try {
        	URI uri = new URI(url);
        	
            postRequest = new HttpPost(uri);

            postRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            postRequest.addHeader(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
            postRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));
            postRequest.addHeader(Commons.LAT_HEADER, StringUtil.formatCoordE6(latitude));
            postRequest.addHeader(Commons.LNG_HEADER, StringUtil.formatCoordE6(longitude));
            
            String myPosKey = (String)ConfigurationManager.getInstance().removeObject(Commons.MY_POS_CODE, String.class);
            if (StringUtils.isNotEmpty(myPosKey)) {
            	 postRequest.addHeader(Commons.MYPOS_KEY_HEADER, myPosKey);
            }
            
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
            errorMessage = null;

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
            	ConfigurationManager.getAppUtils().increaseCounter(1024 + file.length, byteBuffer.length);
            }
            if (responseCode == HttpStatus.SC_OK) {
            } else {
                errorMessage = handleHttpStatus(responseCode);
            }
            respEntity.consumeContent();
        } catch (Throwable e) {
            LoggerUtils.debug("HttpUtils.uploadScreenshot() exception: " + e.getMessage(), e);
            errorMessage = handleHttpException(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }
        }
        return postResponse;
    }

    public byte[] loadFile(String url, boolean auth, String format) {
        /*getThreadSafeClientConnManagerStats();     
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
            getRequest.addHeader(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
            getRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

            // HTTP Response
            if (auth) {
               setAuthHeader(getRequest, url.contains(ConfigurationManager.SERVICES_SUFFIX));
            }
            
            if (!getRequest.isAborted() && !closeConnManager) {
            	HttpResponse httpResponse = getHttpClient().execute(getRequest, httpContext);

            	responseCode = httpResponse.getStatusLine().getStatusCode();
            	errorMessage = null;
            	
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
                        	ConfigurationManager.getAppUtils().increaseCounter(0, byteBuffer.length);
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

        return byteBuffer;*/
    	return new HttpUtils2().loadFile(url, auth, format);
    }
    
    public List<ExtendedLandmark> loadLandmarkList(String url, Map<String, String> postParams, boolean auth, String[] formats) {
        /*getThreadSafeClientConnManagerStats();
        
        List<ExtendedLandmark> reply = new ArrayList<ExtendedLandmark>();
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> param : postParams.entrySet()) {
        	params.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        
        try {
        	if (ServicesUtils.isNetworkActive()) { 	
        		LoggerUtils.debug("Loading file: " + url);
            
        		if (locale == null) {
        			locale = ConfigurationManager.getInstance().getCurrentLocale();
        		}

        		postRequest = new HttpPost(url);

        		postRequest.addHeader("Accept-Encoding", "gzip, deflate");
        		postRequest.addHeader("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
        		postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        		postRequest.addHeader(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
        		postRequest.addHeader(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
                postRequest.addHeader(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));

        		if (auth) {
        			setAuthHeader(postRequest, url.contains(ConfigurationManager.SERVICES_SUFFIX));
        		}
           
        		postRequest.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            
        		if (!postRequest.isAborted() && !closeConnManager) {
        			HttpResponse httpResponse = getHttpClient().execute(postRequest, httpContext);
        			
        			responseCode = httpResponse.getStatusLine().getStatusCode();
        			errorMessage = null;

        			if (responseCode == HttpStatus.SC_OK) {
        				HttpEntity entity = httpResponse.getEntity();

        				if (entity != null) {
        					Header contentType = entity.getContentType();
        					if (contentType != null) {
        						String contentTypeValue = contentType.getValue();
        						if (!StringUtils.startsWithAny(contentTypeValue, formats)) {
        							responseCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        							throw new IOException("Wrong content format! Expected: " + StringUtils.join(formats," or ") + ", found: " + contentTypeValue + " at url: " + url);
        						}
        					} else {
        						//throw new IOException("Missing content type! Expected: " + format + " at url: " + uri.toString());
        						LoggerUtils.debug("Missing content type! Expected: " + StringUtils.join(formats," or ") + " at url: " + url);
        					}

        					byte[] buffer = EntityUtils.toByteArray(entity);
        					int bufferSize = buffer.length;
                    
        					LoggerUtils.debug("File " + url + " size: " + bufferSize + " bytes");
            		
        					if (bufferSize > 0 && !postRequest.isAborted()) {
        						ConfigurationManager.getAppUtils().increaseCounter(0, bufferSize);
        						ObjectInputStream ois = null;
        						ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        						try {
        							if (contentType != null && contentType.getValue().indexOf("deflate") != -1) {
        								ois = new ObjectInputStream(new InflaterInputStream(bais, new Inflater(false)));
        							} else {
        								ois = new ObjectInputStream(bais);
        							}
        						} catch (IOException e) {
        							responseCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        							throw new IOException("Unable to create object input stream from " + url, e);
        						}
                   
        						if (ois != null && ois.available() > 0) {
        							int size = ois.readInt();
        							LoggerUtils.debug("Reading " + size + " landmarks from " + url);
        							if (size > 0) {
        								for(int i = 0;i < size;i++) {
        									try {
        										ExtendedLandmark landmark = new ExtendedLandmark(); 
        										landmark.readExternal(ois);
        										reply.add(landmark);
        									} catch (IOException e) {
        										LoggerUtils.error("HttpUtils.loadLandmarkList() " + e.getClass().getName() + ": " +  e.getMessage());
        									}
        									if (postRequest.isAborted()) {
        										break;
        									}
        								}
        								//LoggerUtils.debug("Loaded " + reply.size() + " landmarks from " + uri.toString());
        							}
        						}
                            
        						bais.close();
        						ois.close();
        					} 
                    
        					entity.consumeContent();
        				}
        			} else {
        				LoggerUtils.error(url + " loading error: " + responseCode + " " + httpResponse.getStatusLine().getReasonPhrase());
        				errorMessage = handleHttpStatus(responseCode);
        			}
        		}
        	} else {
        		errorMessage = Locale.getMessage(R.string.Network_connection_error_title);
        		LoggerUtils.error("HttpUtils.loadLandmarkList() network exception: " + errorMessage);
        	}
        } catch (Exception e) {
        	if (StringUtils.equals(e.getMessage(), "Connection already shutdown")) {
        		LoggerUtils.debug("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
        	} else {
        		LoggerUtils.error("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
        	}
        	errorMessage = handleHttpException(e);
        }

        //comment
        //System.out.println("Cookies list: ------------------------------------------------------------------------");
        //BasicCookieStore bcs = (BasicCookieStore)httpContext.getAttribute(ClientContext.COOKIE_STORE);
        //for (Cookie c : bcs.getCookies()) {
        //	System.out.println(c.getName() + ": " + c.getValue());
        //}
        
        return reply;*/
    	
    	return new HttpUtils2().loadLandmarksList(url, postParams, auth, formats);
    }

    public void close() throws IOException {
    	if (getRequest != null) {
        	//LoggerUtils.debug("Closing connection to " + getRequest.getURI());
        	getRequest.abort();
        } else if (postRequest != null) {
        	//LoggerUtils.debug("Closing connection to " + postRequest.getURI());
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

    private static void setAuthHeader(HttpRequest request, boolean throwIfEmpty) {
    	if (ConfigurationManager.getUserManager().isTokenPresent()) {
    		request.addHeader(Commons.TOKEN_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_TOKEN));
    		request.addHeader(Commons.SCOPE_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_SCOPE));
    	} else if (throwIfEmpty) {
    		LoggerUtils.error("Missing authorization token");
    		throw new SecurityException("Missing authorization token");
    	}
    }

    public static void closeConnManager() {
    	closeConnManager = true;
        new HttpClientClosingTask(1).execute();
    }

    public String getHeader(String url, String key) {
        //if (headers.containsKey(key)) {
        //    return headers.get(key);
        //} else {
        //    return null;
        //}
    	return HttpUtils2.getHeader(url, key);
    }

    public String getResponseCodeErrorMessage() {
        return errorMessage;
    }
    
    public int getResponseCode() {
    	return responseCode;
    }

    private static String handleHttpException(Throwable e) {
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
        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "IOException")) {
            return Locale.getMessage(R.string.Internet_connection_error);
        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "SSL shutdown failed")) {
            return Locale.getMessage(R.string.Internet_connection_error); //ssl connection error
        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "Wrong content format")) {
            return Locale.getMessage(R.string.Internet_connection_error);
        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "Unable to create object input stream")) {
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
        	return Locale.getMessage(R.string.Forbidden_connection_error);
        } else if (status == HttpStatus.SC_SERVICE_UNAVAILABLE) {
        	return Locale.getMessage(R.string.Service_unavailable_error); 
        }  else {
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
  
    private static class HttpClientClosingTask extends GMSAsyncTask<Void, Void, Void> {

		public HttpClientClosingTask(int priority) {
			super(priority, HttpClientClosingTask.class.getName());
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
	                LoggerUtils.error("HttpUtils.closeConnManager()", e);
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
                 if (executionCount <= getRetryCount() && !closeConnManager) {
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
            	 LoggerUtils.debug("Invoking super class retry request after " + exception.getClass().getName());
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
