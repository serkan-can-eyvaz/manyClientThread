import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server extends JFrame {
    private JTextArea textArea;
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private DefaultListModel<String> usernameListModel;
    private JTextField portField;

    public Server() {
        setTitle("Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        JButton startButton = new JButton("Start Server");
        startButton.addActionListener(e -> startServer());
        add(startButton, BorderLayout.SOUTH);

        JPanel portPanel = new JPanel();
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("12345", 10);
        portPanel.add(portLabel);
        portPanel.add(portField);
        add(portPanel, BorderLayout.NORTH);

        usernameListModel = new DefaultListModel<>();
        JList<String> usernameList = new JList<>(usernameListModel);
        JScrollPane userListScrollPane = new JScrollPane(usernameList);
        userListScrollPane.setPreferredSize(new Dimension(150, 0));
        add(userListScrollPane, BorderLayout.EAST);
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Geçersiz port numarası", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            textArea.append("Server " + port + " numaralı portta başlatıldı.\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Sunucu başlatılamadı: " + port, "Hata", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }

        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    textArea.append("Yeni istemci bağlandı: " + socket.getInetAddress().getHostName() + "\n");
                    ClientHandler client = new ClientHandler(socket);
                    clients.add(client);
                    client.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Server server = new Server();
            server.setVisible(true);
        });
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream inputStream;
        private DataOutputStream outputStream;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                outputStream.writeUTF("Kullanıcı adınızı girin:");
                outputStream.flush();

                username = inputStream.readUTF();
                if (username.length() < 5) {
                    outputStream.writeUTF("Geçersiz kullanıcı adı! Kullanıcı adı en az 5 karakter olmalıdır.");
                    outputStream.flush();
                    socket.close();
                    return;
                }

                synchronized (usernameListModel) {
                    if (usernameListModel.contains(username)) {
                        outputStream.writeUTF("Geçersiz kullanıcı adı! Bu kullanıcı adı zaten kullanılıyor.");
                        outputStream.flush();
                        socket.close();
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {
                        usernameListModel.addElement(username);
                        textArea.append(username + " sunucuya katıldı.\n");
                    });
                }
                broadcastMessage(username + " sunucuya katıldı.");

                while (true) {
                    String message = inputStream.readUTF();
                    textArea.append(username + ": " + message + "\n");
                    broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (username != null) {
                    synchronized (usernameListModel) {
                        SwingUtilities.invokeLater(() -> {
                            usernameListModel.removeElement(username);
                            textArea.append(username + " sunucudan ayrıldı.\n");
                        });
                    }
                    broadcastMessage(username + " sunucudan ayrıldı.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            try {
                outputStream.writeUTF(message);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
        SwingUtilities.invokeLater(() -> textArea.append(message + "\n"));
    }
}
