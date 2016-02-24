package cloplag.simcal;

import java.util.ArrayList;

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
	 * Add new fragment into the list sorted by size descending
	 * @param f new fragment to be added.
	 */
	public void add(Fragment f) {
		//System.out.println("Adding: " + f.getFile() + "," + f.getStartLine()
		//  		+ "," + f.getEndLine() + "," + f.getSize());
		if (fList.isEmpty())
			fList.add(f);
		else {
			boolean done = false;
			for (int i=0; i<fList.size(); i++) {
				Fragment frag = fList.get(i);
				// check duplication with the existing ones.
				// we won't re-add it if it's already there.
				if (f.equals(frag)) {
					done = true;
					break;
				}
				if (f.getSize() > frag.getSize()) {
					fList.add(i, f);
					done = true;
					break; // found the place, quit
				}
			}
			if (!done) 
				fList.add(f); // last one, least size
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
