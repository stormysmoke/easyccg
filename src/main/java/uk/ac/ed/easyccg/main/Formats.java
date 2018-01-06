package uk.ac.ed.easyccg.main;

import uk.ac.ed.easyccg.syntax.ParsePrinter;

public class Formats {

    // Set of supported InputFormats
    public static enum InputFormat {
        TOKENIZED, GOLD, SUPERTAGGED, POSTAGGED, POSANDNERTAGGED
    }

    // Set of supported OutputFormats
    public static enum OutputFormat {
        CCGBANK(ParsePrinter.CCGBANK_PRINTER), 
        HTML(ParsePrinter.HTML_PRINTER), 
        SUPERTAGS(ParsePrinter.SUPERTAG_PRINTER),
        PROLOG(ParsePrinter.PROLOG_PRINTER), 
        EXTENDED(ParsePrinter.EXTENDED_CCGBANK_PRINTER), 
        DEPS(new ParsePrinter.DependenciesPrinter());

        public final ParsePrinter printer;
        OutputFormat(ParsePrinter printer) {
            this.printer = printer;
        }
    }
}
