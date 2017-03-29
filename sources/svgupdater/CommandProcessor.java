package svgupdater;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;

public class CommandProcessor {
	public static final String version = "0.13";

	static final String PARSE_COMMAND = "parse";
	static final String WRITE_COMMAND = "write";
	static final String EXPORT_COMMAND = "export";
	static final String MARKUP_COMMAND = "markup";

	static final String TEXT_FILE_SUFFIX = "_texts.csv";
	static final String IMAGE_FILE_SUFFIX = "_images.csv";

	CommandLine line = null;
    Options options = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CommandProcessor cp = new CommandProcessor();
		cp.initOptions();

		CommandLineParser parser = new DefaultParser();
	    try {
	        // parse the command line arguments
	        cp.line = parser.parse(cp.options, args);
	    }
	    catch( ParseException exp ) {
	        System.err.println( "Parsing error: " + exp.getMessage());
			System.exit(1);
	    }
		
		ImageFileProcessor ifp = new ImageFileProcessor();
		cp.initImageFileProcessor(ifp);
		
		String command = null;
		if (cp.line.hasOption(CommandProcessor.MARKUP_COMMAND)) {
			command = CommandProcessor.MARKUP_COMMAND;
		}
		else if (cp.line.hasOption(CommandProcessor.PARSE_COMMAND)) {
			command = CommandProcessor.PARSE_COMMAND;
		}
		else if (cp.line.hasOption(CommandProcessor.WRITE_COMMAND)) {
			command = CommandProcessor.WRITE_COMMAND;
		}
		else if (cp.line.hasOption(CommandProcessor.EXPORT_COMMAND)) {
			command = CommandProcessor.EXPORT_COMMAND;
		}
		if (command == null) {
			System.out.println("Invalid or missing command option.");
			cp.showHelp(cp.options);
			System.exit(3);
		}

		Document doc = ifp.openSVGFile();
		switch (command) {
			case CommandProcessor.MARKUP_COMMAND:
				ifp.markup(doc);
				break;
			case CommandProcessor.PARSE_COMMAND:
				ifp.parse(doc);
				break;
			case CommandProcessor.WRITE_COMMAND:
				ifp.write(doc);
				break;
			case CommandProcessor.EXPORT_COMMAND:
				ifp.export(doc);
				break;
		}
		System.exit(0);
	}

	private void initOptions() {
		options = new Options();

		Option opt = new Option(CommandProcessor.MARKUP_COMMAND, "set identifiers for all text and image elements");
		options.addOption(opt);

		opt = new Option(CommandProcessor.PARSE_COMMAND, "make lists of text and image elements"); 
		options.addOption(opt);

		opt = new Option(CommandProcessor.WRITE_COMMAND, "write content of text and image elements from items in corresponding list files"); 
		options.addOption(opt);

		opt = new Option(CommandProcessor.EXPORT_COMMAND, "export image from svg to jpg or png format"); 
		options.addOption(opt);
				
		opt = Option.builder("source")
				.argName("file")
                .hasArg()
                .desc("source file")
                .build( );
		options.addOption(opt);

		opt = Option.builder("target")
			.argName("file")
			.hasArg()
			.desc("target file")
			.build();
		options.addOption(opt);

		opt = Option.builder("tempdir")
				.argName("path")
				.hasArg()
				.desc("path to directory with temporary list files")
				.build();
		options.addOption(opt);

		opt = Option.builder("format")
				.hasArg()
				.argName("FORMAT")
				.valueSeparator()
				.desc("target file format for export. Can be PNG or JPG")
				.build();
		options.addOption(opt);

		opt = Option.builder("width")
				.hasArg()
				.argName("WIDTH")
				.valueSeparator()
				.desc("width of the exported target image")
				.build();
		options.addOption(opt);

		opt = Option.builder("quality")
				.hasArg()
				.argName("FACTOR")
				.valueSeparator()
				.desc("jpeg encoder quality factor. Float (between 0 and 1). 1 - no lossy")
				.build();
		options.addOption(opt);

		opt= new Option( "help", "print this message" );
		options.addOption(opt);
	}

	private void initImageFileProcessor(ImageFileProcessor ipf) {
		String str;
		try {
			if (line.hasOption("source")) {
				str = line.getOptionValue("source");
				ipf.setInputFile(new File(str));
			}
			if (line.hasOption("target")) {
				str = line.getOptionValue("target");
				ipf.setOutputFile(new File(str));
			}
			if (line.hasOption("tempdir")) {
				str = line.getOptionValue("tempdir");
				ipf.setTempDirectory(new File(str));
			}
			if (line.hasOption("format")) {
				str = line.getOptionValue("format");
				ipf.setExportFormat(str);
			}
			if (line.hasOption("width")) {
				str = line.getOptionValue("width");
				ipf.setWidth(Integer.parseInt(str));
			}
			if (line.hasOption("quality")) {
				str = line.getOptionValue("quality");
				ipf.setQuality(Float.parseFloat(str));
			}
		}
		catch (Exception exc) {
			System.out.println("Initialisation failed. Illegal or invalid parameter found.");
			showHelp(this.options);
			System.exit(2);
		}
	}
	
	private void showHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp( "svgupdater", options);		
	}
}
