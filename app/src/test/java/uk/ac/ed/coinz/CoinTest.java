package uk.ac.ed.coinz;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CoinTest {

    private Coin coin;

    @Before
    public void setUp() throws Exception {
        coin = new Coin("123456",0.62,"QUID","1111");
    }

    @Test
    public void firstOwnerIdTest() {
        Assert.assertEquals("1111",coin.getFirstOwnerId());
    }

    @Test
    public void coinIdTest() {
        Assert.assertEquals("123456",coin.getId());
    }

    @Test
    public void coinValueTest() {
        Assert.assertEquals(0.62,coin.getValue(),0.1);
    }

    @Test
    public void coinCurrencyTest() {
        Assert.assertEquals("QUID",coin.getCurrency());
    }
}