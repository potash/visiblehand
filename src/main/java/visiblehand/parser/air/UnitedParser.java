package visiblehand.parser.air;

import java.io.IOException;
import java.text.DateFormat;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import visiblehand.entity.AirReceipt;
import visiblehand.entity.Airline;
import visiblehand.entity.Airport;
import visiblehand.entity.Equipment;
import visiblehand.entity.Flight;
import visiblehand.entity.Route;

import com.avaje.ebean.Ebean;

// United Airlines email receipt parser for older receipts
// subjects of the form "Your United flight confirmation..."

public @Data
class UnitedParser extends AirParser {
	private final String fromString = "united-confirmation@united.com";
	private final String[] subjectStrings = {"Your United flight confirmation"};
	private final String bodyString = "";

	@Getter(lazy = true)
	private final Airline airline = Ebean.find(Airline.class).where().eq("name", "United Airlines").findUnique();
	
	private static final DateFormat dateFormat = getGMTSimpleDateFormat("h:mm a EEE, MMM d, yyyy");
	
	private static Pattern confirmationPattern =  Pattern.compile("Confirmation (?:#|number)[^\\w]*(\\w{6})");
	
	private boolean active = true;

	public AirReceipt parse(Message message) throws ParseException,
			MessagingException, IOException {

		AirReceipt receipt = new AirReceipt(message);
		String content = getContent(message);
		receipt.setFlights(getFlights(content));
		receipt.setAirline(getAirline());
		receipt.setConfirmation(getConfirmation(content));
		receipt.setDate(message.getSentDate());

		return receipt;
	}

	protected static String getConfirmation(String content)
			throws ParseException {
		Matcher matcher = confirmationPattern.matcher(content);
		matcher.find();
		return matcher.group(1);
	}

	protected List<Flight> getFlights(String content) throws ParseException {
		List<Flight> flights = new ArrayList<Flight>();

		Document doc = Jsoup.parse(content);
		Elements flightTable = doc.select("#flightTable");

		if (flightTable.size() == 0) {
			throw new ParseException("No flight table found.", 0);
		}

		Elements flightRows = flightTable.get(0).select(
				"tr:has(td:containsOwn(Flight) ~ td:containsOwn(Depart) ~ td:containsOwn(Arrive) "
				+ "~ td:containsOwn(Cabin) ~ td:containsOwn(Seats))");
		for (Element flightRow : flightRows) {

			while ((flightRow = flightRow.nextElementSibling()) != null) {
				Elements cells = flightRow.select("td");
				if (cells.size() == 1) {
					if (cells.get(0).text().equals("<<< connecting to >>>")) {
						flightRow = flightRow.nextElementSibling();
						cells = flightRow.select("td");
					} else {
						break;
					}
				}
				if (cells.size() == 5) {
					String number = cells.get(0).text(), depart = cells.get(1)
							.text(), arrive = cells.get(2).text(), seatClass = cells
							.get(3).text();

					Flight flight = new Flight();
					try {
						flight.setNumber(new Integer(number
								.split("(United | )")[1]));
					} catch (NumberFormatException e) {
						throw new ParseException(
								"Could not parse flight number: " + number, 0);
					}
					Airport source = Airport.findByCode(depart.substring(0, 3));
					Airport destination = Airport.findByCode(arrive.substring(0, 3));

					Date date = dateFormat.parse(depart.substring(4));
					flight.setDate(date);

					Route route = Route.find(getAirline(), source, destination);

					flight.setRoute(route);
					// next line has equipment, duration, fare code, etc.
					flightRow = flightRow.nextElementSibling();
					if (flightRow != null) {
						Elements infoCells = flightRow.select("td");
						if (infoCells.size() > 0) {
							String info = infoCells.get(0).text();
							String equipment = info
									.split("(Equipment:\\s*|\\W*\\|)")[1];
							flight.setEquipment(Equipment.findByName(equipment));
						}
					}

					flights.add(flight);
				}
			}
		}
		return flights;
	}
}
