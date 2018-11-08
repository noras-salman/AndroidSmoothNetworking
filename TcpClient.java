package com.nasable.soothnetworking;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TcpClient {

    private String SERVER_IP;
    private int SERVER_PORT;
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnActionListener mOnActionListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    private Socket socket = null;
    InetAddress serverAddr = null;

    private final int MAX_MESSAGE_LENGTH=2048;

    public boolean isRunning(){
        return mRun;
    }

    public interface OnActionListener {
        public void onMessage(String message);

        public void onError(String errorMessage);

        public void onClose();

        public void onConnect();
    }

    private final String ON_MESSAGE = "ON_MESSAGE";
    private final String ON_ERROR = "ON_ERROR";
    private final String ON_CLOSE = "ON_CLOSE";
    private final String ON_CONNECT = "ON_CONNECT";

    private class Action {
        String type;
        String msg;

        public Action(String type, String msg) {
            this.type = type;
            this.msg = msg;
        }

        public String getType() {
            return type;
        }

        public String getMsg() {
            return msg;
        }
    }

    int corePoolSize = 60;
    int maximumPoolSize = 80;
    int keepAliveTime = 40;

    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
    Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);
    

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(String serverIP, int serverPort, OnActionListener mOnActionListener) {
        SERVER_IP = serverIP;
        SERVER_PORT = serverPort;
        this.mOnActionListener = mOnActionListener;
    }


    public TcpClient(Socket socket, OnActionListener mOnActionListener) {
        this.socket = socket;
        this.mOnActionListener = mOnActionListener;
    }


    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message) {
        new MessageSendingAsyncTask(message).executeOnExecutor(threadPoolExecutor);
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        if(mOnActionListener!=null)
            mOnActionListener.onClose();
        mOnActionListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;

        if(tcpClientAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING))
            tcpClientAsyncTask.cancel(true);

        Log.d("stopClient","done "+tcpClientAsyncTask.isCancelled());
    }
    private TcpClientAsyncTask tcpClientAsyncTask;
    public void run() {
        tcpClientAsyncTask= new TcpClientAsyncTask();
        tcpClientAsyncTask.executeOnExecutor(threadPoolExecutor);

    }

    private class TcpClientAsyncTask extends AsyncTask<Void, Action, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            mRun = true;

            try {

                //here you must put your computer's IP address.
                if (serverAddr == null)
                    serverAddr = InetAddress.getByName(SERVER_IP);

                //create a socket to make the connection with the server
                if (socket == null)
                    socket = new Socket(serverAddr, SERVER_PORT);

                if (mOnActionListener != null)
                    publishProgress(new Action(ON_CONNECT, null));


                try {

                    //sends the message to the server
                    mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    //receives the message which the server sends back
                    mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                    //in this while the client listens for the messages sent by the server
                    while (mRun) {
                            //mServerMessage=mBufferIn.readLine();



                            char[] buff = new char[MAX_MESSAGE_LENGTH];
                            StringBuilder response= new StringBuilder();
                            int read;
                            while((read =  mBufferIn.read(buff))!=-1){
                                response.append( buff,0,read );
                                break;
                            }

                            if(!response.toString().isEmpty()){
                                mServerMessage=response.toString();
                            }

                            if (mServerMessage != null && mOnActionListener != null) {
                                //call the method messageReceived from MyActivity class

                                publishProgress(new Action(ON_MESSAGE, mServerMessage));

                                mServerMessage=null;
                            }



                    }


                } catch (Exception e) {

                    if (mOnActionListener != null)
                        publishProgress(new Action(ON_ERROR, "Exception thrown"));


                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    if (mOnActionListener != null)
                        publishProgress(new Action(ON_CLOSE, null));




                }

            } catch (Exception e) {

                if (mOnActionListener != null)
                    publishProgress(new Action(ON_ERROR, "Exception thrown"));


            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            super.onProgressUpdate(values);
            Action publishedAction = values[0];
            if(mOnActionListener!=null) {
                if (publishedAction.getType().equals(ON_CONNECT))
                    mOnActionListener.onConnect();
                else if (publishedAction.getType().equals(ON_CLOSE))
                    mOnActionListener.onClose();
                else if (publishedAction.getType().equals(ON_ERROR))
                    mOnActionListener.onError(publishedAction.getMsg());
                else if (publishedAction.getType().equals(ON_MESSAGE))
                    mOnActionListener.onMessage(publishedAction.getMsg());
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopClient();
        }
    }

    private class MessageSendingAsyncTask extends AsyncTask<Void, Void, Void> {
        String message;

        public MessageSendingAsyncTask(String message) {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (mBufferOut != null && !mBufferOut.checkError()) {
                mBufferOut.println(message);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mBufferOut != null && !mBufferOut.checkError()) {
                mBufferOut.flush();
            }
        }
    }

}
