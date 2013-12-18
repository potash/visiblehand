package visiblehand.entity.utility;

import java.util.Date;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

import lombok.Data;
import lombok.Getter;
import visiblehand.entity.Emission;
import visiblehand.entity.ZipCode;

@Embeddable
public @Data class Electricity /*implements Emission*/ {
	private Date date;
	//private Utility utility;
	private Double cost;
	private Double energy;
	//private ZipCode zipCode;
//	
//	@Transient
//	@Getter(lazy = true)
//	private final EGridSubregion eGridSubregion = getZipCode().getSubregion();
//	
//	@Transient
//	@Getter(lazy = true)
//	private final double CO2 = getEnergy() * getEGridSubregion().getCO2EmissionRate();
}
