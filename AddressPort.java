import java.net.InetAddress;

public class AddressPort {
    private InetAddress address;
    private int nPort;

    public AddressPort (InetAddress address, int port) {
        this.address = address;
        this.nPort = port;
    }

    public InetAddress getAddress () {
        return this.address;
    }

    public int getPort () {
        return this.nPort;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AddressPort)) {
            return false;
        }
        AddressPort other = (AddressPort) obj;
        
        return this.address.equals(other.getAddress()) && this.nPort == other.getPort();
    }
}