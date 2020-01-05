package ui.Client.mainscreen.rightpanel;

import backend.data.Message;

import javax.swing.*;
import java.awt.*;

public class MessagePanel extends JPanel {

    JPanel infos = new JPanel();
    JLabel author = new JLabel();
    JLabel date = new JLabel();
    JTextArea text = new JTextArea();

    public MessagePanel(Message message) {
        initPanel(message);
        setBackground(Color.BLUE);
        infos.setBackground(Color.BLUE);
        text.setBackground(Color.BLUE);
    }

    private void initPanel(Message message) {
        this.setLayout(new BorderLayout(8, 8));

        infos.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        add(infos, BorderLayout.NORTH);

        author = new JLabel();
        author.setText(message.getUtilisateurID().toString());
        infos.add(author);

        date.setText(message.getHeureEnvoie().toString());
        infos.add(date);

        text.setText(message.getContenu());
        text.setEditable(false);
        this.add(text, BorderLayout.CENTER);
    }

}
