import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;


public class NodeStart {

	public static void main(String[] args) throws IOException, InterruptedException {
		Node node = new Node();
		ConcurrentHashMap<Integer, SctpChannel> connectionDetails = new ConcurrentHashMap<Integer,SctpChannel>();

		String fileName = null;
		if (0 < args.length) {
			fileName = args[0];
		}
		else
		{
			System.out.println("Invalid File Name.. !!! Please run again");
		}
		MakeConfiguration(node,fileName);

		SctpServerChannel serverSocket;
		serverSocket = SctpServerChannel.open();
		InetSocketAddress serverAddress = new InetSocketAddress(node.portNumber);
		serverSocket.bind(serverAddress);
		Thread serverThread = new Thread(new Server(serverSocket));
		serverThread.start();
		System.out.println("Bound port : " + node.portNumber);
		/*System.out.println("Waiting for connection ...");*/

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ConnectAll();

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Thread clientThread = new Thread(new Client());
		clientThread.start();
		Node.InitilizeVector();

		Thread.sleep(10000);

		Thread application = new Thread(new Application());
		application.start();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Thread timerThread = new Thread(new Timer());
		timerThread.start();

		try {
			Thread.sleep(50000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*if(node.NodeId == 0)
		{
			Thread recoveryThread = new Thread(new Recover());
			recoveryThread.start();
		}*/
	}

	public static void ConnectAll() throws IOException {

		for(Entry e : Node.sendConfiguration.entrySet())
		{
			String value = (String) e.getValue();
			String[] values = value.split(" ");
			String hostName = values[0];
			int portNumber = Integer.parseInt(values[1]);

			SctpChannel clientSocket;
			InetSocketAddress serverAddr = new InetSocketAddress(hostName,portNumber); 
			clientSocket = SctpChannel.open();
			clientSocket.connect(serverAddr, 0, 0);
			clientSocket.configureBlocking(false);
			Node.connectionDetails.put((Integer) e.getKey(), clientSocket);
			System.out.println("Node"+Node.NodeId+" sends connection to "+e.getKey());
		}
	}


	public static void MakeConfiguration(Node node,String fileName) throws NumberFormatException, IOException {

		HashMap<Integer, String> configuration = new HashMap<Integer,String>();
		String myHostName;
		myHostName = java.net.InetAddress.getLocalHost().getHostName();

		FileReader file = new FileReader(fileName);
		BufferedReader br = new BufferedReader(file);
		String line;
		//ArrayList<String[]> neighboursList = new ArrayList<String[]>();
		String[] neighbours = null;

		int failed =-1;
		while((line = br.readLine()) != null)
		{
			String[] token = line.split(" ");
			int key = Integer.parseInt(token[0]);
			String hostName = token[1];
			String portNumber = token[2];



			if(myHostName.equals(hostName))
			{
				node.hostName = hostName;
				node.portNumber = Integer.parseInt(portNumber);
				node.setNodeId(key);
				neighbours = token[4].split(",");
				Node.setTimer(Integer.parseInt(token[3]));
			}
			//neighboursList.add(neighbours);
			configuration.put(key, hostName + " " + portNumber);
			
				failed = Integer.parseInt(token[5]);
			
		}
		System.out.println("Faild node :" + Node.failNode);
		Node.nodes = configuration.size();
		System.out.println("My Node ID " + node.getNodeId());
		if(Node.NodeId == failed)
		{
			Node.failNode = failed;
		}
		br.close();
		file.close();

		for(String n : neighbours)
		{
			if(Integer.parseInt(n) < node.getNodeId())
			{
				node.sendConfiguration.put(Integer.parseInt(n), configuration.get(Integer.parseInt(n)));

			}
			else
			{
				node.receiveConfiguration.put(Integer.parseInt(n), configuration.get(Integer.parseInt(n)));
			}
		}

		//return configuration;
	}
}
