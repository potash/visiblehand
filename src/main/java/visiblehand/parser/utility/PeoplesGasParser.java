package visiblehand.parser.utility;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;

import visiblehand.entity.UtilityReceipt;
import lombok.Data;
import lombok.Getter;

import com.sun.mail.imap.Utility;

// United Airlines email receipt parser

public @Data class PeoplesGasParser extends UtilityParser {
	private final String fromString = "CustomerService@peoplesgas.ebillservice.net";
	private final String[] subjectStrings = {"Your latest Peoples Gas e-Bill is now available"};
	private final String  bodyString = "";
	
	private final Date parserDate = null;
	private final Date searchDate = new Date(1387047326);	// December 14, 2013
	
	@Getter(lazy = true)
	private final Utility utility = null;

	public UtilityReceipt parse(Message message) throws ParseException,
			MessagingException, IOException {

		UtilityReceipt receipt = new UtilityReceipt();
		String content = getContent(message);
		receipt.setUtility(getUtility());
	
		return receipt;
	}
}
