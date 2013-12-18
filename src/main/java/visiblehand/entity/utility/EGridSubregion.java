package visiblehand.entity.utility;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.Getter;

@Entity
public @Data class EGridSubregion {
	@Id
	private String id;
	private String name;
	
	// emission factors are in pound per megawatt-hour
	private double CO2EmissionRate;
	private double CH4EmissionRate;
	private double N2OEmissionRate;
}
