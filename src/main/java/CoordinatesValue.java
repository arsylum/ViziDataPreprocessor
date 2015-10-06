
public class CoordinatesValue {
	private double 	lat,
					lon;
	
	/**
	 * 
	 * @param lat Latitude
	 * @param lon Longitude
	 */
	public CoordinatesValue(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}
}
