package uk.ac.ed.coinz;

import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class RegisterActivityTest {

    private RegisterActivity registerActivity;

    @Mock
    private View view;

    @Before
    public void setUp() {
        registerActivity = new RegisterActivity();
        view = Mockito.mock(View.class);
    }

    @Test
    public void testEmptyEmail() {
        assertTrue(!registerActivity.registerUser("","123456123456"));
    }

    @Test
    public void testEmptyPassword() {
        assertTrue(!registerActivity.registerUser("123@gmail.com",""));
    }

    @Test
    public void testInvalidPassword() {
        assertTrue(!registerActivity.registerUser("123@gmail.com","12345"));
    }

    @Test
    public void testInvalidEmailPassword() {
        assertTrue(!registerActivity.registerUser("",""));
    }

    @Test
    public void onCreate() {
    }

    @Test
    public void onClickTextViewRegitster() {
        when(view.getId()).thenReturn(R.id.TextViewRegister);
        registerActivity.onClick(view);
    }
}