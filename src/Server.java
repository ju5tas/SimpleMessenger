import java.io.*;
import java.net.ServerSocket;

public class Server {
    private final int port;
    private ServerSocket serverSocket;
    private SendService sender;

    public Server(int port) {
        this.port = port;
        start();
    }

    private void start() {
        sender = new SendService();
        try {
            serverSocket = new ServerSocket(port);

            while (true){
                sender.handleConnection(serverSocket.accept());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
