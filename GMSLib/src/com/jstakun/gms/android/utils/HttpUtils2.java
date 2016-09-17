package com.jstakun.gms.android.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;

public class HttpUtils2 {
	
	private static final int SOCKET_TIMEOUT = (int) DateTimeUtils.ONE_MINUTE; //DateTimeUtils.THIRTY_SECONDS;
	private static final Map<String, Integer> httpResponseStatuses = new HashMap<String, Integer>();
	private static final Map<String, String> httpErrorMessages = new HashMap<String, String>();
	
	public String uploadScreenshot(String url, boolean auth, double latitude, double longitude, byte[] file, String filename) {
	    //must implement form multi part data
		return null;
	}
	
	/*private static String processRequest(URL fileUrl, boolean auth, String method, String accept, byte[] content, String contentType, boolean compress, Double latitude, Double longitude) throws IOException {
        InputStream is = null;
        String response = null;
        long start = System.currentTimeMillis();

        try {
            HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            
            conn.setRequestProperty("User-Agent", Locale.getMessage(R.string.app_name) + " HTTP client");
            
            conn.setRequestProperty(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            conn.setRequestProperty(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
            conn.setRequestProperty(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));
            if (latitude != null) {
            	conn.setRequestProperty(Commons.LAT_HEADER, StringUtil.formatCoordE6(latitude));
            }
            if (longitude != null) {
            	conn.setRequestProperty(Commons.LNG_HEADER, StringUtil.formatCoordE6(longitude));
            }
            if (auth) {
                setAuthHeader(conn, fileUrl.toExternalForm().contains(ConfigurationManager.SERVICES_SUFFIX));
                String username = ConfigurationManager.getUserManager().getLoggedInUsername();
                if (StringUtils.isNotEmpty(username)) {
                	conn.setRequestProperty("username", username);
                }                      
            }

            java.util.Locale locale = ConfigurationManager.getInstance().getCurrentLocale();
            if (locale != null) {
                conn.setRequestProperty("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
            }

            conn.setRequestProperty("Accept-Charset", "utf-8");

            if (StringUtils.isNotEmpty(accept)) {
                conn.setRequestProperty("Accept", accept);
            }
            
            if (content != null) {
                conn.setRequestProperty("Content-Length", Integer.toString(content.length));
                
                if (contentType != null) {
                	conn.setRequestProperty("Content-Type", contentType);
                } else {
                	conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                }
                
                if (compress) {
                	conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                }
                
                conn.setDoInput(true);
                conn.setDoOutput(true);
                //Send request
                IOUtils.write(content, conn.getOutputStream());
            } else {
                conn.connect();
            }
            
            int responseCode = conn.getResponseCode();
            httpResponseStatuses.put(fileUrl.toExternalForm(), responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();   
            } else if (responseCode >= 400 ){
                is = conn.getErrorStream();
                LoggerUtils.error("Received http status code " + responseCode + " for url " + fileUrl.toString());   
            } else if (responseCode >= 300 && responseCode < 400) {
            	LoggerUtils.error("Received http status code " + responseCode + " for url " + fileUrl.toString());   
            } else if (responseCode > 200) {
            	LoggerUtils.debug("Received http status code " + responseCode + " for url " + fileUrl.toString());
            }
            
            if (is != null) {
            	response = IOUtils.toString(is, "UTF-8");
            	int length = response.length();
            	if (length > 0) {
            		LoggerUtils.debug("Received " + conn.getContentType() + " document having " + length + " characters");
            	}
            }
            
            LoggerUtils.debug("Request processed with status " + responseCode + " in " + (System.currentTimeMillis()-start) + " millis.");
            
        } catch (Exception e) {
        	LoggerUtils.error(e.getMessage(), e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        
        return response;
    }*/

	public static Integer getResponseCode(String url) {
    	return httpResponseStatuses.remove(url);
    }
	
	public static String getErrorMessage(String url) {
		return httpErrorMessages.remove(url);
	}
	
	private static void setAuthHeader(HttpURLConnection conn, boolean throwIfEmpty) {
    	if (ConfigurationManager.getUserManager().isTokenPresent()) {
    		conn.setRequestProperty(Commons.TOKEN_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_TOKEN));
    		conn.setRequestProperty(Commons.SCOPE_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_SCOPE));
    	} else if (throwIfEmpty) {
    		LoggerUtils.error("Missing authorization token");
    		throw new SecurityException("Missing authorization token");
    	}
    }
	
	public static List<ExtendedLandmark> loadLandmarksList(URL fileUrl, Map<String, String> params, boolean auth, String[] formats) {
    	ObjectInputStream ois = null;
    	List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
    	
    	httpErrorMessages.remove(fileUrl.toExternalForm());
        
    	try {
    		HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("User-Agent", Locale.getMessage(R.string.app_name) + " HTTP client");
            conn.setRequestProperty(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            conn.setRequestProperty(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
            conn.setRequestProperty(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));
            
            if (auth) {
                setAuthHeader(conn, fileUrl.toExternalForm().contains(ConfigurationManager.SERVICES_SUFFIX));
            }
            
            conn.setDoInput(true);
            conn.setDoOutput(true);
            
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();            
            
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            httpResponseStatuses.put(fileUrl.toExternalForm(), responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
            	String contentType = conn.getContentType();
            	if (contentType != null) {
					if (!StringUtils.startsWithAny(contentType, formats)) {
						responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
						throw new IOException("Wrong content format! Expected: " + StringUtils.join(formats," or ") + ", found: " + contentType + " at url: " + fileUrl.toExternalForm());
					}
				} else {
					//throw new IOException("Missing content type! Expected: " + format + " at url: " + uri.toString());
					LoggerUtils.debug("Missing content type! Expected: " + StringUtils.join(formats," or ") + " at url: " + fileUrl.toExternalForm());
				}
            	LoggerUtils.debug("Received " + conn.getContentType() + " content"); 
            	if (conn.getContentType().indexOf("deflate") != -1) {
					ois = new ObjectInputStream(new InflaterInputStream(conn.getInputStream(), new Inflater(false)));
				} else if (conn.getContentType().indexOf("application/x-java-serialized-object")  != -1) {
					ois = new ObjectInputStream(conn.getInputStream());
				} 
            	int size = 0;
            	if (ois != null) {
        			size = ois.readInt();
        			LoggerUtils.debug("Reading " + size + " landmarks");
        			if (size > 0) {
        				for(int i = 0;i < size;i++) {
        					try {
        						ExtendedLandmark landmark = new ExtendedLandmark(); 
        						landmark.readExternal(ois);
        						landmarks.add(landmark);
        					} catch (IOException e) {
        						responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
    							throw new IOException("Unable to create object input stream from " + fileUrl.toExternalForm(), e);
        					}
        				}       				
        			}
        		} else {
        			LoggerUtils.error("Object stream is null");
        		}
                LoggerUtils.debug("Received " + conn.getContentType() + " file has " + size + " landmarks");
            } else {
				LoggerUtils.error(fileUrl.toExternalForm() + " loading error: " + responseCode); // + " " + httpResponse.getStatusLine().getReasonPhrase());
				httpErrorMessages.put(fileUrl.toExternalForm(), handleHttpStatus(responseCode));
			}
        } catch (Exception e) {
        	if (StringUtils.equals(e.getMessage(), "Connection already shutdown")) {
        		LoggerUtils.debug("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
        	} else {
        		LoggerUtils.error("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
        	}
        	httpErrorMessages.put(fileUrl.toExternalForm(), handleHttpException(e));
        } finally {
        	try {
        		if (ois != null) {
        			ois.close();
        		}
        	} catch (Exception e) {
        		LoggerUtils.error(e.getMessage(), e);
        	}
        }
        
        return landmarks;
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
	
	private static String handleHttpStatus(int status) {
        if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return Locale.getMessage(R.string.Authz_error);
        } else if (status == HttpURLConnection.HTTP_CONFLICT) {
            return Locale.getMessage(R.string.Venue_exists_error);
        } else if (status == HttpURLConnection.HTTP_FORBIDDEN) {
        	return Locale.getMessage(R.string.Forbidden_connection_error);
        } else if (status == HttpURLConnection.HTTP_UNAVAILABLE) {
        	return Locale.getMessage(R.string.Service_unavailable_error); 
        }  else {
            return Locale.getMessage(R.string.Http_error, Integer.toString(status));
        }
    }
	
	private static String getQuery(Map<String, String> params) throws UnsupportedEncodingException
	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (Map.Entry<String, String> pair : params.entrySet())
	    {
	        if (first)
	            first = false;
	        else
	            result.append("&");

	        result.append(URLEncoder.encode(pair.getKey(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    }

	    return result.toString();
	}
}
