package com.nasable.smoothnetworking;

/**
 * Created by noras on 9/3/2017.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

public class UDPGroupCommunication {


    int port = 4444;
    String ip = "224.0.0.1";
    InetAddress address = null;
    MulticastSocket clientSocket = null;
    OnActionListener onActionListener;

    int corePoolSize = 60;
    int maximumPoolSize = 80;
    int keepAliveTime = 10;
    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
    Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);


    public boolean WORKING = true;

    public interface OnActionListener {
        public void onJoinGroup();

        /** @param errorMessage A description of the error happened */
        public void onError(String errorMessage);

        /** @param message The received message */
        public void onReceived(String message);

        public void onClose();
    }


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


    final String ACTION_ERROR = "ACTION_ERROR";
    final String ACTION_CLOSE = "ACTION_CLOSE";
    final String ACTION_RECEIVE = "ACTION_RECEIVE";
    final String ACTION_JOIN_GROUP = "ACTION_JOIN_GROUP";

    private void actionProcessHandler(Action publishedAction) {
        if (onActionListener != null) {
            if (publishedAction.getType().equals(ACTION_ERROR))
                onActionListener.onError(publishedAction.getMsg());
            else if (publishedAction.getType().equals(ACTION_CLOSE))
                onActionListener.onClose();
            else if (publishedAction.getType().equals(ACTION_RECEIVE))
                onActionListener.onReceived(publishedAction.getMsg());
            else if (publishedAction.getType().equals(ACTION_JOIN_GROUP))
                onActionListener.onJoinGroup();
        }
    }


    /** Constructor
     * @param onActionListener*/
    public UDPGroupCommunication(OnActionListener onActionListener) {
        this.onActionListener = onActionListener;
    }

    /** Constructor
     * @param ip
     * @param port
     * @param onActionListener */
    public UDPGroupCommunication(String ip, int port, OnActionListener onActionListener) {
        this.port = port;
        this.ip = ip;
        this.onActionListener = onActionListener;
    }

    /* Join the group and start receiving*/
    public void start() {
        new ReceiverMulticastAsyncTask().executeOnExecutor(threadPoolExecutor);
    }


    public void stop() {
        WORKING = false;
        PERIODIC_SEND = false;
    }


    private boolean PERIODIC_SEND = false;



    PeriodicSenderAsyncTask periodicSenderAsyncTask;
    public void startPeriodicSending(String message, int millSeconds) {
        PERIODIC_SEND = true;
        periodicSenderAsyncTask= new PeriodicSenderAsyncTask(message, millSeconds);
        periodicSenderAsyncTask.executeOnExecutor(threadPoolExecutor);

    }


    public void stopPeriodicSend() {
        PERIODIC_SEND = false;
        periodicSenderAsyncTask.cancel(true);
    }


    private class PeriodicSenderAsyncTask extends AsyncTask<Void, Action, Void> {
        private int millSeconds;
        private String message;

        public PeriodicSenderAsyncTask(String message, int millSeconds) {
            this.millSeconds = millSeconds;
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);

            while (PERIODIC_SEND && !isCancelled()) {

                try {
                    clientSocket.send(packet);
                } catch (IOException e) {
                    publishProgress(new Action(ACTION_ERROR,"Error while sending data"));
                    if (clientSocket != null) {
                        clientSocket.close();
                        publishProgress(new Action(ACTION_CLOSE,null));
                    }
                }

                SystemClock.sleep(millSeconds);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            actionProcessHandler(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            PERIODIC_SEND=false;

        }
    }


    public void send(String message_to_send) {
        new SenderAsyncTask(message_to_send).executeOnExecutor(threadPoolExecutor);
    }


    private class SenderAsyncTask extends AsyncTask<Void, Action, Void> {
        String message_to_send;

        public SenderAsyncTask(String message_to_send) {
            this.message_to_send = message_to_send;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            byte[] buf = message_to_send.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            try {
                clientSocket.send(packet);
            } catch (IOException e) {
                publishProgress(new Action(ACTION_ERROR,"Error while sending data"));
                if (clientSocket != null) {
                    clientSocket.close();
                    publishProgress(new Action(ACTION_CLOSE,null));
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            actionProcessHandler(values[0]);
        }
    }





     /* Main joining and receiving task*/
    private class ReceiverMulticastAsyncTask extends AsyncTask<Void, Action, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            try {
                clientSocket = new MulticastSocket(port);
                address = InetAddress.getByName(ip);
                clientSocket.joinGroup(address);
                publishProgress(new Action(ACTION_JOIN_GROUP, null));
                ;


            } catch (Exception e) {
                e.printStackTrace();
                // Added this
                if (clientSocket != null)
                    clientSocket.close();
                publishProgress(new Action(ACTION_ERROR, "Error while joining multicast group"));
                publishProgress(new Action(ACTION_CLOSE, null));

            }


            if (clientSocket != null) {
                while (WORKING && !clientSocket.isClosed()) {
                    DatagramPacket packet = null;
                    byte[] buf = new byte[1024];
                    packet = new DatagramPacket(buf, buf.length);
                    try {
                        clientSocket.receive(packet);
                        byte[] data = packet.getData();
                        publishProgress(new Action(ACTION_RECEIVE, new String(data, 0, packet.getLength())));

                    } catch (Exception e) {
                        // Added this
                        if (clientSocket != null)
                            clientSocket.close();

                        publishProgress(new Action(ACTION_ERROR, "Error while receiving data"));
                        publishProgress(new Action(ACTION_CLOSE, null));

                    }
                }
            }


            try {
                if (clientSocket != null) {
                    if (!clientSocket.isClosed()) {
                        clientSocket.leaveGroup(address);
                        clientSocket.close();
                        publishProgress(new Action(ACTION_CLOSE, null));
                    }
                }

            } catch (IOException e) {
                publishProgress(new Action(ACTION_ERROR, "IOException while leaving group"));
                if (clientSocket != null) {
                    clientSocket.close();
                    publishProgress(new Action(ACTION_CLOSE, null));

                }
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(Action... values) {
            actionProcessHandler(values[0]);

        }


        @Override
        protected void onPostExecute(Void aVoid) {
            stop();
        }
    }
}
