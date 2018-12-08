package uk.ac.ed.coinz;

import android.os.Bundle;
import android.view.View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoginTest {

    @Mock
    private MainActivity mainActivity;

    @Mock
    Bundle savedInstanceState;

    @Mock
    private View view;

    @Before
    public void setUp() throws Exception {
        mainActivity = new MainActivity();
    }

    @Test
    public void testEmptyEmail() throws Exception{
        assertTrue(!mainActivity.login("","123456123456"));
    }

    @Test
    public void testEmptyPassword() throws Exception{
        assertTrue(!mainActivity.login("123@gmail.com",""));
    }

    @Test
    public void testInvalidPassword() throws Exception{
        assertTrue(!mainActivity.login("123@gmail.com","12345"));
    }

    @Test
    public void testInvalidEmailPassword() throws Exception{
        assertTrue(!mainActivity.login("",""));
    }

    @Test
    public void onClickSignUpButton() {
        when(view.getId()).thenReturn(R.id.SignUpButton);
        mainActivity.onClick(view);
    }
}