
public class Recover implements Runnable{

	@Override
	public void run() {

		boolean flag = true;
		int i = 0;
		int backedOffTimer = 0;
		int numberOfRecovery = 1;
		while(true){
		while(Node.NodeId == Node.failNode && Node.isRecoveryFinish())
		{
			//if(i != Node.numberOfCheckpoints%2)
			/*if(Node.numberOfCheckpoints != 1)
			{
				try {
					Thread.sleep(50086);
					System.out.println("NODE"+Node.NodeId+" CRASHED!!!! "+i+" time");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}*/
			//else if(Node.numberOfCheckpoints == 1)
			{
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
					
					// block application queue
					Node.setFreezeApplicaion(true);
					System.out.println("Node"+Node.NodeId+" START RECOVERY");
				}
			}

			try
			{
				Thread.sleep(100000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			i++;
			flag = false;
		}
		}
	}

}
