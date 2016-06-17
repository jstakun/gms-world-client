package com.jstakun.gms.android.ads;

public class AdsFactory {
	
	private static TapForTapProvider tapForTapProvider = null;
	
	private static AmazonProvider amazonProvider = null;
	
	public static AmazonProvider getAmazonInstance() {
		if (amazonProvider == null) {
			amazonProvider = new AmazonProvider();
		}
		return amazonProvider;
	}
	
	public static TapForTapProvider getTapForTapInstance() {
		if (tapForTapProvider == null) {
			tapForTapProvider = new TapForTapProvider();
		}
		return tapForTapProvider;
	}
}
