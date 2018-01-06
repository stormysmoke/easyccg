package uk.ac.ed.easyccg.main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.File;
import java.util.Iterator;
import java.util.Scanner;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

import uk.ac.ed.easyccg.syntax.Util;

public class EasyCCG {

    public static void main(String[] args) {

        // Read command line arguments
        CommandLineArguments cmd = null;
        try {
            cmd = CliFactory.parseArguments(CommandLineArguments.class, args);
        } catch (ArgumentValidationException e) {
            e.printStackTrace();
        }

        // Initialise parser according to command line arguments
        ParserManager parser = ParserManager.getInstance();
        parser.setup(cmd);

        // Connect to input, either stdin or file
        Iterator<String> inputLines = null;
        File inputFile = cmd.getInputFile();
        if (inputFile.getName().isEmpty()) {
            inputLines = new Scanner(System.in,"UTF-8");
        } else {
            try {
                inputLines = Util.readFile(inputFile).iterator();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedWriter sysout = new BufferedWriter(new OutputStreamWriter(System.out));

        // Main loop: parse all lines in the input
        System.err.println("Parsing...");
        while (inputLines.hasNext()) { 

            String line = inputLines instanceof Scanner ? ((Scanner) inputLines).nextLine().trim() : inputLines.next();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Parse sentence and write output to stdout
            String output = parser.parse(line);
            try {
                sysout.write(output);
                sysout.newLine();
                sysout.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Print statistics of this parsing session
        parser.printStats();
    }
    
}
