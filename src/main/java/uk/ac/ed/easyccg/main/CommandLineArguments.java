package uk.ac.ed.easyccg.main;

import java.io.File;
import java.util.List;
import uk.co.flamingpenguin.jewel.cli.Option;

/**
 * Command Line Interface
 */
public interface CommandLineArguments  {
    @Option(shortName="m", description = "Path to the parser model")
    File getModel();

    @Option(shortName="f", defaultValue="", description = "(Optional) Path to the input text file. Otherwise, the parser will read from stdin.")
    File getInputFile();

    @Option(shortName="i", defaultValue="tokenized", description = "(Optional) Input Format: one of \"tokenized\", \"POStagged\", \"POSandNERtagged\", \"gold\", \"deps\" or \"supertagged\"")
    String getInputFormat();

    @Option(shortName="o", description = "Output Format: one of \"ccgbank\", \"html\", or \"prolog\"", defaultValue="ccgbank")
    String getOutputFormat();

    @Option(shortName="l", defaultValue="70", description = "(Optional) Maximum length of sentences in words. Defaults to 70.")
    int getMaxLength();

    @Option(shortName="n", defaultValue="1", description = "(Optional) Number of parses to return per sentence. Defaults to 1.")
    int getNbest();

    @Option(shortName="r", defaultValue={"S[dcl]", "S[wq]", "S[q]", "S[qem]", "NP"}, description = "(Optional) List of valid categories for the root node of the parse. Defaults to: S[dcl] S[wq] S[q] NP")
    List<String> getRootCategories();

    @Option(shortName="s", description = "(Optional) Allow rules not involving category combinations seen in CCGBank. Slows things down by around 20%.")
    boolean getUnrestrictedRules();

    @Option(description = "(Optional) If true, print detailed timing information.")
    boolean getTiming();

    @Option(defaultValue="0.0001", description = "(Optional) Prunes lexical categories whose probability is less than this ratio of the best category. Defaults to 0.0001.")
    double getSupertaggerbeam();

    @Option(defaultValue="50", description = "(Optional) Maximum number of categores per word output by the supertagger. Defaults to 50.")
    int getMaxTagsPerWord();

    @Option(defaultValue="0.0", description = "(Optional) If using N-best parsing, filter parses whose probability is lower than this fraction of the probability of the best parse. Defaults to 0.0")
    double getNbestbeam();

    @Option(defaultValue="1", description = "(Optional) Number of threads to use. If greater than 1, the output order may differ from the input.")
    int getThreads();

    @Option(defaultValue="", description = "(Optional) Gold dependencies file, for use in oracle experiments")
    File getGoldDependenciesFile();

    @Option(helpRequest = true, description = "Display this message", shortName = "h")
    boolean getHelp();

    @Option(description = "(Optional) Make a tag dictionary")
    boolean getMakeTagDict();
}
