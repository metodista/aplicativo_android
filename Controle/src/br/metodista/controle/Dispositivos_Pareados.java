package br.metodista.controle;

import java.util.Set;

import br.metodista.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * A classe Dispositivos_Pareados tem o objetivo de armazenar os dispositivos 
 * já pareados pelo celular e exibir em forma de lista para o usuário. 
 * (Somente será listado o dispositivo que conter a categoria UNCATEGORIZED).
 */

public class Dispositivos_Pareados extends Activity {
    
	// Declarações de váriaveis.
    public 	static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);

        //Desabilita botão Pesquisar!
        Button BotaoPesquisa = (Button) findViewById(R.id.button_scan);
        BotaoPesquisa.setVisibility(View.GONE);
        
        //Adaptador para armazenar dados dos dispostivos pareados.
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Inicia a ListView para dispositivos pareados e carrega as informações do adaptador.
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Inicia adaptador Bluetooth.
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Pega as informações de todos os dispositivos pareados.
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Filtra os dispositivos por categoria e armazena no adaptador.
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
            	if(getClassificaçãoDispositivo(device.getBluetoothClass().getMajorDeviceClass()).equals("UNCATEGORIZED")){
                  mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
             }
            }
        }if(mPairedDevicesArrayAdapter.getCount() ==0) {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    //Método acionado ao selecionar um dispositivo da lista.
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            mBtAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Devolve um dispostivo para a classe principal através da Intent.
           if (!info.equals("Não há dispositivos pareados")) {      	
        	Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            setResult(Activity.RESULT_OK, intent);
            finish();
           }
          }
    };

    //Método que representa a classificação/categoria dos dispositivos Bluetooth.
    private String getClassificaçãoDispositivo(int major){
    	  switch(major){ 
    	  case BluetoothClass.Device.Major.UNCATEGORIZED:
       	   return "UNCATEGORIZED";
    	  case BluetoothClass.Device.Major.AUDIO_VIDEO:
    	   return "AUDIO_VIDEO";
    	  case BluetoothClass.Device.Major.COMPUTER:
    	   return "COMPUTER";
    	  case BluetoothClass.Device.Major.HEALTH:
    	   return "HEALTH";
    	  case BluetoothClass.Device.Major.IMAGING:
    	   return "IMAGING"; 
    	  case BluetoothClass.Device.Major.MISC:
    	   return "MISC";
    	  case BluetoothClass.Device.Major.NETWORKING:
    	   return "NETWORKING"; 
    	  case BluetoothClass.Device.Major.PERIPHERAL:
    	   return "PERIPHERAL";
    	  case BluetoothClass.Device.Major.PHONE:
    	   return "PHONE";
    	  case BluetoothClass.Device.Major.TOY:
    	   return "TOY";
    	  case BluetoothClass.Device.Major.WEARABLE:
    	   return "AUDIO_VIDEO";
    	  default: return "outros!";
    	  }
    	 }
}
