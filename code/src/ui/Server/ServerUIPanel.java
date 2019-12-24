package ui.Server;

import backend.database.DatabaseManager;
import backend.modele.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class ServerUIPanel extends JPanel {

    private static final String[] tables = new String[]{"Utilisateur", "Groupe", "Ticket", "Message"};
    public static UserModel userTableModel;
    public static GroupModel groupTableModel;
    public static TicketModel ticketTableModel;
    public static MessageModel messageTableModel;
    public static SearchableModel currentModel;
    private JComboBox table_selector;
    private JTextField search_bar;
    private JButton add_button;
    private JPanel upside_options;
    private JScrollPane table_container;
    private JTable table;
    private ServerUI parent;

    public ServerUIPanel(ServerUI parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        initialize();
    }

    private void initialize() {
        this.setLayout(new BorderLayout(0, 0));
        upside_options = new JPanel();
        upside_options.setLayout(new BorderLayout(0, 0));
        this.add(upside_options, BorderLayout.NORTH);
        table_selector = new JComboBox(tables);
        upside_options.add(table_selector, BorderLayout.WEST);
        search_bar = new JTextField();
        upside_options.add(search_bar, BorderLayout.CENTER);
        add_button = new JButton();
        add_button.setText("Ajouter");
        upside_options.add(add_button, BorderLayout.EAST);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        this.add(panel1, BorderLayout.CENTER);
        table_container = new JScrollPane();
        panel1.add(table_container, BorderLayout.CENTER);

        setUserModel();


        add_button.addActionListener((e) -> {
            final String selectedItem = (String) table_selector.getSelectedItem();
            if (selectedItem != null) {
                parent.edit(selectedItem, null);
            }
        });

        table_selector.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                String selection = (String) itemEvent.getItem();

                switch (selection) {
                    case "Utilisateur":
                        setUserModel();
                        break;

                    case "Groupe":
                        setGroupModel();
                        break;

                    case "Ticket":
                        setTicketModel();
                        break;

                    case "Message":
                        setMessageModel();
                        break;
                }
            }
        });

        search_bar.getDocument().addDocumentListener(new DocumentListener() {
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
                if (currentModel != null) {
                    table.setModel(currentModel.retrieveSearchModel(search_bar.getText()));
                }
            }
        });

    }


    private void setUserModel() {
        if (userTableModel == null) {
            try {
                userTableModel = DatabaseManager.getInstance().retrieveUserModel();
            } catch (SQLException | NoSuchAlgorithmException e) {
                JOptionPane.showMessageDialog(this, "Impossible de joindre la base de donnée !");
                e.printStackTrace();
            }
        }

        setModel(userTableModel);
    }

    private void setGroupModel() {
        if (groupTableModel == null) {
            try {
                groupTableModel = DatabaseManager.getInstance().retrieveGroupModel();
            } catch (SQLException | NoSuchAlgorithmException e) {
                JOptionPane.showMessageDialog(this, "Impossible de joindre la base de donnée !");
                e.printStackTrace();
            }
        }

        setModel(groupTableModel);
    }


    private void setTicketModel() {
        if (ticketTableModel == null) {
            try {
                ticketTableModel = DatabaseManager.getInstance().retrieveTicketModel();
            } catch (SQLException | NoSuchAlgorithmException e) {
                JOptionPane.showMessageDialog(this, "Impossible de joindre la base de donnée !");
                e.printStackTrace();
            }
        }

        setModel(ticketTableModel);
    }

    private void setMessageModel() {
        if (messageTableModel == null) {
            try {
                messageTableModel = DatabaseManager.getInstance().retrieveMessageModel();
            } catch (SQLException | NoSuchAlgorithmException e) {
                JOptionPane.showMessageDialog(this, "Impossible de joindre la base de donnée !");
                e.printStackTrace();
            }
        }

        setModel(messageTableModel);
    }


    private void setModel(SearchableModel model) {
        if (table == null) {
            table = new JTable();
            table_container.setViewportView(table);
        }

        if (model != null) {
            table.setModel(model);
            currentModel = model;
        }
    }


}
