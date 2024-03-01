import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Master {
    private static final int BUFFER_SIZE = 1024;
    private static ArrayList<AddressPort> clients = new ArrayList<AddressPort> ();
    private static ArrayList<AddressPort> slaves = new ArrayList<AddressPort> ();
    private static ArrayList<ArrayList<Integer>> primeDivision = new ArrayList<ArrayList<Integer>> ();
    private static DatagramSocket serverSocket;
    private static ArrayList<Thread> threads = new ArrayList<Thread> ();

    public static final Object primesLock = new Object ();
    public static ArrayList<Integer> primes = new ArrayList<Integer> ();
    
    public static void main (String[] args) {
        System.out.println ();
        InetAddress ipAddress = null;
        int nPort = 8080;
        boolean receivedClientRequest = false;
        int nLimit = 2, nThreadCount = 1;
        int slavePrimes = 0;

        try {
            ipAddress = InetAddress.getByName ("localhost");
            serverSocket = new DatagramSocket (nPort, ipAddress);
            System.out.println ("Server started on IP: " + ipAddress + " and Port " + nPort);

            while (!receivedClientRequest) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket receivedPacket = new DatagramPacket(buffer, BUFFER_SIZE);
                serverSocket.receive (receivedPacket);
                InetAddress senderAddress = receivedPacket.getAddress ();
                int nSenderPort = receivedPacket.getPort ();
                String message = new String (receivedPacket.getData (), 0, receivedPacket.getLength ());
                message = message.toLowerCase ();

                if (message.equals ("client")) {
                    System.out.println ("Client connected");
                    AddressPort addPort = new AddressPort (senderAddress, nSenderPort);
                    clients.add (addPort);
                } else if (message.equals ("slave")) {
                    System.out.println ("Slave connected");
                    AddressPort addPort = new AddressPort (senderAddress, nSenderPort);
                    System.out.println ("" + addPort.getAddress ());
                    System.out.println ("" + addPort.getPort ());
                    slaves.add (addPort);
                } else {
                    receivedClientRequest = true;
                    nLimit = Integer.parseInt (message.split (",")[0]);
                    nThreadCount = Integer.parseInt (message.split (",")[1]);
                    // System.out.println ("Req received: " + message);
                }
            }
        } catch (Exception e) {
            System.err.println ("Error in starting server: ");
            e.printStackTrace ();
            System.exit (1);
        }

        splitNumbers (nLimit, nThreadCount);
        sendToSlaves (nThreadCount);

        initThreads (primeDivision.get (primeDivision.size () - 1), nThreadCount);

        try {
            for (Thread thread : threads) {
                thread.join ();
            }
        } catch (Exception e) {
            System.err.println ("Error in joining threads: ");
            e.printStackTrace ();
        }

        slavePrimes = getSlavePrimes (ipAddress, nPort);

        if (slavePrimes == -1) {
            System.exit (1);
        } else {
            sendToClient (PrimeCheck.allPrimes.size () + slavePrimes);
            System.out.println ("Found: " + PrimeCheck.allPrimes.size ());
        }
    }

private static void splitNumbers (int nLimit, int nThreadCount) {
    int i, nCurNum = 2;
    int j;

    for (i = 0; i < slaves.size () + 1; i++) {
        primeDivision.add (new ArrayList<Integer> ());
    }

    while (nCurNum <= nLimit) {
        for (i = 0; i < slaves.size () + 1; i++) {
            for (j = 0; j < 10; j++) {    
                primeDivision.get (i).add (nCurNum);
                nCurNum++;

                if (nCurNum > nLimit) {
                    i = i + slaves.size () + 2;
                    j = 10 + 1;
                }
            }

        }
    }
}

    private static void sendToSlaves (int threadCount) {
        int i, j;
        int partitionSize = 100;
        String arrayListCSV = "" + threadCount + ",";

        try {
            for (i = 0; i < slaves.size(); i++) {
                for (j = 0; j < primeDivision.get(i).size(); j++) {
                    arrayListCSV += primeDivision.get(i).get(j);

                    if ((j + 1) % partitionSize != 0 && j != primeDivision.get(i).size() - 1)
                        arrayListCSV += ",";

                    if ((j + 1) % partitionSize == 0 || j == primeDivision.get(i).size() - 1) {
                        if (j == primeDivision.get(i).size() - 1)
                            arrayListCSV += ",FIN";

                        byte[] csvBytes = arrayListCSV.getBytes();
                        DatagramPacket csvPacket = new DatagramPacket(csvBytes, csvBytes.length, slaves.get(i).getAddress(), slaves.get(i).getPort());
                        serverSocket.send(csvPacket);
                        arrayListCSV = "";
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending to slaves: ");
            e.printStackTrace();
        }
    }

    private static void initThreads (ArrayList<Integer> primeSet, int nThreadCount) {
        int i;
        PrimeCheck primeCheck = new PrimeCheck ();
        primeCheck.setPrimeSet (primeSet);

        for (i = 0; i < nThreadCount; i++) {
            primeCheck = new PrimeCheck ();
            Thread thread = new Thread (primeCheck);
            threads.add (thread);
            thread.start ();
        }
    }

    private static int getSlavePrimes (InetAddress ipAddress, int nPort) {
        int slaveCount = slaves.size ();
        int i = 0, slavePrimes = 0;

        try {
            for (; i < slaveCount; i++) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket slavePacket = new DatagramPacket(buffer, BUFFER_SIZE);
                serverSocket.receive (slavePacket);
                String slaveMessage = new String (slavePacket.getData (), 0, slavePacket.getLength ());
                slavePrimes += Integer.parseInt (slaveMessage);
            }
        } catch (Exception e) {
            System.err.println ("Error in getting slave " + i + ": ");
            e.printStackTrace ();
            slavePrimes = -1;
        }

        return slavePrimes;
    }

    private static void sendToClient (int primeCount) {
        try {
            String strPrimeCount = "" + primeCount;
            byte[] strPrimeCountBytes = strPrimeCount.getBytes ();
            DatagramPacket primeCountPacket = new DatagramPacket(strPrimeCountBytes, strPrimeCountBytes.length, clients.get(0).getAddress (), clients.get(0).getPort ());
            serverSocket.send (primeCountPacket);
        } catch (Exception e) {
            System.err.println ("Error in sending to client: ");
            e.printStackTrace ();
        }
    }
}