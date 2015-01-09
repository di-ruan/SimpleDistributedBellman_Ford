/**
 * The command thread waits for the user input and act accordingly
 */

import java.util.Scanner;

public class CommandThread extends Thread {

    private bfclient client;

    public CommandThread(bfclient client) {
        this.client = client;
        start();
    }

    public void run() {
        try {
            getUserCmd();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getUserCmd() {
        Scanner scan = new Scanner(System.in);
        try {
            while(true) {
                System.out.print(">");
                String msg = "";
                msg = scan.nextLine();
                if(msg.startsWith("LINKDOWN")) {
                    int pos = msg.indexOf(" ");
                    msg = msg.substring(pos+1);
                    pos = msg.indexOf(" ");
                    String ip = msg.substring(0, pos);
                    msg = msg.substring(pos+1);
                    int port = Integer.parseInt(msg);
                    client.linkDown(ip, port);
                }
                else if(msg.startsWith("LINKUP")) {
                    int pos = msg.indexOf(" ");
                    msg = msg.substring(pos+1);
                    pos = msg.indexOf(" ");
                    String ip = msg.substring(0, pos);
                    msg = msg.substring(pos+1);
                    int port = Integer.parseInt(msg);
                    client.linkUp(ip, port);
                }
                else if(msg.startsWith("NEWLINK")) {    //NEWLINK 127.0.0.1 4118 1.0
                    int pos = msg.indexOf(" ");
                    msg = msg.substring(pos+1);
                    pos = msg.indexOf(" ");
                    String ip = msg.substring(0, pos);
                    msg = msg.substring(pos+1);
                    pos = msg.indexOf(" ");
                    int port = Integer.parseInt(msg.substring(0, pos));
                    msg = msg.substring(pos+1);
                    float cost = Float.parseFloat(msg);
                    if(cost>0) {
                        client.newLink(ip, port, cost);
                    }
                    else {
                        System.out.println("invalid cost");
                    }
                }
                else if(msg.startsWith("TIMEOUT")) {
                    int pos = msg.indexOf(" ");
                    msg = msg.substring(pos+1);
                    float timeout = Float.parseFloat(msg);
                    client.newTimeout(timeout);
                }
                else if(msg.startsWith("NEWCOST")) {   //NEWCOST 127.0.0.1 4115 6.0
                    int pos = msg.indexOf(" ");
                    msg = msg.substring(pos+1);
                    pos = msg.indexOf(" ");
                    String ip = msg.substring(0, pos);
                    msg = msg.substring(pos+1);
                    pos = msg.indexOf(" ");
                    int port = Integer.parseInt(msg.substring(0, pos));
                    msg = msg.substring(pos+1);
                    float cost = Float.parseFloat(msg);
                    if(cost>0) {
                        client.newCost(ip, port, cost);
                    }
                    else {
                        System.out.println("invalid cost");
                    }
                }
                else if(msg.equals("SHOWRT")) {
                    client.showRT();
                }
                else if(msg.equals("SHOWNB")) {
                    client.showNeighbor();
                }
                else if(msg.equals("CLOSE")) {
                    client.close();
                }
                else {
                    System.out.println("invalid command");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
