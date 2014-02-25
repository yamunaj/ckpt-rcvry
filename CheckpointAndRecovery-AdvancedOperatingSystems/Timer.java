
public class Timer implements Runnable{

	@Override
	public void run() 
	{
		boolean flag = true;
		int i = 0;
		int backedOffTimer = 0;
		int numberOfCheckpoints = 10;
		
		while(i <= numberOfCheckpoints)//&& Node.isCheckPointingFinish()
		{
			//System.out.println("Inside TIMER LOOP "+i);
			
			if(Node.isCheckPointingFinish())
			{
				try
				{
					Thread.sleep(Node.getTimer());
					System.out.println("Timer Got up "+i+" time");
					System.out.println("NumberOfCheckpoints "+Node.numberOfCheckpoints);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				if(Node.numberOfCheckpoints == 2 && Node.isRecoveryFinish() && flag)
				{
					if(Node.NodeId == 0)
					{
						/*Thread recoveryThread = new Thread(new Recover());
						recoveryThread.start();*/
						if(Node.isWillingToCheckpoint() && Node.isWillingToRollback() && Node.isReadyToRollback() && (Node.getCrashed() == -1))
						{
							Message message = new Message();
							message.type = Message.messageType.Crashed.toString();
							Node.setPriorityValue(message.type);
							message.priority = Node.getPriorityValue(message.type);
							message.crashedNode = Node.NodeId;
							message.rollbackSender = Node.NodeId;
							message.messageText = "Crashed";
							
							Node.nQueue(message);

							Node.setWillingToRollback(false);
							Node.setWillingToCheckpoint(false);
							Node.setCrashed(Node.NodeId);
							Node.setRecoveryParent(Node.NodeId);
							Node.getLastPermanentCheckpoint();
							Node.setFreezeQueue(true);
							Node.setRecoveryFinish(false);
							Node.setRecovered(true);
							Node.setRecoveryAborted(true);
							Node.setFreezeApplicaion(true);
							// block application queue
							
							System.out.println("Node"+Node.NodeId+" START RECOVERY");
						}
						
					}
					i++;
					flag = false;
				}
				
				else if(Node.isWillingToCheckpoint() && Node.isRecoveryFinish() && i == 0)
				{
					Message message = new Message();
					message.senderNode = Node.NodeId;
					message.initiator = Node.NodeId;
					message.messageText = "Checkpoint";
					message.type = Message.messageType.Checkpoint.toString();
					Node.setPriorityValue(message.type);
					message.priority = Node.getPriorityValue(message.type);
					Node.nQueue(message);

					Node.setWillingToCheckpoint(false);
					Node.setCheckpointInitiator(Node.NodeId);
					Node.setCheckpointParent(Node.NodeId);
					Node.setFreezeQueue(true);
					Node.setCheckPointingFinish(false);
					Node.setCommitedCheckpoint(true);
					Node.setAbortedCheckpoint(true);
					
					Node.setVolatileVectors();
					System.out.println("inside TIMER Checkpoint Message added to queue by :  " + message.senderNode);
					i++;
					try {
						Thread.sleep(Node.getTimer()*4*(Node.NodeId+1));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(Node.isRecoveryFinish() && !Node.isWillingToCheckpoint())
				{
					System.out.println("START EXPONENTIAL BACKOFF");
					backedOffTimer = Node.getTimer();
					Node.setTimer(backedOffTimer);
				}
				
			}
			try {
				Thread.sleep(Node.getTimer()*4*(Node.NodeId+1));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
}
