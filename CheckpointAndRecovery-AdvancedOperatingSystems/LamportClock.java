public class LamportClock {
	volatile int Time;
	int delta; // Incremental value 

	public LamportClock(){ 
		Time = 0;
		delta = 1;
	}
		
	/**
	 * set clock to given time
	 * @param time
	 */
	public synchronized void setTime(int time){ 
		this.Time = time;
	}
	/**
	 * Compare Local Time With Remote Hosts/Senders Time and set clock to correct time
	 * @param remoteTime : Time of Remote/Sender Node
	 * @return
	 */
	public synchronized int compareAndSetTime(int remoteTime){ 
		if(remoteTime > this.Time)
		{
			this.Time = remoteTime;
		}
		this.Time = this.Time +delta;
		return Time;
	}
	
	/**
	 * Increments Time by delta and notify all waiting threads
	 * @return new Time
	 */
	public synchronized int increment(){ 
		this.Time= this.Time+ this.delta;
		return this.Time;
	}
}
