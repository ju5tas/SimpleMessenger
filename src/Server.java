import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
        start();
    }

    private void start() {
        SendService sender = new SendService();
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (true) {
                sender.handleConnection(serverSocket.accept());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
