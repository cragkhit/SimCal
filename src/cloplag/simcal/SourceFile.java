package cloplag.simcal;
public class SourceFile {
	private String filePath;
	private int lines;
	
	public SourceFile(String file, int line) {
		this.setFilePath(file);
		this.setLines(line);
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public int getLines() {
		return lines;
	}

	public void setLines(int lines) {
		this.lines = lines;
	}
}
