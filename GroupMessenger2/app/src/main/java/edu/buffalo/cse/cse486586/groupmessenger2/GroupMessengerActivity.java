package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    /**Declaration of Global Variables */
    int count = 0;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    int gcount = 0;
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    int propCount = 0;
    int selfPort = 0;
    boolean rem = false;
    String deadPort = null;
    int remPort = 0;
    boolean badPortFound = false;
    List<Integer> portList = new ArrayList<Integer>();
    List<Integer> portList2 = new ArrayList<Integer>();


    Map<String,Integer> MsgMap = new HashMap<String,Integer>();
   static  Map<String,Boolean> DelivMap = new HashMap<String,Boolean>();
    //http://stackoverflow.com/questions/683041/how-do-i-use-a-priorityqueue
    //Creating a comparator for the priority queue which will store messages
    Comparator<String> comparator = new MessageComparator();
    PriorityQueue<String> PQ = new PriorityQueue<String>(100, comparator);
    //
    static class MessageComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {
            //The comparator will sort the Priority Queue based on the sequence numbers first.
            //If the sequence numbers are the same, then we will give priority to the message
            // having lower port number of process which sent the max seq number
            String[] ar1 = x.split(":");
            String[] ar2 = y.split(":");
            int port1 = Integer.parseInt(ar1[1]);
            int port2 = Integer.parseInt(ar2[1]);
            boolean del1 = DelivMap.get(ar1[0]);
            boolean del2 = DelivMap.get(ar2[0]);

            if(Integer.parseInt(ar1[2]) < Integer.parseInt(ar2[2]))
                return -1;
            if(Integer.parseInt(ar1[2]) > Integer.parseInt(ar2[2]))
                return 1;
            if(Integer.parseInt(ar1[2]) == Integer.parseInt(ar2[2]))
            {
                if(port1>port2) {
//                    System.out.println("Port Comparison: "+port1+">"+port2 +" MSGS " + x + " AND " + y);
                    return -1;   //Original
//                     return   -1;
                }else {
//                    System.out.println("Port Comparison: "+port1+"<"+port2 +" MSGS " + x + " AND " + y);

                    return 1;  //Original
//                return 1;
                }
//              

            }
            return 0;


            //Check HashMap of deliverable; if x is deliverable and y is not, return 1, else if y is deliverable and x is not,
            //return -1


        }
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        //Adding all the port numbers to a List
        portList.add(11108);
        portList.add(11112);
        portList.add(11116);
        portList.add(11120);
        portList.add(11124);

        portList2.add(11108);
        portList2.add(11112);
        portList2.add(11116);
        portList2.add(11120);
        portList2.add(11124);


        //Code taken from PA1 for getting current AVD port number
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        selfPort = Integer.parseInt(myPort);    //Saving the port number of this AVD in a global variable
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(10000);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
//            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }catch (Exception e){
//            Log.e(TAG, "Server Socket connection cannot be established");
        }

        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        //Referenced from https://developer.android.com/guide/topics/ui/controls/button.html#ClickListener
        // https://developer.android.com/reference/android/widget/EditText.html
        final EditText ed = (EditText) findViewById(R.id.editText1);
        Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = String.valueOf(ed.getText()+"\n");
                String msg = String.valueOf(ed.getText());
                tv.append(s);   //Display message on sender screen and clear the textbox
                ed.setText("");
                //Code below taken from PA1
               // gcount++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Calling client task and sending the message to send to other AVDs
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,  Integer.toString(gcount));
            }
        });


        //Starting a process to ping all the AVDs and find which port is down
//http://stackoverflow.com/questions/24894501/java-timer-every-x-seconds
//        while((!badPortFound) && (remPort==0)) {

            final Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                 while ((!badPortFound) && (remPort == 0)) {

                        // System.out.println("Pinging ports");
                        Socket s;
                        Integer p1 = 0;
                        try {
                            for (Integer port : portList) {
                                boolean ack = false;
                                p1 = port;
                                s = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
                                PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                                pw.println("PING");
                                // System.out.println(s.getInputStream().read());
                                Thread.sleep(100);
                                InputStreamReader is = new InputStreamReader(s.getInputStream());
                                BufferedReader br = new BufferedReader(is);
                                String alive;
                                if ((alive = br.readLine()) != null) {
//                        System.out.println("Received from " + p1 + " " +alive);
//                        System.out.println("Ping Success for " + p1);
                                    ack = true;
                                }
                                if (ack == false) {
                                    badPortFound = true;
                                    deadPort = "deadport:" + p1 + ":";
                                    System.out.println("Port Failure Detected for port " + p1);

                                    remPort = p1;
//                    
                                    t.cancel();

                                }
                            }
                        } catch (Exception e) {
//                System.out.println("Port Failure Detected for port " + p1);
//                deadPort = "deadport:"+p1;
                        }
                    }
//            System.out.println("End of ping cycle");
                }
            }, 7000, 200);
//        }

//    }while (true);

    }


        /*******Starting Destroy TEST****/
    @Override
    public void onDestroy(){

        super.onDestroy();{
//            System.out.println("App is closing");
        }

    finish();
    }


    /*******Ending Destroy TEST****/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

//            try {
            //Open a server socket to listen to incoming messages
            while (true) {
                try {
                    Socket s = serverSocket.accept();
//                    System.out.println("PQ SIZE "+PQ.size());

                    InputStreamReader is = null;

                    is = new InputStreamReader(s.getInputStream());
                    BufferedReader br = new BufferedReader(is);
                    String msg;
                    if ((msg = br.readLine()) != null) {


                        String[] msgAr = msg.split(":");
                        // Check if INITIAL message is received
                        if (msgAr[0].equals("INITIAL")) {
                            // publishProgress(msgAr[1]);
                            propCount++;
                            MsgMap.put(msgAr[1] + ":" + msgAr[2] + ":", propCount);
//                            DelivMap.put(msgAr[1]+":"+ msgAr[2]+":",false);
                            DelivMap.put(msgAr[1], false);

                            PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                            int sendPort = Integer.parseInt(msgAr[2]);
                            String reply = "PROP" + ":" + msgAr[1] + ":" + selfPort + ":" + propCount + ":";
                         
                            PQ.add(msgAr[1] + ":" + selfPort + ":" + propCount + ":" + "dead:" + sendPort + ":");
                            pw.println(reply);
//                           
                        }
                        //If FINAL message is received, remove initial messages from PQ,
                        //Put FINAL message in PQ, and set Delivery Map value for this message as true
                        if (msgAr[0].equals("FINAL")) {



                            //System.out.println("Received Final Proposal");
                            propCount = Math.max(propCount, Integer.parseInt(msgAr[3]));
//                            Log.v(TAG, "Entering Final at " + selfPort + " with max seen " + propCount);

                            int oldcount = MsgMap.get(msgAr[1] + ":" + msgAr[2] + ":");
//                            System.out.println("Received FINAL for old: "+msgAr[1] + ":" + selfPort + ":" + oldcount + ":" + "dead:" + msgAr[2] + ":");
                            PQ.remove(msgAr[1] + ":" + selfPort + ":" + oldcount + ":" + "dead:" + msgAr[2] + ":");
                            MsgMap.put(msgAr[1] + ":" + msgAr[2] + ":", Integer.parseInt(msgAr[3]));
//                            System.out.println("Adding NEW FINAL to QUEUE: " + msgAr[1] + ":" + msgAr[4] + ":" + msgAr[3] + ":"+"dead:" + msgAr[2] + ":");
                            PQ.add(msgAr[1] + ":" + msgAr[4] + ":" + msgAr[3] + ":"+"dead:" + msgAr[2] + ":");

                            DelivMap.put(msgAr[1], true);
                            //If message contains information about a deadport, then remove the messages for that
                            //port from the Priority Queue
//                            start comment
                            if (msg.contains("deadport:")) {
                                badPortFound=true;

                                if (rem == false) {
//                                    rem = true;
//                                    System.out.println(selfPort+"Removing deadport messages for " + msgAr[6]);
                                    String fport = msgAr[6];
/
                                    deadPort = "deadport:" + fport + ":";

                                    remPort = Integer.parseInt(fport);
                                    String find = "dead:" + fport;
                                    Iterator<String> it = PQ.iterator();
                                    while (it.hasNext()) {
                                        String deadMsg = it.next();
                                        if (deadMsg.contains(find))
                                        {
                                                PQ.remove(deadMsg);
                                        }
//                                        
                                    }
//                                    rem = true;
                                }
                            }
                            //End Comment
//
                            //Check again for dead port messages, in case they have been received from another sender simultaneously
//                            sort(PQ,comparator);
                            if(remPort!=0) {
                                Iterator<String> it = PQ.iterator();
//                                System.out.println("Check for IT hasnext");
                                while (it.hasNext()) {
//                                    System.out.println("IT hasnext");
                                    String deadMsg = it.next();
//                                    System.out.println("Checking for dead msgs 2 : " + remPort + " Message " + deadMsg);
//                                    System.out.println("check deadmsg contains: " + "dead:" + Integer.toString(remPort));
                                    if (deadMsg.contains("dead:" + Integer.toString(remPort))) {
//                                    if( DelivMap.get(deadMsg.split(":")[0])!=true) {
//                                        System.out.println("Removing ITER" + deadMsg + "from " + selfPort + "PQ Size " + PQ.size());
                                        PQ.remove(deadMsg);
//                                        System.out.println("Removed ITER" + deadMsg + "from " + selfPort + "PQ Size " + PQ.size());
//                                    }
                                    }

                                }
                            }



                            //Loop at the PQ and send messages to Content Provider if they are deliverable, and poll them afterwards
                            while (PQ.peek() != null && DelivMap.get(PQ.peek().split(":")[0]) == true) {
//                                while(PQ.size()!=0) {
//                              String message =   it.next();
                                //System.out.println(DelivMap);
                                String message = PQ.peek();
//                            if (message!=null) {

                                String[] str = message.split(":");
//                                if (DelivMap.get(str[0]) == false) {
//                                    break;
//                                }
//                                System.out.println("PQ Message " + message + " SeqNo " + str[2] + " Max Seen " + propCount);
//                                if (DelivMap.get(str[0]) == true) {


//                                  System.out.println("Queue Message " + message);
//                                System.out.println("Entering Content Provider " + str[0] + str[1]);
                                String URL = "content://edu.buffalo.cse.cse486586.groupmessenger2.provider";
                                //content://edu.buffalo.cse.cse486586.groupmessenger1.provider
                                Uri uri = Uri.parse(URL);
                                ContentValues values = new ContentValues();
                                values.put(KEY_FIELD, count);
                                values.put(VALUE_FIELD, str[0]);
                                Uri newUri = getContentResolver().insert(uri, values);
//                        publishProgress(str[0]);
//                                System.out.println("Inserted and exiting" + str[0] + str[1]);
                                PQ.poll();
                                count++;
//                                    continue;
//                                }
//                                else break;

//                            }else break;
//                            }
                            }

                        }
                        //If we receive a PING message, send reply back as ALIVE
                        if(msg.equals("PING")){
                            PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
                            pw.println("ALIVE");
//                            System.out.println("Returned ALIVE"+selfPort);
                        }



                        br.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                }
//             }
            }
//

        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim()+selfPort;
            TextView tv1 = (TextView) findViewById(R.id.textView1);
            //remoteTextView.append(strReceived + "\t\n");
            //TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            tv1.append(strReceived+"\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */



            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String msgNo = msgs[1];
            //String sendPort = msgs[1];
            // Send message to all clients
            int[] remotePorts = {11108, 11112, 11116, 11120, 11124};
            Map<String, String> AgrMap = new HashMap<String, String>();
            for (Integer i : portList) {
//                System.out.println("Check if " + i + " Equals " + remPort);
                if (i!=remPort){
                Socket socket = null;
                try {

                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            i);



                    String msgToSend = "INITIAL" + ":" + msgs[0] + ":" + selfPort + ":";

//                    System.out.println("Sending INITAL for " + msgToSend + " to port " + i);
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    // BufferedWriter bw = new BufferedWriter(socket.getOutputStream());
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    pw.println(msgToSend);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    catch(IOException e){}


//                Log.v(TAG, "Message sent to "+i+" from "+selfPort);
                    //Receive Reply from clients, and store max sequence number for message in AgrMap, along with port number of client from
                    //which we have received max seq number for a message
                    String rep;
                    InputStreamReader is = new InputStreamReader(socket.getInputStream());
                    BufferedReader br = new BufferedReader(is);
                    if ((rep = br.readLine()) != null) {
//                        System.out.println("Received" + rep + "at" + selfPort);
                        String[] received = rep.split(":");
                        if (received[0].equals("PROP")) {
                            if (AgrMap.containsKey(received[1])) {
                                if (Integer.parseInt(AgrMap.get(received[1]).split(":")[0]) < Integer.parseInt(received[3]))
                                    AgrMap.put(received[1], received[3] + ":" + received[2]);
//                        Log.v(TAG, "Put to AgrMap"+received[1]+" " + received[3]+":"+received[2]+" at "+selfPort);

                            } else {
                                AgrMap.put(received[1], received[3] + ":" + received[2]);
//                        Log.v(TAG, "Put to AgrMap"+received[1]+" " + received[3]+":"+received[2]+" at "+selfPort);


                            }


                        }


                    }
                    socket.close();

                } catch (IOException e) {

                }
            }
        }
            //Send FINAL message, with max priority to all clients, along with deadport information
            // if a port has died
            for (Integer i : portList) {
//                try {
//                System.out.println("FINAL Check if " + i + " Equals " + remPort);

                //Check for skipping creating socket to a dead port
                if (i!=remPort){

                //String remotePort = REMOTE_PORT0;
                //if (msgs[1].equals(REMOTE_PORT0))
                //  remotePort = REMOTE_PORT1;

                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            i);

                    int maxAgreed = 0, procPort = 0;
                    if (AgrMap.containsKey(msgs[0])) {
                        maxAgreed = Integer.parseInt(AgrMap.get(msgs[0]).split(":")[0]);
                        procPort = Integer.parseInt(AgrMap.get(msgs[0]).split(":")[1]);
                    }


                    String msgToSend = "FINAL" + ":" + msgs[0] + ":" + selfPort + ":" + maxAgreed + ":" + procPort + ":";
                    if (badPortFound == true) {
//                            System.out.println("Attaching deadport to message" + deadPort + " "+ msgToSend);
                        msgToSend = msgToSend + deadPort;
                    }
//                    System.out.println("Sending Final for: " + msgToSend + " to port " + i);
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    // BufferedWriter bw = new BufferedWriter(socket.getOutputStream());
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    pw.println(msgToSend);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    socket.close();

                } catch (IOException e) {
//                    System.out.println("Socket ERROR at port " + i);
//                    portList.remove(i);
//                    deadPort = "deadport:"+i+":";
//                    Log.e(TAG, "ClientTask socket IOException"+ "Cannot reach port" + i);
                }
            }
            }






            return null;
        }
    }

}

