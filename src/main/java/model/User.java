package model;

import java.io.Serializable;
import java.net.InetAddress;

public class User implements Serializable {
    private String phoneNumber;
    private String name;
    private String password;
    private String base64ProfilePic;
    boolean isLoggedIn = false;
    private InetAddress inetAddress;

    private static final long serialVersionUID = 8853772668397633721L;

    public User(String phoneNumber, String name, String password, String base64ProfilePic) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.password = password;
        this.base64ProfilePic = base64ProfilePic;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBase64ProfilePic(String base64ProfilePic) {
        this.base64ProfilePic = base64ProfilePic;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getBase64ProfilePic() { return base64ProfilePic; }
}
