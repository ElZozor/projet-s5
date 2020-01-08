package ui.Client.mainscreen;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;
import backend.data.Utilisateur;
import backend.server.client.Client;
import backend.server.communication.classic.ClassicMessage;
import debug.Debugger;
import ui.Client.mainscreen.leftpanel.TicketTree;
import ui.Client.mainscreen.rightpanel.MessageEditor;
import ui.Client.mainscreen.rightpanel.TicketDisplayer;
import ui.Client.ticketcreation.TicketCreationScreen;
import ui.InteractiveUI;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.TreeSet;

public class ClientMainScreen extends InteractiveUI {

    JSplitPane mainPanel = new JSplitPane();
    TicketTree ticketTree;
    TicketDisplayer ticketDisplayer;
    TreeSet<Groupe> relatedGroups;
    private JPanel leftPanel;
    private JButton addTicketButton = new JButton("Créer un ticket");
    private Client client;
    private TreeSet<String> allGroups;
    private Ticket selectedTicket;

    public ClientMainScreen(Client client, TreeSet<Groupe> groups) {
        super();

        if (groups == null) {
            relatedGroups = new TreeSet<>();
        } else {
            relatedGroups = groups;
        }

        initPanel();

        setSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setVisible(true);

        this.client = client;
        client.setUI(this);
        client.loadContents();
        client.start();
    }

    private void initPanel() {
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        mainPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

        setDividerStyle();
        initLeftSidePanel();
        initRightSidePanel();


    }

    private void setDividerStyle() {
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) mainPanel.getUI()).getDivider();
        divider.setBackground(Color.black);
        divider.setDividerSize(2);
    }

    private void initLeftSidePanel() {
        leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(addTicketButton, BorderLayout.NORTH);

        addTicketButton.addActionListener(action -> {
            createTicket();
        });

        updateTicketTree();
        mainPanel.setLeftComponent(leftPanel);
    }

    private void createTicket() {
        TicketCreationScreen ticketCreationScreen = new TicketCreationScreen(allGroups);
        ticketCreationScreen.setTicketCreationListener((title, group, contents) -> {
            client.sendData(ClassicMessage.createTicket(title, group, contents));
        });
    }

    private void initRightSidePanel() {
        updateTicketDisplayer(null);

        mainPanel.setRightComponent(ticketDisplayer);
    }

    private void updateTicketDisplayer(Ticket ticket) {
        if (ticket != null) {
            Message premierMessage = ticket.premierMessage();
            if (premierMessage == null || Utilisateur.getInstance(premierMessage.getUtilisateurID()) == null) {
                Debugger.logMessage("ClientMainScreen", "User does not exists, deleting ticket");
                deleteTicket(ticket);
                updateTree();
                ticket = null;
            }
        }

        if (ticket == null) {
            ticketDisplayer = new TicketDisplayer();
        } else {
            ticketDisplayer = new TicketDisplayer(ticket);
        }

        selectedTicket = ticket;
        mainPanel.setRightComponent(ticketDisplayer);

        revalidate();
        ticketDisplayer.setViewToBottom();


        ticketDisplayer.setMessageSendDemandListener((affiliatedTicket, text) -> {
            if (affiliatedTicket != null && text != null && !text.isEmpty()) {
                client.postAMessage(affiliatedTicket.getID(), text);
                MessageEditor.oldText = null;
                affiliatedTicket.addPendingMessage(
                        new Message(
                                0L,
                                client.getMyUser().getID(),
                                affiliatedTicket.getID(),
                                new Date(),
                                text,
                                null,
                                null
                        )
                );
            }
        });
    }

    private void updateTicketTree() {
        if (ticketTree != null) {
            leftPanel.remove(ticketTree);
        }

        for (Groupe groupe : relatedGroups) {
            groupe.updateTickets();
        }

        System.out.println(relatedGroups);
        ticketTree = new TicketTree(relatedGroups);
        ticketTree.addTreeSelectionListener(this::elementSelectedOnTree);

        leftPanel.add(ticketTree, BorderLayout.CENTER);
        leftPanel.updateUI();
    }

    private void updateTree() {
        for (Groupe groupe : relatedGroups) {
            groupe.updateTickets();
        }

        ticketTree.updateTree(relatedGroups);
    }

    private void elementSelectedOnTree(Object object) {
        boolean displayATicket = false;
        if (object instanceof Ticket) {
            Debugger.logMessage("ClientMainScreen", "Updating from distant server..");

            client.sendNotificationTicketClicked((Ticket) object);

            displayATicket = true;
        }

        if (displayATicket) {
            updateTicketDisplayer((Ticket) object);
        } else {
            updateTicketDisplayer(null);
        }
    }

    public void updateRelatedGroups(TreeSet<Groupe> relatedGroups) {
        this.relatedGroups = relatedGroups;
        updateTicketTree();
    }

    public void updateGroupsList(TreeSet<String> allGroups) {
        this.allGroups = allGroups;
    }

    public void updateGroupe(Groupe entryAsGroupe) {
        String old_label = null;
        for (Groupe groupe : relatedGroups) {
            if (groupe.getID().equals(entryAsGroupe.getID())) {
                old_label = groupe.getLabel();
                groupe.setLabel(entryAsGroupe.getLabel());

                allGroups.remove(old_label);
                allGroups.add(entryAsGroupe.getLabel());

                updateTree();

                return;
            }
        }
    }

    public void updateTicket(Groupe entryRelatedGroup, Ticket entryAsTicket) {
        if (relatedGroups.contains(entryRelatedGroup)) {
            for (Groupe groupe : relatedGroups) {
                if (groupe.equals(entryRelatedGroup)) {
                    if (entryAsTicket.equals(selectedTicket)) {
                        updateTicketDisplayer(entryAsTicket);
                        selectedTicket.merge(entryAsTicket);

                        if (entryAsTicket.containsUnreadOrUnreceivedMessages()) {
                            client.sendNotificationTicketClicked(entryAsTicket);
                        }
                    } else {
                        for (Ticket ticket : groupe.getTickets()) {
                            if (ticket.equals(entryAsTicket)) {
                                ticket.merge(entryAsTicket);
                                break;
                            }
                        }
                    }


                    updateTree();
                    return;
                }
            }
        } else {
            entryRelatedGroup.addTicket(entryAsTicket);
            relatedGroups.add(entryRelatedGroup);
            updateTree();
        }
    }

    public void updateMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        System.out.println("SELECTED COMPARISON");
        if (relatedGroups.contains(entryRelatedGroup)) {

            for (Groupe groupe : relatedGroups) {
                if (groupe.equals(entryRelatedGroup)) {
                    TreeSet<Ticket> tickets = groupe.getTickets();

                    if (tickets.contains(entryRelatedTicket)) {
                        for (Ticket ticket : tickets) {
                            if (ticket.equals(entryRelatedTicket)) {
                                ticket.merge(entryAsMessage);

                                if (ticket.equals(selectedTicket)) {
                                    updateTicketDisplayer(selectedTicket);
                                    if (entryRelatedTicket.containsUnreadOrUnreceivedMessages()) {
                                        client.sendNotificationTicketClicked(entryRelatedTicket);
                                    }
                                }

                                updateTree();

                                return;
                            }
                        }
                    } else {
                        entryRelatedTicket.addMessage(entryAsMessage);
                        groupe.addTicket(entryRelatedTicket);
                        updateTree();

                        return;
                    }
                }
            }
        } else {
            entryRelatedTicket.addMessage(entryAsMessage);
            entryRelatedGroup.addTicket(entryRelatedTicket);
            relatedGroups.add(entryRelatedGroup);
            updateTree();
        }
    }

    @Override
    public void deleteUser(Utilisateur entryAsUser) {

    }

    public void deleteGroupe(Groupe entryAsGroupe) {
        Debugger.logMessage("ClientMainScreen",
                "Delete following group: " + entryAsGroupe.toJSON() + " present : " + relatedGroups.contains(entryAsGroupe));
        relatedGroups.remove(entryAsGroupe);
        allGroups.remove(entryAsGroupe.getLabel());

        updateTree();
    }

    public void deleteTicket(Groupe entryRelatedGroup, Ticket entryAsTicket) {
        for (Groupe groupe : relatedGroups) {
            if (groupe.equals(entryRelatedGroup)) {
                Debugger.logMessage("ClientMainScreen", "Ticket removed: " + groupe.getTickets().remove(entryAsTicket));
                if (entryAsTicket.equals(selectedTicket)) {
                    updateTicketDisplayer(null);
                }

                updateTree();

                return;
            }
        }
    }

    private void deleteTicket(Ticket ticket) {
        for (Groupe groupe : relatedGroups) {
            if (groupe.getTickets().remove(ticket)) {
                return;
            }
        }
    }

    public void deleteMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        for (Groupe groupe : relatedGroups) {
            if (groupe.equals(entryRelatedGroup)) {
                for (Ticket ticket : groupe.getTickets()) {
                    if (ticket.equals(entryRelatedTicket)) {
                        ticket.getMessages().remove(entryAsMessage);
                        if (ticket.equals(selectedTicket)) {
                            updateTicketDisplayer(ticket);
                        }

                        return;
                    }
                }
            }
        }
    }

    public void addGroupe(Groupe entryAsGroupe) {
        allGroups.add(entryAsGroupe.getLabel());
        relatedGroups.add(entryAsGroupe);
        updateTree();
    }

    public void addTicket(Groupe relatedGroupEntry, Ticket entryAsTicket) {
        if (relatedGroups.contains(relatedGroupEntry)) {
            for (Groupe groupe : relatedGroups) {
                if (groupe.equals(relatedGroupEntry)) {
                    groupe.addTicket(entryAsTicket);
                    ticketTree.updateTree(relatedGroups);
                    ticketTree.revalidate();
                    ticketTree.repaint();
                    return;
                }
            }
        } else {
            relatedGroupEntry.addTicket(entryAsTicket);
            relatedGroups.add(relatedGroupEntry);
            updateTree();
        }
    }

    public void addMessage(Groupe entryRelatedGroup, Ticket entryRelatedTicket, Message entryAsMessage) {
        for (Groupe groupe : relatedGroups) {
            if (groupe.equals(entryRelatedGroup)) {
                for (Ticket ticket : groupe.getTickets()) {
                    if (ticket.equals(entryRelatedTicket)) {
                        ticket.merge(entryAsMessage);
                        if (ticket.equals(selectedTicket)) {
                            updateTicketDisplayer(ticket);
                        }

                        return;
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        try {
            client.disconnect(allGroups, relatedGroups);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
