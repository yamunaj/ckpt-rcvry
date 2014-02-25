import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Queue;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;


public class Server implements Runnable {
	public int count =0;
	SctpServerChannel serverSocket;
	Queue<Message> receiveMessageList = new ArrayDeque<Message>();
	public Server(SctpServerChannel serverSock) {
		this.serverSocket = serverSock;
	}
	@Override
	public void run() {
		ByteBuffer byteBuffer;
		byteBuffer = ByteBuffer.allocate(60000);
		String hostname = null;
		// Accept Connections from all other nodes
		SctpChannel[] clientSockets = new SctpChannel[Node.receiveConfiguration.size()];
		for(int i=0; i<Node.receiveConfiguration.size(); i++)
		{
			try {
				clientSockets[i] = serverSocket.accept();
				clientSockets[i].configureBlocking(false);
				Iterator<SocketAddress> it = clientSockets[i].getRemoteAddresses().iterator();
				boolean flag = false;
				while(it.hasNext())
				{
					InetSocketAddress sc = (InetSocketAddress) it.next();
					String hostName = sc.getHostName().toString();
					for(Entry<Integer,String> entry : Node.receiveConfiguration.entrySet())
					{
						String value = entry.getValue();
						String[] values = value.split(" ");
						if(hostName.equals(values[0]))
						{
							Node.connectionDetails.put(entry.getKey(),clientSockets[i]);
							System.out.println("Node"+Node.NodeId+" accepts connection from "+entry.getKey());
							flag = true;
							break;
						}
					}
					if(flag)
					{
						break;
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		//Accept messages from all the nodes and process it
		boolean flag = true;
		while(flag)
		{
			for(Entry<Integer,SctpChannel> entry : Node.connectionDetails.entrySet())
			{
				try {
					byteBuffer.clear();
					MessageInfo msgInfo = entry.getValue().receive(byteBuffer,null,null);
					byteBuffer.flip();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(byteBuffer.remaining() >0)
				{
					byte[] yourBytes = byteBuffer.array();
					ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
					ObjectInput in = null;
					try {
						in = new ObjectInputStream(bis);
						Message messageInfo =(Message) in.readObject(); 

						//receiveMessageList.add(messageInfo);
						System.out.println("Inside Servertthread : Initiator : "+messageInfo.initiator);
						Node.compareAndSetClock(messageInfo);

						if(messageInfo.type.equals(Message.messageType.Application.toString())&& Node.isFreezeApplicaion())
						{
							
						}
						else
						{
						Thread processMessageThread = new Thread(new ProcessMessage(messageInfo));
						processMessageThread.start();
						}
						Thread.sleep(5000);
						//ProcessMessage(messageInfo);
						//}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					} finally {
						try {
							bis.close();
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					count++;
				}
			}
		}
	}
	/*public void ProcessMessage(Message message) 
	{
		//System.out.println("Message "+message.messageText+"received  from " + message.senderNode +"of type" + message.type);
		Boolean temp_willing;
		

		//case 2 checkpoint request received
		if(message.type.equals(Message.messageType.Checkpoint.toString()))
		{
			System.out.println("Checkpoint Request Recieved from " + message.senderNode+" with LLR value " +message.LLRValue);
			Node.setCommited(true);
			Node.setAborted(true);
			if(Node.isWillingToCheckpoint() == false && message.initiator != Node.getCheckpointInitiator())
			{
				//message object with NO
				Message m = new Message();
				m.type = Message.messageType.Reply.toString();
				m.senderNode = Node.NodeId;
				m.destinationNode = message.senderNode;
				m.messageText = "No";
				Node.nQueue(m);
				//Node.msgQueue.add(m);
			}
			else if(Node.isWillingToCheckpoint() == false && message.initiator == Node.getCheckpointInitiator())
			{
				//message object with YES
				Message m = new Message();
				m.type = Message.messageType.Reply.toString();
				m.senderNode = Node.NodeId;
				m.destinationNode = message.senderNode;
				m.messageText = "Yes";
				Node.nQueue(m);
				//Node.msgQueue.add(m);
			}
			else if(Node.isWillingToCheckpoint() == true && (message.LLRValue >= Node.FLS.get(message.senderNode)) && Node.FLS.get(message.senderNode) >-1 )
			{
				System.out.println("Ready to take a checkpoint : "+Node.FLS.get(message.senderNode));
				Node.setWillingToCheckpoint(false);
				Node.setCheckpointParent(message.senderNode);
				Node.setCheckpointInitiator(message.initiator);
				Node.setVolatileVectors();

				Message m = new Message();
				m.type = Message.messageType.Checkpoint.toString();
				m.senderNode = Node.NodeId;
				m.initiator = message.initiator;
				m.timeStamp = Node.getClock();
				m.messageText = "Checkpointing instance started by ==" + m.initiator;

				Node.nQueue(m);
				Node.setFreezeQueue(true);
			}
		
			
			}
		// case 1 application message...
				if(message.type.equals(Message.messageType.Application.toString()))
				{
					System.out.println(message.messageText + " message recieved from " + message.senderNode + " at timestamp " + message.timeStamp);
					
					Node.setEventCount();
					Node.LLR.put(message.senderNode, message.LLRValue);
					//System.out.println("=============LLR Value =======" + Node.LLR.get(message.senderNode)+"from" +message.senderNode);

					//.....No need as timer is used for clock....if(Node.eventCount % Node.timer == 0)
					{
						Message newMessage = new Message(); // construct a message here for starting a checkpointing...
						newMessage.type = Message.messageType.Checkpoint.toString();
						newMessage.timeStamp = Node.getClock();
						newMessage.initiator = Node.NodeId;
						newMessage.senderNode = Node.NodeId;
						newMessage.messageText = "Checkpoint";
						newMessage.priority = Node.priorityValue - 1;
						Node.startCheckpointing(newMessage);

					}

				}
		//case 3 Reply message received
		if(message.type.equals(Message.messageType.Reply.toString()))
		{
			Node.setTotalReceiveCount();

			if(message.initiator == Node.NodeId)
			{
				//if node is the initiator
				if(message.messageText.equalsIgnoreCase("Yes"))
				{
					Node.setYesReceiveCount();
				}
				else if (message.messageText.equalsIgnoreCase("No"))
				{
					Node.setNoReceiveCount();
				}
				if(Node.getYesReceiveCount() == Node.getTotalReceiveCount()) 
				{
					Message m = new Message();
					m.type = Message.messageType.Commit.toString();
					m.senderNode = Node.NodeId;
					m.initiator = Node.NodeId;
					m.messageText = "Commit Checkpoint";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					Node.nQueue(m); // send commit 
				}
				else if(((Node.getYesReceiveCount() + Node.getNoReceiveCount()) == Node.getTotalReceiveCount()) ||(Node.getNoReceiveCount() == Node.getTotalReceiveCount()))
				{
				
					Message m = new Message();
					m.type = Message.messageType.Abort.toString();
					m.senderNode = Node.NodeId;
					m.initiator = Node.NodeId;
					m.messageText = "Abort Checkpoint";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					// Send abort Message to nodes in the network
					Node.nQueue(m); 
				}
			}
			else if (message.initiator != Node.NodeId)
			{
				//if node is the initiator
				if(message.messageText.equalsIgnoreCase("Yes"))
				{
					Node.setYesReceiveCount();
				}
				else if (message.messageText.equalsIgnoreCase("No"))
				{
					Node.setNoReceiveCount();
				}
				if(Node.getYesReceiveCount() == Node.getTotalReceiveCount()) 
				{
					Message m = new Message();
					m.type = Message.messageType.Reply.toString();
					m.senderNode = Node.NodeId;
					m.destinationNode = Node.getCheckpointParent();
					m.initiator = message.initiator;
					m.messageText = "Yes";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					Node.nQueue(m); // send commit to parent
				}
				else if(((Node.getYesReceiveCount() + Node.getNoReceiveCount()) == Node.getTotalReceiveCount()) ||(Node.getNoReceiveCount() == Node.getTotalReceiveCount()))
				{
					// condition to be added to wait to get all "no" reply messages
					Message m = new Message();
					m.type = Message.messageType.Reply.toString();
					m.senderNode = Node.NodeId;
					m.destinationNode = Node.getCheckpointParent();
					m.initiator = Node.NodeId;
					m.messageText = "No";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					// Send abort Message to nodes in the network
					Node.nQueue(m); 
				}
			}
		}
		// case 4 commit message received
		if(message.type.equals(Message.messageType.Commit.toString()) && Node.isCommited())
		{
			
			Node.setCommited(false);
			// WRITE THE VolatileCheckpoint_LLR and FLS arrays to file
			Node.writeToLog();
			// clear the FLS and LLR arrays.
			Node.InitilizeVector();
			Node.setWillingToCheckpoint(true);
			//send commit msg to cohorts
			Message m = new Message();
			m.type = Message.messageType.Commit.toString();
			m.senderNode = Node.NodeId;
			m.initiator = message.initiator;
			m.messageText = "Commit Checkpoint";
			m.timeStamp = Node.getClock();
			Node.setPriorityValue(m.type);
			m.priority = Node.getPriorityValue(m.type);
			Node.nQueue(m);
			// checkpoint already taken ?
		}

		// case 5 abort ONLY INITIATOR WILL SEND and on receiving just spread it across cohorts and adjust markers
		if(message.type.equals(Message.messageType.Abort.toString()) && Node.isAborted())
		{
			Node.setAborted(false);
			Node.setWillingToCheckpoint(true);
			// SEND ABORT TO ALL COHORTs except PARENT and INITIATOR
			Message m = new Message();
			m.type = Message.messageType.Abort.toString();
			m.senderNode = Node.NodeId;
			m.initiator = message.initiator;
			m.messageText = "Abort Checkpoint";
			m.timeStamp = Node.getClock();
			Node.setPriorityValue(m.type);
			m.priority = Node.getPriorityValue(m.type);
			// Send abort Message to nodes in the network
			Node.nQueue(m); 
			Node.resetVolatileVector();
			// volatile checkpoint vectors i.e. volatilecheckpointLLR and volatilecheckpointFLS are to be reset
		}

	}*/


}
