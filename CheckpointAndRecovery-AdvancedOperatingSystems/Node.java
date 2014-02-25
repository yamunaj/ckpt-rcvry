
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

/**
 * @author Rashmi Lengade
 */
public class Node 
{
	static int failNode;
	static int nodes;
	static int sendAppMsgCount = 0;
	static int receiveAppMsgCount = 0;
	static int numberOfCheckpoints = 0;
	static boolean willingToCheckpoint = true;
	static boolean willingToRollback = true;
	static boolean readyToRollback = true;
	static int crashed = -1;
	static int recoveryParent = -1;
	static int numberOfRequestsSend =0;
	static int numberOfCrashSend =0;
	static int crashedYesReceiveCount = 0;
	static int crashedNoReceiveCount = 0;
	static int totalCrashResponses = 0;
	static int yesReceiveCount=0;
	static int noReceiveCount=0;
	static int totalReceiveCount =0;
	static long controlMsgPriorityValue= Long.MAX_VALUE;
	static long applMsgPriorityValue= Long.MAX_VALUE/2 + 1;
	static int checkpointInitiator = -1;
	static int checkpointParent = -1;
	static int  NodeId;
	static String hostName;
	static int portNumber;
	static SctpServerChannel serverSocket;
	static LamportClock clock = new LamportClock();
	static HashMap<Integer,String> receiveConfiguration = new HashMap<Integer, String>();
	static HashMap<Integer,String> sendConfiguration = new HashMap<Integer, String>();
	static ConcurrentHashMap<Integer,SctpChannel> connectionDetails = new ConcurrentHashMap<Integer, SctpChannel>();
	static queueComparator q = new queueComparator();
	static Queue<Message> msgQueue = new PriorityQueue<Message>(1000,q);
	static int timer;
	static boolean freezeQueue = false;
	static boolean freezeApplicaion = false;
	static boolean checkPointingFinish =true;
	static ConcurrentHashMap<Integer,Integer> FLS = new ConcurrentHashMap<Integer,Integer>();
	static ConcurrentHashMap<Integer,Integer> LLR  = new ConcurrentHashMap<Integer,Integer>();
	static ConcurrentHashMap<Integer,Integer> LLS  = new ConcurrentHashMap<Integer,Integer>();
	static ConcurrentHashMap<Integer,Integer> VolatileCheckpointLLR = new ConcurrentHashMap<Integer,Integer>(); // size should be number of nodes...
	static ConcurrentHashMap<Integer,Integer> VolatileCheckpointFLS = new ConcurrentHashMap<Integer,Integer>();
	static ConcurrentHashMap<Integer,Integer> VolatileCheckpointLLS = new ConcurrentHashMap<Integer,Integer>();
	static ConcurrentHashMap<Integer,Integer> lastPermanentCheckpointLLR = new ConcurrentHashMap<Integer, Integer>();
	static ConcurrentHashMap<Integer,Integer> lastPermanentCheckpointFLS = new ConcurrentHashMap<Integer, Integer>();
	static ConcurrentHashMap<Integer,Integer> lastPermanentCheckpointLLS = new ConcurrentHashMap<Integer, Integer>();
	static Integer checkpointCounter = new Integer(0);
	
	
	static boolean commitedCheckpoint = true;
	static boolean abortedCheckpoint = true; 
	static boolean recoveryAborted = true;
	static boolean recovered = true;
	static boolean recoveryFinish = true;
	
	public static synchronized boolean isRecovered() {
		return recovered;
	}
	public static synchronized void setRecovered(boolean recovered) {
		Node.recovered = recovered;
	}
	public static synchronized boolean isRecoveryAborted() {
		return recoveryAborted;
	}
	public static synchronized void setRecoveryAborted(boolean recoveryAborted) {
		Node.recoveryAborted = recoveryAborted;
	}
	public int getNodeId()	{
		return NodeId;
	}
	public void setNodeId(int nodeId) {
		NodeId = nodeId;
	}
	public static synchronized ConcurrentHashMap<Integer, Integer> getLastPermanentCheckpointLLR() {
		return lastPermanentCheckpointLLR;
	}
	public static synchronized void setLastPermanentCheckpointLLR() {
		Node.lastPermanentCheckpointLLR.putAll(Node.VolatileCheckpointLLR);
	}
	public static synchronized ConcurrentHashMap<Integer, Integer> getLastPermanentCheckpointFLS() {
		return lastPermanentCheckpointFLS;
	}
	public static synchronized void setLastPermanentCheckpointFLS() {
		Node.lastPermanentCheckpointFLS.putAll(Node.VolatileCheckpointFLS);
	}
	public static synchronized ConcurrentHashMap<Integer, Integer> getLastPermanentCheckpointLLS() {
		return lastPermanentCheckpointLLS;
	}
	public static synchronized void setLastPermanentCheckpointLLS() {
		Node.lastPermanentCheckpointLLS.putAll(Node.VolatileCheckpointLLS);
		System.out.println("ADD lastPermanentCheckpointLLS SIze : "+lastPermanentCheckpointLLS.size());
	}
	public static synchronized void setVolatileVectors(){
		VolatileCheckpointFLS.putAll(FLS);
		VolatileCheckpointLLR.putAll(LLR);
		VolatileCheckpointLLS.putAll(LLS);
		System.out.println("Volatile LLS SIze : "+VolatileCheckpointLLS.size());
	}
	public static synchronized void getLastPermanentCheckpoint(){
		LLR.putAll(lastPermanentCheckpointLLR);
		FLS.putAll(lastPermanentCheckpointFLS);
		LLS.putAll(lastPermanentCheckpointLLS);
	}
	public static synchronized void InitilizeVector() {
		for(Entry e : Node.connectionDetails.entrySet())
		{
			FLS.put((Integer) e.getKey(), -1);
			LLR.put((Integer) e.getKey(), -1);
			LLS.put((Integer) e.getKey(), -1);
		}
	}
	public static synchronized void resetVolatileVector() {
		for(Entry e : Node.connectionDetails.entrySet())
		{
			VolatileCheckpointFLS.put((Integer) e.getKey(), -1);
			VolatileCheckpointLLR.put((Integer) e.getKey(), -1);
			VolatileCheckpointLLS.put((Integer) e.getKey(), -1);
		}
	}
	public static class queueComparator implements Comparator<Message>  {
		@Override
		public int compare(Message o1, Message o2) {
			if(o1.priority < o2.priority) 
				return 1;
			return -1;
		}
	}
	static synchronized void nQueue(Message message){
		msgQueue.add(message);	
	}
	static synchronized Message peekQueue()	{
		return msgQueue.peek();
	}
	static synchronized void removeQueue() {
		msgQueue.remove();
	}
	public static synchronized boolean isWillingToCheckpoint() {
		return willingToCheckpoint;
	}
	public static synchronized void setWillingToCheckpoint(boolean willingToCheckpoint) {
		Node.willingToCheckpoint = willingToCheckpoint;
	}
	public static synchronized boolean isWillingToRollback() {
		return willingToRollback;
	}
	public static synchronized void setWillingToRollback(boolean willingToRollback) {
		Node.willingToRollback = willingToRollback;
	}
	public static synchronized boolean isReadyToRollback() {
		return readyToRollback;
	}
	public static synchronized void setReadyToRollback(boolean readyToRollback) {
		Node.readyToRollback = readyToRollback;
	}
	public static synchronized int getCrashed() {
		return crashed;
	}
	public static synchronized void setCrashed(int crashed) {
		Node.crashed = crashed;
	}
	public static int getRecoveryParent() {
		return recoveryParent;
	}
	public static void setRecoveryParent(int recoveryParent) {
		Node.recoveryParent = recoveryParent;
	}
	public static synchronized int getYesReceiveCount() {
		return yesReceiveCount;
	}
	public static synchronized void setYesReceiveCount() {
		Node.yesReceiveCount++;
	}
	public static synchronized int getNoReceiveCount() {
		return noReceiveCount;
	}
	public static synchronized void setNoReceiveCount() {
		Node.noReceiveCount++;
	}
	public static synchronized int getNumberOfRequestsSend() {
		return numberOfRequestsSend;
	}
	public static synchronized void setNumberOfRequestsSend() {
		Node.numberOfRequestsSend++;
	}
	public static synchronized void resetReceiveCounters()
	{
		yesReceiveCount =0;
		noReceiveCount = 0;
		numberOfRequestsSend = 0;
	}
	
	public static synchronized void resetCrashCounters()
	{
		crashedYesReceiveCount =0;
		crashedNoReceiveCount = 0;
		numberOfCrashSend = 0;
	}
	public static synchronized long getPriorityValue(String msgType) {
		if(msgType.equals(Message.messageType.Application.toString()))
		{
			return Node.applMsgPriorityValue;
		}
		else
		{
			return Node.controlMsgPriorityValue;
		}
	}
	public static synchronized void setPriorityValue(String msgType) {
		if(msgType.equals(Message.messageType.Application.toString()))
		{
			Node.applMsgPriorityValue--;
		}
		else
		{
			Node.controlMsgPriorityValue--;
		}
	}
	public static synchronized int getCheckpointInitiator() {
		return checkpointInitiator;
	}
	public static synchronized void setCheckpointInitiator(int checkpointInitiator) {
		Node.checkpointInitiator = checkpointInitiator;
	}
	public static synchronized int getCheckpointParent() {
		return checkpointParent;
	}
	public static synchronized void setCheckpointParent(int checkpointParent) {
		Node.checkpointParent = checkpointParent;
	}
	public static synchronized String getHostName() {
		return hostName;
	}
	public static synchronized void setHostName(String hostName) {
		Node.hostName = hostName;
	}
	public static synchronized int getPortNumber() {
		return portNumber;
	}
	public static synchronized void setPortNumber(int portNumber) {
		Node.portNumber = portNumber;
	}
	public static synchronized SctpServerChannel getServerSocket() {
		return serverSocket;
	}
	public static synchronized void setServerSocket(SctpServerChannel serverSocket) {
		Node.serverSocket = serverSocket;
	}
	public static synchronized int getClock() {
		return clock.Time;
	}
	public static synchronized int incrementClock() {
		return Node.clock.increment();
	}
	public static synchronized int compareAndSetClock(Message messageInfo) {
		return Node.clock.compareAndSetTime(messageInfo.timeStamp);
	}
	public static synchronized int getTimer() {
		return timer;
	}
	public static synchronized void setTimer(int timer) {
		Node.timer = timer;
	}
	public static synchronized boolean isFreezeQueue() {
		return freezeQueue;
	}
	public static synchronized void setFreezeQueue(boolean freezeQueue) {
		Node.freezeQueue = freezeQueue;
	}
	public static synchronized boolean isCheckPointingFinish() {
		return checkPointingFinish;
	}
	public static synchronized void setCheckPointingFinish(boolean checkPointingFinish) {
		Node.checkPointingFinish = checkPointingFinish;
	}
	public static synchronized Integer getCheckpointCounter() {
		return checkpointCounter;
	}
	public static synchronized void setCheckpointCounter(Integer checkpointCounter) {
		Node.checkpointCounter = checkpointCounter;
	}
	public static synchronized boolean isCommitedCheckpoint() {
		return commitedCheckpoint;
	}
	public static synchronized void setCommitedCheckpoint(boolean commited) {
		Node.commitedCheckpoint = commited;
	}
	public static synchronized boolean isAbortedCheckpoint() {
		return abortedCheckpoint;
	}
	public static synchronized void setAbortedCheckpoint(boolean aborted) {
		Node.abortedCheckpoint = aborted;
	}
	
	public static int getCrashedYesReceiveCount() {
		return crashedYesReceiveCount;
	}
	public static void setCrashedYesReceiveCount() {
		Node.crashedYesReceiveCount++;
	}
	public static int getCrashedNoReceiveCount() {
		return crashedNoReceiveCount;
	}
	public static void setCrashedNoReceiveCount() {
		Node.crashedNoReceiveCount++;
	}
	public static synchronized boolean isFreezeApplicaion() {
		return freezeApplicaion;
	}
	public static synchronized void setFreezeApplicaion(boolean freezeApplicaion) {
		Node.freezeApplicaion = freezeApplicaion;
	}
	/*public static synchronized void writeToLog()
	{
		System.out.println("Inside writeto log");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			File file = new File("./checkpoint.xml");
			Document doc;
			if(file.exists())
			{
				doc = docBuilder.parse(file);
			}
			else
			{
				doc = docBuilder.newDocument();
			//}
			Element rootElement = doc.createElement("Checkpoint");
			doc.appendChild(rootElement);
			Attr attr = doc.createAttribute("InstanceNo");
			attr.setValue(Node.checkpointCounter.toString());
			rootElement.setAttributeNode(attr);

			Element FLS = doc.createElement("FLS");
			doc.appendChild(doc.createTextNode("FLS"));
			//doc.appendChild(doc.createTextNode(Node.VolatileCheckpointFLS.toString()));
			rootElement.appendChild(FLS);

			Element LLR = doc.createElement("LLR");
			doc.appendChild(doc.createTextNode(Node.VolatileCheckpointLLR.toString()));
			rootElement.appendChild(LLR);

			Element LLS = doc.createElement("LLS");
			doc.appendChild(doc.createTextNode(Node.VolatileCheckpointLLS.toString()));
			rootElement.appendChild(LLS);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			//StreamResult result = new StreamResult("./checkpoint.xml");

			// Output to console for testing
			StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("File saved!");

		} catch (ParserConfigurationException | TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	public static synchronized boolean isRecoveryFinish() {
		return recoveryFinish;
	}
	public static synchronized void setRecoveryFinish(boolean recoveryFinish) {
		Node.recoveryFinish = recoveryFinish;
	}
	public static synchronized int getSendAppMsgCount() {
		return sendAppMsgCount;
	}
	public static synchronized void setSendAppMsgCount() {
		Node.sendAppMsgCount++;
	}
	public static synchronized void resetSendAppMsgCount(int sendAppMsgCount) {
		Node.sendAppMsgCount = sendAppMsgCount;
	}
	public static synchronized int getReceiveAppMsgCount() {
		return receiveAppMsgCount;
	}
	public static synchronized void resetReceiveAppMsgCount(int receiveAppMsgCount) {
		Node.receiveAppMsgCount = receiveAppMsgCount;
	}
	public static synchronized void setReceiveAppMsgCount() {
		Node.receiveAppMsgCount++;
	}
	
	public static synchronized void writeLog()
	{
		FileWriter fileWriter = null;
		BufferedWriter bufferedWriter = null;
		try {
			fileWriter = new FileWriter("/home/004/y/yx/yxj122030/AOS/Project2/logs/Checkpoint"+NodeId+".txt",false);
			bufferedWriter = new BufferedWriter(fileWriter);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		String llr = "LLR:";
		for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLR.entrySet())
		{
			System.out.println("LLR written "+e.getValue());
			llr = llr.concat(e.getValue()+",");
		}
		try {
			bufferedWriter.write(llr);
			bufferedWriter.write("\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String fls = "FLS:";
		for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointFLS.entrySet())
		{
			System.out.println("FLS written "+e.getValue());
			fls = fls.concat(e.getValue()+",");
		}
		try {
			bufferedWriter.write(fls);
			bufferedWriter.write("\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String lls = "LLS:";
		for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLS.entrySet())
		{
			System.out.println("LLS written "+e.getValue());
			lls = lls.concat(e.getValue()+",");
		}
		try {
			bufferedWriter.write(lls);
			bufferedWriter.write("\n");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			bufferedWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public static synchronized void compareCheckpointFiles()
	{
		File dir = new File("/home/004/y/yx/yxj122030/AOS/Project2/logs");
		File[] filesList = dir.listFiles();
		for(File file : filesList)
		{
			String d = new String();
			if(file.isFile())
			{
				FileReader r;
				try {
					r = new FileReader(file);
				
				BufferedReader bufReader = new BufferedReader(r);
				String s;
				String tem = new String();
				int count = 0;
				while((s = bufReader.readLine()) != null)
				{
					if(count ==0)
					{
						String l1[] = s.split(":");
						String llr[] = l1[1].split(",");
					}
					if(count ==1)
					{
						String l2[] = s.split(":");
						String fls[] = l2[1].split(",");
					}
					if(count ==2)
					{
						String l3[] = s.split(":");
						String lls[] = l3[1].split(",");
					}
				}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	public static synchronized boolean compareFiles()
	{
	
		Map<Integer,int[]> LLR = new HashMap<Integer,int[]>();
		Map<Integer,int[]> LLS = new HashMap<Integer,int[]>();
		
		BufferedReader[] br = new BufferedReader[Node.nodes];
		for (int i = 0; i < Node.nodes; i++) {
			try {
				br[i] = new BufferedReader(new InputStreamReader(new FileInputStream("/home/004/y/yx/yxj122030/AOS/Project2/logs/Checkpoint"+NodeId+".txt")));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
		
		for (int i = 0; i < Node.nodes; i++) {
			int[] llr = new int[Node.nodes];
			int[] lls = new int[Node.nodes];
			String line = null;
			try {
				while ((line = br[i].readLine()) != null) {
					if(line.length() > 0 ){
						String[] L1 = line.split(":");
						String[] L2 = L1[1].split(",");
						if("LLR Value".trim().equals(L1[0].trim()))
						{
							for(int j=0;j<Node.nodes;j++)
							{

								llr[j]=Integer.parseInt(L2[j].trim());
							}
						}
						if("LLS Value".equals(L1[0].trim()))
						{
							for(int j=0;i<Node.nodes;j++)
							{
								lls[j]=Integer.parseInt(L2[j].trim());
							}
						}
					}
				}
			}catch(Exception e){
				
			}
			LLR.put(i,llr);
			LLS.put(i,lls);
		}

		for (int i = 0; i <  Node.nodes; i++) {
			
			for (int j = 0; j < Node.nodes; j++) {
				int[] temp1 =LLR.get(i);
				if(i != j){
							int[] temp2 = LLS.get(j);
							if(temp1[j] > temp2[i])
								return false;
							}
				
			}
		}
		
		return true;
	}
}