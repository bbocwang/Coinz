package uk.ac.ed.coinz;

public class Coin {
    private String id;
    private double value;
    private String currency;

    public Coin(String id, String value, String currency) {
        this.id = id;
        this.value = Double.parseDouble(value);
        this.currency = currency;
    }

    public Coin(String id, Double value, String currency){}

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
