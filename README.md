# AndroidSmoothNetworking
Android Smooth Networking

#  Usage
## TcpServer
```java
 TcpServer tcpServer=new TcpServer(8081, new TcpServer.OnActionListener() {
            @Override
            public void onMessage(int clientID, String message) {

            }
            
            @Override
            public void onMessageSent(int clientID, String message){
            
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

            }
        });
        tcpServer.run();
```

## TcpClient
```java
       TcpClient tcpClient=new TcpClient("127.0.0.1", 8081, new TcpClient.OnActionListener() {
            @Override
            public void onMessage(String message) {

            }

            @Override
            public void onError(String errorMessage) {

            }

            @Override
            public void onClose() {

            }

            @Override
            public void onConnect() {

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

SimpleWebServer simpleWebServersimpleWebServer=new SimpleWebServer(8080);

  simpleWebServer.addRoute(new SimpleWebServer.Route("/auth") {
            @Override
            public String renderContent(SimpleWebServer.Request request) {
                 String username=request.retrievePOSTParameter("username");
                 String password=request.retrievePOSTParameter("password");
                 String ref=request.retrieveGETParameter("ref");
                 String cookie=request.retrieveHeader("Cookie");
                 if(username!=null && password!=null){
                    //do something
                    return "<html></html>";
                 }                  
                return request.toString();
            }
        });
        
   //add everything in the assets/ to the webserver
   simpleWebServer.prepareAssets(getApplicationContext(),"");
   simpleWebServer.run();

```
Todo: fix image files serving