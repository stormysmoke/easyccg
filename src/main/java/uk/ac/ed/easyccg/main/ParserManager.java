package uk.ac.ed.easyccg.main;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multimap;

import uk.ac.ed.easyccg.syntax.Category;
import uk.ac.ed.easyccg.syntax.InputReader;
import uk.ac.ed.easyccg.syntax.InputReader.InputToParser;
import uk.ac.ed.easyccg.syntax.ParsePrinter;
import uk.ac.ed.easyccg.syntax.Parser;
import uk.ac.ed.easyccg.syntax.ParserAStar;
import uk.ac.ed.easyccg.syntax.ParserAStar.SuperTaggingResults;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;
import uk.ac.ed.easyccg.syntax.TagDict;
import uk.ac.ed.easyccg.syntax.TaggerEmbeddings;
import uk.ac.ed.easyccg.syntax.Util;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.DependencyParse;
import uk.ac.ed.easyccg.syntax.evaluation.Evaluate;
import uk.ac.ed.easyccg.syntax.evaluation.Evaluate.Results;
import uk.ac.ed.easyccg.main.Formats.InputFormat;
import uk.ac.ed.easyccg.main.Formats.OutputFormat;

/* The code in the method bodies of this class has been mostly copied and
 * pasted from the original EasyCCG class. It hasn't been refactored yet. */

public class ParserManager {

    private static ParserManager instance;

    private CommandLineArguments args;
    private InputFormat input;
    private OutputFormat outputFormat;
    private InputReader reader;
    private Map<String, Collection<Category>> tagDict;
    private Parser parser;
    private ParsePrinter printer;
    private Stopwatch timer;
    private SuperTaggingResults supertaggingResults;
    private Results dependencyResults;
    private int id = 0;
    private int id2;

    // Gold file
    private Iterator<String> goldDependencyParses;
    private InputReader goldInputReader;
    private boolean usingGoldFile;

    protected ParserManager() {}

    public static ParserManager getInstance() {
        if (instance == null) instance = new ParserManager();
        return instance;
    }

    /**
     * Initialise a parser according to the command line arguments
     */
    public void setup(CommandLineArguments args) {
        this.args = args;
        input = InputFormat.valueOf(args.getInputFormat().toUpperCase());
        testIfMakeTagDict();
        if (!args.getModel().exists()) throw new InputMismatchException("Couldn't load model from from: " + args.getModel());

        System.err.println("Loading model...");

        try {
            parser = new ParserAStar(
                new TaggerEmbeddings(args.getModel(), args.getMaxLength(), args.getSupertaggerbeam(), args.getMaxTagsPerWord()), 
                args.getMaxLength(),
                args.getNbest(),
                args.getNbestbeam(),
                input,
                args.getRootCategories(),
                new File(args.getModel(), "unaryRules"),
                new File(args.getModel(), "binaryRules"),
                args.getUnrestrictedRules() ? null : new File(args.getModel(), "seenRules")
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        outputFormat = OutputFormat.valueOf(args.getOutputFormat().toUpperCase());
        printer = outputFormat.printer;

        if ((outputFormat == OutputFormat.PROLOG || outputFormat == OutputFormat.EXTENDED) && input != InputFormat.POSANDNERTAGGED) throw new Error("Must use \"-i POSandNERtagged\" for this output");
    
        timer = Stopwatch.createStarted();
        supertaggingResults = new SuperTaggingResults();
        dependencyResults = new Results();

        // Used in Oracle experiments.
        if (!args.getGoldDependenciesFile().getPath().isEmpty()) {
            if (!args.getGoldDependenciesFile().exists()) {
                throw new RuntimeException("Can't find gold dependencies file: " + args.getGoldDependenciesFile());
            }

            if (input != InputFormat.GOLD) {
                throw new RuntimeException("If evaluating dependencies, must use \"gold\" input format");
            }

            usingGoldFile = true;
            try {
            goldDependencyParses = Util.readFile(args.getGoldDependenciesFile()).iterator();
            } catch (IOException e) {
                e.printStackTrace();
            }
            goldInputReader  = InputReader.make(InputFormat.GOLD, new SyntaxTreeNodeFactory(args.getMaxLength(), 0));
            while (goldDependencyParses.hasNext()) {
                String line = goldDependencyParses.next();
                // Skip header
                if (!line.isEmpty() && !line.startsWith("#")) break;
            }
        } else {
            goldDependencyParses = null;
            goldInputReader = null;
            usingGoldFile = false;
        }

    }

    /**
     * Parse a single sentence.
     */
    public String parse(String line) {
        id++;
        id2 = id;
        final DependencyParse goldParse;
                
        if (goldDependencyParses != null && goldDependencyParses.hasNext()) {
            // For Oracle experiments, read in the corresponding gold parse.
            InputToParser goldInput = goldInputReader.readInput(line);
            goldParse = CCGBankDependencies.getDependencyParseCandC(goldDependencyParses, goldInput.getInputSupertags1best());          
        } else {
            goldParse = null;
        }
        
        List<SyntaxTreeNode> parses = parser.parse(supertaggingResults, line);
        String output;
        if (parses != null && usingGoldFile) {
            if (goldParse != null) {
                output = printer.print(Evaluate.getOracle(parses, goldParse), id2);                
            } else {
                // Just print 1-best when doing Oracle experiments.
                output = printer.print(parses.subList(0, 1), id2);
            }

        } else {
            // Not doing Oracle experiments - print all ouput.
            output = printer.print(parses, id2);
        }
        return output;
    }

    /**
     * Print statistics about the parsing session.
     */
    public void printStats() {
        DecimalFormat twoDP = new DecimalFormat("#.##");
        System.err.println("Coverage: " + twoDP.format(100.0 * supertaggingResults.parsedSentences.get() / supertaggingResults.totalSentences.get()) + "%");
        if (supertaggingResults.totalCats.get() > 0) {
            System.err.println("Accuracy: " + twoDP.format(100.0 * supertaggingResults.rightCats.get() / supertaggingResults.totalCats.get()) + "%");
        }

        if (!dependencyResults.isEmpty()) {
            System.out.println("F1=" + dependencyResults.getF1());
        }

        System.err.println("Sentences parsed: " + supertaggingResults.parsedSentences);
        System.err.println("Speed: " + twoDP.format(1000.0 * supertaggingResults.parsedSentences.get() / timer.elapsed(TimeUnit.MILLISECONDS)) + " sentences per second");

        if (args.getTiming()) {
            printDetailedTiming(parser, twoDP);
        }
    }

    /**
     * Print detailed statistics about how long the sentences took to parse.
     */ 
    private void printDetailedTiming(final Parser parser, DecimalFormat format) {
        int sentencesCovered = 0;
        Multimap<Integer, Long> sentenceLengthToParseTimeInNanos = parser.getSentenceLengthToParseTimeInNanos();
        int binNumber = 0;
        int binSize = 10;
        while (sentencesCovered < sentenceLengthToParseTimeInNanos.size()) {
            double totalTimeForBinInMillis = 0;
            int totalSentencesInBin = 0;
            for (int sentenceLength=binNumber*binSize + 1; sentenceLength < 1 + (binNumber+1) * binSize; sentenceLength=sentenceLength+1) {
                for (long time : sentenceLengthToParseTimeInNanos.get(sentenceLength)) {
                    totalTimeForBinInMillis += ((double) time / 1000000);
                    totalSentencesInBin++;
                }
            }
            sentencesCovered += totalSentencesInBin;
            double averageTimeInMillis = (double) totalTimeForBinInMillis / (totalSentencesInBin);
            if (totalSentencesInBin > 0) {
                System.err.println("Average time for sentences of length " + (1 + binNumber*binSize) + "-" + (binNumber+1) * binSize + " (" + totalSentencesInBin + "): " + format.format(averageTimeInMillis) + "ms");
            }

            binNumber++;
        }

        int totalSentencesTimes1000 = sentenceLengthToParseTimeInNanos.size() * 1000;
        long totalMillis = parser.getParsingTimeOnlyInMillis() + parser.getTaggingTimeOnlyInMillis();
        System.err.println("Just Parsing Time: " + parser.getParsingTimeOnlyInMillis() + "ms " + (totalSentencesTimes1000 / parser.getParsingTimeOnlyInMillis()) + " per second");
        System.err.println("Just Tagging Time: " + parser.getTaggingTimeOnlyInMillis() + "ms " + (totalSentencesTimes1000 / parser.getTaggingTimeOnlyInMillis()) + " per second");
        System.err.println("Total Time:        " + totalMillis + "ms " + (totalSentencesTimes1000 / totalMillis) + " per second");
    }

    /**
     * Test if we have to make just a tag dict instead of parsing sentences.
     *
     * If yes, create the tag dict and terminate the program.
     */
    private void testIfMakeTagDict() {
        if (args.getMakeTagDict()) {
            reader = InputReader.make(input, new SyntaxTreeNodeFactory(args.getMaxLength(), 0));
            try {
            Map<String, Collection<Category>> tagDict = TagDict.makeDict(reader.readFile(args.getInputFile()));
            TagDict.writeTagDict(tagDict, args.getModel());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

}

