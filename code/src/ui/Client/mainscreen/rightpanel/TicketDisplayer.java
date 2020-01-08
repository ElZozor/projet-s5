package ui.Client.mainscreen.rightpanel;

import backend.data.Message;
import backend.data.Ticket;
import debug.Debugger;

import javax.swing.*;
import java.awt.*;

public class TicketDisplayer extends JPanel {

    private final JLabel emptylabel = new JLabel("Aucun message à afficher");
    private MessageEditor messageEditor;
    private JScrollPane scrollPane = new JScrollPane();
    private JPanel messagePanel = new JPanel();

    private OnMessageSendRequest sendDemandListener;

    private Ticket ticket;

    public TicketDisplayer() {
        initEmptyPanel();
    }

    public TicketDisplayer(Ticket ticket) {
        super(new BorderLayout(8, 8));

        this.ticket = ticket;
        initPanel();
    }

    private void initEmptyPanel() {
        add(emptylabel);
    }

    private void initPanel() {
        initMessageEditor();
        initMessagePanel();
    }

    private void initMessagePanel() {
        if (messagePanel != null) {
            remove(messagePanel);
        }

        messagePanel = new JPanel();
        messagePanel.setLayout(new GridBagLayout());

        updateView();

        scrollPane.setViewportView(messagePanel);
        add(scrollPane, BorderLayout.CENTER);

    }

    private void addPendingMessage(Message message) {
        ticket.addPendingMessage(message);
        initMessagePanel();
    }

    private void updateView() {

        if (ticket != null) {
            int y = 0;
            for (Message message : ticket.getMessages()) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.weightx = 1.0;
                gbc.weighty = 0.0;
                gbc.gridx = 0;
                gbc.gridy = y++;
                gbc.insets = new Insets(8, 8, 8, 8);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.FIRST_LINE_START;

                messagePanel.add(new MessagePanel(message), gbc);
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridx = 0;
            gbc.gridy = y;

            JPanel panel = new JPanel();
            messagePanel.add(panel, gbc);
        }

    }

    private void initMessageEditor() {
        messageEditor = new MessageEditor();
        add(messageEditor, BorderLayout.SOUTH);

        messageEditor.setOnSendButtonClickListener(this::sendMessage);
    }

    private void sendMessage(String text) {
        Debugger.logMessage("Ticket Displayer", "Send button clicked and ticket is: " + (ticket == null ? "null" : "set"));

        if (sendDemandListener != null) {
            sendDemandListener.sendMessage(ticket, text);
        }
    }

    public void setMessageSendDemandListener(OnMessageSendRequest listener) {
        this.sendDemandListener = listener;
    }

    public void setViewToBottom() {
        scrollPane.revalidate();
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }


    public interface OnMessageSendRequest {
        void sendMessage(Ticket ticket, String text);
    }

}
