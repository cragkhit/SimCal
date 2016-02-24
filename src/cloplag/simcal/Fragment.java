package cloplag.simcal;
/***
 * A fragment of cloned clode
 * @author cragkhit
 *
 */
public class Fragment {
	/*** the file name */
	private String file;
	/*** start line of clone fragment */
	private int startLine;
	/*** end line of clone fragment */
	private int endLine;
	
	/***
	 * Check overlapping between 2 fragments and return number of non-overlapped lines, 
	 * also merge the given overlapped fragments to this one. 
	 * @param f the checking fragment
	 * @return number of non-overlapped lines, -1 is not overlap at all.
	 */
	public int mergeIfOverlap(Fragment f) {
		// System.out.println("This: " + this.getStartLine() + ": " + this.getEndLine());
		// System.out.println("f   : " + f.getStartLine() + ": " + f.getEndLine());

		// 1. f is a subset of this fragment
		// this:  -----------------------
		// f   :       -------------
		if (f.getStartLine() >= this.getStartLine() && f.getEndLine() <= this.getEndLine()) 
		{
			// System.out.println("Opt 1");
			return 0;
		}
		// 2. f is a superset of this fragment
		// this:      --------------
		// f   :  ****--------------**********
		else if (f.getStartLine() <= this.getStartLine() && f.getEndLine() >= this.getEndLine())
		{
//			System.out.println("Opt 2");
			this.startLine = f.getStartLine();
			this.endLine = f.getEndLine();
			return (this.startLine - f.getStartLine()) + (f.getEndLine() - this.getEndLine());
		}
		// 3. overlap after this fragment starts
		// this: *****----------
		// f   :      ----------******
		else if (f.getStartLine() >= this.getStartLine() 
				&& f.getStartLine() <= this.getEndLine())
		{
//			System.out.println("Opt 3");
			this.endLine = f.getEndLine();
			return (f.getStartLine() - this.startLine) + (f.getEndLine() - this.getEndLine());
		}
		// 4. overlap before this fragment starts
		// this:      ---------******
		// f   : *****---------
		else if (this.getStartLine() <= f.getEndLine() 
				&& f.getEndLine() <= this.getEndLine())
		{
//			System.out.println("Opt 4");
			this.startLine = f.getStartLine();
			return (this.startLine - f.getStartLine()) + (this.endLine - f.getEndLine());
		}
		// 5. no overlap
		else {
//			System.out.println("Opt 5");
			return -1;
		}
	}
	
	/**
	 * Compare the given fragment with this one by using file name, start line, end line.
	 * @param f the given fragment
	 * @return TRUE if duplicate, FALSE if not.
	 */
	public boolean equals(Fragment f) {
		if (this.file.equals(f.getFile()) && 
				this.startLine == f.getStartLine() &&
				this.endLine == f.getEndLine())
			return true;
		else 
			return false;
	}
	
	public String getInfo() {
		return this.file + "," + this.startLine + "," + this.endLine + "," + this.getSize();
	}
	
	public int getSize() {
		return this.endLine - this.startLine + 1;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}

	public int getEndLine() {
		return endLine;
	}

	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}

}
