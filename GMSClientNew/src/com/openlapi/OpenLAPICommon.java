/*
 * Copyright ThinkTank Maths Limited 2006 - 2008
 *
 * This file is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this file. If not, see <http://www.gnu.org/licenses/>.
 */
package com.openlapi;

/**
 * Some constants and common methods that are used in various places of OpenLAPI.
 */
final class OpenLAPICommon {

	/**
	 * Earth equatorial radius
	 * 
	 * @see http://en.wikipedia.org/wiki/Earth_radius
	 */
	public final static double EARTH_RADIUS = 6378135.0;

	/**
	 * WGS flattening SetPrecision[1/298.257223563, 15]
	 * 
	 * @see http://en.wikipedia.org/wiki/Reference_ellipsoid
	 */
	public final static double FLATTENING = 0.00335281066474748;

	/**
	 * Inverse circumference of earth if the Earth had a radius equal to it's semi minor
	 * axis, times 360.
	 */
	public static final double INV_MINOR_CIRCUMFERENCE =
			360d / (2d * Math.PI * 6356752.3142);

	/**
	 * WGS semi major axis (equatorial axis).
	 * 
	 * @see http://en.wikipedia.org/wiki/Reference_ellipsoid
	 */
	public final static double SEMI_MAJOR = 6378137.0;

	/**
	 * WGS semi minor axis (polar axis)
	 * 
	 * @see http://en.wikipedia.org/wiki/Reference_ellipsoid
	 */
	public final static double SEMI_MINOR = 6356752.3142;
}
