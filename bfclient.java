/**
 * The bfclient and main program
 *
 * The main thead will be waiting for packets from other clients and takes care of the DV algorithm
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

public class bfclient {

    private String myIP;
    private int myPort;
    private String myAddress;
    private float timeout;      //ms
    private boolean connected;
    private SenderThread sender;
    private DatagramSocket listenSocket;
    private HashMap<String, neighbor> neighbors;
    private HashMap<String, distance> distanceVectors;
    private Timer timer;
    private TimerTask timerTask;

    public bfclient(int port, float timeout) {
        myPort = port;
        this.timeout = timeout*1000;
        myIP = "";
        neighbors = new HashMap<String, neighbor>();
        distanceVectors = new HashMap<String, distance>();
        connected = true;
    }

    public int getPort() {
        return myPort;
    }

    public synchronized neighbor addNeighbor(String ip, int port, float cost) {

        if(ip.equals("localhost")) {
            ip = "127.0.0.1";
        }

        String address = getAddress(ip, port);
        neighbor item = new neighbor(cost);
        if(!neighbors.containsKey(address)) {
            neighbors.put(address, item);
        }

        addPathInDV(address, cost);

        return item;
    }

    public void start() {
        try {
            System.out.println("start running");

            sender = new SenderThread(this);

            listenSocket = new DatagramSocket(myPort);
            byte arr[] = new byte[1024];
            DatagramPacket packet = new DatagramPacket(arr, arr.length);

            setTimer();

            new CommandThread(this);

            while(connected) {

                listenSocket.receive(packet);
                int length = packet.getLength();
                byte[] buf = new byte[length];
                System.arraycopy(packet.getData(), 0, buf, 0, length);

                ByteBuffer buffer = ByteBuffer.wrap(buf);
                int msgType = buffer.getShort(0);
                String ip = packet.getAddress().getHostAddress();
                if(ip.equals("localhost")) {
                    ip = "127.0.0.1";
                }
                int port = buffer.getInt(2);

                if(msgType == 1) {
                    content con = new content();
                    con.deserialize(buf);
                    receiveUpdate(con, ip, port);
                }
                else if(msgType == 2) {
                    receiveLinkDown(ip, port);
                }
                else if(msgType == 3) {
                    receiveLinkUp(ip, port);
                }
                else if(msgType == 4) {
                    float cost = buffer.getFloat(6);
                    receiveNewCost(ip, port, cost);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void receiveUpdate(content con, String ip, int port) {
        //System.out.println("getUpdate");
        HashMap<String, distance> vectors = con.getDistanceVectors();
        neighbor nei = findNeighbor(ip, port);

        if(myIP.isEmpty()) {
            myIP = con.getDesIP();
            myAddress = getAddress(myIP, myPort);
        }

        if(nei == null) {
            float cost = con.getCost();
            nei = addNeighbor(ip, port, cost);
        }

        nei.updateTime();
        nei.status = true;
        for(Map.Entry<String, distance> entry : vectors.entrySet()) {
            if(entry.getKey().equals(myAddress) || entry.getValue().firstHop.equals(myAddress))
                continue;

            if(distanceVectors.containsKey(entry.getKey())) {
                if(distanceVectors.get(entry.getKey()).firstHop.equals(getAddress(ip, port))) {
                    distanceVectors.get(entry.getKey()).cost = nei.cost+entry.getValue().cost;
                }
                else if(nei.cost+entry.getValue().cost < distanceVectors.get(entry.getKey()).cost) {
                    distanceVectors.get(entry.getKey()).cost = nei.cost+entry.getValue().cost;
                    distanceVectors.get(entry.getKey()).firstHop = getAddress(ip, port);
                }
            }
            else {
                distance newDis = new distance(nei.cost+entry.getValue().cost, getAddress(ip, port));
                distanceVectors.put(entry.getKey(), newDis);
            }
        }

        Iterator<String> it = distanceVectors.keySet().iterator();

        while(it.hasNext()) {
            String addr = it.next();
            distance item = distanceVectors.get(addr);
            if(item.firstHop.equals(getAddress(ip, port)) && !addr.equals(getAddress(ip, port))) {
                if(!vectors.containsKey(addr)) {
                    it.remove();
                }
            }
        }

        for(Map.Entry<String, neighbor> entry : neighbors.entrySet()) {
            if(!entry.getValue().status)
                continue;

            if(!distanceVectors.containsKey(entry.getKey())) {
                float currentCost = entry.getValue().cost;
                addPathInDV(entry.getKey(), currentCost);
            }
            else {
                float currentCost = distanceVectors.get(entry.getKey()).cost;
                if(entry.getValue().cost < currentCost) {
                    distanceVectors.get(entry.getKey()).cost = entry.getValue().cost;
                    distanceVectors.get(entry.getKey()).firstHop = entry.getKey();
                }
            }
        }
    }

    private synchronized void receiveLinkDown(String ip, int port) {
        neighbor nei = findNeighbor(ip, port);
        if(nei != null) {
            nei.updateTime();
            removePathInDV(getAddress(ip, port));
            nei.status = false;
        }
    }

    private synchronized void receiveLinkUp(String ip, int port) {
        neighbor nei = findNeighbor(ip, port);
        if(nei != null) {
            nei.updateTime();
            nei.status = true;
        }

        addPathInDV(getAddress(ip, port), nei.cost);
    }

    public synchronized void receiveNewCost(String ip, int port, float cost) {
        neighbor nei = findNeighbor(ip, port);
        if(nei != null) {
            nei.updateTime();
            float change = cost - nei.cost;
            nei.cost = cost;
            changePathCostInDV(getAddress(ip, port), change);
        }
    }

    public HashMap<String, neighbor> getNeighbors() {
        return neighbors;
    }

    public HashMap<String, distance> getDistanceVectors() {
        return distanceVectors;
    }

    public void setTimer() {
        timer = new Timer(true);
        timerTask = new MyTimerTask();
        timer.schedule(timerTask, (long)timeout);
    }

    public class MyTimerTask extends TimerTask {
        public void run() {
            update();
            checkActive();
            setTimer();
        }
    }

    public synchronized void update() {
        sender.sendUpdate();
    }

    public synchronized void linkDown(String ip, int port) {
        //System.out.println("Down:" + ip + ":" + port);
        neighbor nei = findNeighbor(ip, port);
        if(nei != null) {
            nei.status = false;
            removePathInDV(getAddress(ip, port));
            sender.sendLinkDown(ip, port);
        }
    }

    public synchronized void newLink(String ip, int port, float cost) {
        //System.out.println("Connect:" + ip + ":" + port);
        addNeighbor(ip, port, cost);
    }

    public synchronized void addPathInDV(String address, float cost) {
        if(!distanceVectors.containsKey(address)) {
            distance newDis = new distance(cost, address);
            distanceVectors.put(address, newDis);
        }
        else {
            float currentCost = distanceVectors.get(address).cost;
            if(cost < currentCost) {
                distanceVectors.get(address).firstHop = address;
                distanceVectors.get(address).cost = cost;
            }
        }
    }

    public void newTimeout(float timeout) {
        this.timeout = timeout*1000;
    }

    public synchronized void linkUp(String ip, int port) {
        //System.out.println("Up:" + ip + ":" + port);
        neighbor nei = findNeighbor(ip, port);
        if(nei != null) {
            nei.status = true;
            nei.updateTime();
            addPathInDV(getAddress(ip, port), nei.cost);
            sender.sendLinkUp(ip, port);
        }
    }

    private synchronized neighbor findNeighbor(String ip, int port) {
        String address = getAddress(ip, port);
        if(neighbors.containsKey(address)) {
            return neighbors.get(address);
        }
        return null;
    }

    public synchronized void showRT() {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        System.out.println("Time: " + time + "   Distance vector list is:");
        for(Map.Entry<String,distance> item: distanceVectors.entrySet()) {
            String output = "";
            output += "Destination = " + item.getKey() + ", ";
            output += "Cost = " + item.getValue().cost + ", ";
            output += "Link = (" + item.getValue().firstHop + ")";
            System.out.println(output);
        }
    }

    public synchronized void showNeighbor() {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        System.out.println("Time: " + time + "   Neighbor list is:");
        for(Map.Entry<String,neighbor> item: neighbors.entrySet()) {
            if(item.getValue().status) {
                String output = "";
                output += "Address = " + item.getKey() + ", ";
                output += "Cost = " + item.getValue().cost + ", ";
                output += "last Update = " + format.format(item.getValue().time);
                System.out.println(output);
            }
        }
    }

    public synchronized void newCost(String ip, int port, float cost) {
        String address = getAddress(ip, port);
        if(neighbors.containsKey(address)) {
            float change = cost - neighbors.get(address).cost;
            neighbors.get(address).cost = cost;
            changePathCostInDV(address, change);
            sender.sendNewCost(ip, port, cost);
        }
    }

    public synchronized void close() {
	connected = false;
        neighbors.clear();
        distanceVectors.clear();      
        System.exit(0);
    }

    public synchronized void checkActive() {
        Date currentTime = Calendar.getInstance().getTime();
        for(Map.Entry<String,neighbor> item: neighbors.entrySet()) {
            if(item.getValue().status) {
                Date lastUpdate = item.getValue().time;
                if((currentTime.getTime() - lastUpdate.getTime())> 3*timeout) {
                    item.getValue().status = false;
                    removePathInDV(item.getKey());
                }
            }
        }
    }

    public synchronized void removePathInDV(String firstHop) {
        //System.out.println("remove" + firstHop + "DV");

        Iterator<String> it = distanceVectors.keySet().iterator();

        while(it.hasNext()) {
            String addr = it.next();
            distance item = distanceVectors.get(addr);
            if(item.firstHop.equals(firstHop)) {
                it.remove();
            }
        }
    }

    public synchronized void changePathCostInDV(String firstHop, float change) {
        for(Map.Entry<String,distance> item: distanceVectors.entrySet()) {
            if(item.getValue().firstHop.equals(firstHop)) {
                item.getValue().cost += change;
            }
        }
    }

    public static String getIP(String address) {
        int index = address.indexOf(":");
        return address.substring(0, index);
    }

    public static int getPort(String address) {
        int index = address.indexOf(":");
        return Integer.parseInt(address.substring(index+1));
    }

    public static String getAddress(String ip, int port) {
        return ip + ":" + port;
    }

    public static void main(String[] args) {
        int count = args.length;
        if(count < 2 || (count-2)%3 != 0) {
            System.out.println("invalid inputs");
            System.exit(0);
        }

        int num = (count-2)/3;

        try{
            int port = Integer.parseInt(args[0]);
            float timeout = Float.parseFloat(args[1]);
            final bfclient client = new bfclient(port, timeout);

            for(int i=0; i<num; i++) {
                String neighbor_ip = args[2+i*3];
                int neighbor_port = Integer.parseInt(args[3+i*3]);
                float neighbor_cost = Float.parseFloat(args[4+i*3]);
                if(neighbor_cost < 0) {
                    System.out.println("invalid cost");
                    System.exit(0);
                }
                client.addNeighbor(neighbor_ip, neighbor_port, neighbor_cost);
            }

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    System.out.println("\nThe client is closing...");
                }
            });

            client.start();
        }
        catch (Exception e) {
            System.out.println("invalid inputs");
            System.exit(0);
        }
    }

}
