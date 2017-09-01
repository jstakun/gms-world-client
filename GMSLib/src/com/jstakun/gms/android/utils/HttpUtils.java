package com.jstakun.gms.android.utils;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.spongycastle.util.encoders.Base64;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.lib.R;

public class HttpUtils {
	
	private static final int SOCKET_TIMEOUT = (int) DateTimeUtils.ONE_MINUTE; //DateTimeUtils.THIRTY_SECONDS;
	private static final String FORM_ENCODING = "application/x-www-form-urlencoded;charset=UTF-8";
	private static final Map<String, Integer> httpResponseStatuses = new HashMap<String, Integer>();
	private static final Map<String, String> httpErrorMessages = new HashMap<String, String>();
	private static final Map<String, String> httpHeaders = new HashMap<String, String>();
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; GMS World; http://www.gms-world.net)";
	
	
	private boolean aborted = false;
	
	public void close() {
		aborted = true;
	}
	
	public HttpUtils() {
		aborted = false;
	}
	
	public String uploadScreenshot(String fileUrl, boolean auth, Double latitude, Double longitude, byte[] file, String filename) {
	    InputStream is = null;
	    String response = null;
	    
	    final String attachmentName = "screenshot";
	    final String crlf = "\r\n";
	    final String twoHyphens = "--";
	    final String boundary =  "*****";
		
		try {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            
            conn.setRequestProperty("User-Agent", USER_AGENT);
            
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
                setAuthHeader(conn, fileUrl.contains(ConfigurationManager.SERVICES_SUFFIX));
                String username = ConfigurationManager.getUserManager().getLoggedInUsername();
                if (StringUtils.isNotEmpty(username)) {
                	conn.setRequestProperty("username", username);
                }                      
            }
            
            conn.setRequestProperty("Accept-Encoding", "gzip");
            
            if (!aborted) {
            	//write file
            	conn.setDoOutput(true);
            	conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            	DataOutputStream request = new DataOutputStream(conn.getOutputStream());

            	request.writeBytes(twoHyphens + boundary + crlf);
            	request.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\"" + filename + "\"" + crlf);
            	request.writeBytes(crlf);
            		
            	IOUtils.write(file, request);
            		
            	request.writeBytes(crlf);
            	request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
            		
            	request.flush();
            	request.close();
            }
            
            if (! aborted) {
            	int responseCode = conn.getResponseCode();
            	httpResponseStatuses.put(fileUrl, responseCode);

            	if (responseCode == HttpURLConnection.HTTP_OK) {
            		if (conn.getContentType().indexOf("gzip") != -1) {
            			is = new GZIPInputStream(conn.getInputStream());
            		} else {
            			is = conn.getInputStream();
            		}
            	} else {
                	is = conn.getErrorStream();
                	LoggerUtils.error(fileUrl + " loading error: " + responseCode); 
        			httpErrorMessages.put(fileUrl, handleHttpStatus(responseCode));
            	}
                
            	if (is != null) {
            		//Read response
            		response = IOUtils.toString(is, "UTF-8");
            	}
            	
            	int sent = file.length;
            	int received = 0;
            	if (response != null) {
            		received = response.getBytes().length;
            	}
            	if (sent > 0 || received > 0) {
            		ConfigurationManager.getAppUtils().increaseCounter(sent, received);
            	}
            }
		} catch (Throwable e) {
            LoggerUtils.debug("HttpUtils.uploadScreenshot() exception: " + e.getMessage(), e);
            httpErrorMessages.put(fileUrl, handleHttpException(e));
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }
        }
		
		return response;
	}
	
	public byte[] loadFile(String url, boolean auth, String userpassword, String format) {
		return processRequest(url, auth, userpassword, "GET", null, null, null, true, format, null, null);
	}
	
	public String sendPostRequest(String url, Map<String, String> postParams, boolean auth) {
		try {
			byte[] response = processRequest(url, auth, null, "POST", null, getQuery(postParams).getBytes(), FORM_ENCODING, true, null, null, null, "key", "name", "hash");
			if (response != null && response.length > 0) {
				return new String(response, "UTF-8");
			} else {
				return null;
			}
		} catch (Exception e) {
			LoggerUtils.error(e.getMessage(), e);
			return null;
		}
	}
	
	private byte[] processRequest(String fileUrl, boolean auth, String userpassword, String method, String accept, byte[] content, String contentType, boolean compress, String format, Double latitude, Double longitude, String... headersToRead) {
        InputStream is = null;
        byte[] response = null;
        long start = System.currentTimeMillis();
        HttpURLConnection conn = null;
        
        httpErrorMessages.remove(fileUrl);

        try {
            conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setReadTimeout(SOCKET_TIMEOUT);
            
            conn.setRequestProperty("User-Agent", USER_AGENT);
            
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
            	if (userpassword != null) {
            		//username : password
                    String encodedAuthorization = Base64.toBase64String(userpassword.getBytes());
                    conn.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
            	} else {
            		setAuthHeader(conn, fileUrl.contains(ConfigurationManager.SERVICES_SUFFIX));
            		String username = ConfigurationManager.getUserManager().getLoggedInUsername();
            		if (StringUtils.isNotEmpty(username)) {
            			conn.setRequestProperty("username", username);
            		}       
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
            
            if (!aborted) {
            	if (content != null) {
            		conn.setRequestProperty("Content-Length", Integer.toString(content.length));
                
            		if (contentType != null) {
            			conn.setRequestProperty("Content-Type", contentType);
            		} else {
            			conn.setRequestProperty("Content-Type", FORM_ENCODING);
            		}
                
            		if (compress) {
            			conn.setRequestProperty("Accept-Encoding", "gzip");
            		}
                
            		conn.setDoInput(true);
                	conn.setDoOutput(true);
                	//Send request
                	IOUtils.write(content, conn.getOutputStream());
            	} else {
            		conn.connect();
            	}
            }
            
            if (!aborted) {
            	int responseCode = conn.getResponseCode();
            	httpResponseStatuses.put(fileUrl, responseCode);

            	if (responseCode == HttpURLConnection.HTTP_OK) {
            		if (format != null) {
        				if (!StringUtils.contains(conn.getContentType(), format)) {
        					responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
        					httpResponseStatuses.put(fileUrl, responseCode);
        					throw new IOException("Wrong content format! Expected: " + format + ", found: " + conn.getContentType() + " at url: " + fileUrl);
        				}
        			} 
            		
            		if (conn.getContentType().indexOf("gzip") != -1) {
            			is = new GZIPInputStream(conn.getInputStream());
            		} else {
            			is = conn.getInputStream();
            		}
            	} else if (responseCode > 399) {
                	is = conn.getErrorStream();
                	LoggerUtils.error(fileUrl + " loading error: " + responseCode); 
        			httpErrorMessages.put(fileUrl, handleHttpStatus(responseCode));
            	} else {
            		LoggerUtils.debug(fileUrl + " loading response code: " + responseCode); 
            	}
                
            	if (is != null) {
            		//Read response
            		response = IOUtils.toByteArray(is);
            		int length = response.length;
            		if (length > 0) {
            			LoggerUtils.debug("Received " + conn.getContentType() + " document having " + length + " characters");
            		}
            	}
            	
            	int sent = 0;
            	if (content != null )
            	{
            		sent = content.length;
            	}
            	int received = 0;
            	if (response != null) {
            		received = response.length;
            	}
            	if (sent > 0 || received > 0) {
            		ConfigurationManager.getAppUtils().increaseCounter(sent, received);
            	}
            	
            	if (headersToRead != null && headersToRead.length > 0) {
            		readHeaders(conn, fileUrl, headersToRead);
            	}
            
            	LoggerUtils.debug("Request processed with status " + responseCode + " in " + (System.currentTimeMillis()-start) + " millis from " + fileUrl  + ".");
            }
        } catch (Exception e) {
        	LoggerUtils.debug(e.getMessage(), e);
        	httpErrorMessages.put(fileUrl, handleHttpException(e));
        } finally {
            if (is != null) {
            	try {
            		is.close();
            	} catch (Exception e) {
            		LoggerUtils.error(e.getMessage(), e);
            	}
            	if (conn != null) {
            		conn.disconnect();
            	}
            }
            if (conn != null) {
        		conn.disconnect();
        	}
        }
        
        return response;
    }

	public List<ExtendedLandmark> loadLandmarkList(String fileUrl, Map<String, String> params, boolean auth, String[] formats) {
    	ObjectInputStream ois = null;
    	HttpURLConnection conn = null;
    	List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
    	String redirectUrl = null;
    	
    	httpErrorMessages.remove(fileUrl);
        
    	try {
    		if (ServicesUtils.isNetworkActive()) { 	
    			conn = (HttpURLConnection) new URL(fileUrl).openConnection();
    			conn.setInstanceFollowRedirects(false);
    			conn.setRequestMethod("POST");
    			conn.setConnectTimeout(SOCKET_TIMEOUT);
    			conn.setReadTimeout(SOCKET_TIMEOUT);
            	conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            	conn.setRequestProperty("User-Agent", USER_AGENT);
            	conn.setRequestProperty(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
            	conn.setRequestProperty(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));
            	conn.setRequestProperty(Commons.USE_COUNT_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.USE_COUNT));
            
            	if (auth) {
            		setAuthHeader(conn, fileUrl.contains(ConfigurationManager.SERVICES_SUFFIX));
            	}
            	
            	java.util.Locale locale = ConfigurationManager.getInstance().getCurrentLocale();
                if (locale != null) {
                    conn.setRequestProperty("Accept-Language", locale.getLanguage() + "-" + locale.getCountry());
                }
            
            	conn.setDoInput(true);
            	conn.setDoOutput(true);
            
            	conn.setRequestProperty("Content-Type", FORM_ENCODING);
            
            	String queryString = getQuery(params);
            	OutputStream os = conn.getOutputStream();
            	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            	writer.write(queryString);
            	writer.flush();
            	writer.close();
            	os.close();            
            
            	if (!aborted) {
            		conn.connect();
            
            		int responseCode = conn.getResponseCode();
            		httpResponseStatuses.put(fileUrl, responseCode);

            		if (responseCode == HttpURLConnection.HTTP_OK && !aborted) {
            			String contentType = conn.getContentType();
            			if (contentType != null) {
            				if (!StringUtils.startsWithAny(contentType, formats)) {
            					responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
            					throw new IOException("Wrong content format! Expected: " + StringUtils.join(formats," or ") + ", found: " + contentType + " at url: " + fileUrl);
            				}
            			} else {
            				//throw new IOException("Missing content type! Expected: " + format + " at url: " + uri.toString());
            				LoggerUtils.debug("Missing content type! Expected: " + StringUtils.join(formats," or ") + " at url: " + fileUrl);
            			}
            			ConfigurationManager.getAppUtils().increaseCounter(queryString.getBytes().length, conn.getContentLength());
            			if (conn.getContentLength() > 0) {
            				if (conn.getContentType().indexOf("deflate") != -1) {
            					ois = new ObjectInputStream(new InflaterInputStream(conn.getInputStream(), new Inflater(false)));
            				} else if (conn.getContentType().indexOf("application/x-java-serialized-object")  != -1) {
            					ois = new ObjectInputStream(conn.getInputStream());
            				} 
            			} else {
            				LoggerUtils.debug("Received no content from " + fileUrl); 	
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
            							throw new IOException("Unable to create object input stream from " + fileUrl, e);
            						}
            					}       				
            				}
            			} else {
            				LoggerUtils.error("Object stream is null");
            			}
            			LoggerUtils.debug("Received " + size + " landmarks from " + fileUrl);
            		} else if ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM
            				|| responseCode == HttpURLConnection.HTTP_SEE_OTHER) && !aborted) {
            			redirectUrl = conn.getHeaderField("Location");
            		} else if (!aborted) {
            			LoggerUtils.error(fileUrl + " loading error response: " + responseCode); 
            			httpErrorMessages.put(fileUrl, handleHttpStatus(responseCode));
            			String contentType = conn.getContentType();
            			String file = null;
            			int length = conn.getContentLength();
            			try {
            				file = IOUtils.toString(conn.getErrorStream(), "UTF-8");
            				length = file.length();
            			} catch (Exception e) {
            				LoggerUtils.debug("Reading error response exception:", e);
            			} 
                    	if (length > 0) {
                    		LoggerUtils.debug("Received " + contentType + " document with " + length + " characters:\n" + file);
                    	}
            		} else {
            			LoggerUtils.debug("Request to " + fileUrl + " has been aborted");
            		}
            	} else {
        			String errorMessage = Locale.getMessage(R.string.Network_connection_error_title);
        			httpErrorMessages.put(fileUrl, errorMessage);
        			LoggerUtils.error("HttpUtils.loadLandmarkList() network exception: " + errorMessage);
        		}
    		} else {
        		String errorMessage = Locale.getMessage(R.string.Network_connection_error_title);
        		httpErrorMessages.put(fileUrl, errorMessage);
        		LoggerUtils.error("HttpUtils.loadLandmarkList() network exception: " + errorMessage);
        	}
        } catch (Exception e) {
        	if (StringUtils.equals(e.getMessage(), "Connection already shutdown")) {
        		LoggerUtils.debug("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
        	} else {
        		LoggerUtils.error("HttpUtils.loadLandmarkList() exception: " + e.getMessage(), e);
        	}
        	httpErrorMessages.put(fileUrl, handleHttpException(e));
        } finally {
        	try {
        		if (ois != null) {
        			ois.close();
        		}
        	} catch (Exception e) {
        		LoggerUtils.error(e.getMessage(), e);
        	}
        	if (conn != null) {
        		conn.disconnect();
        	}
        }
        
    	if (redirectUrl != null && !aborted) {
    		LoggerUtils.debug("Sending redirect to: " + redirectUrl);
    		return loadLandmarkList(redirectUrl, params, auth, formats);
    	} else {
    		return landmarks;
    	}
    }
	
	public int getResponseCode(String url) {
    	Integer status =  httpResponseStatuses.remove(url);
    	if (status == null) {
    		return -1;
    	} else {
    		return status.intValue();
    	}
    }
	
	public String getResponseErrorMessage(String url) {
		return httpErrorMessages.remove(url);
	}
	
	public String getHeader(String url, String headerName) {
		return httpHeaders.remove(url + "->" + headerName);
	}
	
	private static void readHeaders(HttpURLConnection conn, String url, String... headerNames) {
		for (int i = 0; i < headerNames.length; i++) {
            String headerName = headerNames[i];
            String headerValue = conn.getHeaderField(headerName);
            httpHeaders.put(url + "->" + headerName, headerValue);
		}
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
	
	private static String getQuery(Map<String, String> params) 
	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    try {
	    	for (Map.Entry<String, String> pair : params.entrySet())
	    	{
	    		if (first) {
	    			first = false;
	    		} else {
	    			result.append("&");
	    		}
	    		
	    		result.append(URLEncoder.encode(pair.getKey(), "UTF-8"));
	    		result.append("=");
	    		result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    	}
	    } catch (Exception e) {
	    	LoggerUtils.error(e.getMessage(), e);
	    }

	    return result.toString();
	}
}
