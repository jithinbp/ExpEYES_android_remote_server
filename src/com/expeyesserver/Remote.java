package com.expeyesserver;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Enumeration;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import explib.devhandler;


public class Remote extends Activity{
	TextView ip,port,msgs,r_c;
	boolean busy=false;
	ServerSocket serverSocket;
	String message=new String();
	Thread socketServerThread;
	expeyesCommon ej;
	CommunicationThread socketServerReplyThread;
	float[] temp=new float[2000];
	boolean slave=false;
	Socket socket;
    DecimalFormat df1 = new DecimalFormat("#.####");
    ScrollView sv;
    LinearLayout prlayout;
    
    private UsbManager mUsbManager;
    private devhandler mcp2200;
    
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
	IntentFilter filter;
	public Builder about_dialog;
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    //MenuItem refresh = menu.getItem(R.id.menu_refresh);
	    //refresh.setEnabled(true);
	    return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = this;
	    switch(item.getItemId())	    

	    {
	    case R.id.menu_reconnect:
	    	askForPermission();
	    	break;
	    case R.id.credits:
	    	//display_about_dialog();
	    	about_dialog.show();
	    	break;
	    case R.id.help:
	    	//display_about_dialog();
	    	show_help();
	    	break;

	    }
	    return true;
	}
			
	
    
    
    
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.remote_xml);
	  ip = (TextView) findViewById(R.id.IP);
	  port = (TextView) findViewById(R.id.PORT);
	  msgs = (TextView) findViewById(R.id.msgs);
	  r_c = (TextView) findViewById(R.id.r_c);
	  sv = (ScrollView) findViewById(R.id.sv);
	  prlayout = (LinearLayout) findViewById(R.id.prlayout);
	  
	  msgs.setText(Html.fromHtml(getString(R.string.remote_help)));
	  ip.setText("Waiting for device permissions...");
	  
	  
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);
      
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mcp2200 = new devhandler(mUsbManager);
				  
		if(mcp2200.device_found )mUsbManager.requestPermission(mcp2200.mDevice, mPermissionIntent);
		else Toast.makeText(getBaseContext(),"No device connected. check connections.",Toast.LENGTH_SHORT).show();

        
        about_dialog = new AlertDialog.Builder(this);
        about_dialog.setMessage("e-mail:jithinbp@gmail.com.\n https://github.com/jithinbp");
        about_dialog.setTitle("Developed by Jithin B.P");
        about_dialog.setCancelable(true);
	  
	  ej=expeyesCommon.getInstance();
	  socketServerThread = new Thread(new SocketServerThread());
	  
	 }

		public void show_help(){
			
			final Dialog dialog = new Dialog(this);

	        dialog.setContentView(R.layout.help);
	        dialog.setTitle("Help:");
	        dialog.getWindow().setLayout(prlayout.getWidth(),prlayout.getHeight());
	     	dialog.show();
		}


	 @Override
	 protected void onDestroy() {
	  super.onDestroy();
	  unregisterReceiver(mUsbReceiver);

	  if (serverSocket != null) {
	   try {
		serverSocket.close();
	    
	   } catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	   }
	  }
	 }
	 
	 @Override
	 protected void onPause(){
		 super.onPause();
		 
	 }

	  void scrollUp(){
  		sv.post(new Runnable() {

	    		   @Override
	    		   public void run() {
	    		     sv.fullScroll(View.FOCUS_DOWN);
	    		   }
	    		});
	  }
	 private class SocketServerThread extends Thread {

		  static final int SocketServerPORT = 8080;
		  @Override
		  public void run() {
		   try {
			   slave=false;
		    serverSocket = new ServerSocket(SocketServerPORT);
		    Log.e("CREATE SERVER","DONE");
		    
		    Remote.this.runOnUiThread(new Runnable() {
		    @Override
		     public void run() {
		      String p=getIpAddress();
		  	  if(p.length()>1)ip.setText("IP address: "+p);
		  	  else ip.setText("Could not determine IP address. Please check your WiFi");
		  	  port.setText("PORT: " + serverSocket.getLocalPort());
		     }
		    });
		     
		    while(!Thread.currentThread().isInterrupted())
		    	{
		    		 socket = serverSocket.accept();
				     if(slave){//reject the client requesting access.
				    	 socket.sendUrgentData('N');
				    	 
					     Remote.this.runOnUiThread(new Runnable() {
								  @Override
								  public void run() {
								   msgs.append(">Rejected connection request from "+socket.getInetAddress()+"\n");
								   scrollUp();
								  }
								 });
				    	 socket.close();
				    	 continue;
				     	}
				     slave=true; 
				     message = " Connected to: " + socket.getInetAddress()  + ":" + socket.getPort() + "\n";
		
				     Remote.this.runOnUiThread(new Runnable() {
							  @Override
							  public void run() {
							   msgs.append(message);
							   scrollUp();
							  }
							 });
		
				    socketServerReplyThread = new CommunicationThread( socket);
				    socketServerReplyThread.start();
				    Log.e("CREATED SERVER","Running  communications");
				    
		   		}
		    
		   } catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		    Log.e("CREATE SERVER","EXITING due to exception. socket closed");
		   }
		  }

		 }

		 private class CommunicationThread extends Thread {

		 private Socket socket;
		 private BufferedReader input;
		 private BufferedWriter output;

		  String cmd=new String(),read=new String();
		  CommunicationThread(Socket socket) {
		   this.socket = socket;
		  }


		  @Override
		  public void run() {
		   cmd="";
			try {
				this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Log.e("COMMUNICATION","NOW LISTENING...");
			while (!Thread.currentThread().isInterrupted()) {
				try {
					   read = input.readLine();
					   if(read==null){break;}
					   
					   Remote.this.runOnUiThread(new Runnable() {
						    @Override
						    public void run() {
						     r_c.setText("Remote Command > "+read);
						     
						    }
						   });
					   
					   String[] pieces = read.split("(\\()|(\\))");
					      if(pieces[0].equals("read")){
					    	  if(pieces.length>1){
									   String[] args = pieces[1].split(",");
									   for(int i=0;i<args.length;i++){
										   
										   boolean send_array=false;
										   if(args[i].equals("value"))output.append(df1.format(ej.ej.ejdata.ddata));
										   else if(args[i].equals("timestamp"))output.append(df1.format(ej.ej.ejdata.timestamp));
										   else if(args[i].equals("t1")){temp=ej.ej.ejdata.t1;send_array=true;}
										   else if(args[i].equals("t2")){temp=ej.ej.ejdata.t2;send_array=true;}
										   else if(args[i].equals("t3")){temp=ej.ej.ejdata.t3;send_array=true;}
										   else if(args[i].equals("t4")){temp=ej.ej.ejdata.t4;send_array=true;}
										   else if(args[i].equals("ch1")){temp=ej.ej.ejdata.ch1;send_array=true;}
										   else if(args[i].equals("ch2")){temp=ej.ej.ejdata.ch2;send_array=true;}
										   else if(args[i].equals("ch3")){temp=ej.ej.ejdata.ch3;send_array=true;}
										   else if(args[i].equals("ch4")){temp=ej.ej.ejdata.ch4;send_array=true;}
										   
										   if(send_array){
											   int j=0;
											   for(j=0;j<ej.ej.ejdata.length-1;j++){output.append(df1.format(temp[j])+",");}
											   output.append(""+temp[j]);
											   
											   
										   }
											   
										   output.append("\n");
										   output.flush();
									   }
					    	  }
					       }
					      else if(pieces[0].equals("*IDN?")){
					    	  output.append("Expeyes Remote Server : "+ej.ej.version+"\n");
					    	  output.flush();
					      }
						   else if(pieces.length>1){
							   ej.ej.executeString(read);
						   }
						   else{
							   output.append("Error\n");
						       output.flush();
						   }
						   
					   

					
					
				} catch (IOException e) {
					
					e.printStackTrace();
				}
			}
   
			try {
				socket.close();
				slave=false;
				Log.e("CLOSING SERVER","DONE");
				Remote.this.runOnUiThread(new Runnable() {
				    @Override
				    public void run() {
				     msgs.append(" bye bye...\nClient left\n\n");
				     scrollUp();
				     //onBackPressed();
				    }
				   });
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}

		 }

		 }
		 
		 
		 
		 private String getIpAddress() {
		  String ip = "";
		  try {
		   Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
		     .getNetworkInterfaces();
		   while (enumNetworkInterfaces.hasMoreElements()) {
		    NetworkInterface networkInterface = enumNetworkInterfaces
		      .nextElement();
		    Enumeration<InetAddress> enumInetAddress = networkInterface
		      .getInetAddresses();
		    while (enumInetAddress.hasMoreElements()) {
		     InetAddress inetAddress = enumInetAddress.nextElement();

		     if (inetAddress.isSiteLocalAddress()) {
		      ip += inetAddress.getHostAddress();
		     }
		     
		    }

		   }

		  } catch (SocketException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
		   ip += "Something Went Wrong! " + e.toString();
		  }

		  return ip;
		 }


		 
		 
		 
			
			public void askForPermission(){
		        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
				IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
				registerReceiver(mUsbReceiver, filter);
			
		        mcp2200 = new devhandler(mUsbManager);
		        
		        if(mcp2200.device_found )mUsbManager.requestPermission(mcp2200.mDevice, mPermissionIntent);
		        else{
			    	
		        	Toast.makeText(getBaseContext(),"No device connected. check connections.",Toast.LENGTH_SHORT).show();
		        }
				
			}	

			
			
			
		 		
		 		/*---------------------REQUEST USB PERMISSION WITHIN THE APPLICATION--------------------------*/
		 		
		 		private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		 		    public void onReceive(Context context, Intent intent) { //called when permission request reply received
		 		        String action = intent.getAction();
		 		        if (ACTION_USB_PERMISSION.equals(action)) {
		 		            synchronized (this) {
		 		            	if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) ) { //permission granted
		 		                    if(mcp2200.mDevice != null){
		 		                      //call method to set up device communication singleton class
		 		                    	
		 		                    	
		 		                    	if(ej.open_device(mcp2200)){
		 		                    			setTitle(ej.title+ej.ej.version);
		 		                    			Toast.makeText(getBaseContext(),"Device found!!  Server started!!",Toast.LENGTH_SHORT).show();
		 		                    			if(!socketServerThread.isAlive()) socketServerThread.start();
		 		                    	}
		 		                    	else{
		 		                    		Toast.makeText(getBaseContext(),"Something went wrong!!  Reconnect device",Toast.LENGTH_SHORT).show();
		 		                    	}
		 		                   }
		 		                    else{
		 		                    	Toast.makeText(getBaseContext(),"No device connected. check connections.",Toast.LENGTH_LONG).show();
		 		                    }
		 		                } 
		 		                else {																		//permission denied
		 	            	    	Toast.makeText(getBaseContext(),"Please grant permissions to access the device",Toast.LENGTH_LONG).show();
		 		                   //Log.d("UH-OH", "permission denied for device " + mcp2200.mDevice);
		 		                }
		 		            }
		 		        }
		 		    }
		 		};
		 		
		 		/*-----------------------------------------------*/

		 
		 
		 
		 
		 
		 
		 
		 

}
