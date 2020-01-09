package ui.Client.mainscreen.rightpanel;

import com.mysql.jdbc.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class MessageEditor extends JPanel {

    private static final String HINT_TEXT = "Entrez le message à envoyer";

    public static String oldText;

    private JButton send_button = new JButton("Envoyer");
    private JTextArea text = new JTextArea(HINT_TEXT);
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

        if (oldText != null) {
            text.setText(oldText);
        }

        setListeners();
        text.requestFocus();
    }

    private void setListeners() {
        send_button.addActionListener(actionEvent -> {
            String t = text.getText();
            if (listener != null && !StringUtils.isEmptyOrWhitespaceOnly(t) && !t.equals(HINT_TEXT)) {
                listener.clicked(text.getText());
            }
        });

        text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                if (text.getText().equals(HINT_TEXT)) {
                    text.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (StringUtils.isEmptyOrWhitespaceOnly(text.getText())) {
                    text.setText(HINT_TEXT);
                }
            }
        });

        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                warn();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                warn();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                warn();
            }

            private void warn() {
                oldText = text.getText();
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
