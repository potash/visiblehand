package visiblehand.parser;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
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
		List<Flight> flights = new ArrayList<Flight>();

		Pattern pattern = Pattern
				.compile("(?<date>(?<day>\\d{2})(?<month>JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)).*\\b"
						//+ "\\s*LV  (?<source>(?:\\w+\\s?)+\\w).*\\b"
						+ "\\s*LV  (?<source>(?:\\w+\\s*)+[A-Z])\\s*(?<time>\\d{1,2}:\\d{2} (AM|PM)) (?<number>\\d+).*\\b"
						+ "\\s*AR  (?<destination>(?:\\w+\\s?)+[A-Z])\\s*(\\d{1,2}:\\d{2} (AM|PM)).*\\b"
						+ "\\s*(?:OPERATED BY (?<operator>(?:\\w+\\s?)+\\w))?");
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			Date date = getDate(messageDate, matcher.group("date"), matcher.group("time"));//matcher.group("month"), matcher.group("day"));
			Airport source = getAirport(matcher.group("source")),
					destination = getAirport(matcher.group("destination"));
			Airline airline = getAirline();
			String operator = matcher.group("operator");
			if (operator != null
					&& !operator.equalsIgnoreCase("American Eagle")) {
				List<Airline> airlines = Ebean.find(Airline.class).where()
						.ieq("name", operator).eq("active", true)
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
			flight.setNumber(Integer.parseInt(matcher.group("number")));
			flights.add(flight);
		}

		return flights;
	}

	protected static Date getDate(Date sentDate, String dateString, String timeString) throws ParseException {
		Calendar cal = Calendar.getInstance();
		cal.setTime(sentDate);
		int sentYear = cal.get(Calendar.YEAR);
		
		DateFormat format = new SimpleDateFormat("ddMMMyyyyhh:mm aa");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = format.parse(dateString + sentYear + timeString);
		if (date.compareTo(sentDate) < 0) {
			date = format.parse(dateString + (sentYear + 1) + timeString);
		}
		return date;
	}

	protected Airport getAirport(String string) throws ParseException {
		string = string.trim();
		string = string.replaceAll("\\s+", " ");
		int index = string.lastIndexOf(' ');
		List<Airport> airports = null;
		airports = Ebean.find(Airport.class).where().ieq("city", string).findList();
		if (airports == null || airports.size() == 0) {
			String string1 = "", string2 = "";
			string2 = string.substring(index + 1);
			string1 = string.substring(0, index);

			if (string2.length() == 3) {
				// guessing its an airport code
				airports = Ebean.find(Airport.class).where()
						.istartsWith("city", string1).icontains("code", string2)
						.findList();
			} else if (string2.length() == 4) {
				airports = Ebean.find(Airport.class).where()
						.istartsWith("city", string1).icontains("ICAO", string2)
						.findList();
			}

			if (airports == null || airports.size() == 0) {
				airports = Ebean.find(Airport.class).where()
						.istartsWith("city", string1).icontains("name", string2)
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
			//if more than one, look for one that aa actually flies to!
			for (Airport airport : airports) {
				List<Route> routes = Ebean.find(Route.class).where()
						.eq("airline", getAirline()).eq("source", airport).findList();
				if (routes.size() > 0)
					return airport;
			}
			return airports.get(0);
		}
	}
}
