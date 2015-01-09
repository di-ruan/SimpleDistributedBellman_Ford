/**
 * implement the structure of the packet
 */

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class content {

    private HashMap<String, distance> distanceVectors;
    private short msgType;
    private int srcPort;
    private float desCost;
    private String desIP;

    public content() {
        distanceVectors = new HashMap<String, distance>();
    }

    public byte[] serialize() {
        int size = distanceVectors.size();
        ByteBuffer buffer = ByteBuffer.allocate(46*size+25);

        buffer.putShort(msgType);   //2
        buffer.putInt(srcPort);   //4
        buffer.putFloat(desCost);   //4

        String desIP_temp = desIP;
        for(int i=0; i<15-desIP.length(); i++) {
            desIP_temp = desIP_temp + " ";
        }

        buffer.put(desIP_temp.getBytes());  //15

        for(Map.Entry<String, distance> entry : distanceVectors.entrySet()) {
            String destination = entry.getKey();
            float cost = entry.getValue().cost;
            String firsHop = entry.getValue().firstHop;

            String add1 = "";
            String add2 = "";
            for(int i=0; i<21-destination.length(); i++) {
                add1 += " ";
            }
            for(int i=0; i<21-firsHop.length(); i++) {
                add2 += " ";
            }

            destination = destination + add1;
            firsHop = firsHop + add2;

            buffer.put(destination.getBytes());
            buffer.putFloat(cost);
            buffer.put(firsHop.getBytes());
        }

        return buffer.array();
    }

    public void deserialize(byte[] buf) {
        int size = buf.length;
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        msgType = buffer.getShort(0);
        srcPort = buffer.getInt(2);
        desCost = buffer.getFloat(6);

        byte[] desIP_temp = new byte[15];
        System.arraycopy(buf, 10, desIP_temp, 0, 15);
        String desIP_str = new String(desIP_temp);
        desIP = desIP_str.trim();

        int pos = 25;

        while(pos < size) {
            byte[] des = new byte[21];
            byte[] hop = new byte[21];

            System.arraycopy(buf, pos, des, 0, 21);
            String nei = new String(des);
            nei = nei.trim();

            float cost = buffer.getFloat(pos+21);

            System.arraycopy(buf, pos+25, hop, 0, 21);
            String firstHop = new String(hop);
            firstHop = firstHop.trim();

            pos += 46;

            distance item = new distance(cost, firstHop);
            distanceVectors.put(nei, item);
        }
    }

    public HashMap<String, distance> getDistanceVectors() {
        return distanceVectors;
    }

    public void setDistanceVectors(HashMap<String, distance> vec) {
        distanceVectors = vec;
    }

    public short getMsgType() {
        return msgType;
    }

    public void setMsgType(short type) {
        msgType = type;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int port) {
        srcPort = port;
    }

    public float getCost() {
        return desCost;
    }

    public void setCost(float cost) {
        this.desCost = cost;
    }

    public String getDesIP() {
        return desIP;
    }

    public void setDesIP(String ip) {
        this.desIP = ip;
    }

}
