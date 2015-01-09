/**
 * The sender thread sends packet to other clients. It is created in the main thread.
 *
 * Created by diruan on 11/27/14.
 *     1 - update
 *     2 - link down
 *     3 - link up
 *     4 - new cost
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public class SenderThread extends Thread  {

    private bfclient client;
    private DatagramSocket socket;

    public SenderThread(bfclient client) {
        try{
            this.client = client;
            socket = new DatagramSocket();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendUpdate() {
        try {
            //System.out.println("sending update");
            for(Map.Entry<String, neighbor> temp : client.getNeighbors().entrySet()) {
                if(!temp.getValue().status)
                    continue;

                String ip = bfclient.getIP(temp.getKey());
                int port = bfclient.getPort(temp.getKey());

                content con = new content();
                con.setDistanceVectors(client.getDistanceVectors());
                con.setMsgType((short)1);
                con.setSrcPort(client.getPort());
                con.setCost(temp.getValue().cost);
                con.setDesIP(ip);

                byte[] buf = con.serialize();

                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                packet.setAddress(InetAddress.getByName(ip));
                packet.setPort(port);
                socket.send(packet);
                //System.out.println("send update to" + temp.getKey());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendLinkDown(String ip, int port) {
        try {
            //System.out.println("sending link down");
            ByteBuffer buffer = ByteBuffer.allocate(6);
            buffer.putShort(0,(short)2);
            buffer.putInt(2, client.getPort());
            DatagramPacket packet = new DatagramPacket(buffer.array(),6);
            packet.setAddress(InetAddress.getByName(ip));
            packet.setPort(port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendLinkUp(String ip, int port) {
        try {
            //System.out.println("sending link up");
            ByteBuffer buffer = ByteBuffer.allocate(6);
            buffer.putShort(0,(short)3);
            buffer.putInt(2, client.getPort());
            DatagramPacket packet = new DatagramPacket(buffer.array(),6);
            packet.setAddress(InetAddress.getByName(ip));
            packet.setPort(port);
            socket.send(packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendNewCost(String ip, int port, float cost) {
        try {
            //System.out.println("sending new cost");
            ByteBuffer buffer = ByteBuffer.allocate(10);
            buffer.putShort(0,(short)4);
            buffer.putInt(2, client.getPort());
            buffer.putFloat(6, cost);
            DatagramPacket packet = new DatagramPacket(buffer.array(),10);
            packet.setAddress(InetAddress.getByName(ip));
            packet.setPort(port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
