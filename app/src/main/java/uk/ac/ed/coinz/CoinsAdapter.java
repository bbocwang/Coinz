package uk.ac.ed.coinz;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

public class CoinsAdapter extends ArrayAdapter<Coin> {
    private final String tag = "CoinsAdapter";
    private Activity context;
    private List<Coin> coinList;

    public CoinsAdapter(Activity context, List<Coin> coinList){
        super(context,R.layout.list_view,coinList);
        this.context = context;
        this.coinList = coinList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View listView = inflater.inflate(R.layout.list_view,null);

        TextView currency = (TextView)listView.findViewById(R.id.currency);
        TextView value = (TextView)listView.findViewById(R.id.value);
        ImageView icon = (ImageView)listView.findViewById(R.id.imageView2);
        Coin coin = coinList.get(position);
        currency.setText(coin.getCurrency());
        Double coinValue = coin.getValue();
        DecimalFormat df = new DecimalFormat("#.##");
        coinValue = Double.valueOf(df.format(coinValue));
        String value_string = String.valueOf(coinValue);
        value.setText(value_string);
        Log.d(tag,"[OnCreate Listview] adding coins");
        if(coin.getCurrency().equals("QUID")){
            icon.setImageResource(R.drawable.pound_icon);
        }
        if(coin.getCurrency().equals("DOLR")){
            icon.setImageResource(R.drawable.dollar_icon);
        }
        if(coin.getCurrency().equals("PENY")){
            icon.setImageResource(R.drawable.penny_icon);
        }
        if(coin.getCurrency().equals("SHIL")){
            icon.setImageResource(R.drawable.shil_icon);
        }
        return listView;
    }
}
