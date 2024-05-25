import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client2 extends JFrame {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private JTextArea textArea;
    private JTextField inputField;
    private JTextField ipField;
    private JTextField portField;
    private JTextField usernameField;
    private JButton connectButton;
    private JButton disconnectButton;

    public Client2() {
        setTitle("Client 2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400); // Frame boyutunu artırdık
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        add(inputField, BorderLayout.SOUTH);

        inputField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        add(sendButton, BorderLayout.EAST);

        JPanel connectionPanel = new JPanel(new GridLayout(4, 2)); // Satır sayısını artırdık
        JLabel ipLabel = new JLabel("Server IP:");
        ipField = new JTextField("localhost");
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("12345");
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField();
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnectFromServer());
        connectionPanel.add(ipLabel);
        connectionPanel.add(ipField);
        connectionPanel.add(portLabel);
        connectionPanel.add(portField);
        connectionPanel.add(usernameLabel);
        connectionPanel.add(usernameField);
        connectionPanel.add(connectButton); // Connect butonunu panel'e ekle
        connectionPanel.add(disconnectButton); // Disconnect butonunu panel'e ekle
        add(connectionPanel, BorderLayout.NORTH);
    }

    private void sendMessage() {
        if (outputStream == null) {
            JOptionPane.showMessageDialog(this, "Sunucu ile bağlantı kurulmadı!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String message = inputField.getText();
        if (!message.isEmpty()) {
            try {
                outputStream.writeUTF(message);
                outputStream.flush();
                inputField.setText("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void connectToServer() {
        String serverIP = ipField.getText();
        int serverPort = Integer.parseInt(portField.getText());
        String username = usernameField.getText();

        try {
            socket = new Socket(serverIP, serverPort);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            // Kullanıcı adını sunucuya gönder
            outputStream.writeUTF(username);

            ClientReadThread readThread = new ClientReadThread();
            readThread.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Bağlantı hatası! Sunucuya bağlanılamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void disconnectFromServer() {
        try {

            if (socket != null && !socket.isClosed()) {
                socket.close();
                textArea.append("Sunucu bağlantısı kesildi.\n");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Bağlantı kesilirken hata oluştu.", "Hata", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private class ClientReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    String message = inputStream.readUTF();
                    textArea.append("Received message: " + message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client2 client = new Client2();
            client.setVisible(true);
        });
    }
}
