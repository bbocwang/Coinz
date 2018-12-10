package uk.ac.ed.coinz;

import java.util.List;
import java.util.Objects;

/*
* This Class is a CoinList, used to construct the history in the database
* to avoide double collect.
* */

class CoinList {
    private List<Coin> coinList;


    CoinList(){};

    CoinList(List<Coin> coinList){
        this.coinList = coinList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoinList)) return false;
        CoinList coinList1 = (CoinList) o;
        return Objects.equals(coinList, coinList1.coinList);
    }

    public void setCoinList(List<Coin> coinList) {
        this.coinList = coinList;
    }

    @Override
    public int hashCode() {

        return Objects.hash(coinList);
    }

    public List<Coin> getCoinList() {
        return coinList;
    }
}
