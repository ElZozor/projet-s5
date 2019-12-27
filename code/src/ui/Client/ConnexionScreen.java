package ui.Client;

import backend.server.message.Message;
import launch.ClientLaunch;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ConnexionScreen extends JFrame {
    JPanel mainPanel = new JPanel(new GridBagLayout());
    JButton connexionButton = new JButton("Se connecter");
    JButton closeButton = new JButton("Fermer");
    JLabel connexionLabel = new JLabel("CONNEXION");
    JLabel ineLabel = new JLabel("INE");
    JLabel passwordLabel = new JLabel("Mot de passe");
    JTextField ineField = new JTextField();
    JPasswordField passwordField = new JPasswordField();

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
            Message result = ClientLaunch.client.sendConnectionMessage(ineField.getText(), new String(passwordField.getPassword()));

            if (result == null) {
                JOptionPane.showMessageDialog(this, "Une erreur est survenue..", "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
            } else if (result.isNack()) {
                try {
                    JOptionPane.showMessageDialog(this, result.getNackReason(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
                } catch (Message.WrongMessageTypeException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    ClientLaunch.client.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dispose();
            }
        });
    }
}
