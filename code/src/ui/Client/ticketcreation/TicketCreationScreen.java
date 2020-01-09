package ui.Client.ticketcreation;

import com.mysql.jdbc.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.TreeSet;

public class TicketCreationScreen extends JFrame {

    public final static String TITLE_HINT = "Titre du ticket";
    public final static String TEXT_HINT = "Votre message de haine";

    JScrollPane textcontainer = new JScrollPane();
    TicketCreationListener listener;
    private JButton createTicket = new JButton("Créer le ticket");
    private JButton abort = new JButton("Annuler");
    private JTextField title = new JTextField(TITLE_HINT);
    private JTextArea text = new JTextArea(TEXT_HINT);
    private JComboBox<String> groups;

    public TicketCreationScreen(TreeSet<String> availableGroups) {
        super();
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String[] gs = new String[availableGroups.size()];
        int i = 0;
        for (String s : availableGroups) {
            gs[i++] = s;
        }

        groups = new JComboBox<String>(gs);

        setSize(new Dimension(640, 480));
        initPanel();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initPanel() {
        setContentPane(new JPanel(new GridBagLayout()));
        Container contentPane = getContentPane();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(title, gbc);


        gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(groups, gbc);


        gbc = new GridBagConstraints();
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.BOTH;
        textcontainer.setViewportView(text);
        contentPane.add(textcontainer, gbc);


        gbc = new GridBagConstraints();
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(createTicket, gbc);


        gbc = new GridBagConstraints();
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(abort, gbc);

        setListeners();
    }

    private boolean containsNullOrEmpty(String... strs) {
        for (String s : strs) {
            if (StringUtils.isNullOrEmpty(s) || StringUtils.isEmptyOrWhitespaceOnly(s)) {
                return true;
            }
        }

        return false;
    }

    private void setListeners() {
        createTicket.addActionListener(action -> {
            final String titre = title.getText();
            final String groupe = (String) groups.getSelectedItem();
            final String contenu = text.getText();

            if (containsNullOrEmpty(titre, groupe, contenu)) {
                return;
            }

            if (listener != null) {
                listener.ticketCreation(titre, groupe, contenu);
                dispose();
            }
        });

        abort.addActionListener(actionEvent -> {
            dispose();
        });

        title.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                if (title.getText().equals(TITLE_HINT)) {
                    title.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (StringUtils.isEmptyOrWhitespaceOnly(title.getText())) {
                    title.setText(TITLE_HINT);
                }
            }
        });


        text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                if (text.getText().equals(TEXT_HINT)) {
                    text.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                if (StringUtils.isEmptyOrWhitespaceOnly(text.getText())) {
                    text.setText(TEXT_HINT);
                }
            }
        });
    }

    public void setTicketCreationListener(TicketCreationListener ticketCreationListener) {
        listener = ticketCreationListener;
    }

    public interface TicketCreationListener {
        void ticketCreation(final String title, final String group, final String contents);
    }

}
