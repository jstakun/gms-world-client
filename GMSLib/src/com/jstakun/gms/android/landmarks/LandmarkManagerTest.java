package com.jstakun.gms.android.landmarks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.openlapi.QualifiedCoordinates;

public class LandmarkManagerTest {

	private static final String layer1 = "testing"; 
	
	@Test
	public void test() {
		LandmarkManager lm = new LandmarkManager();
		
	    QualifiedCoordinates qc = new QualifiedCoordinates();
	    qc.setLatitude(52.25);
	    qc.setLongitude(20.95);
	    
	    lm.getLandmarkStoreLayer(layer1).add(LandmarkFactory.getLandmark("testname", "testdesc", qc, layer1, System.currentTimeMillis()));
	    
	    assertEquals("Wrong layer size", lm.getLandmarkStoreLayer(layer1).size(), 1);
	    
	}

}
