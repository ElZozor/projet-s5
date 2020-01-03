package ui.Client;

import backend.data.Groupe;
import backend.server.communication.CommunicationMessage;
import launch.ClientLaunch;
import org.json.JSONArray;

import javax.swing.*;
import java.awt.*;
import java.util.TreeSet;

public class ConnexionScreen extends JFrame {
    private JPanel mainPanel = new JPanel(new GridBagLayout());
    private JButton connexionButton = new JButton("Se connecter");
    private JButton closeButton = new JButton("Fermer");
    private JLabel connexionLabel = new JLabel("CONNEXION");
    private JLabel ineLabel = new JLabel("INE");
    private JLabel passwordLabel = new JLabel("Mot de passe");
    private JTextField ineField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();

    public ConnexionScreen() {
        setUndecorated(true);

        initPanel();
        setContentPane(mainPanel);

        setSize(new Dimension(300, 400));
        setResizable(false);
        setLocationRelativeTo(null);

        setVisible(true);
    }

    private void initPanel() {
        initLabelConnexion();
        initINELabelAndField();
        initMDPLabelAndField();
        initConnexionButton();
        initCloseButton();

        setActionsListeners();
    }

    private void initLabelConnexion() {
        connexionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 16, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 0;
        constraints.gridx = 0;

        mainPanel.add(connexionLabel, constraints);
    }

    private void initINELabelAndField() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 1;
        constraints.gridx = 0;

        mainPanel.add(ineLabel, constraints);


        constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 2;
        constraints.gridx = 0;

        mainPanel.add(ineField, constraints);
    }

    private void initMDPLabelAndField() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 3;
        constraints.gridx = 0;

        mainPanel.add(passwordLabel, constraints);


        constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 16, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 4;
        constraints.gridx = 0;

        mainPanel.add(passwordField, constraints);

    }

    private void initConnexionButton() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 5;
        constraints.gridx = 0;

        mainPanel.add(connexionButton, constraints);
    }

    private void initCloseButton() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = 6;
        constraints.gridx = 0;

        mainPanel.add(closeButton, constraints);
    }

    private void setActionsListeners() {
        closeButton.addActionListener(action -> {
            dispose();
        });

        connexionButton.addActionListener(action -> {
            connect();
        });
    }


    private void connect() {
        CommunicationMessage result = ClientLaunch.client.sendConnectionMessage(ineField.getText(), new String(passwordField.getPassword()));

        if (result != null && result.isAck()) {
            TreeSet<Groupe> groups = sendUpdateMessage();

            if (groups == null) {
                showConnectionErrorDialog("Impossible de mettre à jour les données locales..");
            }

            new ClientMainScreen(groups);
            dispose();
        } else {
            showConnectionErrorDialog(result);
        }
    }

    private void showConnectionErrorDialog() {
        showConnectionErrorDialog("Une erreur est survenue..");
    }

    private void showConnectionErrorDialog(String reason) {
        JOptionPane.showMessageDialog(this, reason, "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
    }

    private void showConnectionErrorDialog(CommunicationMessage message) {
        if (message == null) {
            showConnectionErrorDialog();
        } else {
            try {
                showConnectionErrorDialog(message.getNackReason());
            } catch (CommunicationMessage.WrongMessageTypeException e) {
                e.printStackTrace();
            }
        }
    }

    private TreeSet<Groupe> sendUpdateMessage() {
        CommunicationMessage groupUpdate = ClientLaunch.client.updateLocalDatabase();

        if (groupUpdate.isLocalUpdateResponse()) {
            TreeSet<Groupe> groups = new TreeSet<>();

            JSONArray array = groupUpdate.getLocalUpdateResponseGroups();
            for (int i = 0; i < array.length(); ++i) {
                groups.add(new Groupe(array.getJSONObject(i)));
            }

            return groups;
        }

        return null;
    }
}
