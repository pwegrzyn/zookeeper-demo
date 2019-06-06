import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;


public class ZKManager {

    private static final Logger LOGGER = Logger.getLogger(ZKManager.class.getName());
    private final ZooKeeper zooKeeper;
    private final String watchedZnode;
    private static final String ZK_SERVERS = "localhost:2181,localhost:2182,localhost:2183";
    private static final int sessionTimeout = 5000;
    private String appToRun;
    private final ZnodeWatcher znodeWatcher;
    private final ChildrenWatcher childrenWatcher;

    public ZKManager() throws IOException {
        this.childrenWatcher = new ChildrenWatcher();
        this.znodeWatcher = new ZnodeWatcher();
        this.watchedZnode = "/z";
        this.zooKeeper = new ZooKeeper(ZK_SERVERS, sessionTimeout, null);
    }

    public void run() {
        try {
            this.zooKeeper.exists(this.watchedZnode, this.znodeWatcher);
        } catch (KeeperException | InterruptedException e) {
            LOGGER.severe("Error while checking the existence of watched znode!");
        }
    }

    public void close() {
        try {
            this.zooKeeper.close();
        } catch (InterruptedException e) {
            LOGGER.warning("Error while closing ZooKeeper instance!");
            return;
        }
    }

    public void printTree() throws KeeperException, InterruptedException {
        printTree(this.watchedZnode);
    }

    public void setApp(String appName) {
        this.appToRun = appName;
    }

    private void printTree(String znode) throws KeeperException, InterruptedException {
        if (this.zooKeeper.exists(this.watchedZnode, false) == null) {
            System.out.println("The tree has no Z root!");
            return;
        }
        List<String> children = this.zooKeeper.getChildren(znode, false);
        System.out.println(znode);
        for (String child: children) {
            printTree(znode + "/" + child);
        }
    }

    private int countChildren(String path) throws KeeperException, InterruptedException {
        int amount = 0;
        List<String> children = this.zooKeeper.getChildren(path, false);
        for (String child: children){
            String childrenPath = path + "/" + child;
            amount += countChildren(childrenPath);
        }
        amount += children.size();
        return amount;
    }

    private void runApplication(String command)  {
        if (command == null) {
            System.out.println("App not specified! Can't open.");
            return;
        }
        Runtime run = Runtime.getRuntime();
        String[] cmd = new String[3];
        cmd[0] = "cmd.exe" ;
        cmd[1] = "/C" ;
        cmd[2] = command;
        try {
            Process process = run.exec(cmd);
            System.out.println("App successfully launched");
        } catch (IOException e) {
            LOGGER.severe("Could not launch custom app!");
        }
    }

    private void stopApplication() {
        if (this.appToRun == null) {
            System.out.println("App not specified! Can't close.");
            return;
        }
        try {
            Runtime.getRuntime().exec("taskkill /F /IM " + this.appToRun + ".exe");
            System.out.println("Stopped custom app.");
        } catch (IOException e) {
            LOGGER.severe("Error while force-closing app!");
        }
    }


    private class ZnodeWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            switch (event.getType()) {
                case NodeCreated:
                    try {
                        // Start watching children
                        ZKManager.this.zooKeeper.getChildren(ZKManager.this.watchedZnode, ZKManager.this.childrenWatcher);
                    } catch (KeeperException | InterruptedException e) {
                        LOGGER.severe("Error while setting up children watcher!");
                        return;
                    }
                    ZKManager.this.runApplication(ZKManager.this.appToRun);
                    break;
                case NodeDeleted:
                    ZKManager.this.stopApplication();
                    break;
                default: break;
            }
            try {
                // Call self again
                ZKManager.this.zooKeeper.exists(ZKManager.this.watchedZnode, this);
            } catch (KeeperException | InterruptedException e) {
                LOGGER.severe("Error while setting the lasting Z node watcher!");
                return;
            }
        }

    }

    private class ChildrenWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            switch (event.getType()) {
                case NodeChildrenChanged:
                    try {
                        // Print new number of children
                        int newChildrenCount = ZKManager.this.countChildren(ZKManager.this.watchedZnode);
                        System.out.println("The Z node has currently: " + newChildrenCount + " children.");

                        // Start watching the children's children
                        List<String> children = ZKManager.this.zooKeeper.getChildren(event.getPath(), null);
                        for (String child : children) {
                            ZKManager.this.zooKeeper.getChildren(event.getPath() + "/" + child, this);
                        }
                    } catch (KeeperException | InterruptedException e) {
                        LOGGER.severe("Error while counting children during children creation/deletion!");
                        return;
                    }
                    try {
                        // Call self again
                        ZKManager.this.zooKeeper.getChildren(event.getPath(), this);
                    } catch (KeeperException | InterruptedException e) {
                        LOGGER.severe("Error while setting the lasting children watcher!");
                        return;
                    }
                    break;
                default: break;
            }
        }
    }

}
