package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/*
* This is the Wallet activity
*
* There's three fragments in the activity
* */

public class WalletActivity extends AppCompatActivity {
    String tag = "WalletActivity";
    @SuppressLint("LogNotTimber")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new WalletFragment()).commit();

        Log.d(tag,"[OnCreate] Wallet Activity created");
    }

    @SuppressLint("LogNotTimber")
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            menuItem -> {
                Fragment selectedFragment = null;

                switch (menuItem.getItemId()){
                    case R.id.nav_wallet:
                        Log.d(tag,"[Select Fragment] wallet fragment selected");
                        selectedFragment = new WalletFragment();
                        break;
                    case R.id.nav_mail:
                        selectedFragment = new TransferFragment();
                        Log.d(tag,"[Select Fragment] Transfer fragment selected");
                        break;
                    case R.id.nav_bank:
                        Log.d(tag,"[Select Fragment] Bank fragment selected");
                        selectedFragment = new BankFragment();
                        break;
                }
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                }
                return true;
            };
}
