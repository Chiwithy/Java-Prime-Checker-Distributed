import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;

public class Slave {
    private static final int BUFFER_SIZE = 1024;
    private static DatagramSocket slaveEndpoint;
    private static ArrayList<Thread> threads = new ArrayList<Thread> ();
    private static ArrayList<Integer> numsReceived = new ArrayList<Integer> ();
    private static SocketAddress sAddress;
    private static InetAddress ipAddress;
    private static int nPort = 8080;
    private static int nThreadCount = 1;
    private static boolean keepListening = true;
    private static boolean isFirstPacket = true;

    public static void main (String[] args) {
        boolean masterConnectSuccess = connectToMaster ();

        if (masterConnectSuccess) {
            try {
                while (keepListening) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket receivedPacket = new DatagramPacket (buffer, BUFFER_SIZE);
                    String primesCSV = "";
                    slaveEndpoint = new DatagramSocket (sAddress);
                    slaveEndpoint.receive (receivedPacket);
                    primesCSV = new String (receivedPacket.getData (), 0, receivedPacket.getLength ());

                    slaveEndpoint.close ();

                    extractCSV (primesCSV);
                }
                System.out.println ("Received " + numsReceived.size ());
                initThreads ();

                try {
                    for (Thread thread : threads) {
                        thread.join ();
                    }
                } catch (Exception e) {
                    System.err.println ("Error in joining threads: ");
                    e.printStackTrace ();
                }

                sendToMaster (PrimeCheck.allPrimes.size ());
                System.out.println ("Found: " + PrimeCheck.allPrimes.size ());
            } catch (Exception e) {
                System.err.println ("Error: Receiving primes went wrong. ");
                e.printStackTrace ();
            }

        }
    }

    private static void extractCSV (String primesCSV) {
        String[] numbers = primesCSV.split (",");
        int i = 0;

        if (isFirstPacket) {
            nThreadCount = Integer.parseInt (numbers[0]);
            isFirstPacket = false;
            i = 1;
        }

        for (; i < numbers.length; i++) {
            if (numbers[i].toLowerCase ().equals ("fin"))
                keepListening = false;
            else
                numsReceived.add (Integer.parseInt (numbers[i]));
        }
    }

    private static void initThreads () {
        int i;
        PrimeCheck primeCheck = new PrimeCheck ();
        primeCheck.setPrimeSet (numsReceived);

        for (i = 0; i < nThreadCount; i++) {
            primeCheck = new PrimeCheck ();
            Thread thread = new Thread (primeCheck);
            threads.add (thread);
            thread.start ();
        }
    }

    private static void printReceived () {
        for (int i = 0; i < numsReceived.size (); i++)
            System.out.println ("" + numsReceived.get (i) + ", ");
    }

    private static void sendToMaster (int primeCount) {
        try {
            String strPrimeCount = "" + primeCount;
            byte[] strPrimeCountBytes = strPrimeCount.getBytes ();
            DatagramPacket primeCountPacket = new DatagramPacket (strPrimeCountBytes, strPrimeCountBytes.length, ipAddress, nPort);

            slaveEndpoint = new DatagramSocket (sAddress);
            slaveEndpoint.send (primeCountPacket);
            slaveEndpoint.close ();
        } catch (Exception e) {
            System.err.println ("Error in sending to master: ");
            e.printStackTrace ();
        }
    }

    public static boolean connectToMaster () {
        String slaveMsg = "slave";
        try {
            slaveEndpoint = new DatagramSocket ();
            sAddress = slaveEndpoint.getLocalSocketAddress ();
            slaveEndpoint.close ();
            slaveEndpoint = new DatagramSocket (sAddress);

            byte[] slaveBytes = slaveMsg.getBytes ();
            ipAddress = InetAddress.getByName ("192.168.1.13");

            DatagramPacket outPacket = new DatagramPacket (slaveBytes, slaveBytes.length, ipAddress, nPort);
            slaveEndpoint.send (outPacket);
            slaveEndpoint.close ();
            return true;
        } catch (Exception e) {
            System.err.println ("Error: Connection to the master server failed: ");
            e.printStackTrace ();
            return false;
        }
    }
}