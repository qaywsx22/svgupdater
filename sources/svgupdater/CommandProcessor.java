package svgupdater;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathResult;

public class CommandProcessor {

	private static final String PARSE_COMMAND = "parse";
	private static final String WRITE_COMMAND = "write";
	private static final String EXPORT_COMMAND = "export";

	private static final String TEXT_FILE_NAME = "/texts.csv";
	private static final String IMAGE_FILE_NAME = "/images.csv";

	private File infile;
	private File outfile;
	private File tmpDir;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CommandProcessor cp = new CommandProcessor();
		if (args.length == 0) {
			cp.showUsage(null);
			return;
		}
		String infilePath = null;
		String outfilePath = null;
		String tmpDirPath = null;
		String command = args[0].toLowerCase();
		switch (command) {
			case CommandProcessor.PARSE_COMMAND:
				if (args.length < 2) {
					cp.showUsage("Missing input file path and path to tmp directory");
					return;
				}
				else if (args.length < 3) {
					cp.showUsage("Missing path to tmp directory");
					return;
				}
				infilePath = args[1];
				tmpDirPath = args[2];
				try {
					cp.setInputFile(new File(infilePath));
				}
				catch (NullPointerException npe) {
					cp.showUsage("Input file missing or corupted.");
					return;
				}
				try {
					cp.setTempDirectory(new File(tmpDirPath));
					if (!cp.getTempDirectory().isDirectory()) {
						cp.showUsage("Third argument must be a path to directory.");
						return;
					}
				}
				catch (NullPointerException npe) {
					cp.showUsage("Tmp directory missing or corupted.");
					return;
				}
			break;
			case CommandProcessor.WRITE_COMMAND:
				if (args.length < 2) {
					cp.showUsage("Missing input and output file pathes and path to tmp directory");
					return;
				}
				else if (args.length < 3) {
					cp.showUsage("Missing output file path and path to tmp directory");
					return;
				}
				else if (args.length < 4) {
					cp.showUsage("Missing path to tmp directory");
					return;
				}
				infilePath = args[1];
				outfilePath = args[2];
				tmpDirPath = args[3];
				try {
					cp.setInputFile(new File(infilePath));
				}
				catch (NullPointerException npe) {
					cp.showUsage("Input file missing or corupted.");
					return;
				}
				try {
					cp.setOutputFile(new File(outfilePath));
				}
				catch (NullPointerException npe) {
					cp.showUsage("Output file error.");
					return;
				}
				try {
					cp.setTempDirectory(new File(tmpDirPath));
					if (!cp.getTempDirectory().isDirectory()) {
						cp.showUsage("Third argument must be a path to directory.");
						return;
					}
				}
				catch (NullPointerException npe) {
					cp.showUsage("Tmp directory missing or corupted.");
					return;
				}
			break;
			case CommandProcessor.EXPORT_COMMAND:
				if (args.length < 2) {
					cp.showUsage("Missing input and output file pathes");
					return;
				}
				else if (args.length < 3) {
					cp.showUsage("Missing path to output file");
					return;
				}
				infilePath = args[1];
				outfilePath = args[2];
				try {
					cp.setInputFile(new File(infilePath));
				}
				catch (NullPointerException npe) {
					cp.showUsage("Input file missing or corupted.");
					return;
				}
				try {
					cp.setOutputFile(new File(outfilePath));
				}
				catch (NullPointerException npe) {
					cp.showUsage("Output file error.");
					return;
				}
			break;
			default:
				cp.showUsage("Illegal command: " + command + ". Try again.");
				return;
		}
		
		Document doc = cp.openSVGFile();
		switch (command) {
			case CommandProcessor.PARSE_COMMAND:
			List<String> imageList = cp.getListOfImages(doc);
			if (imageList != null && !imageList.isEmpty()) {
				try {
					File imgListFile = new File(cp.getTempDirectory(), CommandProcessor.IMAGE_FILE_NAME);
					PrintWriter w = new PrintWriter(imgListFile);
					for (String str : imageList) {
						w.println(str);
					}
					w.flush();
					w.close();
				}
				catch (Exception exc) {
					exc.printStackTrace(System.out);
				}
			}
			else {
				System.out.println("No images found");
			}
			List<String> textList = cp.getListOfTexts(doc);
			if (textList != null && !textList.isEmpty()) {
				try {
					File textListFile = new File(cp.getTempDirectory(), CommandProcessor.TEXT_FILE_NAME);
					PrintWriter w = new PrintWriter(textListFile);
					for (String str : textList) {
						w.println(str);
					}
					w.flush();
					w.close();
				}
				catch (Exception exc) {
					exc.printStackTrace(System.out);
				}
			}
			else {
				System.out.println("No texts found");
			}
			break;
		}
	}

	private void showUsage(String message) {
		if (message != null && message.length() > 0) {
			System.out.println(message);
		}
		else {
			System.out.println("The programm must have arguments!!!");
		}
	}

	private void setInputFile(File file) {
		this.infile = file;
	}

	private File getInputFile() {
		return this.infile;
	}

	private void setOutputFile(File file) {
		this.outfile = file;
	}

	private File getOutputFile() {
		return this.outfile;
	}

	private void setTempDirectory(File dir) {
		this.tmpDir = dir;
	}

	private File getTempDirectory() {
		return this.tmpDir;
	}
	
	private Document openSVGFile() {
		Document doc = null;
		FileInputStream svgInputStream = null;
		try {
			// Load input file
			svgInputStream = new FileInputStream(infile);
			// Load SVG into DOM-Tree
			String parser = XMLResourceDescriptor.getXMLParserClassName();
			SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
			doc = factory.createDocument(parser, svgInputStream);
		} catch (Exception exc) {
			exc.printStackTrace(System.out);
		}
		return doc;
	}

	public List<String> getListOfImages(Document doc) {
		XPathEvaluator xpathEvaluator = (XPathEvaluator) doc;
		SVGElement searchRoot = ((SVGDocument)doc).getRootElement();
//		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"image\" and @class=\"img-editable\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"image\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);

		int count= 0;
		List<String> list = new ArrayList<String>();
		String str;
		Node node;
		SVGElement el;
		while ((node = result.iterateNext()) != null) {
			if (node instanceof SVGElement) {
				el = (SVGElement)node;
				str = el.getAttribute("id") + "," + el.getAttributeNS("http://www.w3.org/1999/xlink", "href");
				list.add(str);
			}

			count++;
		}
		System.out.println("Found "+count+" images");
		return list;
	}

	public List<String> getListOfTexts(Document doc) {
		XPathEvaluator xpathEvaluator = (XPathEvaluator) doc;
		SVGElement searchRoot = ((SVGDocument)doc).getRootElement();
//		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"image\" and @class=\"img-editable\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"text\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);

		int count= 0;
		List<String> list = new ArrayList<String>();
		String str;
		Node node;
		SVGElement el;
		while ((node = result.iterateNext()) != null) {
			if (node instanceof SVGElement) {
				el = (SVGElement)node;
//				str = el.getAttribute("id") + "," + el.getAttributeNS("http://www.w3.org/1999/xlink", "href");
//				list.add(str);
			}

			count++;
		}
		System.out.println("Found "+count+" texts");
		return list;
	}
}
