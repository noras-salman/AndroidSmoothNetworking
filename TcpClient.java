
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private DataOutputStream mBufferOutData;
    private DataInputStream mBufferInData;

    private Socket socket = null;
    InetAddress serverAddr = null;

    private final int MAX_MESSAGE_LENGTH = 1024*1024*1;

    public boolean isRunning() {
        return mRun;
    }

    public interface OnActionListener {
        public void onMessage(NetworkData networkData);
        public void onMessage(byte[] rawData);

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
        byte[] data;

        public Action(String type, String msg, byte[] data) {
            this.type = type;
            this.msg = msg;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public String getMsg() {
            return msg;
        }

        public byte[] getData() {
            return data;
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
     * @param data text entered by client
     */
    public void send(NetworkData data ) {
        new MessageSendingAsyncTask(data).executeOnExecutor(threadPoolExecutor);
    }


    public void send(byte[] byteData) {
        new MessageSendingAsyncTask(byteData).executeOnExecutor(threadPoolExecutor);
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message byteArray
     */
    public void sendMessage(String message) {
        NetworkData networkData = NetworkData.newStringDataInstance(message);
        new MessageSendingAsyncTask(networkData).executeOnExecutor(threadPoolExecutor);
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

        if (mBufferOutData != null) {
            try {
                mBufferOutData.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                mBufferOutData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mOnActionListener != null)
            mOnActionListener.onClose();
        mOnActionListener = null;
        mBufferInData = null;
        mBufferOutData = null;
        mServerMessage = null;

        if (tcpClientAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING))
            tcpClientAsyncTask.cancel(true);

        Log.d("stopClient", "done " + tcpClientAsyncTask.isCancelled());
    }

    private TcpClientAsyncTask tcpClientAsyncTask;

    public void run() {
        tcpClientAsyncTask = new TcpClientAsyncTask();
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
                    publishProgress(new Action(ON_CONNECT, null, null));


                try {


                    //sends the message to the server
                    mBufferOutData = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    //receives the message which the server sends back
                    mBufferInData = new DataInputStream(new BufferedInputStream(socket.getInputStream()));


                    //in this while the client listens for the messages sent by the server
                    while (mRun) {
                        //mServerMessage=mBufferIn.readLine();


                        byte[] buff = new byte[MAX_MESSAGE_LENGTH];
                        ByteArrayOutputStream builder = new ByteArrayOutputStream();
                        int read;
                        while ((read = mBufferInData.read(buff)) != -1) {
                            builder.write(buff, 0, read);
                            Log.d("debug",builder.toString());
                             break; // was here for the web_server
                        }


                        if (builder.size() != 0 && mOnActionListener != null) {
                            //call the method messageReceived from MyActivity class

                            publishProgress(new Action(ON_MESSAGE, null, builder.toByteArray()));


                        }


                    }


                } catch (Exception e) {

                    if (mOnActionListener != null)
                        publishProgress(new Action(ON_ERROR, "Exception thrown", null));


                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.


                }

            } catch (Exception e) {

                if (mOnActionListener != null)
                    publishProgress(new Action(ON_ERROR, "Exception thrown:" + e.getMessage(), null));


            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            super.onProgressUpdate(values);
            Action publishedAction = values[0];
            if (mOnActionListener != null) {
                if (publishedAction.getType().equals(ON_CONNECT))
                    mOnActionListener.onConnect();
                else if (publishedAction.getType().equals(ON_CLOSE))
                    mOnActionListener.onClose();
                else if (publishedAction.getType().equals(ON_ERROR))
                    mOnActionListener.onError(publishedAction.getMsg());
                else if (publishedAction.getType().equals(ON_MESSAGE)) {
                    mOnActionListener.onMessage(new NetworkData(publishedAction.getData()));
                    mOnActionListener.onMessage(publishedAction.getData());
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopClient();
        }
    }

    private class MessageSendingAsyncTask extends AsyncTask<Void, Void, Void> {


        InputStream inputStream;
        byte[] buffer = new byte[MAX_MESSAGE_LENGTH];
        int bufferCount;
        boolean completeRaw=false;


        public MessageSendingAsyncTask(NetworkData networkData) {
            inputStream = new ByteArrayInputStream(networkData.asByteArray());
        }

        public MessageSendingAsyncTask(byte[] byteData) {

            inputStream = new ByteArrayInputStream(byteData);
            completeRaw=true;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (mBufferOutData != null) {


                try {
                    while ((bufferCount = inputStream.read(buffer)) > 0) {
                        mBufferOutData.write(buffer, 0, bufferCount);
                    }

                    mBufferOutData.flush();
                } catch (IOException e) {
                    e.printStackTrace();

                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

}
