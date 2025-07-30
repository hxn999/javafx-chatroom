package model;

import java.io.Serializable;

public class User implements Serializable {
    private String phoneNumber;
    private String name;
    private String password;
    private String base64ProfilePic;

    public User(String phoneNumber, String name, String password, String base64ProfilePic) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.password = password;
        this.base64ProfilePic = base64ProfilePic;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getBase64ProfilePic() { return base64ProfilePic; }
}
