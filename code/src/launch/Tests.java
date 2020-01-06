package launch;

import backend.data.Groupe;
import backend.data.Message;
import backend.data.Ticket;

import java.util.Date;
import java.util.TreeSet;

public class Tests {

    public static void main(String[] args) {
        Message messageGrpOH1 = new Message(1l, 1l, 1l, new Date(1), "message1", new TreeSet<>());
        Message messageGrpOH2 = new Message(2l, 1l, 1l, new Date(3), "message2", new TreeSet<>());
        Message messageGrpAH1 = new Message(3l, 2l, 2l, new Date(2), "message 1 bis", new TreeSet<>());
        Message messageGrpAH2 = new Message(1l, 1l, 2l, new Date(3), "message 2 bis", new TreeSet<>());

        TreeSet<Message> oh = new TreeSet<>();
        oh.add(messageGrpOH1);
        oh.add(messageGrpOH2);

        TreeSet<Message> ah = new TreeSet<>();
        ah.add(messageGrpAH1);
        ah.add(messageGrpAH2);

        Ticket ticketOH = new Ticket(1l, "Le premier ticket", oh);
        Ticket ticketAH = new Ticket(1l, "Le premier ticket", ah);

        Groupe groupe1 = new Groupe(1l, "le groupe 1");
        Groupe groupe2 = new Groupe(2l, "le groupe 2");

        groupe1.addTicket(ticketOH);
        groupe2.addTicket(ticketAH);


        final String json1 = groupe1.toJSON().toString();
        final String json1bis = new Groupe(groupe1.toJSON()).toJSON().toString();

        System.out.println(json1);
        System.out.println(json1bis);

        System.out.println(json1.equals(json1bis));
    }

}
