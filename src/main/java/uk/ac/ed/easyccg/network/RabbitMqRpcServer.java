package uk.ac.ed.easyccg.network;

import com.rabbitmq.client.StringRpcServer;
import com.rabbitmq.client.Channel;
import uk.co.flamingpenguin.jewel.cli.CliFactory;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import uk.ac.ed.easyccg.main.ParserManager;
import uk.ac.ed.easyccg.main.CommandLineArguments;

/**
 * RabbitMQ RPC server that accepts a string sentence as a request and parses
 * it with the EasyCCG parser.
 *
 * The server returns the first parse tree in default format of the supplied
 * sentence.
 */
class RabbitMqRpcServer extends StringRpcServer {

    private ParserManager parser;

    public RabbitMqRpcServer(Channel channel, String queueName) throws Exception {
        super(channel, queueName);
        parser = ParserManager.getInstance();
        parser.setup(fakeCommandLineArguments("-m models/model"));
    }

    @Override
    public String handleStringCall(String sentence) {
        return parser.parse(sentence);
    }

    /**
     * Convert a command line argument string, like "-m model -f file" to a
     * CommandLineArguments object.
     */
    private CommandLineArguments fakeCommandLineArguments(String cmdStr) {
        String[] args = cmdStr.split("\\s+");
        CommandLineArguments cmdArgs = null;
        try {
            cmdArgs = CliFactory.parseArguments(CommandLineArguments.class, args);
        } catch (ArgumentValidationException e) {
            e.printStackTrace();
        }
        return cmdArgs;
    }
} 
