package visiblehand.entity.utility;

import java.util.Date;

import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import visiblehand.entity.Receipt;
import visiblehand.entity.ReceiptMessage;
import lombok.Data;

import com.sun.mail.imap.Utility;

public @Data class UtilityReceipt implements Receipt {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	private Utility utility;
	private double cost;
	private Date date;

	@Embedded
	private ReceiptMessage message;
}