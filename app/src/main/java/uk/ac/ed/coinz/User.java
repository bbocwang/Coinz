package uk.ac.ed.coinz;

public class User {
    private String email;
    private String uid;

    public User(){}

    User(String email, String uid) {
        this.email = email;
        this.uid = uid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }
}
