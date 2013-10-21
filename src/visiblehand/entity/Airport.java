package visiblehand.entity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.ToString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;

@Entity
@ToString(of = { "id", "name" })
public @Data class Airport {
	@Id
	private int id;
	private String name;
	private String city;
	private String country;
	private String code;
	private String ICAO;
	private double latitude;
	private double longitude;
	private int altitude;
	private double timezone;
	private char DST;

	public static List<Airport> readAirports() throws JsonProcessingException,
			IOException {
		List<Airport> airports = new ArrayList<Airport>();

		MappingIterator<Airport> it = new CsvMapper().readerWithSchemaFor(
				Airport.class).readValues(new File("data/airports.dat"));
		while (it.hasNextValue()) {
			airports.add(it.nextValue());
		}
		return airports;
	}

	// return distance between airports in nautical miles using haversine
	// formula
	public static double getDistance(Airport a1, Airport a2) {
		if (a1 == null || a2 == null) {
			return 0;
		}
		double lat1 = Math.toRadians(a1.getLatitude()), lat2 = Math
				.toRadians(a2.getLatitude()), lon1 = Math.toRadians(a1
				.getLongitude()), lon2 = Math.toRadians(a2.getLongitude());

		double a = Math.pow(Math.sin((lat2 - lat1) / 2), 2) + Math.cos(lat1)
				* Math.cos(lat2) * Math.pow(Math.sin((lon2 - lon1) / 2), 2);

		// haversine formula for central angle
		double angle = 2 * Math.asin(Math.min(1, Math.sqrt(a)));

		// one degree is 60 nautical miles
		return 60 * Math.toDegrees(angle);
	}
}
