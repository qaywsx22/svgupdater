package svgupdater;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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

public class ImageFileProcessor {

	private File infile;
	private File outfile;
	private File tmpDir;

	private float quality = 0.8f;
	private int width = 1920;
	private String format = "jpg";
	
	public void setInputFile(File file) {
		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("Source file not specified or not exist");
		}
		this.infile = file;
	}

	public File getInputFile() {
		return this.infile;
	}

	public void setOutputFile(File file) {
		if (file == null || (file.exists() && (file.isDirectory() || !file.canWrite()))) {
			throw new IllegalArgumentException("Target file not specified or is readonly");
		}
		this.outfile = file;
	}

	public File getOutputFile() {
		return this.outfile;
	}

	public void setTempDirectory(File dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			throw new IllegalArgumentException("Temp directory not specified or is not directory");
		}
		this.tmpDir = dir;
	}

	public File getTempDirectory() {
		return this.tmpDir;
	}

	public void setQuality(float q) {
		this.quality = q;
	}

	public float getQuality() {
		return this.quality;
	}

	public void setWidth(int w) {
		this.width = w;
	}

	public int getWidth() {
		return this.width;
	}

	public void setExportFormat(String f) {
		if (f == null || !(f.toLowerCase().equals("jpg") || f.toLowerCase().equals("png"))) {
			throw new IllegalArgumentException("Export format " + f + " false or not specified. Allowed only jpg or png");
		}
		this.format = f;
	}

	public String getExportFormat() {
		return this.format;
	}
	
	public Document openSVGFile() {
		Document doc = null;
		FileInputStream svgInputStream = null;
		try {
			// Load input file
			svgInputStream = new FileInputStream(getInputFile());
			// Load SVG into DOM-Tree
			String parser = XMLResourceDescriptor.getXMLParserClassName();
			SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
			doc = factory.createDocument(parser, svgInputStream);
		} catch (Exception exc) {
			System.out.println("Error open SVG source file");
			System.exit(4);
		}
		return doc;
	}

	public void saveSVGFile(Document doc) {
		try {
			byte[] fileData = transcodeToSVG(doc);
			FileOutputStream fileSave = new FileOutputStream(getOutputFile());
			fileSave.write(fileData);
			fileSave.close();		
		} catch (Exception e) {
			System.out.println("Error write SVG target file");
			System.exit(5);
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
			System.out.println("Error in SVG file transcoding");
			System.exit(6);
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
        t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(getWidth()));
        t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(getQuality()));
        // Set the transcoder input and output.
        TranscoderInput input = new TranscoderInput(doc);
        OutputStream ostream = new FileOutputStream(getOutputFile());
        TranscoderOutput output = new TranscoderOutput(ostream);
        // Perform the transcoding.
        t.transcode(input, output);
        ostream.flush();
        ostream.close();
		System.out.println("Export "+getInputFile().getPath()+" to "+getOutputFile().getPath());
    }

    public void exportPNG(Document doc) throws Exception {
        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(getWidth()));
        // Set the transcoder input and output.
        TranscoderInput input = new TranscoderInput(doc);
        OutputStream ostream = new FileOutputStream(getOutputFile());
        TranscoderOutput output = new TranscoderOutput(ostream);
        // Perform the transcoding.
        t.transcode(input, output);
        ostream.flush();
        ostream.close();
		System.out.println("Export "+getInputFile().getPath()+" to "+getOutputFile().getPath());
    }

    public void markup(Document doc) {
		int imgCount = markupImages(doc);
		int txtCount = markupTexts(doc);
		System.out.println("Found "+imgCount+" images and "+txtCount+" texts");
		saveSVGFile(doc);
    }

    public void parse(Document doc) {
		List<String> imageList = null;
		List<String> textList = null;
		File imgListFile;
		File textListFile;
		imageList = getListOfImages(doc);
		if (imageList != null && !imageList.isEmpty()) {
			try {
				imgListFile = new File(getTempDirectory(), getInputFile().getName() + CommandProcessor.IMAGE_FILE_SUFFIX);
				PrintWriter w = new PrintWriter(imgListFile);
				for (String str : imageList) {
					w.println(str);
				}
				w.flush();
				w.close();
			}
			catch (Exception exc) {
				System.out.println("Error write image list file");
				System.exit(7);
			}
		}
		else {
			System.out.println("No images found");
		}
		textList = getListOfTexts(doc);
		if (textList != null && !textList.isEmpty()) {
			try {
				textListFile = new File(getTempDirectory(), getInputFile().getName() + CommandProcessor.TEXT_FILE_SUFFIX);
				PrintWriter w = new PrintWriter(textListFile);
				for (String str : textList) {
					w.println(str);
				}
				w.flush();
				w.close();
			}
			catch (Exception exc) {
				System.out.println("Error write text list file");
				System.exit(8);
			}
		}
		else {
			System.out.println("No texts found");
		}
    }

    public void write(Document doc) {
		File imgListFile;
		File textListFile;
		List<String> imageList = null;
		List<String> textList = null;
		Scanner scanner = null;
		try {
			imgListFile = new File(getTempDirectory(), getInputFile().getName() + CommandProcessor.IMAGE_FILE_SUFFIX);
			scanner = new Scanner(imgListFile);
			if (scanner.hasNextLine()) {
				imageList = new ArrayList<String>();
			}
			while(scanner.hasNextLine()) {
				imageList.add(scanner.nextLine());
			}
		}
		catch (Exception exc) {
			System.out.println("Error in image list file processing");
			System.exit(9);
		}
		if (imageList == null || imageList.isEmpty()) {
			System.out.println("No images found");
		}

		try {
			textListFile = new File(getTempDirectory(), getInputFile().getName() + CommandProcessor.TEXT_FILE_SUFFIX);
			scanner = new Scanner(textListFile);
			if (scanner.hasNextLine()) {
				textList = new ArrayList<String>();
			}
			while(scanner.hasNextLine()) {
				textList.add(scanner.nextLine());
			}
		}
		catch (Exception exc) {
			System.out.println("Error in text list file processing");
			System.exit(10);
		}

		if (textList == null || textList.isEmpty()) {
			System.out.println("No texts found");
		}
		modifySVGFile(doc, imageList, textList);
		saveSVGFile(doc);
    }

    public void export(Document doc) {
		try {
			if ("png".equals(format)) {
				exportPNG(doc);
			}
			else {
				exportJPEG(doc);
			}
		}
		catch (Exception exc) {
			System.out.println("Error in export file processing");
			System.exit(11);
		}
    }
}
