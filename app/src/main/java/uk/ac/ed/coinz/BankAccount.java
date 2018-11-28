package uk.ac.ed.coinz;

public class BankAccount {
    private Double gold;
    private String id;
    private String email;
    private String lastCountDate;
    private int remainingCoin;

    public BankAccount(){}

    public BankAccount(Double gold, String id, String email, String lastCountDate, int remainingCoin) {
        this.gold = gold;
        this.id = id;
        this.email = email;
        this.lastCountDate = lastCountDate;
        this.remainingCoin = remainingCoin;
    }

    public String getLastCountDate() {
        return lastCountDate;
    }

    public void setLastCountDate(String lastCountDate) {
        this.lastCountDate = lastCountDate;
    }

    public void setRemainingCoin(int remainingCoin) {
        this.remainingCoin = remainingCoin;
    }

    public int getRemainingCoin() {
        return remainingCoin;
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
