package ui.Client.mainscreen.rightpanel;

import javax.swing.*;
import java.awt.*;

public class MessageEditor extends JPanel {

    private JButton send_button = new JButton("Envoyer");
    private JTextArea text = new JTextArea("Entrez le message à envoyer");
    private JScrollPane scrollPane;

    private OnSendButtonClickListener listener;


    public MessageEditor() {
        super(new GridBagLayout());

        initPanel();
        setMinimumSize(new Dimension(200, 250));
    }

    private void initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(text);
        add(scrollPane, gbc);


        gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;

        add(send_button, gbc);

        send_button.addActionListener(actionEvent -> {
            if (listener != null) {
                listener.clicked(text.getText());
            }
        });
    }


    public void setOnSendButtonClickListener(OnSendButtonClickListener listener) {
        this.listener = listener;
    }


    public interface OnSendButtonClickListener {
        void clicked(String text);
    }
}
