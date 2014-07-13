/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;


/**
 *
 * @author jstakun
 */

public class EventfulReader extends AbstractSerialReader {

    @Override
	protected String getUri() {
		return "eventfulProvider";
	}
}
