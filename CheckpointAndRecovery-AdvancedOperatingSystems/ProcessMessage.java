import java.util.Map.Entry;


public class ProcessMessage implements Runnable{

	Message message;
	public ProcessMessage(Message msg)
	{
		this.message = msg;
		//System.out.println("Constructor MESSAGE: "+message.initiator + ", parameter : "+msg.initiator);
	}

	@Override
	public void run() {
		//System.out.println("Message "+message.messageText+"received  from " + message.senderNode +"of type" + message.type);
		Boolean temp_willing;


		//case 2 checkpoint request received
		if(message.type.equals(Message.messageType.Checkpoint.toString()))
		{
			System.out.println("Checkpoint Request Recieved from " + message.senderNode+"initiatied by "+ message.initiator+" with LLR value " +message.LLRValue);

			Node.setCommitedCheckpoint(true);
			Node.setAbortedCheckpoint(true);
			System.out.println("Is willing to checkpoint : "+Node.isWillingToCheckpoint());
			System.out.println("Node.FLS.get(message.senderNode) : "+Node.FLS.get(message.senderNode));
			if(Node.isWillingToCheckpoint() == false && message.initiator != Node.getCheckpointInitiator())
			{
				System.out.println("Not ready to take checkpoint");
				//message object with NO
				Message m = new Message();
				m.type = Message.messageType.Reply.toString();
				m.senderNode = Node.NodeId;
				m.destinationNode = message.senderNode;
				m.initiator = message.initiator;
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				m.messageText = "No";
				Node.nQueue(m);
			}
			else if(Node.isWillingToCheckpoint() == false && message.initiator == Node.getCheckpointInitiator())
			{
				System.out.println("Already taken a checkpoint");
				//message object with YES
				Message m = new Message();
				m.type = Message.messageType.Reply.toString();
				m.senderNode = Node.NodeId;
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				m.destinationNode = message.senderNode;
				m.initiator = message.initiator;
				m.messageText = "Yes";
				Node.nQueue(m);
			}
			else if(Node.isWillingToCheckpoint() == true && (message.LLRValue >= Node.FLS.get(message.senderNode)) && Node.FLS.get(message.senderNode) >-1 )
			{
				System.out.println("Ready to take a checkpoint : "+Node.FLS.get(message.senderNode));
				//Node.setWillingToCheckpoint(false);

				Message m = new Message();
				m.type = Message.messageType.Checkpoint.toString();
				m.senderNode = Node.NodeId;
				m.initiator = message.initiator;
				m.messageText = "Checkpointing instance started by == " + m.initiator +"Propagated by : " + m.senderNode;
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				Node.nQueue(m);
				System.out.println("Checkpointing msg added to queue for propagation");
				Node.setFreezeQueue(true);
				Node.setWillingToCheckpoint(false);
				Node.setCheckPointingFinish(false);
				Node.setCheckpointParent(message.senderNode);
				Node.setCheckpointInitiator(message.initiator);

				Node.setVolatileVectors();

			}
		else if(Node.isWillingToCheckpoint() &&(message.LLRValue >= Node.FLS.get(message.senderNode)) && (Node.FLS.get(message.senderNode) ==-1))
		{
			//reply yes
			System.out.println("Already taken a checkpoint");
			//message object with YES
			Message m = new Message();
			m.type = Message.messageType.Reply.toString();
			m.senderNode = Node.NodeId;
			m.destinationNode = message.senderNode;
			m.initiator = message.initiator;
			m.messageText = "Yes";
			Node.setPriorityValue(m.type);
			m.priority = Node.getPriorityValue(m.type);
			Node.nQueue(m);
		}
		}

		// case 1 application message...
		if(message.type.equals(Message.messageType.Application.toString()))
		{
			Node.setReceiveAppMsgCount();
			Node.clock.compareAndSetTime(message.timeStamp);
			System.out.println(message.messageText + " message recieved from " + message.senderNode + " at timestamp " + message.timeStamp);

			Node.LLR.put(message.senderNode, message.timeStamp);
			System.out.println("=============LLR Value ======= " + Node.LLR.get(message.senderNode)+" from " +message.senderNode);

			for(Entry<Integer,Integer> e : Node.LLR.entrySet())
			{
				System.out.println("LLR value after the message is received  for "+ e.getKey() + "is  : === " + e.getValue());
			}
		}

		//case 3 Reply message received
		if(message.type.equals(Message.messageType.Reply.toString()))
		{
			//	Node.setTotalReceiveCount();
			System.out.println("Reply msg "+message.messageText+"received from "+message.senderNode+" of type "+message.type);

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
				System.out.println("No Receive Cunt : "+Node.getNoReceiveCount());
				System.out.println("Yes Receive Cunt : "+Node.getYesReceiveCount());
				System.out.println("Total Send Cunt : "+Node.numberOfRequestsSend);
				if(Node.getYesReceiveCount() == Node.numberOfRequestsSend)//Node.getTotalReceiveCount()) 
				{
					System.out.println("Initiator with Node ID: " + Node.NodeId + " READY TO SEND COMMIT TO COHORTS");
					Message m = new Message();
					m.type = Message.messageType.Commit.toString();
					m.senderNode = Node.NodeId;
					m.initiator = Node.NodeId;
					m.messageText = "Commit Checkpoint";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					Node.nQueue(m); // send commit
					Node.setLastPermanentCheckpointFLS();
					Node.setLastPermanentCheckpointLLR();
					Node.setLastPermanentCheckpointLLS();
					Node.writeLog();
					Node.resetReceiveCounters();
					Node.resetReceiveAppMsgCount(0);
					Node.resetSendAppMsgCount(0);

					System.out.println("======Last Permanent Checkpoint Taken=not rea===");
					for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLR.entrySet())
					{
						System.out.println("LLR value at last CheckPoint  "+ e.getKey() + "is  : === " + e.getValue());
					}
					for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointFLS.entrySet())
					{
						System.out.println("FLS value at last CheckPoint  "+ e.getKey() + "is  : === " + e.getValue());
					}
					System.out.println("Size of lastPermanentCheckpointLLS "+Node.lastPermanentCheckpointLLS.size());
					for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLS.entrySet())
					{
						System.out.println("LLS value at last CheckPoint  "+ e.getKey() + "is  : === " + e.getValue());
					}
				}
				else if(((Node.getYesReceiveCount() + Node.getNoReceiveCount()) == Node.numberOfRequestsSend) /*Node.getTotalReceiveCount())*/ ||(Node.getNoReceiveCount() == Node.numberOfRequestsSend/*Node.getTotalReceiveCount()*/))
				{
					System.out.println("Initiator with Node ID: " + Node.NodeId + " READY TO SEND ABORT TO COHORTS");

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
					Node.resetReceiveCounters();
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
				if(Node.getYesReceiveCount() == Node.numberOfRequestsSend)//Node.getTotalReceiveCount()) 
				{
					System.out.println("Initiator with Node ID: " + Node.NodeId + " READY TO SEND YES TO PARENT");

					Message m = new Message();
					m.type = Message.messageType.Reply.toString();
					m.senderNode = Node.NodeId;
					m.destinationNode = Node.getCheckpointParent();
					m.initiator = message.initiator;
					m.messageText = "Yes";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					Node.nQueue(m); // send YES message to parent

					//reset receive counter
					Node.resetReceiveCounters();
				}
				else if(((Node.getYesReceiveCount() + Node.getNoReceiveCount()) == Node.numberOfRequestsSend) /*Node.getTotalReceiveCount())*/ ||(Node.getNoReceiveCount() == Node.numberOfRequestsSend)/*Node.getTotalReceiveCount())*/)
				{
					System.out.println("Initiator with Node ID: " + Node.NodeId + " READY TO SEND NO TO PARENT");

					// condition to be added to wait to get all "no" reply messages
					Message m = new Message();
					m.type = Message.messageType.Reply.toString();
					m.senderNode = Node.NodeId;
					m.destinationNode = Node.getCheckpointParent();
					m.initiator = message.initiator;
					m.messageText = "No";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					// Send abort Message to parent
					Node.nQueue(m); 

					//reset receive counters
					Node.resetReceiveCounters();
				}
			}
		}

		// case 4 commit message received
		if(message.type.equals(Message.messageType.Commit.toString()) && Node.isCommitedCheckpoint())
		{
			System.out.println("Commit message received from "+message.senderNode);
			if(Node.commitedCheckpoint)
			{
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

				Node.setLastPermanentCheckpointFLS();
				Node.setLastPermanentCheckpointLLR();
				Node.setLastPermanentCheckpointLLS();
				Node.writeLog();
				Node.InitilizeVector();
				Node.setVolatileVectors();
				Node.setCommitedCheckpoint(false);
				Node.resetReceiveAppMsgCount(0);
				Node.resetSendAppMsgCount(0);
				Node.setCheckPointingFinish(true);
				Node.numberOfCheckpoints++;
				//Node.writeToLog();
				System.out.println("Write to log DONE");
				Node.setWillingToCheckpoint(true);
				Node.numberOfRequestsSend =0;
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

				System.out.println("======Last Permanent Checkpoint Taken=not rea===");
				for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLR.entrySet())
				{
					System.out.println("LLR value at last CheckPoint  "+ e.getKey() + "is  : === " + e.getValue());
				}
				for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointFLS.entrySet())
				{
					System.out.println("FLS value at last CheckPoint  "+ e.getKey() + "is  : === " + e.getValue());
				}
				for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLS.entrySet())
				{
					System.out.println("LLS value at last CheckPoint  "+ e.getKey() + "is  : === " + e.getValue());
				}
			}	


			/*Node.setCommited(false);
					// WRITE THE VolatileCheckpoint_LLR and FLS arrays to file
					Node.writeToLog();
					// clear the FLS and LLR arrays.

					Node.setWillingToCheckpoint(true);
					Node.numberOfRequestsSend =0;
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
			 */			
		}

		// case 5 abort ONLY INITIATOR WILL SEND and on receiving just spread it across cohorts and adjust markers
		if(message.type.equals(Message.messageType.Abort.toString()) && Node.isAbortedCheckpoint())
		{
			System.out.println("Abort message received from "+message.senderNode);
			if(Node.abortedCheckpoint)
			{
				Message m = new Message();
				m.type = Message.messageType.Abort.toString();
				m.senderNode = Node.NodeId;
				m.initiator = message.initiator;
				m.messageText = "Abort Checkpoint";
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				// Send abort Message to nodes in the network
				Node.nQueue(m); 

				Node.setVolatileVectors();
				Node.setAbortedCheckpoint(false);
				Node.setWillingToCheckpoint(true);
				Node.setCheckPointingFinish(true);
				Node.numberOfRequestsSend =0;
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
			/*
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
			 */				}


		//=====================CRASHED====================
		else if(message.type.equals(Message.messageType.Crashed.toString()))
		{
			System.out.println("RECEIVED CRASHED MSG from "+message.rollbackSender+ " with LLS value : "+message.LLSValue);
			System.out.println("LLR value : "+ Node.LLR.get(message.rollbackSender));

			if(Node.isWillingToRollback() && Node.isWillingToCheckpoint() && Node.isReadyToRollback() && Node.LLR.get(message.rollbackSender) > message.LLSValue)
			{
				System.out.println("READY TO ROLL BACK AND PROPAGATE MSG TO COHORTS");

				Message m = new Message();
				m.type = Message.messageType.Crashed.toString();
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				m.crashedNode = message.crashedNode;
				m.rollbackSender = Node.NodeId;
				m.messageText = " Progagate Crashed message from "+m.rollbackSender;
				Node.nQueue(m);


				//Node.setReadyToRollback(false);
				Node.setWillingToRollback(false);
				Node.setCrashed(message.crashedNode);
				Node.setRecoveryParent(message.rollbackSender);
				Node.setFreezeApplicaion(true);
				Node.setFreezeQueue(true);
				Node.setRecovered(true);
				Node.setRecoveryAborted(true);

			}
			else if(Node.isWillingToRollback() && Node.isWillingToCheckpoint() && Node.isReadyToRollback() && Node.LLR.get(message.rollbackSender) <= message.LLSValue)
			{
				System.out.println("NO NEED FOR ROLLING BACK");
				//send i am not rolling back msg
				Message m = new Message();
				m.type = Message.messageType.ReadyToRollback.toString();
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				m.crashedNode = message.crashedNode;
				m.rollbackSender = Node.NodeId;
				m.rollbackDestination = Node.getRecoveryParent();
				m.messageText = "Yes";
				Node.nQueue(m);


				Node.setRecoveryParent(message.rollbackSender);
				Node.setCrashed(message.crashedNode);
				Node.setReadyToRollback(false);
				Node.setWillingToRollback(false);



			}
			else if((!Node.isWillingToRollback() || !Node.isWillingToCheckpoint() || !Node.isReadyToRollback()) && message.crashedNode == Node.getCrashed())
			{
				System.out.println("NO NEED FOR ROLLING BACK");
				//send i am not rolling back msg
				Message m = new Message();
				m.type = Message.messageType.ReadyToRollback.toString();
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				m.crashedNode = message.crashedNode;
				m.rollbackSender = Node.NodeId;
				m.rollbackDestination = Node.getRecoveryParent();
				m.messageText = "Yes";
				Node.nQueue(m);


				Node.setRecoveryParent(message.rollbackSender);
				Node.setCrashed(message.crashedNode);
				Node.setReadyToRollback(false);
				Node.setWillingToRollback(false);


			}
			else if (!Node.isWillingToRollback() || !Node.isWillingToCheckpoint() || !Node.isReadyToRollback())
			{
				System.out.println("CANNOT ROLLBACK");

				Message m = new Message();
				m.type = Message.messageType.ReadyToRollback.toString();
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				m.crashedNode = message.crashedNode;
				m.rollbackSender = Node.NodeId;
				m.rollbackDestination = Node.getRecoveryParent();
				m.messageText = "NO";
				Node.nQueue(m);

				Node.setReadyToRollback(false);
				// **********add a case of NO response to sender node and put it in queue

			}
		}

		//=====================READYTOROLLBACK=============================
		if(message.type.equals(Message.messageType.ReadyToRollback.toString()))
		{
			System.out.println("Received Message" + message.messageText+" of type "+message.type);
			//initiator of recovery
			if(Node.NodeId == message.crashedNode)
			{
				if(message.messageText.equalsIgnoreCase("Yes"))
				{
					Node.setCrashedYesReceiveCount();
				}
				else if(message.messageText.equals("No"))
				{
					Node.setCrashedNoReceiveCount();
				}

				if(Node.getCrashedYesReceiveCount() == Node.numberOfCrashSend)
				{
					// new message for recover final
					System.out.println("Crashed Process ready to send ROLLBACK to all");

					Message message = new Message();
					message.messageText = "Rollback";
					message.crashedNode = Node.NodeId;
					message.type = Message.messageType.Rollback.toString();
					Node.setPriorityValue(message.type);
					message.priority = Node.getPriorityValue(message.type);
					message.rollbackSender = Node.NodeId;
					message.timeStamp = Node.getClock();

					Node.nQueue(message);

					// reset counters

				}
				else if(((Node.getCrashedYesReceiveCount() + Node.getCrashedNoReceiveCount()) == Node.numberOfCrashSend) /*Node.getTotalReceiveCount())*/ ||(Node.getCrashedNoReceiveCount() == Node.numberOfCrashSend/*Node.getTotalReceiveCount()*/))
				{
					System.out.println("Crashed node "+ Node.NodeId+" Aborting the instance");

					Message message = new Message();
					message.messageText = "Abort Recovery";
					message.crashedNode = Node.NodeId;
					message.type = Message.messageType.AbortRollback.toString();
					Node.setPriorityValue(message.type);
					message.priority = Node.getPriorityValue(message.type);
					message.rollbackSender = Node.NodeId;
					message.timeStamp = Node.getClock();

					Node.nQueue(message);

					/*Node.resetCrashCounters();
					Node.setRecoveryFinish(true);
					Node.setRecoveryParent(-1);
					Node.setCrashed(-1);
					Node.setFreezeApplicaion(false);
					Node.setFreezeQueue(false);
					Node.setRecoveryFinish(true);
					Node.setReadyToRollback(true);
					Node.setWillingToRollback(true);
					Node.setWillingToCheckpoint(true);*/
				}
			}

			else if(message.crashedNode != Node.NodeId)
			{
				if(message.messageText.equalsIgnoreCase("Yes"))
				{
					Node.setCrashedYesReceiveCount();
				}
				else if(message.messageText.equals("No"))
				{
					Node.setCrashedNoReceiveCount();
				}

				if(Node.getCrashedYesReceiveCount() == Node.numberOfCrashSend)
				{
					System.out.println("Active Process ready to send REPLY AS YES to Parent");

					Message m = new Message();
					m.type = Message.messageType.ReadyToRollback.toString();
					m.rollbackSender = Node.NodeId;
					m.rollbackDestination = Node.getRecoveryParent();
					m.crashedNode = message.crashedNode;
					m.messageText = "Yes";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					Node.nQueue(m); // send YES message to parent
					Node.resetReceiveCounters();
				}
				else if (((Node.getCrashedYesReceiveCount() + Node.getCrashedNoReceiveCount()) == Node.numberOfCrashSend) /*Node.getTotalReceiveCount())*/ ||(Node.getCrashedNoReceiveCount() == Node.numberOfCrashSend/*Node.getTotalReceiveCount()*/))
				{
					System.out.println("Active Process with Node ID: " + Node.NodeId + " ready to SEND NO TO PARENT");
					// condition to be added to wait to get all "no" reply messages
					Message m = new Message();
					m.type = Message.messageType.ReadyToRollback.toString();
					m.rollbackSender = Node.NodeId;
					m.rollbackDestination = Node.getRecoveryParent();
					m.crashedNode = message.crashedNode;
					m.messageText = "No";
					m.timeStamp = Node.getClock();
					Node.setPriorityValue(m.type);
					m.priority = Node.getPriorityValue(m.type);
					// Send NO Message to parent
					Node.nQueue(m); 
					Node.resetReceiveCounters();
				}
			}
		}

		//====================ROLLBACK======================
		if(message.type.equals(Message.messageType.Rollback.toString()))
		{
			System.out.println("Rollback message received from "+message.rollbackSender);
			if(Node.recovered)
			{
				if(Node.isReadyToRollback())
				{
					//READ from log and load the vectors

					System.out.println("Read from log DONE");

				}

				//reset the llr fls and lls with last permanent checkpoint when final rollback message is received

				//propogate the recover message
				Message m = new Message();
				m.type = Message.messageType.Rollback.toString();
				m.rollbackSender = Node.NodeId;
				m.crashedNode = message.crashedNode;
				m.messageText = "Rollback";
				m.timeStamp = Node.getClock();
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				Node.nQueue(m);

				//just to ensure after an abort is got what are the values of llr fls n lls
				for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLR.entrySet())
				{
					System.out.println("LLR value of last permanent checkpoint  for  "+ e.getKey() + "is  : === " + e.getValue());
				}
				for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointFLS.entrySet())
				{
					System.out.println("FLS value of last permanent checkpoint  for  "+ e.getKey() + "is  : === " + e.getValue());
				}
				for(Entry<Integer,Integer> e : Node.lastPermanentCheckpointLLS.entrySet())
				{
					System.out.println("LLS value of last permanent checkpoint  for  "+ e.getKey() + "is  : === " + e.getValue());
				}

				Node.setFreezeApplicaion(false);
				Node.setFreezeQueue(false);
				System.out.println("Is the application unfreezed : " + Node.isFreezeApplicaion());

			}
		}

		//====================ABORTROLLBACK===================================
		if(message.type.equals(Message.messageType.AbortRollback.toString()) && Node.isAbortedCheckpoint())
		{
			System.out.println("Abort Recovery message received from "+message.rollbackSender);
			if(Node.recoveryAborted)
			{

				Message m = new Message();
				m.type = Message.messageType.AbortRollback.toString();
				m.rollbackSender = Node.NodeId;
				m.crashedNode = message.crashedNode;
				m.messageText = "Abort Rollback";
				Node.setPriorityValue(m.type);
				m.priority = Node.getPriorityValue(m.type);
				Node.nQueue(m);

				Node.setRecoveryAborted(false);
				//Node.numberOfRequestsSend =0;
				Node.setFreezeQueue(false);
				Node.setFreezeApplicaion(false);
				Node.setCrashed(-1);
				Node.setRecoveryParent(-1);
				Node.setRecoveryFinish(true);
				Node.setReadyToRollback(true);
				Node.setWillingToRollback(true);
				Node.setWillingToCheckpoint(true);
				//resend recovery request

				//????????????????????????????????????????????????????????????
				//just to ensure after an abort is got what are the values of llr fls n lls
				for(Entry<Integer,Integer> e : Node.LLR.entrySet())
				{
					System.out.println("LLR value of last permanent checkpoint  for  "+ e.getKey() + "is  : === " + e.getValue());
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
	}
}
