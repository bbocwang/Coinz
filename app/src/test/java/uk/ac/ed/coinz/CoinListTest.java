package uk.ac.ed.coinz;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CoinListTest {

    private CoinList coinList;
    private CoinList coinList2;
    @Before
    public void setUp() throws Exception {
        coinList = new CoinList(){};
        coinList2 = new CoinList(){};
    }

    @Test
    public void equals() {
        List<Coin> list = new ArrayList<Coin>();
        list.add(new Coin("1111",0.62,"QUID","1111"));
        list.add(new Coin("2222",0.62,"QUID","1111"));
        list.add(new Coin("3333",0.62,"QUID","1111"));
        list.add(new Coin("4444",0.62,"QUID","1111"));
        coinList.setCoinList(list);
        coinList2.setCoinList(list);
        Assert.assertTrue(coinList.equals(coinList2));
    }
}