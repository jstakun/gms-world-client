/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

/**
 *
 * @author jstakun
 */
public class FlickrReader extends AbstractSerialReader {
	@Override
	protected String getUri() {
		return "flickrProvider";
	}
}
