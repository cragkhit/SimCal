package cloplag.simcal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.SimpleLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tokenizer.JavaTokenizer;

/***
 * A class to read GCF xml file along with another file containing line numbers
 * of all inspected files to create a similarity measure of Simian clone
 * detection
 * 
 * @author Chaiyong Ragkhitwetsagul, UCL
 * @version 0.3
 * @since 2015-02-20
 */
public class SimCalMain {
	/***
	 * An array list to store list of all files and their line counts.
	 */
	private static HashMap<String, SourceFile> fileHash = new HashMap<String, SourceFile>();
	private static GCF gcf = new GCF();
	private static Logger log;
	private static String linecountFile;
	private static String gcfFile;
	private static String fileLocation;
	// create the Options
	private static Options options = new Options();
	private static String mainFile;
	// private static String logProperties;
	private static String mode;
	private static String calMode;
	private static int runningCount = -1;
	private static FragmentList fragmentList = new FragmentList();

	public static void main(String[] args) {
		// Initialize all the strings
		linecountFile = "";
		gcfFile = "";
		mainFile = "";
		mode = "all";

		// process the command line arguments
		processCommandLine(args);
		
		// BasicConfigurator.configure();
		// PropertyConfigurator.configure(logProperties);
		log = Logger.getLogger(SimCalMain.class);
		
		// setting up a FileAppender dynamically...
		SimpleLayout layout = new SimpleLayout();
		RollingFileAppender appender;
		
		// use the file name to create different log each time
		// System.out.println("mainFile = " + mainFile);
		Path p = Paths.get(gcfFile);
		String gcfFileName = p.getFileName().toString().replace(".xml", "");
		String logFile = gcfFileName + ".log.txt";
		// System.out.println("Log file = " + logFile);
		try {
			appender = new RollingFileAppender(layout, logFile, false);
			// appender.setMaxFileSize("100MB");
			log.addAppender(appender);
		} catch (IOException e1) {
			// e1.printStackTrace();
			// If cannot use the log file output, show it on screen.
			BasicConfigurator.configure();
		}

		// set logging level
		log.setLevel(Level.DEBUG);

		log.debug("Running SimCal v. 0.2");
		
		// if main file is provided, do the 1-sided comparison
		// i.e. mainFile vs others
		// if not, do the all comparisons
		// i.e. all possible combinations
		if (!mainFile.equals("")) {
			mode = "one";
			log.debug("Main file = " + mainFile);
		}
		
		// initialize the GCF object and source file line count obj
		try {
			// read values from GCF file into GCF object
			log.debug("\n\n");
			log.debug("GCF file = " + gcfFile);
			log.debug("=================================");
			log.debug("1. Start extracting clone classes");
			log.debug("=================================");
			readGCF(fileReader(gcfFile));
			
			log.debug("\n\n");
			log.debug("line count file = " + linecountFile);
			log.debug("=================================");
			log.debug("2. Start reading linecount file");
			log.debug("=================================");
			readLineCount(linecountFile);
	
			// calculate similarity
			calculateSimilarity();
		} catch (IOException e1) {
			String err = "Error: couldn't open the GCF or linecount file. Please recheck its location.";
			log.error(err);
			System.err.println(err);
		} catch (SAXException e2) {
			String err = "Error: couldn't parse the GCF file. Please recheck its format.";
			log.error(err);
			System.err.println(err);
		} catch (Exception e3) {
			String err = "Unkown error: see below:\n " + e3.getMessage();
			log.error(err);
			System.err.println(err);
			e3.printStackTrace();
		}
	}

	/***
	 * Calculate similarity (in percentage) from given values
	 */
	private static void calculateSimilarity() {
		// go through all clone classes
		ArrayList<CloneClass> ccArr = gcf.getCloneClasses();
		log.debug("\n\n");
		log.debug("=========================================");
		log.debug("3. calculateSimilarity");
		log.debug("=========================================");
		log.debug("Info: total clone classes = " + ccArr.size());
		log.debug("Info: looking for clones in = " + mainFile);
		// comparing results
		// boolean isSameFile = false;
		
		if (ccArr.size() == 0) { // no clone found
			System.out.println("0");
		}
		else { // some clones found
			for (int j = 0; j < ccArr.size(); j++) {
				log.debug("-------------- Index = " + j + "-------------");
				// each clone class
				CloneClass cc = ccArr.get(j);
				ArrayList<Clone> cArr = cc.getClones();
				log.debug("clones = " + cArr.size());
				
				for (int l = 0; l < cArr.size(); l++) {
					for (int m = l; m < cArr.size(); m++) {
						if (l == m)
							continue; // 1. skip comparing to itself
						else {
							// log.debug("Size of hash = " + fileHash.size());
							Clone c1 = cArr.get(l);
							Fragment f1 = c1.getFragmentList().get(0);

							Clone c2 = cArr.get(m);
							Fragment f2 = c2.getFragmentList().get(0);

							// get file's info
							String firstFile = f1.getFile() + "," + f1.getStartLine()
									+ "," + f1.getEndLine() + ","
									+ (f1.getEndLine() - f1.getStartLine() + 1);
							
							String secondFile = f2.getFile() + "," + f2.getStartLine()
									+ "," + f2.getEndLine() + ","
									+ (f2.getEndLine() - f2.getStartLine() + 1);

							// 2. check clone in the same file, skip
							 if (f1.getFile().equals(f2.getFile())) { 
							 	// compare the same file, and it's not the main file
							 	// SKIP
								log.debug("Comparing the same file. Skip.");
								continue;
							} 

							// 3. not the same file, add to the list.
							if (mode.equals("one")) {
								// if (f1.getFile().contains(mainFile)) { // 1-sided comparison
								if (f1.getFile().startsWith(mainFile)) { // 1-sided comparison
									log.debug("Found ---> f1: " + firstFile);
									log.debug("f2: " + secondFile);
									// remove the .java.java that we used to compare the 
									// same file
									f1.setFile(f1.getFile().replace(".java.java", ".java"));
									// copy to get the file name
									mainFile = f1.getFile();
									fragmentList.add(f1);
									// log.debug("Main file after comparing = " + mainFile);
									log.debug("Adding " + f1.getFile());
								}
								else if (f2.getFile().startsWith(mainFile)){
									log.debug("f1: " + firstFile);
									log.debug("Found ---> f2: " + secondFile);
									// remove the .java.java that we used to compare the 
									// same file
									f2.setFile(f2.getFile().replace(".java.java", ".java"));
									// copy to get the file name
									mainFile = f2.getFile();
									fragmentList.add(f2);
									// log.debug("Main file after comparing = " + mainFile);
									log.debug("Adding " + f2.getFile());
								}
								else {
									log.debug("f1: " + firstFile);
									log.debug("f2: " + secondFile);
									log.debug("Couldn't find main file. Skip.");
								}
							}
							else
								System.out.println(firstFile + "\n" + secondFile);
						}
					}
				}
				
			}
			
			if (mode.equals("one")) {
				log.debug("\n\n");
				log.debug("==================================================================");
				log.debug("4. Finished finding all the clone fragments.");
				log.debug("   Start merging fragments.");
				log.debug("==================================================================");
				
				log.debug("\n\n");
				log.debug("=============================");
				log.debug("5. Summary");
				log.debug("Mode: " + calMode);
				log.debug("=============================");
				
				if (calMode.equals("l")) {
					int totalSize = calSumClonedLines();
					log.debug("total size = " + totalSize);
					if (totalSize == 0) { // no clone between 2 files found
						log.debug("No clone between 2 files found.");
						log.debug("0");
						System.out.println("0");
					} else {
						try {
							log.debug("file size = " + fileHash.get(mainFile).getLines());
							log.debug("Similarity = " + (float) (totalSize * 100) / fileHash.get(mainFile).getLines());
							System.out.println((float) (totalSize * 100) / fileHash.get(mainFile).getLines());
						} catch (Exception e) {
							String err = "ERROR: error(s) in calculating similarity. Check the file path and hash.";
							log.debug(err);
							System.err.println(err);
						}
					}
				} else if (calMode.equals("c")) {
					int[] sizes = calSumClonedChars();
					int totalSize = sizes[1];
					log.debug("total chars = " + totalSize);
					if (totalSize == 0) { // no clone between 2 files found
						log.debug("No clone between 2 files found.");
						log.debug("0");
						System.out.println("0");
					} else {
						try {
							log.debug("file size = " + sizes[0]);
							log.debug("Similarity = " + (float) (totalSize * 100) / sizes[0]);
							System.out.println((float) (totalSize * 100) / sizes[0]);
						} catch (Exception e) {
							String err = "ERROR: error(s) in calculating similarity. Check the file's location.";
							log.debug(err);
							System.err.println(err);
						}
					}
				} else if (calMode.equals("w")) {
					int[] sizes = calSumClonedWords();
					int totalSize = sizes[1];
					log.debug("total words = " + totalSize + " words");
					if (totalSize == 0) { // no clone between 2 files found
						log.debug("No clone between 2 files found.");
						log.debug("0");
						System.out.println("0");
					} else {
						try {
							log.debug("file size = " + sizes[0] + " words");
							log.debug("Similarity = " + (float) (totalSize * 100) / sizes[0]);
							System.out.println((float) (totalSize * 100) / sizes[0]);
						} catch (Exception e) {
							String err = "ERROR: error(s) in calculating similarity. Check the file's location.";
							log.debug(err);
							System.err.println(err);
						}
					}
				}
			}
		}
	}
	
	/***
	 * Calculate a number of appropriate lines to add to cloned code lines. Consider overlaps between existing lines
	 * and newly added lines as well.
	 * @param start
	 * @param end
	 */
	private static int calSumClonedLines() {
		int sum = 0, lines = 0;
		ArrayList<Fragment> mergedList = new ArrayList<Fragment>();
		ArrayList<Fragment> currentList = fragmentList.getList();

		for (int i = 0; i < currentList.size(); i++) {
			Fragment f1 = currentList.get(i);
			boolean isOverlap = false;
			log.debug("---------------------------");
			log.debug("Frag " + i + " " + f1.getInfo());
			if (mergedList.isEmpty())
				mergedList.add(f1);
			else {
				int size = mergedList.size();
				for (int j = 0; j < size; j++) {
					log.debug("Round: " + j);
					Fragment f2 = mergedList.get(j);
					lines = f2.mergeIfOverlap(f1);
					
					log.debug("Non-overlapped lines: " + lines);
					log.debug("Comparing with: " + f2.getFile() + "," + f2.getStartLine() + "," + f2.getEndLine() + "," + f2.getSize());
					
					// has overlaps, check near by fragments
					if (lines != -1) {
						isOverlap = true;
						if (j > 0) { // if not the first one
							// get a previous one
							Fragment f3 = mergedList.get(j - 1);
							// overlap to the previous one as well.
							if (f1.getStartLine() <= f3.getEndLine()) {
								// extend the fragment size
								f3.setEndLine(f2.getEndLine());
								log.debug("Extend to the previous one.");
								// remove the current one since we already
								// include it into the previous one.
								mergedList.remove(f2);
							}
						}
						
						if (j < mergedList.size() - 1) { // if not the last one
							Fragment f3 = mergedList.get(j + 1);
							// overlap the next one as well
							if (f1.getEndLine() >= f3.getStartLine()) {
								// extend the fragment size
								f2.setEndLine(f3.getEndLine());
								log.debug("Extend to the next one.");
								// remove the next one since we already
								// merge it into the current one
								mergedList.remove(f3);
							}
						}
						break;
					}
				}
				// no overlap, add it to the merged list
				if (!isOverlap) {
					boolean added = false;
					for (int k = 0; k < mergedList.size(); k++) {
//						log.debug("f1 start: " + f1.getStartLine()
//								+ ", f2 start: "
//								+ mergedList.get(k).getStartLine());
						if (f1.getStartLine() < mergedList.get(k)
								.getStartLine()) {
							mergedList.add(k, f1);
							added = true;
							break;
						}
					}
					if (!added)
						mergedList.add(f1);
				}
			}
			// calculate total cloned line counts
			log.debug(">>>>>>>>>>>> Merged list: size = " + mergedList.size());
			for (int l=0; l<mergedList.size(); l++) {
				Fragment fx = mergedList.get(l);
				log.debug(l + ":" + fx.getFile() + "," + fx.getStartLine() + "," + fx.getEndLine() + "," + fx.getSize());
			}
		}
		// calculate total cloned line counts
		log.debug(">>>>>>>>>>>> Final merged list: size = " + mergedList.size());
		for (int l=0; l<mergedList.size(); l++) {
			Fragment fx = mergedList.get(l);
			log.debug(l + ":" + fx.getFile() + "," + fx.getStartLine() + ","
					+ fx.getEndLine() + "," + fx.getSize());
			sum += fx.getSize();
		}

		return sum;
	}
	
	/***
	 * Calculate a number of clones in term of no. of characters
	 * Consider overlaps between existing lines
	 * and newly added lines as well.
	 * @param start
	 * @param end
	 */
	private static int[] calSumClonedChars() {
		int lines = 0;
		ArrayList<Fragment> mergedList = new ArrayList<Fragment>();
		ArrayList<Fragment> currentList = fragmentList.getList();

		for (int i = 0; i < currentList.size(); i++) {
			Fragment f1 = currentList.get(i);
			boolean isOverlap = false;
			log.debug("---------------------------");
			log.debug("Frag " + i + " " + f1.getInfo());
			if (mergedList.isEmpty())
				mergedList.add(f1);
			else {
				int size = mergedList.size();
				for (int j = 0; j < size; j++) {
					log.debug("Round: " + j);
					Fragment f2 = mergedList.get(j);
					lines = f2.mergeIfOverlap(f1);
					
					log.debug("Non-overlapped lines: " + lines);
					log.debug("Comparing with: " + f2.getFile() + "," + f2.getStartLine() + "," + f2.getEndLine() + "," + f2.getSize());
					
					// has overlaps, check near by fragments
					if (lines != -1) {
						isOverlap = true;
						if (j > 0) { // if not the first one
							// get a previous one
							Fragment f3 = mergedList.get(j - 1);
							// overlap to the previous one as well.
							if (f1.getStartLine() <= f3.getEndLine()) {
								// extend the fragment size
								f3.setEndLine(f2.getEndLine());
								log.debug("Extend to the previous one.");
								// remove the current one since we already
								// include it into the previous one.
								mergedList.remove(f2);
							}
						}
						
						if (j < mergedList.size() - 1) { // if not the last one
							Fragment f3 = mergedList.get(j + 1);
							// overlap the next one as well
							if (f1.getEndLine() >= f3.getStartLine()) {
								// extend the fragment size
								f2.setEndLine(f3.getEndLine());
								log.debug("Extend to the next one.");
								// remove the next one since we already
								// merge it into the current one
								mergedList.remove(f3);
							}
						}
						break;
					}
				}
				// no overlap, add it to the merged list
				if (!isOverlap) {
					boolean added = false;
					for (int k = 0; k < mergedList.size(); k++) {
//						log.debug("f1 start: " + f1.getStartLine()
//								+ ", f2 start: "
//								+ mergedList.get(k).getStartLine());
						if (f1.getStartLine() < mergedList.get(k)
								.getStartLine()) {
							mergedList.add(k, f1);
							added = true;
							break;
						}
					}
					if (!added)
						mergedList.add(f1);
				}
			}
			// calculate total cloned line counts
			log.debug(">>>>>>>>>>>> Merged list: size = " + mergedList.size());
			for (int l=0; l<mergedList.size(); l++) {
				Fragment fx = mergedList.get(l);
				log.debug(l + ":" + fx.getFile() + "," + fx.getStartLine() + "," + fx.getEndLine() + "," + fx.getSize());
			}
		}
		
		// calculate total cloned line counts
		log.debug(">>>>>>>>>>>> Final merged list: size = " + mergedList.size());
		
		int[] startLines = new int[mergedList.size()];
		int[] endLines = new int[mergedList.size()];
		String file = "";
		
		for (int l=0; l<mergedList.size(); l++) {
			Fragment fx = mergedList.get(l);
			file = fx.getFile();
			log.debug(l + ":" + fx.getFile() + "," + fx.getStartLine() + ","
					+ fx.getEndLine() + "," + fx.getSize());
			startLines[l] = fx.getStartLine();
			endLines[l] = fx.getEndLine();
		}
		
		if (mergedList.size()>0) {
			String[] cloneLines = readFileFromLineToLine(file, startLines, endLines);
			// System.out.println("Clone lines: " + cloneLines[1]);
			String cloneLinesClean = "";
			for (int i=0; i<cloneLines.length-1; i++)
				cloneLinesClean += cloneLines[i].replaceAll("\\s+","");
			//System.out.println("Clone lines (whitespace removed): " + cloneLinesClean);
			
			String wholeFile = cloneLines[cloneLines.length-1].replaceAll("\\s+","");
			//System.out.println("Whole file (whitespace removed): " + wholeFile);
			
			int[] sum = new int[2];
			sum[0] = wholeFile.length();
			sum[1] = cloneLinesClean.length();
			log.debug("clone size (c): " + cloneLinesClean.length());
			log.debug("file size (c): " + wholeFile.length());
			
			return sum;
		} else {
			// no clone between files found. return something that will compute as zero
			int[] sum = {1,0};
			return sum;
		}
	}
	
	/***
	 * Calculate a number of clones in term of no. of characters
	 * Consider overlaps between existing lines
	 * and newly added lines as well.
	 * @param start
	 * @param end
	 */
	private static int[] calSumClonedWords() {
		int lines = 0;
		ArrayList<Fragment> mergedList = new ArrayList<Fragment>();
		ArrayList<Fragment> currentList = fragmentList.getList();

		for (int i = 0; i < currentList.size(); i++) {
			Fragment f1 = currentList.get(i);
			boolean isOverlap = false;
			log.debug("---------------------------");
			log.debug("Frag " + i + " " + f1.getInfo());
			if (mergedList.isEmpty())
				mergedList.add(f1);
			else {
				int size = mergedList.size();
				for (int j = 0; j < size; j++) {
					log.debug("Round: " + j);
					Fragment f2 = mergedList.get(j);
					lines = f2.mergeIfOverlap(f1);
					
					log.debug("Non-overlapped lines: " + lines);
					log.debug("Comparing with: " + f2.getFile() + "," + f2.getStartLine() + "," + f2.getEndLine() + "," + f2.getSize());
					
					// has overlaps, check near by fragments
					if (lines != -1) {
						isOverlap = true;
						if (j > 0) { // if not the first one
							// get a previous one
							Fragment f3 = mergedList.get(j - 1);
							// overlap to the previous one as well.
							if (f1.getStartLine() <= f3.getEndLine()) {
								// extend the fragment size
								f3.setEndLine(f2.getEndLine());
								log.debug("Extend to the previous one.");
								// remove the current one since we already
								// include it into the previous one.
								mergedList.remove(f2);
							}
						}
						
						if (j < mergedList.size() - 1) { // if not the last one
							Fragment f3 = mergedList.get(j + 1);
							// overlap the next one as well
							if (f1.getEndLine() >= f3.getStartLine()) {
								// extend the fragment size
								f2.setEndLine(f3.getEndLine());
								log.debug("Extend to the next one.");
								// remove the next one since we already
								// merge it into the current one
								mergedList.remove(f3);
							}
						}
						break;
					}
				}
				// no overlap, add it to the merged list
				if (!isOverlap) {
					boolean added = false;
					for (int k = 0; k < mergedList.size(); k++) {
//						log.debug("f1 start: " + f1.getStartLine()
//								+ ", f2 start: "
//								+ mergedList.get(k).getStartLine());
						if (f1.getStartLine() < mergedList.get(k)
								.getStartLine()) {
							mergedList.add(k, f1);
							added = true;
							break;
						}
					}
					if (!added)
						mergedList.add(f1);
				}
			}
			// calculate total cloned line counts
			log.debug(">>>>>>>>>>>> Merged list: size = " + mergedList.size());
			for (int l=0; l<mergedList.size(); l++) {
				Fragment fx = mergedList.get(l);
				log.debug(l + ":" + fx.getFile() + "," + fx.getStartLine() + "," + fx.getEndLine() + "," + fx.getSize());
			}
		}
		
		// calculate total cloned line counts
		log.debug(">>>>>>>>>>>> Final merged list: size = " + mergedList.size());
		
		int[] startLines = new int[mergedList.size()];
		int[] endLines = new int[mergedList.size()];
		String file = "";
		
		for (int l=0; l<mergedList.size(); l++) {
			Fragment fx = mergedList.get(l);
			file = fx.getFile();
			log.debug(l + ":" + fx.getFile() + "," + fx.getStartLine() + ","
					+ fx.getEndLine() + "," + fx.getSize());
			startLines[l] = fx.getStartLine();
			endLines[l] = fx.getEndLine();
		}
		
		int[] sum = new int[2];
		JavaTokenizer tokenizer = new JavaTokenizer();
		if (mergedList.size() > 0) {
			String[] cloneLines = readFileFromLineToLine(file, startLines, endLines);
			// System.out.println("Clone lines: " + cloneLines[1]);
			ArrayList<String> cloneWords = new ArrayList<String>();
			try {
				for (int i = 0; i < cloneLines.length - 1; i++) {
					StringReader sr = new StringReader(cloneLines[i]);
					cloneWords = tokenizer.tokenize(sr);
					// System.out.println("Clone words: " + cloneWords.toString());
				}
				// System.out.println("Clone lines (whitespace removed): " +
				// cloneLinesClean);
				// StringReader sr = new StringReader(cloneLines[cloneLines.length - 1]);
				
				JavaTokenizer tokenizerf = new JavaTokenizer();
				StringReader srf = new StringReader(cloneLines[cloneLines.length - 1]);
				ArrayList<String> wholeFile = new ArrayList<String>();
				wholeFile = tokenizerf.tokenize(srf);
//				 System.out.println("File: " + cloneLines[cloneLines.length - 1]);
//				 System.out.println();
//				 System.out.println("File words: " + wholeFile.toString());
				
				sum[0] = wholeFile.size();
				sum[1] = cloneWords.size();
				log.debug("clone size (c): " + sum[1]);
				log.debug("file size (c): " + sum[0]);
				
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
			// System.out.println("Whole file (whitespace removed): " + wholeFile);
		} else {
			// no clone between files found. return something that will compute as zero
			sum[0] = 1;
			sum[1] = 0;
			return sum;
		}

		return sum;
	}

	/***
	 * Read the line counts into srcFileArr
	 * 
	 * @param filepath the linecount file (.csv).
	 * @throws IOException
	 */
	private static void readLineCount(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			// StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				// split at ',' to separate between file path and line count
				String[] splittedLine = line.split(",");
				SourceFile sf = new SourceFile(splittedLine[0].trim(),
						Integer.parseInt(splittedLine[1].trim()));
				line = br.readLine();
				// add only the path from "tests/..." as a key of the hash
				String filepath = splittedLine[0];
				if (splittedLine[0].indexOf("tests")!=-1)
					filepath = splittedLine[0].substring(
						splittedLine[0].indexOf("tests") + 6,
						splittedLine[0].length());
				
				fileHash.put(filepath, sf);
			}
		}
		finally {
			br.close();
		}

		log.debug("Read line count successfully. Number of total files = "
						+ fileHash.size());
		// loop through all items in the hashmap
//		Iterator<Entry<String, SourceFile>> it = fileHash.entrySet().iterator();
//		while (it.hasNext()) {
//			HashMap.Entry<String, SourceFile> pair = (HashMap.Entry<String, SourceFile>) it
//					.next();
//			SourceFile s = (SourceFile) pair.getValue();
//			log.debug(pair.getKey() + "," + s.getLines());
//		}
	}
	
	/***
	 * A method to read file from start to end line and return the text in that region
	 * @param filepath the file to be read
	 * @param startline starting line
	 * @param endline ending line
	 * @return the string from startline to endline
	 */
	private static String[] readFileFromLineToLine(String filepath, int[]	startlines, int[] endlines) {
		BufferedReader br = null;
		
		String[] s = new String[startlines.length + 1]; // include the whole file too (index = last)
		for (int i=0; i<s.length; i++) s[i] = ""; // cleaning the string
		
		int linecount = 0;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileLocation + "/" + filepath));
			while ((sCurrentLine = br.readLine()) != null) {
				linecount++;
				// totalString keeps all the file's content
				s[s.length-1] += sCurrentLine; 
				for (int i=0; i<s.length-1; i++) {
					// only get the string within the given range.
					if (linecount>=startlines[i] && linecount<=endlines[i])
						s[i] += sCurrentLine;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return s;
	}

	/***
	 * Read the GCF file in XML format
	 * 
	 * @param the
	 *            GCF file (.xml). Some part of codes copied from
	 *            http://www.mkyong
	 *            .com/java/how-to-read-xml-file-in-java-dom-parser/
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	private static void readGCF(File f) throws ParserConfigurationException,
			SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(f);

		doc.getDocumentElement().normalize();

		log.debug("Root element :"
				+ doc.getDocumentElement().getNodeName());

		NodeList nList = doc.getElementsByTagName("CloneClass");
		log.debug("Number of clone classes = " + nList.getLength());
		// loop through all clone classes (skip the first child which is the ID)
		for (int i = 0; i < nList.getLength(); i++) {
			CloneClass cc = new CloneClass();
			Node n = nList.item(i);
			NodeList clones = n.getChildNodes();
			log.debug(i + ": Node name = " + n.getNodeName() + ", child nodes = " + clones.getLength());
			// loop through all clones
			for (int j = 0; j < clones.getLength(); j++) {
				Clone c = new Clone();
				Node clone = clones.item(j);

				// Add only the real clones, skip ID
				if (!clone.getNodeName().toString().equals("#text")
						&& !clone.getNodeName().toString().equals("ID")) {
					NodeList fragments = clone.getChildNodes();
					log.debug("> " + j + ": Node name = " + clone.getNodeName() + ", childs = " + fragments.getLength());
					// loop through all fragments
					for (int k = 0; k < fragments.getLength(); k++) {
						Fragment frag = new Fragment();
						Node fNode = fragments.item(k);
						if (!fNode.getNodeName().toString().equals("#text")) {
							log.debug(
									">> Node name = " + fNode.getNodeName());
							NodeList fChildNodes = fNode.getChildNodes();
							int vindex = 0;
							for (int l = 0; l < fChildNodes.getLength(); l++) {
								Node fragNode = fChildNodes.item(l);
								if (!fragNode.getNodeName().toString()
										.equals("#text")) {
									log.debug("      >> Node name = "
											+ fragNode.getNodeName() + ", value = "
													+ fragNode.getTextContent());

									if (vindex == 0) // file
										frag.setFile(fragNode.getTextContent());
									else if (vindex == 1) // start
										frag.setStartLine(Integer
												.parseInt(fragNode
														.getTextContent()));
									else
										// end
										frag.setEndLine(Integer
												.parseInt(fragNode
														.getTextContent()));
									vindex++;
								}
							}

							// add fragment to the clone
							c.addFragment(frag);
						}
					}
					// add clone to the clone class
					cc.addClone(c);
				}
			}
			// add clone class to the gcf obj
			gcf.addCloneClass(cc);
		}
		log.debug("Number of clone classes (after parsing) = "
						+ gcf.getCloneClassSize());
	}

	/***
	 * A helper to open a given file
	 * 
	 * @param filePath
	 *            - location of the file
	 * @return a File object of the given file
	 */
	private static File fileReader(String filePath) {
		File f = new File(filePath);
		return f;
	}
	
	private static void processCommandLine(String[] args) {
		// create the command line parser
		CommandLineParser parser = new BasicParser();

		options.addOption("l", "linecount", true, "linecount csv file location.");
		options.addOption("g", "gcf", true, "GCF file location.");
		options.addOption("h", "help", false, "Display help message.");
		options.addOption("m", "main", true, "Main file to compare.");
		options.addOption("p", "path", true, "file's location");
		options.addOption("s", "switchmode", true, "specify the mode (l=line, c=char, w=word)");
		options.addOption("c", "count", true, "Running count (number only)");

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			
			// validate that line count has been set
			if (line.hasOption("l")) {
				// System.out.println("Found: " + line.getOptionValue("l"));
				linecountFile = line.getOptionValue("l");
			} else {
				showHelp();
				throw new ParseException("No linecount file provided.");
			}

			if (line.hasOption("g")) {
				// log.debug("Found: " + line.getOptionValue("g"));
				gcfFile = line.getOptionValue("g");
			} else {
				showHelp();
				throw new ParseException("No GCF file provided.");
			}
			
			if (line.hasOption("m")) {
				mainFile = line.getOptionValue("m");
			} else {
				showHelp();
				throw new ParseException("No main file provided. Using all comparison mode.");
			}
			
			if (line.hasOption("s")) {
				calMode = line.getOptionValue("s");
			} else {
				log.debug("No mode provided. Use line mode.");
				calMode = "l";
			}
			
			if (line.hasOption("p")) {
				fileLocation = line.getOptionValue("p");
			}
			
			if (line.hasOption("c")) {
				runningCount = Integer.parseInt(line.getOptionValue("c"));
			}
			
			if (line.hasOption("h")) {
				showHelp();
			}
			
		} catch (ParseException exp) {
			System.out.println("Warning: " + exp.getMessage());
		}
	}
	
	private static void showHelp() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("SimCal 0.3", options);
		System.exit(0);
	}
}
