/*
 * Copyright 2015 tbking.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package router;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author tbking
 */
public class TorRouter {
    
    static final Logger routerlog= Logger.getLogger("router");
    private DataInputStream din;
    private  DataOutputStream dout;
    private BufferedReader br ;
    private Socket router;
    private String[] E,N,D;
    private byte[] data,decryptedData;
    private final String DirIP="192.168.0.102";
    private Random r;
    private BigInteger p,q,e,d,n,phi;
    private final int RouterPort=9091,DirPort=9090;
    
    TorRouter()
    {
        routerlog.info("Tor Router Initialized.");
        //routerlog.setUseParentHandlers(false);
        E=new String[3];
        N=new String[3];
        D=new String[3];
    }
    public static void main(String args[])
    {
        routerlog.info("Tor Router running");
        TorRouter OR=new TorRouter();
        OR.loginit();
        OR.genKey();
        OR.sendToDir();
        while(true)
        {
            OR.data=OR.getData();
            OR.decryptedData=OR.decrypt(1);
            OR.sendData();

        } 
        
    }
    
    private void loginit()
    {
        FileHandler logFile;
        try
        {
            logFile=new FileHandler("/home/tbking/Development/netbeans-8.0.2/Projects/TOR/src/router/TorRouter.log");
            routerlog.addHandler(logFile);
            SimpleFormatter formatter = new SimpleFormatter();
            logFile.setFormatter(formatter);
        }
        catch (IOException | SecurityException ex) {
            routerlog.severe("Exception raised in creating log file. Exiting program.");
        }
    }
    
    private void genKey()
    {
        routerlog.info("RSA keys being generated...");
        key(256,0);
        key(512,1);
        key(1024,2);
        routerlog.info("RSA keys generated.");
    }
    
    private void key(int bitlength,int index)
    {
        r=new Random();
        p=BigInteger.probablePrime(bitlength, r);
        q=BigInteger.probablePrime(bitlength, r);
        n=p.multiply(q);
        phi=p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        e=BigInteger.probablePrime(bitlength/2, r);

        while(phi.gcd(e).compareTo(BigInteger.ONE)>0 && e.compareTo(phi)<0)
        {
        e.add(BigInteger.ONE);
        }

        d=e.modInverse(phi);
        E[index]=e.toString();
        N[index]=n.toString();
        D[index]=d.toString();
    }
    
    private void sendToDir()
    {
        try 
        {
            router=new Socket(DirIP,DirPort);
            dout=new DataOutputStream(router.getOutputStream());
            dout.writeUTF("0/"+E[0]+"/"+N[0]+"/"+E[1]+"/"+N[1]+"/"+E[1]+"/"+N[2]);
            
            for(int i=0;i<3;i++)
            {
                System.out.println("E["+i+"]="+E[i]);
                System.out.println("N["+i+"]="+N[i]);
            }
            dout.flush();
            dout.close();
            router.close();
        } 
        catch (IOException ex) 
        {
            routerlog.severe("Unable to connect to Directory. Exiting program.");
            System.exit(0);
        }
    }
    
    private byte[] getData()
    {
        routerlog.info("Waiting to receive data.");
        try {
            ServerSocket RouterAsServer=new ServerSocket(RouterPort,10);
            Socket DataSender=RouterAsServer.accept();
            routerlog.info("Connection with client established.");
            din=new DataInputStream(DataSender.getInputStream());
            int len=din.readInt();
            byte[] receivedData = new byte[len];
            din.readFully(receivedData);
            din.close();
            DataSender.close();
            RouterAsServer.close();
            routerlog.info("Data Received.");
            System.out.println("Data received in Bytes"+bytesToString(receivedData));
            return receivedData;
        } 
        catch (IOException ex) 
        {
            routerlog.severe("Data receiving failed.");
        }
        return null;
    }
    private byte[] decrypt(int len)
    {
        data=this.data;
        System.out.println("Data to decrypt in bytes: "+bytesToString(data));
        return (new BigInteger(data)).modPow(d, n).toByteArray();
    }
    
    private void sendData()
    {
        try 
        {
            String s = new String(decryptedData);
            System.out.println("decrypted data in bytes :"+bytesToString(decryptedData));
            String token[]=s.split("/");
            String IP=token[0];
            byte[] dataToSend=token[1].getBytes();
            Socket RouterAsClient=new Socket(IP,RouterPort);
            dout=new DataOutputStream(RouterAsClient.getOutputStream());
            dout.write(dataToSend);
            dout.flush();
            RouterAsClient.close();
        } 
        catch (IOException ex) 
        {
            routerlog.severe("Attempt to establish connection with other router failed. Exiting progream.");
        }
    }
    private static String bytesToString(byte[] encrypted) {
        String test = "";
        for (byte b : encrypted) {
            test += Byte.toString(b);
        }
        return test;
    }
    
}
