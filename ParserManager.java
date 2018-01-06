package uk.ac.ed.easyccg.main;


public class ParserManager {

    private ParserManager instance;
    private CommandLineArguments args;

    protected ParserManager() {}

    public ParserManager getInstance() {
        if (instance == null) instance = new ParserManager();
        return instance;
    }

    public void setCommandLineArgs(CommandLineArguments args) {
        this.args = args;
    }

}

