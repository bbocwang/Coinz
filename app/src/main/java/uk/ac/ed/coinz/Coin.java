package uk.ac.ed.coinz;

public class Coin {
    private String id;
    private double value;
    private String currency;
    private String firstOwnerId;

    Coin(){}

    Coin(String id, String value, String currency, String firstOwnerId) {
        this.id = id;
        this.value = Double.parseDouble(value);
        this.currency = currency;
        this.firstOwnerId = firstOwnerId;
    }

    Coin(String id, Double value, String currency, String firstOwnerId){
        this.id = id;
        this.value = value;
        this.currency = currency;
        this.firstOwnerId = firstOwnerId;
    }

    public void setFirstOwnerId(String firstOwnerId) {
        this.firstOwnerId = firstOwnerId;
    }

    public String getFirstOwnerId() {
        return firstOwnerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
