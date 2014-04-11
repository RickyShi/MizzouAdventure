package com.example.bodysensor_test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	public final static String BASE_PATH = "sdcard/MiAResults/";
    // Message types sent from the DeviceConnect Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_LEISURE = 6;
    // Key names received from the DeviceConnect Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";     
    private BluetoothAdapter btAdapter;		
	private final static String TAG = "MainActivity";	
	public static final int REQUEST_ENABLE_BT = 3;
	public static final int INTENT_SELECT_DEVICES = 0;
	public static final int INTENT_DISCOVERY = 1;
	public static final int INTENT_VIEW_DEVICES = 2;
	
	protected static final int START = 0;
	protected static final int STOP = 1;
	protected static final int SURVEY = 2;
	protected static final int STATE = 3;
	private static final int REQUEST_CONNECT_DEVICE = 4;
	private static final boolean D = true;	
	private TextView tvSetData;	

	private Button btnStart;
	private Button btnCancel;
	private Button btnSetUID;
	
	
	private String UID = "null";
	private TextView tvTest;
	private TextView tvAvgTemp;
	private TextView tvUID;
	private String filename;
	private TextView tvEDAVaule;
	
	private static Context context;
	
	private static boolean startFlag = false;
	
	public String datatoWrite="";
	private TransmitData transmitData;
	private DeviceConnect SensorConnect;
	
	private ArrayList<Float> TempList = null;
	private ArrayList<Float> EDAList = null;
	
	private float avgTemp = 0;
	private float avgEDA = 0;
	private final String USER_INPUT_HINT = "Please input 4 digits for UserID!";
	
	private TextView mTextField;
	private int i = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        File DIR = new File(BASE_PATH);
		if(!DIR.exists())
			DIR.mkdir();
		
		context = this;
		
		TempList = new ArrayList<Float>();
		EDAList = new ArrayList<Float>();
		
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        tvSetData=(TextView)findViewById(R.id.tvSetData);
        tvUID=(TextView)findViewById(R.id.tvUID);
        tvTest=(TextView)findViewById(R.id.tvTest);
        tvAvgTemp=(TextView)findViewById(R.id.tvFright);
        tvEDAVaule=(TextView)findViewById(R.id.tvEDAVaule);
        btnStart=(Button)findViewById(R.id.btnStart);
        btnCancel=(Button)findViewById(R.id.btnCancel);
        btnSetUID=(Button)findViewById(R.id.btnSetUID);
        mTextField=(TextView)findViewById(R.id.mTextField);
        btnSetUID.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				// get prompts.xml view
				LayoutInflater li = LayoutInflater.from(context);
				View promptsView = li.inflate(R.layout.prompts, null);
 
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						context);
 
				// set prompts.xml to alertdialog builder
				alertDialogBuilder.setView(promptsView);
				alertDialogBuilder.setMessage(USER_INPUT_HINT);
 
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.editTextDialogUserInput);
 
				// set dialog message
				alertDialogBuilder
					.setCancelable(false)
					.setPositiveButton("OK",
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						// get user input and set it to tvUID
					    tvUID.setText(userInput.getText());
					    UID = String.valueOf(userInput.getText());
					    
					    }
					  })
					.setNegativeButton("Cancel",
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					    }
					  });
 
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
 
				// show it
				alertDialog.show();
			}
		});
        
        btnStart.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				if (!UID.equals("null")){
					if (UID.length()!=4){
						Toast.makeText(getApplicationContext(),"User ID must be 4 digits.",Toast.LENGTH_LONG).show();
					}
					else {
						startFlag = true;
						new CountDownTimer(30000, 1000) {

						     public void onTick(long millisUntilFinished) {
						         mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
						     }

						     public void onFinish() {
						        mTextField.setText("done!");
						        startFlag = false;
								UID = "null";
								//tvUID.setText(UID);
								//transmitData.cancel(true);
								if (SensorConnect!=null)
								SensorConnect.disconnect();
								//tvSetData.setText("not connected");
								Iterator<Float> it = TempList.iterator();
								while(it.hasNext()) {
				        			avgTemp += it.next().floatValue();
				        		}
								avgTemp = avgTemp / TempList.size();
								it = EDAList.iterator();
								while(it.hasNext()) {
				        			avgEDA += it.next().floatValue();
				        		}
								avgEDA = avgEDA / EDAList.size();
								if (TempList!=null)
									TempList.clear();
								if (EDAList!=null)
									EDAList.clear();
								tvTest.setText(String.valueOf(avgTemp));
								tvEDAVaule.setText(String.valueOf(avgEDA));
								avgTemp = 0;
								avgEDA = 0;
								i = 0;
								datatoWrite = "";
						     }
						  }.start();
					}
//					if (TempList!=null)
//						TempList.clear();
//					if (EDAList!=null)
//						EDAList.clear();
//					avgTemp = 0;
//					avgEDA = 0;
				} else {
					Toast.makeText(getApplicationContext(),"User ID is not set.",Toast.LENGTH_LONG).show();
				}
			}
			
        });
        
       btnCancel.setOnClickListener(new View.OnClickListener() {
		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startFlag = false;
				UID = "null";
				//tvUID.setText(UID);
				//transmitData.cancel(true);
				if (SensorConnect!=null)
				SensorConnect.disconnect();
				//tvSetData.setText("not connected");
				Iterator<Float> it = TempList.iterator();
				while(it.hasNext()) {
        			avgTemp += it.next().floatValue();
        		}
				avgTemp = avgTemp / TempList.size();
				it = EDAList.iterator();
				while(it.hasNext()) {
        			avgEDA += it.next().floatValue();
        		}
				avgEDA = avgEDA / EDAList.size();
				if (TempList!=null)
					TempList.clear();
				if (EDAList!=null)
					EDAList.clear();
				tvTest.setText(String.valueOf(avgTemp));
				tvEDAVaule.setText(String.valueOf(avgEDA));
				avgTemp = 0;
				avgEDA = 0;
				i = 0;
				datatoWrite = "";
			}
	});
        
        if(btAdapter==null)
    	{
    		Toast.makeText(getApplicationContext(),"No Bluetooth Detected",Toast.LENGTH_LONG).show();
    		finish();    		
    	}    	
    	else
    	{
    		if(!btAdapter.isEnabled())
    		{
    			turnOnBt();
    		}
  		
    	}     		
    }
    
   
    public boolean turnOnBt() {
		// TODO Auto-generated method stub
		Intent Enable_Bluetooth=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(Enable_Bluetooth, 1234);
		return true;
	}
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bs_menu, menu);
        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item){
		
		if(item.getItemId() == R.id.Connect){
			if(btAdapter.isEnabled())
			{			
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
            }
			else
			{
			    
				Toast.makeText(getApplicationContext(),"Enable BT before connecting",Toast.LENGTH_LONG).show();
				
				
			}
			
			
		}
		else if (item.getItemId() == R.id.Enable){
			if(btAdapter.isEnabled())
			{
				Toast.makeText(getApplicationContext(),"Bluetooth is already enabled ",Toast.LENGTH_LONG).show();
				
			}
			else
			{
				
				turnOnBt();
				
			}
			
            return true;
		}
		else if (item.getItemId() == R.id.Disable){
			btAdapter.disable();
			Toast.makeText(getApplicationContext(),"Bluetooth is disabled",Toast.LENGTH_LONG).show();
			
            return true;
		}
		return false;
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		SensorConnect.disconnect();
		super.onDestroy();		
		}


	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case DeviceConnect.STATE_CONNECTED:
                    tvSetData.setText("Device Connected");
                    break;
                case DeviceConnect.STATE_CONNECTING:
                	tvSetData.setText("Connecting Device...");
                    break;
                case DeviceConnect.STATE_LISTEN:
                	tvSetData.setText("Listening for incoming data");
                	break;
                case DeviceConnect.STATE_NONE:
                	tvSetData.setText("not connected");
                	break;
                case DeviceConnect.STATE_ERROR:
                	tvSetData.setText("Disconnected due to an error");
                	break;
                }
                break;
            
            case MESSAGE_READ:
            	if (startFlag){
	                byte[] readBuf = (byte[]) msg.obj;
	                // construct a string from the valid bytes in the buffer
	                String readMessage = new String(readBuf, 0, msg.arg1);
//	                Log.d("addTest",readMessage);
	                readMessage = replaceBlank(readMessage);
//	                	Log.d("addTest",readMessage);
//	                	Log.d("addTest","-------------");
	                String[] splitString = readMessage.split(",");
	                if(splitString.length == 7 && Math.abs(Float.parseFloat(splitString[3]))<=2.0) {
		                try {
		                	i++;
		                	//4: battery, 5: Temperature, 6: EDA
		                	Calendar cal=Calendar.getInstance();
		        			cal.setTimeZone(TimeZone.getTimeZone("US/Central"));			
		        			String currentTime=String.valueOf(cal.getTime());				
		        			datatoWrite+=currentTime+","+splitString[3]+","+
		        							splitString[2]+","+
		        							splitString[1]+","+
		        					        splitString[5]+","+
		        					        String.valueOf(Float.parseFloat(splitString[6]))+"\n";
		                } catch (NumberFormatException e) {
		                	Log.i(TAG, "FAIL");
		                }	
//		                Log.d("addTest",datatoWrite);
//	                	Log.d("addTest","-----"+ splitString.length +"--------");
		                filename = "WristSensor"+UID+".txt";
						
	                	File f = new File(BASE_PATH, filename);
	                	try {
							writeToFile(f,datatoWrite);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                	TempList.add(Float.parseFloat(splitString[5]));
	                	EDAList.add(Float.parseFloat(splitString[6]));
	                	if (i==30){
	                		transmitData=new TransmitData();
							transmitData.execute(UID,datatoWrite);
							i=0;
							datatoWrite = "";
	                	}
	                }
            	}
                break;
            }
        }
    };

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub		
		if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
        	// When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);                
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                Toast.makeText(getApplicationContext(),"Trying to connect to"+ device.getName(), Toast.LENGTH_LONG).show();
                
                SensorConnect=new DeviceConnect(mHandler);
                SensorConnect.connect(address);
                
              }
           
            break;

        }
	}

	protected void writeToFile(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite+'\n');		
        fw.flush();
		fw.close();
	
	}

	private class TransmitData extends AsyncTask<String,Void, Boolean>
	{

		@Override
		protected Boolean doInBackground(String... strings) {
			// TODO Auto-generated method stub
			 String tuid=strings[0];
	         String dataToSend=strings[1];
	         if(checkDataConnectivity())
	 		{
//	         HttpPost request = new HttpPost("http://babbage.cs.missouri.edu/~rs79c/MizzouAdventure/w.php");
	         HttpPost request = new HttpPost("http://how-shocking-app-102417.use1-2.nitrousbox.com/submit.php");
	         List<NameValuePair> params = new ArrayList<NameValuePair>();
	         //file_name 
	         params.add(new BasicNameValuePair("uid",tuid));        
	         //data                       
	         params.add(new BasicNameValuePair("data",dataToSend));
	         try {
	         	        	
	             request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
	             HttpResponse response = new DefaultHttpClient().execute(request);
	             if(response.getStatusLine().getStatusCode() == 200){
	                 String result = EntityUtils.toString(response.getEntity());
	                 Log.d("Sensor Data Point Info",result);
//	                 File f = new File(BASE_PATH, "responses");
//	                 writeToFile(f,response+"\n");
	                // Log.d("Wrist Sensor Data Point Info","Data Point Successfully Uploaded!");
	             }
	             return true;
	         } 
	         catch (Exception e) 
	         {	             
	             e.printStackTrace();
	             return false;
	         }
	 	  }
	     	
	     else 
	     {
	     	Log.d("Sensor Data Point Info","No Network Connection:Data Point was not uploaded");
	     	Toast.makeText(context, "No Network Connection", Toast.LENGTH_LONG).show();
	     	return false;
	      } 
		    
		}
		
	}

	public static boolean checkDataConnectivity() {
    	ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static String replaceBlank(String str) {
    	
        String dest = "";

        if (str!=null) {

            Pattern p = Pattern.compile("\\s*|\t|\r|\n");

            Matcher m = p.matcher(str);

            dest = m.replaceAll("");

        }

        return dest;

    }

	
    
    
    
    
    
    
    
}
