package visiblehand.parser;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;

import lombok.Data;
import lombok.Getter;
import visiblehand.entity.Airline;
import visiblehand.entity.Airport;
import visiblehand.entity.Flight;
import visiblehand.entity.Route;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;

// American Airlines email receipt parser

public @Data class AAParser extends AirParser {
	private final String fromString = "notify@aa.globalnotifications.com";
	private final String subjectString = "E-Ticket Confirmation-";
	private final String bodyString = "";
	
	private boolean active = true;

	@Getter(lazy=true)
	private final Airline airline = Ebean.find(Airline.class, 24);

	public AirReceipt parse(Message message) throws ParseException,
			MessagingException, IOException {
		AirReceipt receipt = new AirReceipt();
		receipt.setFlights(getFlights(getContent(message),
				message.getSentDate()));
		receipt.setAirline(getAirline());
		receipt.setConfirmation(getConfirmation(message.getSubject()));
		receipt.setDate(message.getSentDate());
		
		return receipt;
	}

	protected static String getConfirmation(String subject) {
		return subject.substring(22, 28);
	}

	protected List<Flight> getFlights(String content, Date messageDate)
			throws ParseException {
		// try {
		List<Flight> flights = new ArrayList<Flight>();

		Pattern pattern = Pattern
				.compile("(\\d{2})(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC).*\\b"
						+ "\\s*LV  ((?:\\w+\\s?)+\\w).*\\b"
						+ "\\s*AR  ((?:\\w+\\s?)+\\w).*\\b"
						+ "\\s*(?:OPERATED BY ((?:\\w+\\s?)+\\w))?");
		Matcher matcher = pattern.matcher(content);
		// groups are month, day, from airport, to airport
		while (matcher.find()) {
			Date date = getDate(messageDate, matcher.group(1), matcher.group(2));
			Airport source = getAirport(matcher.group(3)), destination = getAirport(matcher
					.group(4));
			Airline airline = getAirline();
			String operator = matcher.group(5);
			if (operator != null
					&& !operator.equalsIgnoreCase("American Eagle")) {
				List<Airline> airlines = Ebean.find(Airline.class).where()
						.ieq("name", matcher.group(5)).eq("active", true)
						.findList();
				if (airlines.size() > 0) {
					// TODO don't pick an airline with zero routes
					airline = airlines.get(0);
				}
			}
			Route route = Ebean.find(Route.class).where()
					.eq("airline", airline).eq("source", source)
					.eq("destination", destination).findUnique();
			// TODO: if route doesn't exist, add it!
			Flight flight = new Flight();
			flight.setDate(date);
			flight.setRoute(route);
			flight.setAirline(airline);
			flights.add(flight);
		}

		return flights;
	}

	protected static Date getDate(Date sentDate, String month, String day) {
		// TODO Auto-generated method stub
		return null;
	}

	protected static Airport getAirport(String string) throws ParseException {
		string = string.trim();
		int index = string.lastIndexOf(' ');
		List<Airport> airports = null;
		if(index == -1) {
			if (string.length() == 3) {
				airports = Ebean.find(Airport.class).where().eq("code", string).findList();
			} else if (string.length() == 4) {
				airports = Ebean.find(Airport.class).where().eq("ICAO", string).findList();
			} else {
				airports = Ebean.find(Airport.class).where().istartsWith("city", string).findList();
			}
		} else if (index > 0) {
			String city = "", airport = "";
			airport = string.substring(index + 1);
			city = string.substring(0, index);

			if (airport.length() == 3) {
				// guessing its an airport code
				airports = Ebean.find(Airport.class).where()
						.istartsWith("city", city).icontains("code", airport)
						.findList();
			} else if (airport.length() == 4) {
				airports = Ebean.find(Airport.class).where()
						.istartsWith("city", city).icontains("ICAO", airport)
						.findList();
			}

			if (airports == null || airports.size() == 0) {
				airports = Ebean.find(Airport.class).where()
						.istartsWith("city", city).icontains("name", airport)
						.findList();
			}
		}

		// if no match just try Levenshtein distance on airport name
		if (airports.size() == 0) {
			String sql = "select id, name, city, country, code, icao as ICAO, latitude, longitude, altitude, timezone, dst as DST "
					+ "from airport "
					+ "order by levenshtein(upper(replace(name, ' Intl', '')), '" + string + "') asc limit 1";
			RawSql rawSql = RawSqlBuilder.parse(sql).create();
			Query<Airport> query = Ebean.find(Airport.class);
			query.setRawSql(rawSql);
			Airport airport = query.findUnique();
			if (airport == null)
				throw new ParseException("Airport not found: " + string, 0);
			return airport;
		} else if (airports.size() == 1) {
			return airports.get(0);
		} else {
			//System.out.println(airports);
			// if there is more than one, prefer one that has an ICAO?
			List<Airport> filtered = Ebean.filter(Airport.class)
					.ne("ICAO", "").filter(airports);
			// then pick the first (arbitrary)
			if (filtered.size() >= 1) {
				return filtered.get(0);
			} else {
				return airports.get(0);
			}
		}
	}
}
