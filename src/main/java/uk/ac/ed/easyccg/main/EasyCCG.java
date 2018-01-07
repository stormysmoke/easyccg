package uk.ac.ed.easyccg.main;

import uk.ac.ed.easyccg.network.ServerManager;

public class EasyCCG {

    public static void main(String[] args) throws Exception {
        ServerManager server = ServerManager.getInstance();
        server.start();
    }
    
}
