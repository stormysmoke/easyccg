package uk.ac.ed.easyccg.network;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

/**
 * Manager class for the server that handles requests for the EasyCCG parser.
 */
public class ServerManager {

    private static ServerManager instance;
    private final static String QUEUE = System.getenv("CHATBOT_TO_EASYCCG");

    protected ServerManager() {}

    public static ServerManager getInstance() {
        if (instance == null) instance = new ServerManager();
        return instance;
    }

    /**
     * Start listening for RPC requests to the EasyCCG parser.
     */
    public void start() {
        try {
            // Establish connection to RabbitMQ server
            String uri = System.getenv("CLOUDAMQP_URL");
            if (uri == null) uri = "amqp://guest:guest@localhost";
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(uri);
            Channel channel = factory.newConnection().createChannel();

            // Declare queue on which to listen for RPC calls
            channel.queueDeclare(QUEUE, false, false, false, null);

            // Create RPC server
            RabbitMqRpcServer server = new RabbitMqRpcServer(channel, QUEUE);

            // Start listening for RPC requests
            System.out.println(" [x] Awaiting RPC requests");
            server.mainloop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
