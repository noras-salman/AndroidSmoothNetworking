
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.Context.WIFI_SERVICE;


public class TcpServer {

    private int SERVER_PORT;
    // sends message received notifications
    private OnActionListener mOnActionListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;


    private int clientIDCounter;
    private Vector<ServerClientSocket> clients;

    private ServerSocket serverSocket;

    private int corePoolSize = 60;
    private int maximumPoolSize = 80;
    private int keepAliveTime = 10;
    private BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
    private Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);


    public interface OnActionListener {

        public void onMessage(int clientID, NetworkData networkData);
        public void onMessage(int clientID, byte[] rawData);

        public void onMessageSent(int clientID, NetworkData networkData);

        public void onClientConnect(int clientID);

        public void onClientDisconnect(int clientID);

        public void onError(String errorMessage);

        public void onClose();

        public void onStart();
    }

    /*Requires android.permission.ACCESS_WIFI_STATE*/
    public String getIP(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("getIP", ip);
        return ip;
    }

    public TcpServer(int serverPort, OnActionListener mOnActionListener) {
        SERVER_PORT = serverPort;
        this.mOnActionListener = mOnActionListener;
        clientIDCounter = 0;
        clients = new Vector<ServerClientSocket>();
    }

    public void setOnActionListener(OnActionListener mOnActionListener) {
        this.mOnActionListener = mOnActionListener;
    }


    public void stopServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRun = false;
        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mOnActionListener = null;

        mBufferOut = null;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.elementAt(i).getTcpClient().isRunning())
                clients.elementAt(i).getTcpClient().stopClient();
        }

        clients.removeAllElements();

        if (mOnActionListener != null)
            mOnActionListener.onClose();

        if (tcpServerAsyncTask.getStatus().equals(AsyncTask.Status.RUNNING))
            tcpServerAsyncTask.cancel(true);
    }


    private int getNewClientId() {
        clientIDCounter++;
        return clientIDCounter;
    }

    private void addNewClient(ServerClientSocket clientSocketObject) {
        clients.add(clientSocketObject);
    }

    public void removeClient(int clientID) {
        try {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.elementAt(i).getID() == clientID) {

                    if (clients.elementAt(i).getTcpClient().isRunning())
                        clients.elementAt(i).getTcpClient().stopClient();

                    clients.removeElementAt(i);
                    return;
                }
            }
        } catch (Exception e) {
        }

        if (mOnActionListener != null)
            mOnActionListener.onError("Could not find client ID=" + clientID);

    }

    public int getNumOfClients() {
        return clients.size();
    }

    private TcpServerAsyncTask tcpServerAsyncTask;

    public void run() {
        tcpServerAsyncTask = new TcpServerAsyncTask();
        tcpServerAsyncTask.executeOnExecutor(threadPoolExecutor);
    }

    private class Action {
        String type;
        String msg;
        int clientID;
        Socket clientSocket;

        public Action(String type, String msg, int clientID, Socket clientSocket) {
            this.type = type;
            this.msg = msg;
            this.clientID = clientID;
            this.clientSocket = clientSocket;
        }

        public String getType() {
            return type;
        }

        public String getMsg() {
            return msg;
        }

        public int getClientID() {
            return clientID;
        }

        public Socket getClientSocket() {
            return clientSocket;
        }
    }


    private class TcpServerAsyncTask extends AsyncTask<Void, Action, Void> {
        private final String ON_ERROR = "ON_ERROR";
        private final String ON_START = "ON_START";
        private final String ON_NEW_CLIENT = "ON_NEW_CLIENT";

        @Override
        protected Void doInBackground(Void... voids) {
            mRun = true;

            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                if (mOnActionListener != null)
                    publishProgress(new Action(ON_START, null, 0, null));

                while (mRun) {
                    Socket clientSocket = null;
                    try {
                        clientSocket = serverSocket.accept();
                    } catch (IOException e) {
                        if (mOnActionListener != null)
                            publishProgress(new Action(ON_ERROR, "Error accepting client", 0, null));
                        break;
                    }
                    int newClientId = getNewClientId();
                    publishProgress(new Action(ON_NEW_CLIENT, null, newClientId, clientSocket));
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (mOnActionListener != null) {
                    publishProgress(new Action(ON_ERROR, "Could not create server " + e.getMessage(), 0, null));

                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            Action publishedAction = values[0];

            if (publishedAction.getType().equals(ON_START))
                mOnActionListener.onStart();
            else if (publishedAction.getType().equals(ON_ERROR))
                mOnActionListener.onError(publishedAction.getMsg());
            else if (publishedAction.getType().equals(ON_NEW_CLIENT))
                addNewClient(new ServerClientSocket(publishedAction.getClientID(), publishedAction.getClientSocket()));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopServer();
        }


    }


    /* A wrapper for the TcpClient Class
    *  It represents each clientSocket connected to this server*/
    private class ServerClientSocket {
        private int ID;
        private TcpClient tcpClient;

        /* Constructor */
        public ServerClientSocket(final int ID, Socket socket) {
            this.ID = ID;


            this.tcpClient = new TcpClient(socket, new TcpClient.OnActionListener() {
                @Override
                public void onMessage(NetworkData networkData) {
                    if (mOnActionListener != null)
                        mOnActionListener.onMessage(ID, networkData);
                }

                @Override
                public void onMessage(byte[] rawData) {
                    if (mOnActionListener != null)
                        mOnActionListener.onMessage(ID, rawData);
                }

                @Override
                public void onError(String errorMessage) {
                    if (mOnActionListener != null)
                        mOnActionListener.onError("Client ID=(" + ID + ")," + errorMessage);
                }

                @Override
                public void onClose() {
                    if (mOnActionListener != null) {
                        mOnActionListener.onClientDisconnect(ID);
                        removeClient(ID);
                    }
                }

                @Override
                public void onConnect() {
                    if (mOnActionListener != null)
                        mOnActionListener.onClientConnect(ID);
                }
            });
            this.tcpClient.run();
        }

        public int getID() {
            return this.ID;
        }

        public TcpClient getTcpClient() {
            return tcpClient;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ServerClientSocket && ((ServerClientSocket) obj).getID() == ID;
        }
    }

    public void send(int clientID, byte[] byteData) {
        new MessageSendingAsyncTask(clientID, byteData).executeOnExecutor(threadPoolExecutor);
    }

    public void send(int clientID, NetworkData networkData) {
        new MessageSendingAsyncTask(clientID, networkData).executeOnExecutor(threadPoolExecutor);
    }
    /**
     * Sends the message entered by client to the server
     *
     * @param message byteArray
     */
    public void sendMessage(int clientID,String message) {
        NetworkData networkData = NetworkData.newStringDataInstance(message);
        new MessageSendingAsyncTask(clientID, networkData).executeOnExecutor(threadPoolExecutor);;
    }



    private class MessageSendingAsyncTask extends AsyncTask<Void, Void, Void> {
        int clientID;

        boolean withErrors = false;
        NetworkData networkData;
        boolean completeRaw=false;
        byte[] byteData;

        public MessageSendingAsyncTask(int clientID, NetworkData networkData) {
            this.clientID = clientID;
            this.networkData = networkData;
        }
        public MessageSendingAsyncTask(int clientID,byte[] byteData) {
            this.clientID = clientID;
            this.byteData = byteData;
            completeRaw=true;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.elementAt(i).getID() == clientID) {
                    if(!completeRaw)
                        clients.elementAt(i).getTcpClient().send(networkData);
                    else
                        clients.elementAt(i).getTcpClient().send(byteData);


                    return null;
                }
            }
            if (mOnActionListener != null)
                publishProgress();

            withErrors = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            mOnActionListener.onError("Could not find client ID=" + clientID);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!withErrors && mOnActionListener != null)
                mOnActionListener.onMessageSent(clientID, networkData);
        }
    }

}