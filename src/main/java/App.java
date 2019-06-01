import org.apache.zookeeper.KeeperException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;


public class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static ZKManager zkManager;

    public static void main(String[] args) throws IOException {

        zkManager = new ZKManager();
        zkManager.run();
        handleUserInput();

    }

    private static void handleUserInput() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("ZooKeeper Demo v1.0");
        boolean running = true;
        while(running) {
            String line = null;
            try {
                line = input.readLine().toLowerCase().trim();
            } catch (IOException e) {
                LOGGER.warning("Error while reading input from user!");
                running = false;
                continue;
            }
            switch (line) {
                case "print":
                    try {
                        zkManager.printTree();
                    } catch (KeeperException | InterruptedException e) {
                        LOGGER.severe("Error while print tree!");
                    }
                    break;
                case "quit":
                case "exit":
                    running = false;
                    break;
                case "":
                    break;
                default:
                    System.out.println("Unknown command!");
                    break;
            }
        }
        zkManager.close();
    }

}
