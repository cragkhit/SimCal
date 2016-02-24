package cloplag.simcal;
import java.util.ArrayList;

/***
 * A clone - can contains many fragment
 * 
 * @author cragkhit
 *
 */
public class Clone {
	private int id;
	
	public Clone() {
		this.fragmentList = new ArrayList<Fragment>();
	}

	public int getId() { return id; }

	public void setId(int id) { this.id = id; }

	private ArrayList<Fragment> fragmentList;
	
	public void addFragment(Fragment f) {
		this.fragmentList.add(f);
	}
	
	public int getFragmentCount() {
		return this.fragmentList.size();
	}

	public ArrayList<Fragment> getFragmentList() {
		return fragmentList;
	}

	public void setFragmentList(ArrayList<Fragment> fragmentList) {
		this.fragmentList = fragmentList;
	}
}
