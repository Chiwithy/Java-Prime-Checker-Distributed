import java.util.Scanner;
import java.net.*;

public class Client {
    private static final int BUFFER_SIZE = 1024;
    private static DatagramSocket clientEndpoint;
    private static SocketAddress sAddress;
    private static InetAddress ipAddress;
    private static int nPort = 8080;

    public static void main (String[] args) {
        long timeStart = 0;
        long timeFinish, timeElapsed;
        int nLimit, nThreadCount, nPrimeCount;
        boolean masterConnectSuccess;
        String strMasterPrimeCount = "";
        Scanner scanny = new Scanner (System.in);
        
        System.out.print ("\nPrime check up to number: ");
        nLimit = scanny.nextInt ();
        System.out.print ("Number of threads to use: ");
        nThreadCount = scanny.nextInt ();
        scanny.close ();

        masterConnectSuccess = connectToMaster ();

        if (masterConnectSuccess) {
            String primeCSV = "" + nLimit + "," + nThreadCount;
            try {
                byte[] primeCSVBytes = primeCSV.getBytes ();
                clientEndpoint = new DatagramSocket (sAddress);

                DatagramPacket outPacket = new DatagramPacket (primeCSVBytes, primeCSVBytes.length, ipAddress, nPort);
                
                timeStart = System.nanoTime ();

                clientEndpoint.send (outPacket);
                clientEndpoint.close ();
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
        else {
            System.err.println ("Connection failed. Please restart the program.");
            System.exit (1);
        }

        try {
            while (strMasterPrimeCount.trim () == "") {
                byte[] buffer = new byte[Client.BUFFER_SIZE];
                DatagramPacket inPacket = new DatagramPacket (buffer, buffer.length);
                clientEndpoint = new DatagramSocket (sAddress);
                clientEndpoint.receive (inPacket);
                strMasterPrimeCount = new String (inPacket.getData (), 0, inPacket.getLength ());

                clientEndpoint.close ();
            }
        } catch (Exception e) {
            System.err.println (e);
        }


        timeFinish = System.nanoTime ();
        timeElapsed = timeFinish - timeStart;
        timeElapsed /= 1000;

        nPrimeCount = Integer.parseInt (strMasterPrimeCount);

        System.out.println ("\n" + nPrimeCount + " primes were found.");
        System.out.println ("Execution time: " + timeElapsed);
    }

    public static boolean connectToMaster () {
        String clientMsg = "client";
        try {
            clientEndpoint = new DatagramSocket ();
            sAddress = clientEndpoint.getLocalSocketAddress ();
            clientEndpoint.close ();
            clientEndpoint = new DatagramSocket (sAddress);

            byte[] clientBytes = clientMsg.getBytes ();
            ipAddress = InetAddress.getByName ("192.168.1.13");

            DatagramPacket outPacket = new DatagramPacket (clientBytes, clientBytes.length, ipAddress, nPort);
            clientEndpoint.send (outPacket);
            clientEndpoint.close ();
            return true;
        } catch (Exception e) {
            System.err.println ("Error: Connection to the master server failed: ");
            e.printStackTrace ();
            return false;
        }
    }
}