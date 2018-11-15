# AndroidSmoothNetworking
Android Smooth Networking

#  Usage
Requires <uses-permission android:name="android.permission.INTERNET"/>

and if you want to use getIP <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>

## TcpServer
```java
 TcpServer tcpServer =new TcpServer(8080, new TcpServer.OnActionListener() {
                       @Override
                       public void onMessage(int clientID, NetworkData networkData) {
                           if(networkData.isFileData()){
                               networkData.getMetaData().getFileName();
                               try {
                                   ////<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
                                   FileNetworkData.writeToDownload(getApplicationContext(),networkData);
                               } catch (Exception e) {
                                   e.printStackTrace();
                               }
                           }else if(networkData.isStringData()){
                               networkData.isStringData();
                           }else if(networkData.isRawData()){
                               networkData.getRawData();
                           }
                       }
           
                       @Override
                       public void onMessage(int clientID, byte[] rawData) {
           
                       }
           
                       @Override
                       public void onMessageSent(int clientID, NetworkData networkData) {
           
                       }
           
                       @Override
                       public void onClientConnect(int clientID) {
           
                       }
           
                       @Override
                       public void onClientDisconnect(int clientID) {
           
                       }
           
                       @Override
                       public void onError(String errorMessage) {
           
                       }
           
                       @Override
                       public void onClose() {
           
                       }
           
                       @Override
                       public void onStart() {
                           tcpServer.getIP(getApplicationContext());
                       }
                   });
        tcpServer.run();
```

## TcpClient
```java
       TcpClient tcpClient=new TcpClient(tcpServer.getIP(getApplicationContext()), 8080, new TcpClient.OnActionListener() {
                             @Override
                             public void onMessage(NetworkData networkData) {
                 
                             }
                 
                             @Override
                             public void onMessage(byte[] rawData) {
                 
                             }
                 
                             @Override
                             public void onError(String errorMessage) {
                                 Log.d("onClientError",""+errorMessage);
                             }
                 
                             @Override
                             public void onClose() {
                                 Log.d("onClose","onCloseclient");
                             }
                 
                             @Override
                             public void onConnect() {
                                 Log.d("clientStart","clientStart");
                 
                                 tcpClient.sendMessage("hi");
                                 try {
                                     tcpClient.send(FileNetworkData.sendAssetFile(getApplicationContext(),"c.png"));
                                     //<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
                                     tcpClient.send(FileNetworkData.sendExternalStorageFile("/some/path"));
                                 } catch (IOException e) {
                                     e.printStackTrace();
                                 }
                 
                 
                             }
                         });
        tcpClient.run();
```

## UDP Multicating 
```java
  UDPGroupCommunication udpGroupCommunication=new UDPGroupCommunication(new UDPGroupCommunication.OnActionListener() {
            @Override
            public void onJoinGroup() {

            }

            @Override
            public void onError(String errorMessage) {

            }

            @Override
            public void onReceived(String message) {

            }

            @Override
            public void onClose() {

            }
        });

        udpGroupCommunication.start();
```

## Web Server
```java

 SimpleWebServer simpleWebServer=new SimpleWebServer(8080);
        simpleWebServer.getIP(getApplicationContext());
        simpleWebServer.addRoute(new SimpleWebServer.Route("/auth") {
            @Override
            public byte[] renderContent(SimpleWebServer.Request request) {
                String username=request.retrievePOSTParameter("username");
                String password=request.retrievePOSTParameter("password");
                String ref=request.retrieveGETParameter("ref");
                String cookie=request.retrieveHeader("Cookie");
                if(username!=null && password!=null){
                    //do something
                    return "<html>hi</html>".getBytes();
                }
                return request.toString().getBytes();
            }
        });

        //add everything in the assets/ to the webserver
        simpleWebServer.prepareAssets(getApplicationContext(),"");
        simpleWebServer.run();

```
Todo: fix image files serving