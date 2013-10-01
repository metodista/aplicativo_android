/*
 * Projeto de comunicação entre o Android e o Arduino para enviar comandos do controle remoto.
 * Este projeto é opensource disponível para estudo e melhoria do aplicativo.
 * Foi utilizado como referência a aplicação blueterm também opensource disponível no link abaixo:
 * https://play.google.com/store/apps/details?id=es.pymasde.blueterm
 * Ano: 2013
 */


package br.metodista.controle;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import br.metodista.R;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
//import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
//import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

/**
 * Classe principal do aplicativo, tem o objetivo de gerenciar as funcionalidade do software.
 *  
 *  - Solicitar lista de dispositivos pareados e novos.
 *  - Solicitar pedido de conexão.
 *  - Enviar comandos para a placa Arduino.
 *  - Gerencia a mudança de estado dos layouts dos controles remotos.
 */

public class Main_Controle_Remoto extends Activity implements OnGestureListener {

	// Declarações de váriaveis
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static int controleTela =0;
    
    //Botões TV.
    private Button buttonTv_tv,buttonPower_tv,buttonInput_tv,buttonMts_tv,buttonAprog_tv,buttonMemory_tv,buttonCaption_tv,
    buttonUm_tv,buttonDois_tv,buttonTres_tv,buttonQuatro_tv,buttonCinco_tv,buttonSeis_tv,buttonSete_tv,buttonOito_tv,
    buttonNove_tv,buttonZero_tv,buttonMute_tv,buttonFcr_tv,buttonCanal_Mais_tv,buttonVolume_Menos_tv,buttonVolume_Mais_tv,buttonCanal_Menos_tv,
    buttonEnter_tv,buttonMenu_tv,buttonReview_tv,buttonSleep_tv,buttonApc_tv,buttonDasp_tv,buttonArc_tv;
    //Botões DVD.
    private Button buttonPower_dvd,buttonMute_dvd,buttonFunction_dvd,buttonInput_dvd,buttonSleep_dvd,buttonReturn_dvd,buttonUm_dvd,buttonDois_dvd,
	buttonTres_dvd,buttonQuatro_dvd,buttonCinco_dvd,buttonSeis_dvd,buttonSete_dvd,buttonOito_dvd,buttonNove_dvd,buttonZero_dvd,buttonEq_dvd,buttonVsm_dvd,
	buttonEcho_Menos_dvd,buttonEcho_Mais_dvd,buttonMic_Menos_dvd,buttonMic_Mais_dvd,buttonSetup_dvd,buttonDisplay_dvd,buttonPreset_Mais_dvd,buttonPreset_Menos_dvd,
	buttonEnter_dvd,buttonTun_Menos_dvd,buttonTun_Mais_dvd,buttonMenu_dvd,buttonTitle_dvd,buttonScan_Menos_dvd,buttonScan_Mais_dvd,buttonSkip_Menos_dvd,
	buttonSkip_Mais_dvd,buttonPlay_dvd,buttonPause_dvd,buttonProg_dvd,buttonRec_dvd,buttonStop_dvd,buttonVolume_Menos_dvd,buttonVolume_Mais_dvd,
	buttonRepeat_dvd,buttonStitle_dvd,buttonClear_dvd;
    
    private GestureDetector detector = null;
    private static TextView mTitle;
    private static TextView mTitleC;
    private String mConnectedDeviceName = null;
    
    public static final boolean DEBUG = true;
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && false;
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && false;
	public static final String LOG_TAG = "Controle Remoto";

    // Mensagem enviadas para o Handler.
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;	

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	private BluetoothAdapter mBluetoothAdapter = null;
    private TermKeyListener mKeyListener;
	private static BluetoothSerialService mSerialService = null;
   
	
	private boolean mEnablingBT;
	private boolean autoConnect;
    private int mControlKeyCode;
    private MenuItem mMenuItemConnect;
    
    //Guarda o último dispositivo conectado.
    private static String FILENAME="LOAD_MEMORY_DEVICE";
  
    //Guarda a mudança do status de conexão.
    public static int mudancaStatus;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (DEBUG)
			Log.e(LOG_TAG, "+++ ON CREATE +++");	
		
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
        //Controla a ordem dos layouts (TV ou DVD).
        if(controleTela == 0){
        	setContentView(R.layout.funcoes_tv);        	
        }
        else{
        	setContentView(R.layout.funcoes_dvd);  
        }

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

       
        //Título do cabeçalho.
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitleC = (TextView) findViewById(R.id.b_dvd_tv);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
 
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Valida se o dispositivo suporta a tecnologia Bluetooth.
		if (mBluetoothAdapter == null) {
            finishDialogNoBluetooth(); 
			return;
		}
		
	   carregaBotoesLayout();
    	
       mKeyListener = new TermKeyListener();
       mSerialService = new BluetoothSerialService(this, mHandlerBT);        
       detector = new GestureDetector(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (DEBUG)
			Log.e(LOG_TAG, "++ ON START ++");
		
		mEnablingBT = false;
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (DEBUG) {
			Log.e(LOG_TAG, "+ ON RESUME +");
		}
		
		// Se a solicitação (pedido de ativação Bluetooth) já foi feita, então, não precisa fazer novamente.
		if (!mEnablingBT) {
			//Solicita ativação do Bluetooth.
		    if ((mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
		    	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);		
        		mEnablingBT = true;
		    }		
		    
		    //Inicia serviços do Bluetooth.
		    if (mSerialService != null) {
		    	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
		    		mSerialService.start();    		
		    	}
		    }
		}
		//Lê arquivo e tenta conectar automaticamente no último dispositivo que foi conectado.
		if (mBluetoothAdapter.isEnabled() && mSerialService.getState() != BluetoothSerialService.STATE_CONNECTED && !autoConnect){
        	Log.d(LOG_TAG, "READ FILENAME: "+ FILENAME);
    		read(FILENAME);
    		autoConnect = true;
        }
	}
	
	public void carregaBotoesLayout() {
		//Comandos enviados por cada botão (TV).
		if (controleTela == 0) {
			mTitleC.setText(R.string.using_control_tv);
			buttonInput_tv = (Button) findViewById(R.id.buttonTv1);
			buttonTv_tv = (Button) findViewById(R.id.buttonTv2);
			buttonPower_tv = (Button) findViewById(R.id.buttonTv3);
			buttonMts_tv = (Button) findViewById(R.id.buttonTv4);
			buttonAprog_tv = (Button) findViewById(R.id.buttonTv5);
			buttonMemory_tv = (Button) findViewById(R.id.buttonTv6);
			buttonCaption_tv = (Button) findViewById(R.id.buttonTv7);
			buttonUm_tv = (Button) findViewById(R.id.buttonTv8);
			buttonDois_tv = (Button) findViewById(R.id.buttonTv9);
			buttonTres_tv = (Button) findViewById(R.id.buttonTv10);
			buttonQuatro_tv = (Button) findViewById(R.id.buttonTv11);
			buttonCinco_tv = (Button) findViewById(R.id.buttonTv12);
			buttonSeis_tv = (Button) findViewById(R.id.buttonTv13);
			buttonSete_tv = (Button) findViewById(R.id.buttonTv14);
			buttonOito_tv = (Button) findViewById(R.id.buttonTv15);
			buttonNove_tv = (Button) findViewById(R.id.buttonTv16);
			buttonZero_tv = (Button) findViewById(R.id.buttonTv18);
			buttonMute_tv = (Button) findViewById(R.id.buttonTv17);
			buttonFcr_tv = (Button) findViewById(R.id.buttonTv19);
			buttonCanal_Mais_tv = (Button) findViewById(R.id.buttonTv20);
			buttonVolume_Menos_tv = (Button) findViewById(R.id.buttonTv21);
			buttonVolume_Mais_tv = (Button) findViewById(R.id.buttonTv22);
			buttonCanal_Menos_tv = (Button) findViewById(R.id.buttonTv23);
			buttonEnter_tv = (Button) findViewById(R.id.buttonTv24);
			buttonMenu_tv = (Button) findViewById(R.id.buttonTv25);
			buttonReview_tv = (Button) findViewById(R.id.buttonTv26);
			buttonSleep_tv = (Button) findViewById(R.id.buttonTv27);
			buttonApc_tv = (Button) findViewById(R.id.buttonTv28);
			buttonDasp_tv = (Button) findViewById(R.id.buttonTv29);
			buttonArc_tv = (Button) findViewById(R.id.buttonTv30);

			buttonPower_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 65;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonInput_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 66;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonTv_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 67;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonMts_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 68;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonAprog_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 69;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonMemory_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 70;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonCaption_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 71;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonUm_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 49;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonDois_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 50;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonTres_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 51;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonQuatro_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 52;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonCinco_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 53;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSeis_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 54;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSete_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 55;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonOito_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 56;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonNove_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 57;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonZero_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 48;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonCanal_Mais_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 74;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonCanal_Menos_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 75;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonVolume_Mais_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 76;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonVolume_Menos_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 77;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonEnter_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 78;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonMenu_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 79;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonMute_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 72;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonFcr_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 73;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonReview_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 80;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSleep_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 81;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonApc_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 82;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonDasp_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 83;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonArc_tv.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 84;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonCanal_Mais_tv.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 74;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});

			buttonCanal_Menos_tv.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 75;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});

			buttonVolume_Mais_tv.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 76;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});

			buttonVolume_Menos_tv.setOnTouchListener(new View.OnTouchListener() {

						private Handler mHandler;

						@Override
						public boolean onTouch(View v, MotionEvent event) {
							switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN:
								if (mHandler != null)
									return true;
								mHandler = new Handler();
								mHandler.postDelayed(mAction, 500);
								break;
							case MotionEvent.ACTION_UP:
								if (mHandler == null)
									return true;
								mHandler.removeCallbacks(mAction);
								mHandler = null;
								break;
							case MotionEvent.ACTION_CANCEL:
								if (mHandler == null)
									return true;
								mHandler.removeCallbacks(mAction);
								mHandler = null;
								break;
							}
							return false;
						}

						Runnable mAction = new Runnable() {
							@Override
							public void run() {
								int numeroConvertido = 77;
								byte[] mBuffer = new byte[1];
								mBuffer[0] = (byte) mKeyListener
										.mapControlChar(numeroConvertido);
								send(mBuffer);
								mHandler.postDelayed(this, 500);
								Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
							    vib.vibrate(100);
							}
						};

					});

		} else {
			//Comandos enviados por cada botão (DVD).
			mTitleC.setText(R.string.using_control_dvd);
			buttonPower_dvd = (Button) findViewById(R.id.button1);
			buttonMute_dvd = (Button) findViewById(R.id.button2);
			buttonFunction_dvd = (Button) findViewById(R.id.button3);
			buttonInput_dvd = (Button) findViewById(R.id.button4);
			buttonSleep_dvd = (Button) findViewById(R.id.button5);
			buttonReturn_dvd = (Button) findViewById(R.id.button6);
			buttonUm_dvd = (Button) findViewById(R.id.button7);
			buttonDois_dvd = (Button) findViewById(R.id.button8);
			buttonTres_dvd = (Button) findViewById(R.id.button9);
			buttonEq_dvd = (Button) findViewById(R.id.button10);
			buttonVsm_dvd = (Button) findViewById(R.id.button11);
			buttonQuatro_dvd = (Button) findViewById(R.id.button12);
			buttonCinco_dvd = (Button) findViewById(R.id.button13);
			buttonSeis_dvd = (Button) findViewById(R.id.button14);
			buttonSete_dvd = (Button) findViewById(R.id.button15);
			buttonOito_dvd = (Button) findViewById(R.id.button16);
			buttonNove_dvd = (Button) findViewById(R.id.button17);
			buttonZero_dvd = (Button) findViewById(R.id.button18);
			buttonEcho_Menos_dvd = (Button) findViewById(R.id.button19);
			buttonEcho_Mais_dvd = (Button) findViewById(R.id.button20);
			buttonMic_Menos_dvd = (Button) findViewById(R.id.button21);
			buttonMic_Mais_dvd = (Button) findViewById(R.id.button22);
			buttonPreset_Mais_dvd = (Button) findViewById(R.id.button23);
			buttonSetup_dvd = (Button) findViewById(R.id.button24);
			buttonDisplay_dvd = (Button) findViewById(R.id.button25);
			buttonTun_Menos_dvd = (Button) findViewById(R.id.button26);
			buttonEnter_dvd = (Button) findViewById(R.id.button27);
			buttonTun_Mais_dvd = (Button) findViewById(R.id.button28);
			buttonPreset_Menos_dvd = (Button) findViewById(R.id.button29);
			buttonMenu_dvd = (Button) findViewById(R.id.button30);
			buttonTitle_dvd = (Button) findViewById(R.id.button31);
			buttonScan_Menos_dvd = (Button) findViewById(R.id.button32);
			buttonScan_Mais_dvd = (Button) findViewById(R.id.button33);
			buttonPlay_dvd = (Button) findViewById(R.id.button34);
			buttonSkip_Menos_dvd = (Button) findViewById(R.id.button35);
			buttonSkip_Mais_dvd = (Button) findViewById(R.id.button36);
			buttonPause_dvd = (Button) findViewById(R.id.button37);
			buttonProg_dvd = (Button) findViewById(R.id.button38);
			buttonRec_dvd = (Button) findViewById(R.id.button39);
			buttonStop_dvd = (Button) findViewById(R.id.button40);
			buttonVolume_Mais_dvd = (Button) findViewById(R.id.button41);
			buttonVolume_Menos_dvd = (Button) findViewById(R.id.button42);
			buttonRepeat_dvd = (Button) findViewById(R.id.button43);
			buttonStitle_dvd = (Button) findViewById(R.id.button44);
			buttonClear_dvd = (Button) findViewById(R.id.button45);

			buttonPower_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 97;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}

			});

			buttonMute_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 98;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonFunction_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 99;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonInput_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 100;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSleep_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 101;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonReturn_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 102;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonUm_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 103;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonDois_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 104;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonTres_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 105;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonQuatro_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 106;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonCinco_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 107;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSeis_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 108;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSete_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 109;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonOito_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 110;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonNove_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 111;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonZero_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 112;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonEq_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 113;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonVsm_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 114;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonEcho_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 116;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonEcho_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 115;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonMic_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 118;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonMic_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 117;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSetup_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 119;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonDisplay_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 120;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonPreset_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 121;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonPreset_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 122;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonEnter_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 45;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonTun_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 43;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonTun_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 42;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonMenu_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 94;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonTitle_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 126;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonScan_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 64;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonScan_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 124;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSkip_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {;
					int numeroConvertido = 37;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonSkip_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 33;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonPlay_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 38;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonPause_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 40;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonProg_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 35;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonRec_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 58;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonStop_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 59;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonVolume_Menos_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 36;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonVolume_Mais_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 41;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonRepeat_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 123;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			buttonStitle_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 125;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});

			buttonClear_dvd.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					int numeroConvertido = 63;
					byte[] mBuffer = new byte[1];
					mBuffer[0] = (byte) mKeyListener
							.mapControlChar(numeroConvertido);
					send(mBuffer);
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				    vib.vibrate(100);
				}
			});
			
			buttonVolume_Mais_dvd.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 41;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});
			
			buttonVolume_Menos_dvd.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 36;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});
			
			buttonPreset_Mais_dvd.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 121;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});
			
			buttonPreset_Menos_dvd.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 122;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});
			
			buttonTun_Menos_dvd.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 43;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});
			
			buttonTun_Mais_dvd.setOnTouchListener(new View.OnTouchListener() {

				private Handler mHandler;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (mHandler != null)
							return true;
						mHandler = new Handler();
						mHandler.postDelayed(mAction, 500);
						break;
					case MotionEvent.ACTION_UP:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					case MotionEvent.ACTION_CANCEL:
						if (mHandler == null)
							return true;
						mHandler.removeCallbacks(mAction);
						mHandler = null;
						break;
					}
					return false;
				}

				Runnable mAction = new Runnable() {
					@Override
					public void run() {
						int numeroConvertido = 42;
						byte[] mBuffer = new byte[1];
						mBuffer[0] = (byte) mKeyListener
								.mapControlChar(numeroConvertido);
						send(mBuffer);
						mHandler.postDelayed(this, 500);
						Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					    vib.vibrate(100);
					}
				};

			});
		}
	}
	
	//Métodos para controlar o movimento da tela (Movimento de mudança de tela - Direite para a Esquerda).
	public boolean onDown(MotionEvent arg0) {
		return false;
	}
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		if (Math.abs(e1.getY() - e2.getY()) > 250) {
			return false;	}
		// Movimento da direita para esquerda
		if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 50) {
			if(controleTela == 0){
				controleTela =1;
			}
			else{
				controleTela =0;
			}
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		switch (keyCode) {
		
		case KeyEvent.KEYCODE_BACK:
			mSerialService.stop();
			Main_Controle_Remoto.this.finish();
		default:
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

	public void onLongPress(MotionEvent e) {
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	public void onShowPress(MotionEvent e) {
	}

	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}
	    
	 @Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		  boolean handled = super.dispatchTouchEvent(ev);
	       handled = detector.onTouchEvent(ev);    
	       return handled;
	    }
	 

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

   }

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (DEBUG)
			Log.e(LOG_TAG, "- ON PAUSE -");
	}

    @Override
    public void onStop() {
        super.onStop();
        if(DEBUG)
        	Log.e(LOG_TAG, "-- ON STOP --");
    }


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG)
			Log.e(LOG_TAG, "--- ON DESTROY ---");
		
        if (mSerialService != null)
        	mSerialService.stop();
        
	}
   
	public int getConnectionState() {
		return mSerialService.getState();
	}


    public void send(byte[] out) {
    	mSerialService.write( out );//j
    }
    
    public int getTitleHeight() {
    	return mTitle.getHeight();
    }
    
    //Thread que controla os status das conexões.
    private final Handler mHandlerBT = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {      
        	
           switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
               if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
               mudancaStatus = msg.arg1;
               switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
            	 if (mMenuItemConnect != null) {
            		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            		mMenuItemConnect.setTitle(R.string.disconnect);
            	}
        	
            	 	mTitle.setText(R.string.title_connected_to);
            	 	mTitle.append(mConnectedDeviceName);
                break;
                
                case BluetoothSerialService.STATE_CONNECTING:
                	mTitle.setText(R.string.title_connecting);
                 break;
                
                case BluetoothSerialService.STATE_LISTEN:
                case BluetoothSerialService.STATE_NONE:
            	 if (mMenuItemConnect != null) {
            		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
            		mMenuItemConnect.setTitle(R.string.connect);
            	 }                	
                mTitle.setText(R.string.title_not_connected);

                break;
                }
                break;
            	case MESSAGE_WRITE:
                break;
              
            	case MESSAGE_DEVICE_NAME:
            		mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
            		Toast.makeText(getApplicationContext(), "Conectado com "+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            	case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };    
    
    //Método para finalizar a aplicação.
	public void finishDialogNoBluetooth() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_dialog_no_bt)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(R.string.app_name)
				.setCancelable(false)
				.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								finish();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}
 
	//Método que recebe os dispositivos para conexão enviados pelas classes Novos_Dispositivos e Dispositivos_Pareados.
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (controleTela == 0) {
			setContentView(R.layout.funcoes_tv);
			carregaBotoesLayout();
		} else {
			setContentView(R.layout.funcoes_dvd);
			carregaBotoesLayout();
		}
    	
    	if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            if (resultCode == Activity.RESULT_OK) {
            	// Pega o dispositivo e envia para conexão.
            	String address = data.getExtras().getString(Dispositivos_Pareados.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mSerialService.connect(device);
                //Grava último disposito no arquivo.
                write(device.getAddress(), FILENAME);
            }
            break;

        case REQUEST_ENABLE_BT:
            // Resultado requisição de ativação Bluetooth 
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(LOG_TAG, "BT not enabled");
              //Método que cria um alerta e finaliza a aplicação.
                finishDialogNoBluetooth();
            }           
        }
    }
    //Cria os menus.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        mMenuItemConnect = menu.getItem(0);
        if(mudancaStatus == 3){
        	mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    		mMenuItemConnect.setTitle(R.string.disconnect);
        }
        
        return true;
    }
    //Funções de cada item do menu.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect:       	
        	if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
        		// Invoca a classe Dispositivos_Pareados e aguarda por resultados.
        		Intent serverIntent = new Intent(this, Dispositivos_Pareados.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        	else
            	if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            		mSerialService.stop();
		    		mSerialService.start();
            	}
            return true;
       case R.id.newconnect:
    	   if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
    			// Invoca a classe Novos_Dispositivos e aguarda por resultados.
       		Intent serverIntent = new Intent(this, Novos_Dispositivos.class);
       		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
       	}
       	else
           	if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
           		mSerialService.stop();
		    		mSerialService.start();
           	}
            return true;
        }
        return false;
    }
    
    //Escreve no arquivo os dados da última conexão.
    public boolean write(String texto, String FILENAME){
    	try {
    		FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
    		fos.write(texto.getBytes());
    		fos.close();
    		return true;
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    		return false;
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	}
    	}
    //Lê os dados da última conexão e tenta conectar automatico.
    public boolean read(String FILENAME){
    	try {
            FileInputStream inputStream = openFileInput(FILENAME);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            r.close();
            inputStream.close();
            Log.d("File", "FILE CONTENTS: " + total);
            
            String address = String.valueOf(total);
            try{
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mSerialService.connect(device);
            }catch(Exception e){
            	e.getMessage();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    

    private byte[] mBuffer;
    private int mHead;
    private int mStoredBytes;
}

//Classes e métodos necessário para enviar o comando para o Arduino.
class TermKeyListener {
    private class ModifierKey {

        private int mState;

        private static final int UNPRESSED = 0;

        private static final int PRESSED = 1;

        private static final int RELEASED = 2;

        private static final int USED = 3;

        private static final int LOCKED = 4;

    public ModifierKey() {
            mState = UNPRESSED;
        }

        public void adjustAfterKeypress() {
            switch (mState) {
            case PRESSED:
                mState = USED;
                break;
            case RELEASED:
                mState = UNPRESSED;
                break;
            default:
                break;
            }
        }

        public boolean isActive() {
            return mState != UNPRESSED;
        }
    }

    private ModifierKey mAltKey = new ModifierKey();

    private ModifierKey mCapKey = new ModifierKey();

    private ModifierKey mControlKey = new ModifierKey();

    public TermKeyListener() {
    }

    public int mapControlChar(int ch) {
        int result = ch;
        if (mControlKey.isActive()) {
            if (result >= 'a' && result <= 'z') {
                result = (char) (result - 'a' + '\001');
            } else if (result == ' ') {
                result = 0;
            } else if ((result == '[') || (result == '1')) {
                result = 27;
            } else if ((result == '\\') || (result == '.')) {
                result = 28;
            } else if ((result == ']') || (result == '0')) {
                result = 29;
            } else if ((result == '^') || (result == '6')) {
                result = 30; // control-^
            } else if ((result == '_') || (result == '5')) {
                result = 31;
            }
        }

        if (result > -1) {
            mAltKey.adjustAfterKeypress();
            mCapKey.adjustAfterKeypress();
            mControlKey.adjustAfterKeypress();
        }
        return result;
    }
}
