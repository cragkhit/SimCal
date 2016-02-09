package cloplag.simcal;

import java.util.ArrayList;
import java.util.Iterator;

/***
 * List of fragments
 * @author cragkhit
 *
 */
public class FragmentList {
	private ArrayList<Fragment> fList;

	public FragmentList() {
		fList = new ArrayList<Fragment>();
	}

	/***
	 * Add new fragment into the list sorted by size ascending
	 * @param f new fragment to be added.
	 */
	public void add(Fragment f) {
		//System.out.println("Adding: " + f.getFile() + "," + f.getStartLine()
		//  		+ "," + f.getEndLine() + "," + f.getSize());
		if (fList.isEmpty())
			fList.add(f);
		else {
			boolean added = false;
			for (int i=0; i<fList.size(); i++) {
				Fragment frag = fList.get(i);
				if (f.getSize() > frag.getSize()) {
					fList.add(0, f);
					added = true;
					break; // found the place, quit
				}
			}
			if (!added) 
				fList.add(f);
		}
		// print(); 
		// System.out.println("Size = " + fList.size());
	}
	
	public ArrayList<Fragment> getList() {
		return fList;
	}
	
	public int size() {
		return fList.size();
	}
	
	public Fragment getItem(int i) {
		return fList.get(i);
	}
	
	public void print() {
		for (int i=0; i<fList.size(); i++) {
			Fragment frag = fList.get(i);
			System.out.println(frag.getFile() + "," + frag.getStartLine() + ","
					+ frag.getEndLine() + "," + frag.getSize());
		}
	}
}
