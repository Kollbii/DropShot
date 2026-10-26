package pl.maciek.squashscoreboard;

import android.os.Handler;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Small localhost-only bridge for the Zepp App Side Service.  It deliberately
 * does not use, replace, or inspect Android media-button/Bluetooth events.
 */
final class WatchCommandServer {
    static final int PORT=28282;
    private static final String PATH="/zepp-command";
    private static final String TAG="WatchCommandServer";

    interface CommandListener { boolean onCommand(String command); }

    private final Handler mainHandler;
    private final CommandListener listener;
    private volatile boolean stopping;
    private ServerSocket serverSocket;
    private Thread worker;

    WatchCommandServer(Handler mainHandler,CommandListener listener){this.mainHandler=mainHandler;this.listener=listener;}

    synchronized void start(){
        if(worker!=null&&worker.isAlive())return;
        try{
            ServerSocket socket=new ServerSocket();
            socket.setReuseAddress(true);
            // Zepp App Side Service runs on the same phone and calls IPv4 localhost.
            // Binding explicitly to 127.0.0.1 avoids an IPv6 ::1-only socket.
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),PORT));
            serverSocket=socket;
            stopping=false;
            worker=new Thread(()->serve(socket),"zepp-watch-command-server");
            worker.setDaemon(true);
            worker.start();
        }catch(IOException e){
            // A disconnected watch or a temporarily unavailable port must never affect a match.
            Log.w(TAG,"Cannot start local Zepp bridge",e);
            closeServerSocket();
        }
    }

    synchronized void stop(){stopping=true;closeServerSocket();worker=null;}

    private void serve(ServerSocket listeningSocket){
        while(!stopping&&serverSocket==listeningSocket){
            try(Socket socket=listeningSocket.accept()){
                socket.setSoTimeout(2500);
                handle(socket);
            }catch(SocketException e){if(!stopping)Log.w(TAG,"Zepp bridge socket error",e);
            }catch(IOException e){if(!stopping)Log.w(TAG,"Zepp bridge request error",e);}
        }
    }

    private void handle(Socket socket) throws IOException{
        BufferedInputStream input=new BufferedInputStream(socket.getInputStream());
        String requestLine=readLine(input);
        if(requestLine==null){return;}
        String[] request=requestLine.split(" ");
        int contentLength=0;
        String line;
        while((line=readLine(input))!=null&&!line.isEmpty()){
            int colon=line.indexOf(':');
            if(colon>0&&"content-length".equals(line.substring(0,colon).trim().toLowerCase(Locale.US))){
                try{contentLength=Integer.parseInt(line.substring(colon+1).trim());}catch(NumberFormatException ignored){contentLength=-1;}
            }
        }
        if(request.length<2||!"POST".equals(request[0])||!PATH.equals(request[1])){writeJson(socket,404,"{\"accepted\":false}");return;}
        if(contentLength<2||contentLength>256){writeJson(socket,400,"{\"accepted\":false}");return;}
        byte[] body=readExactly(input,contentLength);
        String command;
        try{command=new JSONObject(new String(body,StandardCharsets.UTF_8)).optString("command","");}catch(Exception e){writeJson(socket,400,"{\"accepted\":false}");return;}
        if(!"POINT_1".equals(command)&&!"POINT_2".equals(command)&&!"UNDO".equals(command)&&!"RESET".equals(command)){writeJson(socket,400,"{\"accepted\":false}");return;}
        Log.i(TAG,"Received command from Zepp: "+command);

        final boolean[] accepted={false};
        CountDownLatch completed=new CountDownLatch(1);
        mainHandler.post(()->{try{accepted[0]=listener.onCommand(command);}finally{completed.countDown();}});
        try{
            if(!completed.await(2,TimeUnit.SECONDS)){writeJson(socket,503,"{\"accepted\":false}");return;}
        }catch(InterruptedException e){Thread.currentThread().interrupt();writeJson(socket,503,"{\"accepted\":false}");return;}
        Log.i(TAG,"Processed Zepp command "+command+", accepted="+accepted[0]);
        writeJson(socket,200,"{\"accepted\":"+accepted[0]+"}");
    }

    private static String readLine(InputStream input) throws IOException{
        ByteArrayOutputStream line=new ByteArrayOutputStream();
        int value;
        while((value=input.read())!=-1){
            if(value=='\n')break;
            if(value!='\r')line.write(value);
            if(line.size()>1024)throw new IOException("HTTP header too large");
        }
        return value==-1&&line.size()==0?null:line.toString(StandardCharsets.US_ASCII.name());
    }

    private static byte[] readExactly(InputStream input,int length) throws IOException{
        byte[] data=new byte[length];int offset=0;
        while(offset<length){int count=input.read(data,offset,length-offset);if(count<0)throw new EOFException();offset+=count;}
        return data;
    }

    private static void writeJson(Socket socket,int status,String json) throws IOException{
        byte[] data=json.getBytes(StandardCharsets.UTF_8);
        OutputStream output=socket.getOutputStream();
        output.write(("HTTP/1.1 "+status+" OK\r\nContent-Type: application/json\r\nContent-Length: "+data.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        output.write(data);output.flush();
    }

    private synchronized void closeServerSocket(){if(serverSocket!=null){try{serverSocket.close();}catch(IOException ignored){}serverSocket=null;}}
}
