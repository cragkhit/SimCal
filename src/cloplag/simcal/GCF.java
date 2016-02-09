package cloplag.simcal;
import java.util.ArrayList;

/*** A GCF file - contains many clone classes
 * @author cragkhit
 *
 */
public class GCF {
	/***
	 * Initialize the clone class array
	 */
	public GCF() {
		this.cloneClasses = new ArrayList<CloneClass>();
	}
	
	private ArrayList<CloneClass> cloneClasses;
	
	public void addCloneClass(CloneClass cc) {
		this.cloneClasses.add(cc);
	}
	
	public int getCloneClassSize() {
		return this.cloneClasses.size();
	}

	public ArrayList<CloneClass> getCloneClasses() {
		return cloneClasses;
	}

	public void setCloneClasses(ArrayList<CloneClass> cloneClasses) {
		this.cloneClasses = cloneClasses;
	}
}
