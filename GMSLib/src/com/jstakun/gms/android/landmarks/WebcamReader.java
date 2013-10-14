/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class WebcamReader extends AbstractSerialReader  {

    @Override
	protected String getUrl() {
		return ConfigurationManager.getInstance().getServicesUrl() + "webcamProvider";
	}
}
