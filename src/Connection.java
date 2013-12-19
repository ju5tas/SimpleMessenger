import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class Connection implements Runnable {
    private static int idGenerator = 0;

    private final String selfName;
    private final Socket socket;
    private final SendService sender;

    private Connection partner;

    private PrintStream printStream;
    private BufferedReader reader;

    public Connection(Socket socket, SendService sender) {
        selfName = String.valueOf(++idGenerator);
        this.socket = socket;
        this.sender = sender;
    }
    
    private PrintStream getPrintStream() throws IOException {
        if (printStream == null){
            printStream = new PrintStream(socket.getOutputStream());
        }
        return printStream;
    }

    private BufferedReader getReader() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        return reader;
    }

    private void unbindUnregisterClose() throws IOException {
        System.out.println("Connection "+selfName+" closing");
        unbind();
        sender.unregisterConnection(this);
        socket.close();
    }

    public void sendText(String text) throws IOException {
        getPrintStream().println(text);
    }

    private String receiveText() throws IOException {
        return getReader().readLine();
    }

    public String getName(){
        return selfName;
    }

    public boolean isConnected(){
        return partner != null;
    }

    private void setPartner(Connection partner){
        this.partner = partner;
    }

    private void bind(Connection partner){
        System.out.println("Bind " + selfName + " and " + partner);
        if (!isConnected()){
            partner.setPartner(this);
            this.setPartner(partner);
        }
    }

    public void unbind(){
        System.out.println("Unbind "+selfName+" and "+partner);
        if (isConnected()){
            sender.sendTo(partner.getName(), "Client interrupted the connection.");
            partner.setPartner(null);
            this.setPartner(null);
        }
    }

    @Override
    public void run() {
        try {
            commandHelp();

            boolean quit = false;
            boolean skip;

            while (!quit){
                String message;
                if ((message = receiveText()) == null) {
                    System.out.println("Received null text. Connection will close.");
                    unbindUnregisterClose();
                    return;
                }

                message = message.trim();

                int a = message.indexOf(' ');
                String paramStr = message.substring(0, a == -1 ? message.length() : a).toLowerCase();

                skip = true;
                switch (paramStr){
                    case "":
                        break;
                    case "/quit":
                        quit = true;
                        unbindUnregisterClose();
                        break;
                    case "/help":
                        commandHelp();
                        break;
                    case "/hangup":
                        commandHangUp();
                        break;
                    case "/connect":
                        commandConnect(message.replace("/connect", "").trim());
                        break;
                    case "/who":
                        commandWho();
                        break;
                    default:
                        skip = false;
                }
                if (skip) continue;
//                if (message.isEmpty()) {
//                    continue;
//                }
//                else if (message.equals("/help")){
//                    commandHelp();
//                    continue;
//                }
//                else if (message.equals("/quit")){
//                    unbindUnregisterClose();
//                    quit = true;
//                    continue;
//                }
//                else if (message.equals("/hangup")){
//                    commandHangUp();
//                    continue;
//                }
//                else if (message.startsWith("/connect ")){
//                    partnerName = message.substring(9).trim();
//                    commandConnect(partnerName);
//                    continue;
//                }

                if (isConnected()){
                    message = "> "+message;
                    sender.sendTo(partner.getName(), message);
                }
                else {
                    message = "Unsupported command";
                    sendText(message);
                }
            }
        } catch (Exception e) {
            System.out.println(">>> Exception in connection "+selfName+". "+e.getMessage());
            try {
                unbindUnregisterClose();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    private void commandHangUp() throws IOException {
        if (isConnected()){
            sendText("Connection closed");
            unbind();
        }
        else {
            sendText("You must /connect before /hangup");
        }
    }

    private void commandConnect(String partnerName) throws IOException {
        System.out.println("Connecting "+ selfName +" -> "+partnerName);
        boolean reject = true;
        if (partnerName.isEmpty()){
            sendText("Command /connect must have one parameter");
        }
        else if (isConnected()){
            sendText("You must /hangup before /connect");
        }
        else if (partnerName.equals(selfName)){
            sendText("You can not connect with yourself");
        }
        else if (!sender.checkExist(partnerName)) {
            sendText(partnerName+" was not found");
            System.out.println(partnerName + " was not found");
        }
        else if (sender.checkBusy(partnerName)){
            sendText(partnerName+" is busy");
            System.out.println(partnerName + " is busy");
        }
        else {
            bind(sender.get(partnerName));
            sendText("Connected with "+partnerName);
            sender.sendTo(partner, "Incoming connection with "+selfName);
            reject = false;
        }
        if (reject){
            System.out.println("Reject connection "+ selfName +" -> "+partnerName+" ");
        }
    }

    private void commandHelp() throws IOException {
        sendText("Your number is " + selfName);
        sendText("Supported commands:");
        sendText("\t/who\t\t- list of active users");
        sendText("\t/connect id\t- connect to user with id");
        sendText("\t/hangup\t\t- interrupt conversation");
        sendText("\t/quit\t\t- close terminal");
        sendText("\t/help\t\t- display this list of commands\r\n");
    }

    private void commandWho() throws IOException {
        sendText("Active connections: "+sender.activeConnections());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connection that = (Connection) o;

        return selfName.equals(that.selfName);
    }

    @Override
    public int hashCode() {
        return selfName.hashCode();
    }

    @Override
    public String toString() {
        return selfName;
    }
}
