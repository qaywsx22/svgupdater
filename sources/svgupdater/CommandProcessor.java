package svgupdater;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathResult;

public class CommandProcessor {

	private static final String PARSE_COMMAND = "parse";
	private static final String WRITE_COMMAND = "write";
	private static final String EXPORT_COMMAND = "export";
	private static final String MARKUP_COMMAND = "setids";

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
		List<String> imageList = null;
		List<String> textList = null;
		String infilePath = null;
		String outfilePath = null;
		String tmpDirPath = null;
		String command = args[0].toLowerCase();
		switch (command) {
			case CommandProcessor.MARKUP_COMMAND:
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
			case CommandProcessor.MARKUP_COMMAND:
				int imgCount = cp.markupImages(doc);
				int txtCount = cp.markupTexts(doc);
				System.out.println("Found "+imgCount+" images and "+txtCount+" texts");
				cp.saveSVGFile(doc);
				break;
			case CommandProcessor.PARSE_COMMAND:
				imageList = cp.getListOfImages(doc);
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
				textList = cp.getListOfTexts(doc);
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
			case CommandProcessor.WRITE_COMMAND:
				File imgListFile = null;
				File txtListFile = null;
				Scanner scanner = null;
				try {
					imgListFile = new File(cp.getTempDirectory(), CommandProcessor.IMAGE_FILE_NAME);
					scanner = new Scanner(imgListFile);
					if (scanner.hasNextLine()) {
						imageList = new ArrayList<String>();
					}
					while(scanner.hasNextLine()) {
						imageList.add(scanner.nextLine());
					}
				}
				catch (Exception exc) {
					exc.printStackTrace(System.out);
				}
				if (imageList == null || imageList.isEmpty()) {
					System.out.println("No images found");
				}

				try {
					txtListFile = new File(cp.getTempDirectory(), CommandProcessor.TEXT_FILE_NAME);
					scanner = new Scanner(txtListFile);
					if (scanner.hasNextLine()) {
						textList = new ArrayList<String>();
					}
					while(scanner.hasNextLine()) {
						textList.add(scanner.nextLine());
					}
				}
				catch (Exception exc) {
					exc.printStackTrace(System.out);
				}

				if (textList == null || textList.isEmpty()) {
					System.out.println("No texts found");
				}
				cp.modifySVGFile(doc, imageList, textList);
				cp.saveSVGFile(doc);
				break;
			case CommandProcessor.EXPORT_COMMAND:
				try {
					cp.exportJPEG(doc);
//					cp.exportPNG(doc);
				}
				catch (Exception exc) {
					exc.printStackTrace(System.out);
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

	public void saveSVGFile(Document doc) {
		try {
			byte[] fileData = transcodeToSVG(doc);
			FileOutputStream fileSave = new FileOutputStream(outfile);
			fileSave.write(fileData);
			fileSave.close();		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	public int markupImages(Document doc) {
		int count= 0;
		XPathEvaluator xpathEvaluator = (XPathEvaluator) doc;
		SVGElement searchRoot = ((SVGDocument)doc).getRootElement();
		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"image\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
		Node node;
		SVGElement el;
		while ((node = result.iterateNext()) != null) {
			if (node instanceof SVGElement) {
				el = (SVGElement)node;
				el.setAttribute("id", "img" + String.valueOf(count++));
			}
		}
		return count;
	}
	
	public int markupTexts(Document doc) {
		int count= 0;
		XPathEvaluator xpathEvaluator = (XPathEvaluator) doc;
		SVGElement searchRoot = ((SVGDocument)doc).getRootElement();
		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"text\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
		Node node, child;
		while ((node = result.iterateNext()) != null) {
			if (node.hasChildNodes()) {
				NodeList children = node.getChildNodes();
				int len = children.getLength();
				if (len > 1) {
					for (int i = 0; i < len; i++) {
						child = children.item(i);
						if ("tspan".equals(child.getNodeName())) {
							((SVGElement)child).setAttribute("id", "txt" + String.valueOf(count++));
						}
//						else {
//							System.out.println("Strange child found. i="+i+", name="+child.getNodeName()+", value="+child.getNodeValue());
//						}
					}
				}
				else {
					((SVGElement)node).setAttribute("id", "txt" + String.valueOf(count++));
				}
			}
		}
		return count;
	}

	public List<String> getListOfTexts(Document doc) {
		XPathEvaluator xpathEvaluator = (XPathEvaluator) doc;
		SVGElement searchRoot = ((SVGDocument)doc).getRootElement();
//		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"image\" and @class=\"img-editable\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
		XPathResult result = (XPathResult) xpathEvaluator.evaluate(".//*[local-name()=\"text\"]", searchRoot, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);

		int count= 0;
		List<String> list = new ArrayList<String>();
		String str;
		Node node, child;
		while ((node = result.iterateNext()) != null) {
			if (node.hasChildNodes()) {
				NodeList children = node.getChildNodes();
				int len = children.getLength();
				if (len > 1) {
					for (int i = 0; i < len; i++) {
						child = children.item(i);
						if ("tspan".equals(child.getNodeName())) {
							str = ((SVGElement)child).getAttribute("id") + "," + child.getTextContent();
							list.add(str);
							count++;
						}
//						else {
//							System.out.println("Strange child found. i="+i+", name="+child.getNodeName()+", value="+child.getNodeValue());
//						}
					}
				}
				else {
					str = ((SVGElement)node).getAttribute("id") + "," + node.getTextContent();
					list.add(str);
					count++;
				}
			}
		}
		System.out.println("Found "+count+" texts");
		return list;
	}

	public byte[] transcodeToSVG(Document doc) throws TranscoderException {
	    try {
	        //Determine output type:
	        SVGTranscoder t = new SVGTranscoder();

	        //Set transcoder input/output
	        TranscoderInput input = new TranscoderInput(doc);
	        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
	        OutputStreamWriter ostream = new OutputStreamWriter(bytestream, "UTF-8");
	        TranscoderOutput output = new TranscoderOutput(ostream);

	        //Perform transcoding
	        t.transcode(input, output);
	        ostream.flush();
	        ostream.close();

	        return bytestream.toByteArray();

	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	    return null;
	}	

	public void modifySVGFile(Document doc, List<String> imageList, List<String> textList) {
		String id, value;
		Element el;
		//
		for (String str : imageList) {
			int komma = str.indexOf(",");
			if (komma < 0) {
				continue;
			}
			id = str.substring(0,  komma);
			value = str.substring(komma + 1);
			el = doc.getElementById(id);
			if (el != null) {
				el.setAttributeNS("http://www.w3.org/1999/xlink", "href", value);
			}
		}
		Map<String, String> textMap = new HashMap<String, String>();
		for (String str : textList) {
			int komma = str.indexOf(",");
			if (komma < 0) {
				continue;
			}
			id = str.substring(0,  komma);
			value = str.substring(komma + 1);
			el = doc.getElementById(id);
			if (el != null) {
				((Node)el).setTextContent(value);
			}
		}
		
	}

    public void exportJPEG(Document doc) throws Exception {
        JPEGTranscoder t = new JPEGTranscoder();
        t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(3800));
        t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));
        // Set the transcoder input and output.
        TranscoderInput input = new TranscoderInput(doc);
        OutputStream ostream = new FileOutputStream(outfile);
        TranscoderOutput output = new TranscoderOutput(ostream);
        // Perform the transcoding.
        t.transcode(input, output);
        ostream.flush();
        ostream.close();
		System.out.println("Export "+infile.getPath()+" to "+outfile.getPath());
    }

    public void exportPNG(Document doc) throws Exception {
        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(3800));
//        t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));
        // Set the transcoder input and output.
        TranscoderInput input = new TranscoderInput(doc);
        OutputStream ostream = new FileOutputStream(outfile);
        TranscoderOutput output = new TranscoderOutput(ostream);
        // Perform the transcoding.
        t.transcode(input, output);
        ostream.flush();
        ostream.close();
		System.out.println("Export "+infile.getPath()+" to "+outfile.getPath());
    }
}
