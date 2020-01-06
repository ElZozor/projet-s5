package ui.Client.mainscreen.rightpanel;

import backend.data.Message;
import backend.data.Utilisateur;

import javax.swing.*;
import java.awt.*;

public class MessagePanel extends JPanel {

    private static final Color COLOR_PENDING = new Color(0xB0BEC5);
    private static final Color COLOR_SENDING = new Color(0xe57373);
    private static final Color COLOR_READING = new Color(0xFF8A65);
    private static final Color COLOR_FINISH = new Color(0x4DB6AC);

    private JPanel infos = new JPanel();
    private JLabel author = new JLabel();
    private JLabel date = new JLabel();
    private JTextArea text = new JTextArea();
    private JButton infoButton = new JButton("Status");

    private Message message;

    public MessagePanel(Message message) {
        initPanel(message);
    }

    private void initPanel(Message message) {
        this.setLayout(new BorderLayout());

        infos.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        add(infos, BorderLayout.NORTH);

        author = new JLabel();
        Utilisateur user = Utilisateur.getInstance(message.getUtilisateurID());
        if (user != null) {
            author.setText(String.format("%s %s", user.getNom(), user.getPrenom()));
        } else {
            author.setText(message.getUtilisateurID().toString());
        }

        infos.add(author);

        date.setText(message.getHeureEnvoie().toString());
        infos.add(date);
        infos.add(infoButton);

        text.setText(message.getContenu());
        text.setEditable(false);
        this.add(text, BorderLayout.CENTER);

        this.message = message;

        setColor(message);

        infoButton.addActionListener(action -> {
            showInfoDialog();
        });
    }

    private void setColor(Message message) {
        switch (message.state()) {
            case 1:
                setColor(COLOR_PENDING);
                break;

            case 2:
                setColor(COLOR_SENDING);
                break;

            case 3:
                setColor(COLOR_READING);
                break;

            default:
                setColor(COLOR_FINISH);
        }
    }

    private void setColor(Color color) {
        infos.setBackground(color);
    }

    private void showInfoDialog() {
        JOptionPane.showMessageDialog(null, message.getFormattedState(), "Status", JOptionPane.INFORMATION_MESSAGE);
    }

}
