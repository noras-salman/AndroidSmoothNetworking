# AndroidSmoothNetworking
Android Smooth Networking

## Usage
TcpServer
```
 TcpServer tcpServer=new TcpServer(8081, new TcpServer.OnActionListener() {
            @Override
            public void onMessage(int clientID, String message) {

            }

            @Override
            public void onClientConnect(int clientID) {
           // tcpServer.send(clientID,"Hello");
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

TcpClient
```
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

UDP Multicating 
```
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
