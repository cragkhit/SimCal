package cloplag.simcal;

import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/***
 * A class to read GCF xml file along with another file containing line numbers
 * of all inspected files to create a similarity measure of Simian clone
 * detection
 * 
 * @author Chaiyong Ragkhitwetsagul, UCL
 * @version 1.0
 * @since 2015-02-20
 */
public class SimCalMain {
	/***
	 * An array list to store list of all files and their line counts.
	 */
	private static HashMap<String, SourceFile> fileHash = new HashMap<String, SourceFile>();
	private static GCF gcf = new GCF();
	private final static Logger log = Logger.getLogger(SimCalMain.class.getName());
	private static String linecountFile;
	private static String gcfFile;
	// create the Options
	private static Options options = new Options();
	private static String mainFile;
	private static String mode;
	private static FragmentList fragmentList = new FragmentList();

	public static void main(String[] args) {
		// set logging level
		log.setLevel(Level.OFF);
		// Initialize all the strings
		linecountFile = "";
		gcfFile = "";
		mainFile = "";
		mode = "all";
		// process the command line arguments
		processCommandLine(args);
		// if main file is provided, do the 1-sided comparison
		// i.e. mainFile vs others
		// if not, do the all comparisons
		// i.e. all possible combinations
		if (!mainFile.equals(""))
			mode = "one";
		
		// initialize the GCF object and source file line count obj
		try {
			// read values from GCF file into GCF object
			readGCF(fileReader(gcfFile));
			readLineCount(linecountFile);

			// calculate similarity
			calculateSimilarity();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error: see below:");
			e.printStackTrace();
		}
	}

	/***
	 * Calculate similarity (in percentage) from given values
	 */
	private static void calculateSimilarity() {
		// go through all clone classes
		ArrayList<CloneClass> ccArr = gcf.getCloneClasses();
		System.out.println("========= calculateSimilarity ===========");
		log.log(Level.INFO, "=============================");
		log.log(Level.INFO, "Clone classes = " + ccArr.size());
		
		if (ccArr.size() == 0) // no clone found
			System.out.println("0.00");
		else { // some clones found
			for (int j = 0; j < ccArr.size(); j++) {
				log.log(Level.WARNING, "Index = " + j);
				// each clone class
				CloneClass cc = ccArr.get(j);
				ArrayList<Clone> cArr = cc.getClones();
				log.log(Level.INFO, "clones = " + cArr.size());
				// comparing results
				boolean isSameFile = false;
				
				for (int l = 0; l < cArr.size(); l++) {
					for (int m = l; m < cArr.size(); m++) {
						if (l == m)
							continue; // 1. skip comparing to itself
						else {
							log.log(Level.INFO,
									"Size of hash = " + fileHash.size());
							Clone c1 = cArr.get(l);
							Fragment f1 = c1.getFragmentList().get(0);
							/* SourceFile sf1 = fileHash.get(f1.getFile());
							
							if (sf1 == null)
								log.log(Level.SEVERE, "Not found"); */

							Clone c2 = cArr.get(m);
							Fragment f2 = c2.getFragmentList().get(0);
							/* SourceFile sf2 = fileHash.get(f2.getFile());
							if (sf2 == null)
								log.log(Level.SEVERE, "Not found"); */

							// 2. check clone in the same file, skip
							if (f1.getFile().equals(f2.getFile())) {
								isSameFile = true;
								continue;
							} 

							// 3. if not in the same file, print it
							String firstFile = f1.getFile() + "," + f1.getStartLine()
									+ "," + f1.getEndLine() + ","
									+ (f1.getEndLine() - f1.getStartLine() + 1);
							
							String secondFile = f2.getFile() + "," + f2.getStartLine()
									+ "," + f2.getEndLine() + ","
									+ (f2.getEndLine() - f2.getStartLine() + 1);
											
							if (mode.equals("one")) {
								if (mainFile.equals(f1.getFile())) { // 1-sided comparison 
									fragmentList.add(f1);
									// System.out.println(firstFile);
								}
								else {
									fragmentList.add(f2);
									// System.out.println(secondFile);
								}
							}
							else
								System.out.println(firstFile + "\n" + secondFile);
						}
					}
				}
				// if the same file, print zero
				if (isSameFile)
					System.out.println("0.00");
			}	
		}
		if (mode.equals("one")) {
			int totalSize = calSumClonedLines();
			System.out.println("========= Summary ===========");
			System.out.println("total size = " + totalSize);
			System.out.println("file size = " + fileHash.get(mainFile).getLines());
			System.out.println("Similarity = " + (float)totalSize/fileHash.get(mainFile).getLines());
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
			System.out.println("------\nFrag " + i + " " + f1.getInfo());
			if (mergedList.isEmpty())
				mergedList.add(f1);
			else {
				int size = mergedList.size();
				for (int j = 0; j < size; j++) {
					System.out.println("Round: " + j);
					Fragment f2 = mergedList.get(j);
					lines = f2.mergeIfOverlap(f1);
					
					System.out.println("non-overlapped lines = " + lines);
					// System.out.println(f2.getFile() + "," + f2.getStartLine()
					//		+ "," + f2.getEndLine() + "," + f2.getSize());
					
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
								System.out.println("Extend to the previous one.");
								// remove the current one since we already
								// include it into the previous one.
								mergedList.remove(f2);
							}
						}
						
						if (j < mergedList.size() - 1) { // if not the last one
							Fragment f3 = mergedList.get(mergedList.size() - 1);
							// overlap the next one as well
							if (f1.getEndLine() >= f3.getStartLine()) {
								// extend the fragment size
								f2.setEndLine(f3.getEndLine());
								System.out.println("Extend to the next one.");
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
						System.out.println("f1 start: " + f1.getStartLine()
								+ ", f2 start: "
								+ mergedList.get(k).getStartLine());
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
			System.out.println("Merged list: size = " + mergedList.size());
			for (int l=0; l<mergedList.size(); l++) {
				Fragment fx = mergedList.get(l);
				System.out.println(l + ":" + fx.getFile() + "," + fx.getStartLine() + ","
						+ fx.getEndLine() + "," + fx.getSize());
			}
		}
		// calculate total cloned line counts
		System.out.println("Final merged list: size = " + mergedList.size());
		for (int l=0; l<mergedList.size(); l++) {
			Fragment fx = mergedList.get(l);
			System.out.println(l + ":" + fx.getFile() + "," + fx.getStartLine() + ","
					+ fx.getEndLine() + "," + fx.getSize());
			sum += fx.getSize();
		}

		return sum;
	}

	/***
	 * Read the line counts into srcFileArr
	 * 
	 * @param the
	 *            line count file (.csv).
	 * @throws IOException
	 */
	private static void readLineCount(String filePath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			StringBuilder sb = new StringBuilder();
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
		} finally {
			br.close();
		}

		log.log(Level.INFO,
				"Read line count successfully. Number of total files = "
						+ fileHash.size());
		// loop through all items in the hashmap
		Iterator<Entry<String, SourceFile>> it = fileHash.entrySet().iterator();
		while (it.hasNext()) {
			HashMap.Entry<String, SourceFile> pair = (HashMap.Entry<String, SourceFile>) it
					.next();
			SourceFile s = (SourceFile) pair.getValue();
			log.log(Level.WARNING, pair.getKey() + "," + s.getLines());
		}
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

		log.log(Level.INFO, "Root element :"
				+ doc.getDocumentElement().getNodeName());

		NodeList nList = doc.getElementsByTagName("CloneClass");
		log.log(Level.INFO, "Number of clone classes = " + nList.getLength());
		// loop through all clone classes (skip the first child which is the ID)
		for (int i = 0; i < nList.getLength(); i++) {
			CloneClass cc = new CloneClass();
			Node n = nList.item(i);
			log.log(Level.INFO, "Node name = " + n.getNodeName());
			NodeList clones = n.getChildNodes();
			log.log(Level.INFO, ", Child nodes = " + clones.getLength());
			// loop through all clones
			for (int j = 0; j < clones.getLength(); j++) {
				Clone c = new Clone();
				Node clone = clones.item(j);

				// Add only the real clones, skip ID
				if (!clone.getNodeName().toString().equals("#text")
						&& !clone.getNodeName().toString().equals("ID")) {
					log.log(Level.INFO, " > Node name = " + clone.getNodeName());
					NodeList fragments = clone.getChildNodes();
					log.log(Level.INFO, ", childs = " + fragments.getLength());
					// loop through all fragments
					for (int k = 0; k < fragments.getLength(); k++) {
						Fragment frag = new Fragment();
						Node fNode = fragments.item(k);
						if (!fNode.getNodeName().toString().equals("#text")) {
							log.log(Level.INFO,
									"   >> Node name = " + fNode.getNodeName());
							NodeList fChildNodes = fNode.getChildNodes();
							int vindex = 0;
							for (int l = 0; l < fChildNodes.getLength(); l++) {
								Node fragNode = fChildNodes.item(l);
								if (!fragNode.getNodeName().toString()
										.equals("#text")) {
									log.log(Level.INFO, "      >> Node name = "
											+ fragNode.getNodeName());
									log.log(Level.INFO,
											", value = "
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
		log.log(Level.INFO,
				"Number of clone classes (after parsing) = "
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

		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			
			// validate that line count has been set
			if (line.hasOption("l")) {
				// System.out.println("Found: " + line.getOptionValue("l"));
				linecountFile = line.getOptionValue("l");
			} else {
				throw new ParseException("No linecount file provided.");
			}

			if (line.hasOption("g")) {
				// System.out.println("Found: " + line.getOptionValue("g"));
				gcfFile = line.getOptionValue("g");
			} else {
				throw new ParseException("No GCF file provided.");
			}
			
			if (line.hasOption("m")) {
				mainFile = line.getOptionValue("m");
			} else {
				throw new ParseException("No main file provided. Using all comparison mode.");
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
		formater.printHelp("SimCal", options);
		System.exit(0);
	}
}
