package ui.Server;

import backend.server.host.Host;

import javax.swing.*;
import java.awt.*;

public class ServerStopUI extends JFrame {

    private final Host host;
    private JButton killButton = new JButton("Arrêter le serveur");
    private JLabel nbClient = new JLabel("Connectés : 0");
    private JLabel nbAdmin = new JLabel("Admins : 0");
    private JPanel logPanel = new JPanel();
    private JScrollPane messages = new JScrollPane();
    private int messagesCount = 0;

    public ServerStopUI(Host host) {
        super("Status du serveur");
        setMinimumSize(new Dimension(640, 480));
        setContentPane(new JPanel(new GridBagLayout()));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.host = host;
        killButton.addActionListener(action -> dispose());

        initPanel();

        setVisible(true);
    }

    private void initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 8, 8, 8);
        getContentPane().add(nbClient, gbc);

        gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 8, 8, 8);
        getContentPane().add(nbAdmin, gbc);

        gbc = new GridBagConstraints();
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weighty = 1.0;
        getContentPane().add(new JPanel(), gbc);

        gbc = new GridBagConstraints();
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 8, 8, 8);
        getContentPane().add(killButton, gbc);

        logPanel.setBackground(new Color(0x37474F));
        logPanel.setLayout(new GridBagLayout());

        messages.setViewportView(logPanel);
        messages.setSize(300, 300);
        gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridheight = 4;
        gbc.insets = new Insets(8, 8, 8, 8);
        getContentPane().add(messages, gbc);
        messages.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void setConnectionNumber(int number) {
        nbClient.setText("Connectés : " + number);
    }

    public void setAdminNumber(int number) {
        nbAdmin.setText("Admins : " + number);
    }

    public synchronized void addLogMessage(String message) {
        if (messagesCount > 0) {
            logPanel.remove(messagesCount);
        }

        message = "############################################\n"
                + message
                + "\n############################################\n";
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridy = messagesCount++;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextArea log = new JTextArea(message);
        log.setForeground(Color.WHITE);
        log.setBackground(new Color(0x37474F));
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);


        logPanel.add(log, gbc);
        logPanel.revalidate();
        logPanel.repaint();

        addBottomMessage();

        messages.revalidate();
        messages.getVerticalScrollBar().setValue(messages.getVerticalScrollBar().getMaximum());
    }

    private void addBottomMessage() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridy = messagesCount;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel panel = new JPanel();
        panel.setBackground(new Color(0x37474F));
        logPanel.add(panel, gbc);
    }

    @Override
    public void dispose() {
        host.stopServer();
        super.dispose();
    }
}
