import java.util.ArrayList;

public class PrimeCheck implements Runnable {
    public static final Object primeSetLock = new Object ();
    public static ArrayList<Integer> primeSet = new ArrayList<Integer> ();

    public static final Object allPrimesLock = new Object ();
    public static ArrayList<Integer> allPrimes = new ArrayList<Integer> ();

    private ArrayList<Integer> primes = new ArrayList<Integer> ();

    @Override
    public void run () {
        this.checkPrimeSet ();

        synchronized (PrimeCheck.allPrimesLock) {
            int i;
            
            for (i = 0; i < this.primes.size (); i++) {
                PrimeCheck.allPrimes.add (this.primes.get (i));
            }
        }
    }

    public void setPrimeSet (ArrayList<Integer> primeSet) {
        PrimeCheck.primeSet = new ArrayList<> (primeSet);
    }

    private void checkPrimeSet () {
        boolean listNotEmpty = true;

        while (listNotEmpty) {
            int nToCheck;

            synchronized (PrimeCheck.primeSetLock) {
                if (PrimeCheck.primeSet.isEmpty ()) {
                    listNotEmpty = false;
                    continue;
                }

                nToCheck = PrimeCheck.primeSet.get (0);
                PrimeCheck.primeSet.remove (0);
            }

            if (checkPrime (nToCheck)) {
                this.primes.add (nToCheck);
            }
        }
    }

    private boolean checkPrime (int n) {
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }

        return true;
    }
}