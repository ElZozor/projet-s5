package ui.Server;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.modele.UserModel;

import javax.swing.*;
import java.awt.*;

public class ServerUI extends JFrame {
    private static String SERVER_FRAME_TITLE = "Administration";

    Container mainPanel;
    Container secondPanel = null;

    UserModel userModel;

    public ServerUI() {
        super(SERVER_FRAME_TITLE);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(800, 600));
        this.setLocationRelativeTo(null);

        mainPanel = this.getContentPane();
        initPanel();


        this.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ui.Server.ServerUI::new);
    }

    public void setMainPanel() {
        this.setContentPane(mainPanel);
        secondPanel = null;

        this.pack();
    }

//    void search(final String string);
//    void add(final String table_name);
//    void editUserPanel();
//    void addGroupPanel();

    public void delete(Object arg) {
        int input = JOptionPane.showConfirmDialog(
                null,
                "Voulez-vous vraiment supprimer cette entrée ?",
                "Suppression",
                JOptionPane.YES_NO_OPTION
        );

        if (input == 1) {
            return;
        }

        if (arg instanceof Utilisateur) {
            //delUser((Utilisateur) arg);
        } else if (arg instanceof Groupe) {
            //delGroup((Groupe) arg);
        } else if (arg instanceof Ticket) {
            //delTicket((Ticket) arg);
        } else if (arg instanceof Message) {
            //delMessage((Message) arg);
        }
    }

    void delUser(Utilisateur user) {

    }

    void delGroup(Groupe group) {

    }

    void delTicket(Ticket ticket) {

    }

    void delMessage(Message message) {

    }

    void edit(final String table_name, Object arg) {
        switch (table_name) {
            case "Utilisateur":
                editUserPanel((Utilisateur) arg);
                break;

            case "Groupe":
                editGroupPanel((Groupe) arg);
                break;

            default:
                break;
        }
    }

    void editUserPanel(Utilisateur arg) {
        mainPanel = this.getContentPane();

        secondPanel = new EditUserPanel(this);
        this.setContentPane(secondPanel);

        this.pack();
    }

    void editGroupPanel(Groupe arg) {
        mainPanel = this.getContentPane();

        secondPanel = new EditGroupPanel(this);
        this.setContentPane(secondPanel);

        this.pack();
    }

    private void initPanel() {
        Container pane = this.getContentPane();
        pane.add(new ServerUIPanel(this), BorderLayout.CENTER);
    }

}
