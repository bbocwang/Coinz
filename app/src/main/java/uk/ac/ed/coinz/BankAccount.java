package uk.ac.ed.coinz;

public class BankAccount {
    private Double gold;
    private String id;
    private String email;

    public BankAccount(){}

    public BankAccount(Double gold, String id, String email) {
        this.gold = gold;
        this.id = id;
        this.email = email;
    }

    public void setGold(Double gold) {
        this.gold = gold;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getGold() {
        return gold;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
