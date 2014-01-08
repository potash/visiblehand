package visiblehand.parser.utility;

import org.junit.Test;

import visiblehand.EbeanTest;
import visiblehand.entity.utility.Utility;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;

public class UtilityTest extends EbeanTest {

	@Test
	public void test() {
		Utility comed = new Utility();
		comed.setName("Commonwealth Edison");
		Ebean.save(comed);
		
		Utility peoples = new Utility();
		peoples.setName("People's Gas");
		Ebean.save(peoples);
		
		SqlQuery writeQuery = Ebean
				.createSqlQuery("call csvwrite('data/csv/utility.csv', 'SELECT * FROM UTILITY')");
		writeQuery.findList();
	}

}
