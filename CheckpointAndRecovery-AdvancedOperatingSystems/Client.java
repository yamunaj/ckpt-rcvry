import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;



public class Client implements Runnable
{
	boolean flag = true;
	int NOMSend [] = new int[10];
	int count =0;
	public int counter=0;

	@Override
	public void run() 
	{
		while(true)
		{
			if(count == 0)
			{
				System.out.println("Message Queue Size : "+Node.msgQueue.size());
				count++;
			}
			while(!(Node.msgQueue.isEmpty()))
			{
				System.out.println("Message Queue Size : "+Node.msgQueue.size() +" for Node "+ Node.NodeId);
				Message message = Node.peekQueue();
				//System.out.println("Peeked Message : " + msg.messageText + " to be sent of type "+msg.type + "from initiator"+msg.initiator +"to the destination"+msg.destinationNode);

				if(message.type.equals(Message.messageType.Application.toString()) && Node.isFreezeQueue() == false && Node.isFreezeApplicaion() == false)
				{
					Node.removeQueue();
					sendApplicationOrReply(Node.connectionDetails,message);
				}
				else if(message.type.equals(Message.messageType.Checkpoint.toString()))
				{
					for(Entry<Integer,Integer> e : Node.LLR.entrySet())
					{
						System.out.println("****************LLR value before taking the checkpoint for  "+ e.getKey() + "is  : === " + e.getValue());
					}
					Node.removeQueue();

					Node.setCheckPointingFinish(false);

					if(message.initiator == Node.NodeId)
					{
						Node.setCheckpointInitiator(message.initiator);
						Node.setCheckpointParent(message.senderNode);
					}
					//freeze queue
					Node.VolatileCheckpointLLR.putAll(Node.LLR);
					Node.VolatileCheckpointFLS.putAll(Node.FLS);
					Node.setWillingToCheckpoint(false);
					Node.setCommitedCheckpoint(true);
					Node.setAbortedCheckpoint(true);

					sendCheckpoint(Node.connectionDetails,message);

					Node.checkpointCounter++;
				}
				else if(message.type.equals(Message.messageType.Commit.toString()))
				{
					Node.msgQueue.remove();
					
					Node.writeLog();
					sendCommitOrAbort(Node.connectionDetails,message);	
					boolean output = Node.compareFiles();
					if(output)
					{
						System.out.println("CHECKPOINTING CONSISTENT!!!!!!");
					}
					
					Node.setCheckPointingFinish(true);
					Node.setWillingToCheckpoint(true);
					Node.setFreezeQueue(false);
					for(int i =0; i<=NOMSend.length-1 ; i++)
					{
						NOMSend[i]=0;
					}
					if(message.senderNode == message.initiator)
					{
						//write to file volatile vectors
						//reset LLR 
						Node.resetVolatileVector();
						Node.InitilizeVector();
						Node.setWillingToCheckpoint(true);
						Node.numberOfCheckpoints++;
						for(Entry<Integer,Integer> e : Node.LLR.entrySet())
						{
							System.out.println("LLR value after resetting for  "+ e.getKey() + "is  : === " + e.getValue());
						}
						for(Entry<Integer,Integer> e : Node.FLS.entrySet())
						{
							System.out.println("FLS value after resetting for  "+ e.getKey() + "is  : === " + e.getValue());
						}
						for(Entry<Integer,Integer> e : Node.LLS.entrySet())
						{
							System.out.println("LLS value after resetting for  "+ e.getKey() + "is  : === " + e.getValue());
						}
					}
				}
				else if(message.type.equals(Message.messageType.Abort.toString()))
				{
					Node.msgQueue.remove();
				

					sendCommitOrAbort(Node.connectionDetails,message);
					Node.setCheckPointingFinish(true);
					Node.setWillingToCheckpoint(true);
					Node.setFreezeQueue(false);

					for(int i =0; i<=NOMSend.length-1 ; i++)
					{
						NOMSend[i]=0;
					}

					if(message.senderNode == message.initiator)
					{
						//reset LLR FLS LLS
						Node.resetVolatileVector();
						Node.setWillingToCheckpoint(true);
						for(Entry<Integer,Integer> e : Node.VolatileCheckpointFLS.entrySet())
						{
							System.out.println("LLR value after resetting for  "+ e.getKey() + "is  : === " + e.getValue());
						}
						for(Entry<Integer,Integer> e : Node.VolatileCheckpointLLR.entrySet())
						{
							System.out.println("FLS value after resetting for  "+ e.getKey() + "is  : === " + e.getValue());
						}
						for(Entry<Integer,Integer> e : Node.VolatileCheckpointLLS.entrySet())
						{
							System.out.println("LLS value after resetting for  "+ e.getKey() + "is  : === " + e.getValue());
						}
					}
				}
				else if(message.type.equals(Message.messageType.Reply.toString()))
				{
					Node.removeQueue();
					sendApplicationOrReply(Node.connectionDetails,message);
				}
				else if(message.type.equals(Message.messageType.Crashed.toString()))
				{
					Node.msgQueue.remove();
					sendCrashed(Node.connectionDetails, message);
				}
				else if(message.type.equals(Message.messageType.ReadyToRollback.toString()))
				{
					Node.msgQueue.remove();
					sendReadToRollback(Node.connectionDetails, message);
				}
				else if(message.type.equals(Message.messageType.Rollback.toString()))
				{
					Node.msgQueue.remove();

					//reset values of flags

					sendRollbackOrAbortRollback(Node.connectionDetails, message);
					boolean output = Node.compareFiles();
					if(output)
					{
						System.out.println("ROLLBACK CONSISTENT!!!!!!");
					}
					Node.setFreezeQueue(false);
					Node.getLastPermanentCheckpoint();
					Node.setWillingToRollback(true);
					Node.setRecovered(false);
					Node.setFreezeQueue(false);
					Node.setFreezeApplicaion(false);

					Node.setRecoveryFinish(true);
					Node.setReadyToRollback(true);
					Node.setWillingToCheckpoint(true);




					/*if(message.rollbackSender ==  message.crashedNode)
					{
						Node.setFreezeApplicaion(false);
						// what else to reset???????????????????????????
					}*/
				}
				else if(message.type.equals(Message.messageType.AbortRollback.toString()))
				{
					Node.msgQueue.remove();
					sendRollbackOrAbortRollback(Node.connectionDetails, message);
					Node.setRecoveryFinish(true);
					Node.setFreezeApplicaion(false);
					Node.setFreezeQueue(false);
					Node.setRecoveryFinish(true);
					Node.setReadyToRollback(true);
					Node.setWillingToRollback(true);
					Node.setWillingToCheckpoint(true);
					//reset values of flags
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
		}
	}

	//SEND **********APPLICATION/REPLY********** TYPE MESSAGE
	public void sendApplicationOrReply (ConcurrentHashMap<Integer, SctpChannel> connectionDetails, Message message)
	{
		for(Entry<Integer , SctpChannel> entry : connectionDetails.entrySet())
		{
			if(entry.getKey().equals(message.destinationNode))
			{
				try
				{
					message.senderNode = Node.NodeId;

					System.out.println("==== "+message.type+"message sent====by : " + Node.NodeId);

					if(message.type.equals(Message.messageType.Application.toString()))
					{
						message.timeStamp = Node.incrementClock();
						if(Node.isCheckPointingFinish() == true && NOMSend[message.destinationNode] == 0)
						{
							Node.FLS.put(message.destinationNode, message.timeStamp);
							System.out.println("===========FLS Value====== " +Node.FLS.get(message.destinationNode) +"from "+Node.NodeId);
						}
						Node.LLS.put(message.destinationNode, message.timeStamp);
						System.out.println("===========LLS Value====== " +Node.LLS.get(message.destinationNode) +"from "+Node.NodeId);
						NOMSend[message.destinationNode]++;
						Node.setSendAppMsgCount();
					}
					sendMessage(entry.getValue(),message);
					System.out.println("Message sent to "+message.destinationNode+ " at timestamp " + message.timeStamp);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//SEND **********CHECKPOINT********** TYPE MESSAGE
	public  void sendCheckpoint(ConcurrentHashMap<Integer, SctpChannel> connectionDetails, Message message) 
	{
		int sendMsgCount =0;
		boolean flaginit = false;
		boolean flagParent = false;
		ConcurrentHashMap<Integer, SctpChannel> destination = new ConcurrentHashMap<Integer, SctpChannel>();

		for(Entry<Integer , SctpChannel> entry : connectionDetails.entrySet())
		{
			int en = entry.getKey();
			int cpInit = Node.checkpointInitiator;
			int cpParent = Node.checkpointParent;
			System.out.println("entry key : "+en +" and parent : "+cpParent+" and intiator : "+cpInit);
			if(en == cpInit)
			{
				flaginit = true;
			}
			else if(en == cpParent)
			{
				flagParent = true;
			}
			else
			{
				destination.put(entry.getKey(), entry.getValue());
			}
			flaginit = false;
			flagParent = false;
		}
		if(flaginit == false && flagParent == false)
		{
			for(Entry<Integer , SctpChannel> entry : destination.entrySet())
			{
				if(Node.LLR.get(entry.getKey()) !=-1)
				{
					try
					{
						message.LLRValue = Node.LLR.get(entry.getKey());
						sendMessage(entry.getValue(),message);
						sendMsgCount++;

						Node.numberOfRequestsSend++;
						System.out.println("Sending checkpointing Request to : "+entry.getKey()+" with LLR value "+message.LLRValue);
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if(sendMsgCount ==0)
		{
			Message m = new Message();
			m.type = Message.messageType.Reply.toString();
			m.senderNode = Node.NodeId;
			m.destinationNode = Node.getCheckpointParent();
			m.initiator = Node.getCheckpointInitiator();
			m.messageText = "Yes";
			Node.setPriorityValue(m.type);
			m.priority = Node.getPriorityValue(m.type);
			Node.nQueue(m);
		}
	}

	//SEND **********COMMIT/ABORT********** TYPE MESSAGE
	public  void sendCommitOrAbort(ConcurrentHashMap<Integer, SctpChannel> connectionDetails, Message message) 
	{
		boolean flaginit = false;
		boolean flagParent = false;
		ConcurrentHashMap<Integer, SctpChannel> destination = new ConcurrentHashMap<Integer, SctpChannel>();

		for(Entry<Integer , SctpChannel> entry : connectionDetails.entrySet())
		{
			int en = entry.getKey();
			int cpInit = Node.checkpointInitiator;
			int cpParent = Node.checkpointParent;
			System.out.println("entry key : "+en +" and parent : "+cpParent +" and intiator : "+cpInit);
			if(en == cpInit)
			{
				flaginit = true;
			}
			else if(en == cpParent)
			{
				flagParent = true;
			}
			else
			{
				destination.put(entry.getKey(), entry.getValue());
			}
			flaginit = false;
			flagParent = false;
		}
		if(flaginit == false && flagParent == false)
		{
			for(Entry<Integer , SctpChannel> entry : destination.entrySet())
			{
				try
				{
					sendMessage(entry.getValue(),message);
					System.out.println("Sending to : "+entry.getKey()+"of type "+message.type);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			Node.setCheckPointingFinish(true);
			Node.setWillingToCheckpoint(true);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
	}

	//SEND **********CRASHED********** TYPE MESSAGE
	public  void sendCrashed(ConcurrentHashMap<Integer, SctpChannel> connectionDetails, Message message) 
	{
		int sendMsgCount =0;
		boolean flaginit = false;
		boolean flagParent = false;
		ConcurrentHashMap<Integer, SctpChannel> destination = new ConcurrentHashMap<Integer, SctpChannel>();

		for(Entry<Integer , SctpChannel> entry : connectionDetails.entrySet())
		{
			int en = entry.getKey();
			int cpInit = Node.crashed;
			int cpParent = Node.recoveryParent;
			System.out.println("entry key : "+en +" and parent : "+cpParent+" and intiator : "+cpInit);
			if(en == cpInit)
			{
				flaginit = true;
			}
			else if(en == cpParent)
			{
				flagParent = true;
			}
			else
			{
				destination.put(entry.getKey(), entry.getValue());
			}
			flaginit = false;
			flagParent = false;
		}
		if(flaginit == false && flagParent == false)
		{
			for(Entry<Integer , SctpChannel> entry : destination.entrySet())
			{
				try
				{
					message.LLSValue = Node.lastPermanentCheckpointLLS.get(entry.getKey());
					sendMessage(entry.getValue(),message);
					sendMsgCount++;

					Node.numberOfCrashSend++;
					System.out.println("Sending recovery Request to : "+entry.getKey()+" with LLS value "+message.LLSValue);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("sendMSGCOUNT : "+sendMsgCount);
		if(sendMsgCount ==0)
		{
			Message m = new Message();
			m.type = Message.messageType.ReadyToRollback.toString();
			m.rollbackSender = Node.NodeId;
			m.rollbackDestination = Node.getRecoveryParent();
			m.crashedNode = message.crashedNode;
			m.messageText = "Yes";
			Node.setPriorityValue(m.type);
			m.priority = Node.getPriorityValue(m.type);
			Node.nQueue(m);
		}

	}

	//SEND **********READYTOROLLBACK********** TYPE MESSAGE
	public void sendReadToRollback(ConcurrentHashMap<Integer, SctpChannel> connectionDetails,Message message) {
		for(Entry<Integer , SctpChannel> entry : connectionDetails.entrySet())
		{
			if(entry.getKey().equals(message.rollbackDestination))
			{
				try
				{
					System.out.println("Message sent to "+message.rollbackDestination);
					System.out.println("==== ROLLBACK REPLY MESSAGE : "+message.messageText+"of type "+message.type+" SENT====by : " + Node.NodeId);

					sendMessage(entry.getValue(),message);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}

	}

	//SEND **********ROLLBACK/ABORTROLLBACK********** TYPE MESSAGE
	public void sendRollbackOrAbortRollback(ConcurrentHashMap<Integer, SctpChannel> connectionDetails,Message message)
	{
		boolean flaginit = false;
		boolean flagParent = false;
		ConcurrentHashMap<Integer, SctpChannel> destination = new ConcurrentHashMap<Integer, SctpChannel>();

		for(Entry<Integer , SctpChannel> entry : connectionDetails.entrySet())
		{
			int en = entry.getKey();
			int cpInit = Node.crashed;
			int cpParent = Node.recoveryParent;
			System.out.println("entry key : "+en +" and parent : "+cpParent +" and intiator : "+cpInit);
			if(en == cpInit)
			{
				flaginit = true;
			}
			else if(en == cpParent)
			{
				flagParent = true;
			}
			else
			{
				destination.put(entry.getKey(), entry.getValue());
			}
			flaginit = false;
			flagParent = false;
		}
		if(flaginit == false && flagParent == false)
		{
			for(Entry<Integer , SctpChannel> entry : destination.entrySet())
			{
				try
				{
					sendMessage(entry.getValue(),message);
					System.out.println("Sending MESSAGE : "+message.messageText+"of type "+message.type+" SENT====by : " + Node.NodeId);
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Node.setCrashed(-1);
		Node.setRecoveryParent(-1);
		Node.resetCrashCounters();

		Node.setWillingToRollback(true);
		Node.setCrashed(-1);
		Node.setFreezeApplicaion(false);
		Node.setFreezeQueue(false);
		Node.setRecovered(false);
		Node.setRecoveryAborted(false);


		//Node.setFreezeQueue(false);
		//Node.setFreezeApplicaion(false);
	}	

	private static  void sendMessage(SctpChannel clientSock, Message message) throws IOException
	{
		// prepare byte buffer to send massage
		ByteBuffer sendBuffer = ByteBuffer.allocate(60000);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] yourBytes;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(message);
			yourBytes = bos.toByteArray();
		} finally {
			out.close();
			bos.close();
		}

		sendBuffer.clear();
		//Reset a pointer to point to the start of buffer 
		sendBuffer.put(yourBytes);
		sendBuffer.flip();
		try {
			//Send a message in the channel 
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			clientSock.send(sendBuffer, messageInfo);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
