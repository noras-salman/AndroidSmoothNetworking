
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by noras on 2018-11-06.
 */

public class SimpleWebServer extends TcpServer {

    final int TIME_OUT_MS = 10000;

    public SimpleWebServer(int serverPort) {
        super(serverPort, null);
        routes = new ArrayList<Route>();


        setOnActionListener(new OnActionListener() {

            @Override
            public void onMessage(int clientID, NetworkData networkData) {
                Log.d("onMessage", clientID  +"");

            }

            @Override
            public void onMessage(int clientID, byte[] rawData) {
                handleRequest(clientID, new String(rawData));
            }

            @Override
            public void onMessageSent(final int clientID,  NetworkData networkData) {
                Log.d("onMessage", clientID  +"");
                setCloseHandler(clientID);
            }

            @Override
            public void onClientConnect(int clientID) {

                Log.d("onClientConnect", clientID + "");
                setTimeOutHandler(clientID);

            }

            @Override
            public void onClientDisconnect(int clientID) {
                Log.d("onClientDisconnect", clientID + "");
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("onError", errorMessage + "");
            }

            @Override
            public void onClose() {
                Log.d("onClose", "onClose");
            }

            @Override
            public void onStart() {
                Log.d("onStart", "onStart");
            }
        });
    }



    private void handleRequest(int clientID,String requestString) {

        Request request = parseRequest(requestString);

        //parse and set action
        if (request != null) {

            int index;
            Route comparable = new Route(request.getRoute()) {
                @Override
                public byte[] renderContent(Request request) {
                    return null;
                }
            };

            if ((index = routes.indexOf(comparable)) == -1) {
                send(clientID, throwNotFound("Not Found").getBytes());
            } else {
               // if(routes.get(index).getContentType().contains("text"))
                    send(clientID, getHtmlResponse(routes.get(index).renderContent(request),routes.get(index).getContentType()));
              //  else
                //    send(clientID, getHtmlResponse(routes.get(index).renderContent(request),routes.get(index).getContentType()).getBytes());
            }


        } else {
            send(clientID, throwError("Internal Server Error").getBytes());

        }

    }

    private List<Route> routes;

    public void addRoute(Route route) {
        routes.add(route);
    }

    public abstract static class Route {
        private String path;
        private String contentType="text/html";


        public Route(String path) {
            this.path = path;
        }


        public String getPath() {
            return path;
        }

        public String getContentType() {
            return contentType;
        }

        public abstract byte[] renderContent(Request request);

        public void setContentType(String contentType){
            this.contentType=contentType;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Route && ((Route) obj).path.equals(this.path);
        }
    }



    private void setCloseHandler(final int clientID) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                removeClient(clientID);
            }
        }, 10000);
    }


    private void setTimeOutHandler(final int clientID) {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                Log.d("onClientDisconnect", clientID + " TIMEOUT  " + getNumOfClients());
                removeClient(clientID);
            }
        }, TIME_OUT_MS);
    }

    private String getMimeBasedOnExtension(String uri){


        try {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            int index = uri.lastIndexOf('.')+1;
            String ext = uri.substring(index).toLowerCase();
            return mime.getMimeTypeFromExtension(ext);
        }catch (Exception e){
            return "text/html";
        }

    }

    private byte[] getHtmlResponse(byte[] htmlResponse,String contentType) {
        String head="HTTP/1.1 200 OK\r\nConnection: close\r\nContent-type: "+contentType+"\r\n" + "Content-length: " + htmlResponse.length + "\r\n\r\n";
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        builder.write(head.getBytes(),0,head.getBytes().length);
        builder.write(htmlResponse,0,htmlResponse.length);
        return builder.toByteArray();
    }

    private String throwNotFound(String htmlResponse) {
        return "HTTP/1.1 404 Not Found\r\nConnection: close\r\nContent-type: text/html\r\n" + "Content-length: " + htmlResponse.length() + "\r\n\r\n" + htmlResponse;

    }

    private String throwError(String htmlResponse) {
        return "HTTP/1.1  500 Internal Server Error\r\nConnection: close\r\nContent-type: text/html\r\n" + "Content-length: " + htmlResponse.length() + "\r\n\r\n" + htmlResponse;

    }



    private final String headBreak = ": ";
    private final String parameterBreak = "&";
    private final String parameterValueBreak = "=";
    private final String TYPE_GET = "GET";
    private final String TYPE_POST = "POST";

    private Request parseRequest(String message) {
        String lines[] = message.split("\\r?\\n");
        try {
            String head = lines[0];
            String headSplit[] = head.split(" ");
            String type = headSplit[0];
            String route = headSplit[1];
            String protocol = headSplit[2];
            List<RequestHeader> requestHeaders = new ArrayList<RequestHeader>();
            List<Parameter> parameters = new ArrayList<Parameter>();
            if (route.contains("?")) {
                String parametersArray[] = route.substring(route.indexOf('?') + 1).split(parameterBreak);
                route = route.substring(0, route.indexOf('?'));
                for (int j = 0; j < parametersArray.length; j++) {
                    String singleParameter[] = parametersArray[j].split(parameterValueBreak);
                    parameters.add(new Parameter(singleParameter[0], singleParameter[1], TYPE_GET));
                }
            }


            boolean beginData = false;
            for (int i = 1; i < lines.length; i++) {

                if (lines[i].equals("")) {
                    beginData = true;
                    continue;
                }

                if (beginData) {
                    //parse data
                    String parametersArray[] = lines[i].split(parameterBreak);
                    for (int j = 0; j < parametersArray.length; j++) {
                        String singleParameter[] = parametersArray[j].split(parameterValueBreak);
                        parameters.add(new Parameter(singleParameter[0], singleParameter[1], TYPE_POST));
                    }

                } else {
                    //add headers
                    String singleHeader[] = lines[i].split(headBreak);

                    requestHeaders.add(new RequestHeader(singleHeader[0], singleHeader[1]));
                }

            }


            return new Request(type, protocol, route, requestHeaders, parameters);
        } catch (Exception e) {
            //internal error
            return null;
        }
    }



    public class Request {
        private String type;
        private String protocol;
        private String route;
        private List<RequestHeader> headers;
        private List<Parameter> parameters;

        public Request(String type, String protocol, String route, List<RequestHeader> headers, List<Parameter> parameters) {
            this.type = type;
            this.protocol = protocol;
            this.route = route;
            this.headers = headers;
            this.parameters = parameters;
        }

        public String getType() {
            return type;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getRoute() {
            return route;
        }

        public List<RequestHeader> getHeaders() {
            return headers;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "type='" + type + '\'' +
                    ", protocol='" + protocol + '\'' +
                    ", route='" + route + '\'' +
                    ", headers=" + headers +
                    ", parameters=" + parameters +
                    '}';
        }

        public String retrieveGETParameter(String name){
            Parameter comparable=new Parameter(name,null,TYPE_GET);
            int index=this.parameters.indexOf(comparable);
            if(index!=-1)
                return this.parameters.get(index).value;
            return null;
        }

        public String retrievePOSTParameter(String name){
            Parameter comparable=new Parameter(name,null,TYPE_POST);
            int index=this.parameters.indexOf(comparable);
            if(index!=-1)
                return this.parameters.get(index).value;
            return null;
        }

        public String retrieveHeader(String name){
            RequestHeader comparable=new RequestHeader(name,null );
            int index=this.headers.indexOf(comparable);
            if(index!=-1)
                return this.headers.get(index).value;
            return null;
        }
    }

    public class Parameter {
        private String name;
        private String value;
        private String type;

        public Parameter(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return "Parameter{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Parameter && ((Parameter)obj).name.equals(this.name) && ((Parameter)obj).type.equals(this.type);
        }
    }

    public class RequestHeader {
        private String name;
        private String value;

        public RequestHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "RequestHeader{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }

        @Override
        public boolean equals( Object obj) {
            return obj instanceof RequestHeader && ((RequestHeader)obj).name.equals(this.name);
        }
    }


    public boolean prepareAssets(final Context context,String path) {
        String [] list;
        try {
            list = context.getAssets().list(path);
            if (list.length > 0) {
                // This is a folder
                for (String file : list) {
                    if (!prepareAssets(context,path + "/" + file))
                        return false;
                    else {
                        // This is a file
                        // TODO: add file name to an array list
                        final String name=path + "/" + file;
                        addRoute(new Route(name) {
                            @Override
                            public byte[] renderContent(Request request) {
                                try {
                                    setContentType(getMimeBasedOnExtension(name.substring(1)));
                                    return readFromAssets(context,name.substring(1));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                return "File Not Found".getBytes();
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }


    public static byte[] byteArrayFromInputStream(InputStream inputStream) throws IOException {

        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    private static byte[] readFromAssets(Context context, String filename) throws IOException {

        return byteArrayFromInputStream(context.getAssets().open(filename));
    }

}
