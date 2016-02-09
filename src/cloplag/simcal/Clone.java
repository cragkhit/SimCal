package cloplag.simcal;
import java.util.ArrayList;

/***
 * A clone - can contains many fragment
 * 
 * @author cragkhit
 *
 */
public class Clone {
	public Clone() {
		this.fragmentList = new ArrayList<Fragment>();
	}

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
