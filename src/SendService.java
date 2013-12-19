import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SendService {
    private Map<String, Connection> connections = Collections.synchronizedMap(new HashMap<String, Connection>());
    private ExecutorService threadPool = Executors.newFixedThreadPool(50);

    public SendService(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(10000);
                        System.out.println("Connected: "+connections.size());
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }).start();
    }

    public void registerConnection(Connection connection) {
        System.out.println("Register "+connection.getName());
        connections.put(connection.getName(), connection);
    }

    public void unregisterConnection(Connection connection) {
        System.out.println("Unregister "+connection.getName());
        connections.remove(connection.getName());
    }

    public void sendTo(String to, String message) {
        Connection connection = connections.get(to);
        sendTo(connection, message);
    }

    public void sendTo(Connection connection, String message) {
        if (connection != null) {
            try {
                connection.sendText(message);

            } catch (IOException e) {
                System.out.println(connection.getName()+" disconnected");
                connection.unbind();
                unregisterConnection(connection);
            }
        }
    }

    public Connection handleConnection(Socket socket) {
        Connection connection = new Connection(socket, this);
        registerConnection(connection);
        threadPool.submit(connection);
        return connection;
    }

    public boolean checkExist(String contact) {
        return null != connections.get(contact);
    }

    public String activeConnections(){
        StringBuilder res = new StringBuilder();
        for (String s : connections.keySet()){
            res.append(s).append(", ");
        }
        int to = res.lastIndexOf(", ");
        return res.substring(0, to);
    }

    public boolean checkBusy(String contact) {
        Connection connection = connections.get(contact);
        return (null == connection || connection.isConnected());
    }

    public Connection get(String contact) {
        return connections.get(contact);
    }
}
