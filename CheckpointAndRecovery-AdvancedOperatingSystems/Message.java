import java.io.Serializable;

public class Message implements Serializable {
	
	public enum messageType {Application , Abort , Commit , Rollback , Checkpoint , Reply , Crashed , ReadyToRollback , AbortRollback};
	
	private static final long serialVersionUID = 1L;
	String messageId;
	int timeStamp;
	long priority;
	String type;
	String messageText;
	
	//Checkpoint Variables
	int LLRValue = 0;
	int FLSValue = 0;
	int senderNode = -1;
	int initiator = -1;
	int destinationNode = -1;
	int counter;
	boolean willingToCheckpoint;
	
	//recovery variables
	int LLSValue = -1;
	int crashedNode = -1;
	int rollbackSender = -1;
	int rollbackDestination = -1;
	boolean willingToRollback;
}
