package visiblehand.entity;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import visiblehand.VisibleHandTest;

import com.avaje.ebean.Ebean;

public class EquipmentTest extends VisibleHandTest {

	// How many equipment have fuel data?
	@Test
	public void test() {
		List<Equipment> equipment = Ebean.find(Equipment.class).findList();
		int count = 0; // the number of equipment with fuel data;
		for (Equipment e : equipment) {
			if (e.getAllFuelData().size() > 0) {
				count++;
			} else {
				System.out.println(e);
			}
		}
		System.out.println("Fuel data exists for " + count + " / "
				+ equipment.size() + " = " + (double) count / equipment.size()*100
				+ "% of equipment");
	}
	
	@Test
	public void test2() {
	}
}
