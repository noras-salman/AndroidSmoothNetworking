package com.nasable.smoothnetworking;

import android.os.AsyncTask;

import java.io.BufferedReader;
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



public class TcpServer {

    private int SERVER_PORT;

    // sends message received notifications
    private OnActionListener mOnActionListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    private int clientIDCounter;
    Vector<ServerClientSocket> clients;

    int corePoolSize = 60;
    int maximumPoolSize = 80;
    int keepAliveTime = 10;
    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
    Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);


    public interface OnActionListener {
        public void onMessage(int clientID, String message);

        public void onClientConnect(int clientID);

        public void onClientDisconnect(int clientID);

        public void onError(String errorMessage);

        public void onClose();

        public void onStart();
    }

    public TcpServer( int serverPort, OnActionListener mOnActionListener) {
        SERVER_PORT = serverPort;
        this.mOnActionListener = mOnActionListener;
        clientIDCounter = 0;
        clients = new Vector<ServerClientSocket>();
    }




    public void stopServer() {
        mRun = false;
        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mOnActionListener = null;
        mBufferIn = null;
        mBufferOut = null;
        for (int i = 0; i < clients.size(); i++)
            clients.elementAt(i).getTcpClient().stopClient();

        clients.removeAllElements();

        if (mOnActionListener != null)
            mOnActionListener.onClose();
    }

    public void send(int clientID, String message) {
        new MessageSendingAsyncTask(clientID,message).executeOnExecutor(threadPoolExecutor);
    }

    private class MessageSendingAsyncTask extends AsyncTask<Void, Void, Void> {
        int clientID;
        String message;

        public MessageSendingAsyncTask(int clientID, String message) {
            this.clientID = clientID;
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.elementAt(i).getID() == clientID) {
                    clients.elementAt(i).getTcpClient().sendMessage(message);
                    return null;
                }
            }
            if (mOnActionListener != null)
                publishProgress();
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            mOnActionListener.onError("Could not find client ID=" + clientID);
        }
    }




    private int getNewClientId() {
        clientIDCounter++;
        return clientIDCounter;
    }

    private void addNewClient(ServerClientSocket clientSocketObject) {
        clients.add(clientSocketObject);
    }

    public void removeClient(int clientID) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.elementAt(i).getID() == clientID) {
                clients.elementAt(i).getTcpClient().stopClient();
                clients.remove(clients.elementAt(i));
                return;
            }
        }
        if (mOnActionListener != null)
            mOnActionListener.onError("Could not find client ID=" + clientID);
    }

    public int getNumOfClients() {
        return clients.size();
    }

    public void run() {
        new TcpServerAsyncTask().executeOnExecutor(threadPoolExecutor);
    }





    private class Action{
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


    private class TcpServerAsyncTask extends AsyncTask<Void,Action,Void>{
        private final String ON_ERROR = "ON_ERROR";
        private final String ON_START = "ON_START";
        private final String ON_NEW_CLIENT = "ON_NEW_CLIENT";

        @Override
        protected Void doInBackground(Void... voids) {
            mRun = true;

            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                if (mOnActionListener != null)
                    publishProgress(new Action(ON_START,null,0,null));

                while (mRun) {
                    Socket clientSocket = null;
                    try {
                        clientSocket = serverSocket.accept();
                    } catch (IOException e) {
                        if (mOnActionListener != null)
                            publishProgress(new Action(ON_ERROR,"Error accepting client",0,null));
                        break;
                    }
                    int newClientId = getNewClientId();
                    publishProgress(new Action(ON_NEW_CLIENT,null,newClientId,clientSocket));
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (mOnActionListener != null){
                    publishProgress(new Action(ON_ERROR,"Could not create server " + e.getMessage(),0,null));
                    //TODO: add on close
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            Action publishedAction=values[0];
            if(publishedAction.getType().equals(ON_START))
                mOnActionListener.onStart();
            else if(publishedAction.getType().equals(ON_ERROR))
                mOnActionListener.onError(publishedAction.getMsg());
            else if(publishedAction.getType().equals(ON_NEW_CLIENT))
                addNewClient(new ServerClientSocket(publishedAction.getClientID(), publishedAction.getClientSocket()));
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
            this.tcpClient=new TcpClient(socket, new TcpClient.OnActionListener() {
                @Override
                public void onMessage(String message) {
                    if (mOnActionListener != null)
                    mOnActionListener.onMessage(ID, message);
                }

                @Override
                public void onError(String errorMessage) {
                    if (mOnActionListener != null)
                        mOnActionListener.onError("Client ID=("+ID+"),"+errorMessage);
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

}