package br.metodista.controle;

import java.util.Set;

import br.metodista.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
 * A classe Novos_Dispositivos tem o objetivo de armazenar os dispositivos 
 * encontrados na pesquisa de novos dispositivos e exibir em forma de lista para o usuário.
 * (Somente será listado o dispositivo que conter a categoria UNCATEGORIZED). 
 */

public class Novos_Dispositivos extends Activity {
	
	// Declarações de váriaveis.
	private static final String TAG = "DeviceListActivity";
	private static final boolean D = true;
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;

	Button botaoPesquisa;
	Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		setResult(Activity.RESULT_CANCELED);

		//Habilita botão Pesquisar!
		botaoPesquisa = (Button) findViewById(R.id.button_scan);
		botaoPesquisa.setVisibility(View.VISIBLE);
		botaoPesquisa.setEnabled(false);
		
		//Inicia a pesquisa de dispositivos
		botaoPesquisa.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mNewDevicesArrayAdapter.clear();
				doDiscovery();
			}
		});
		
		//Adaptador para armazenar dados dos novos dispostivos.
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);

		// Inicia a ListView para novos dispositivos e carrega as informações do adaptador.
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// Inicia 1º intenção para enviar ao sistema operacional (A ação será processada pelo método BroadcastReceiver).
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Inicia 2º intenção para enviar ao sistema operacional (A ação será processada pelo método BroadcastReceiver).
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// Inicia adaptador Bluetooth.
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//Inicia automaticamente a pesquisa por novos dispositivos.
		doDiscovery();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}
		this.unregisterReceiver(mReceiver);
	    }

	//Método para pesquisar por novos dispositivos.
	private void doDiscovery() {
		if (D)
			Log.d(TAG, "doDiscovery()");

		// Altera o título do cabeçalho e desabilita temporariamente o botão pesquisa.
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);
		botaoPesquisa.setEnabled(false);
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}
		mBtAdapter.startDiscovery();
	}

    //Método acionado ao selecionar um dispositivo da lista.
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			
			mBtAdapter.cancelDiscovery();
	
			/** Houve uma mudança no pareamento Bluetooth para rev 10 Androids 2.3.3,
			*   por isso devemos tornar o Bluetooth detectável para que a solicitação
			*   de pareamento apareça.
			*/
			
			//Tornar dispositivo visível.
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			
			String info = ((TextView) v).getText().toString();

			if (!info.equals("Nenhum dispositivo encontrado")) {
				discoverableIntent.putExtra("Info", info);
				startActivityForResult(discoverableIntent, 10);
			}

		}
	};

	/** Método BroadcastReceiver que trata das mensagem enviadas 
	*   pelas intenções ACTION_DISCOVERY_FINISHED e  ACTION_FOUND.
	*/
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			//Recebe a intenção enviada pelo Sistema Operacional.
			String action = intent.getAction();

			// Ação da 1º Intenção.
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				
				//Para cada dispositivo encontrado armazena-se no adaptador.
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					 if(getClassificaçãoDispositivo(device.getBluetoothClass().getMajorDeviceClass()).equals("UNCATEGORIZED")){
					mNewDevicesArrayAdapter.add(device.getName() + "\n"	+ device.getAddress());
					 }
				}
			} 
			
			// Ação da 2º Intençãoo.
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				botaoPesquisa.setEnabled(true);
				// A pesquisa acabou e não retornou nenhum dispositivo.
				if (mNewDevicesArrayAdapter.getCount() == 0) {
					String noDevices = getResources().getText(R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};

	// Método que recebe o resultado do método OnItemClickListener e devolve a intenção para a classe principal.
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 10) {
			switch (resultCode) {
			case 300:
				String info = discoverableIntent.getStringExtra("Info");
				try {
					String address = info.substring(info.length() - 17);
					Intent intent = new Intent();
					intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
					setResult(Activity.RESULT_OK, intent);
					finish();
				} catch (Exception e) {
					// TODO: handle exception
				}
				break;
			case Activity.RESULT_CANCELED:
				break;
			default:
				break;
			}
		}
	}

	// Método que representa a classificação/categoria dos dispositivos Bluetooth.
	private String getClassificaçãoDispositivo(int major) {
		switch (major) {
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
		default:
			return "outros!";
		}
	}
}
