package mnet;

import java.io.Serializable;

public class Lease implements Serializable{
	private static final long serialVersionUID = 3918793671515770547L;
	
	private byte[] ip;
	private byte[] hwAddress;
	private long leaseTime;
	private long leaseStart;
	private long renewalTime;
	private long rebindingTime;

	public  Lease(byte[] ip, byte[] hwAddress, long leaseTime, long renewalTime, long rebindingTime){
		this(ip, hwAddress, leaseTime, System.currentTimeMillis(), renewalTime, rebindingTime);
	}
	
	public  Lease(byte[] ip, byte[] hwAddress, long leaseTime,
			long leaseStart, long renewalTime, long rebindingTime){
		this.ip         = ip        ;
		this.hwAddress  = hwAddress ;
		this.leaseStart = leaseStart;
		this.leaseTime  = leaseTime ;
		this.renewalTime = renewalTime;
		this.rebindingTime = rebindingTime;
	}
	public  byte[] getIP()           { return ip;           }
	public  long   getLeaseTime ()   { return leaseTime;    }
	public  byte[] getHwAddress ()   { return hwAddress;    }
	public  long   getLeaseStart()   { return leaseStart;   }
	public  long   getRenewalTime()  { return renewalTime;  }
	public  long   getRebindingTime(){ return rebindingTime;}
	public  void   setIP        (byte[] ip)         { this.ip = ip;                   }
	public  void   setLeaseTime (long   leaseTime ) { this.leaseTime   = leaseTime;   }
	public  void   setHwAddress (byte[] hwAddress ) { this.hwAddress   = hwAddress;   }
	public  void   setLeaseStart(long   leaseStart) { this.leaseStart  = leaseStart;  }
	public  void   setRenewalTime(long renewalTime) { this.renewalTime = renewalTime; }
	public  void   setRebindingTime(long rebindingTime){ this.rebindingTime = rebindingTime; }
}