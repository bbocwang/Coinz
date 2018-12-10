package uk.ac.ed.coinz;

import java.util.Objects;

/*
* This is a class for storing coin in a Coin object.
*
* It is very useful when storing and fetching coin information from the firebase database
* in this form. The firebase database consturction method requires to have a empty constructor
* in the class, but this brings a null.
*
* */
public class Coin {
    private String id;
    private double value;
    private String currency;
    private String firstOwnerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coin)) return false;
        Coin coin = (Coin) o;
        return Objects.equals(id, coin.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    //empty constructer for creating the coin object from firebase, ignore the warning
    public Coin(){}

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

    // first owner id is the identifier of who collected this coin first
    // this is useful when user storing coins from other people
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
