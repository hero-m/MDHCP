package mnet;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class Lease implements Serializable{
	private static final long serialVersionUID = 3918793671515770547L;
	
	private byte[] ip;
	private byte[] hwAddress;
	private Date   leaseTime;
	private Date   LeaseStart;

	public  Lease(byte[] ip, byte[] hwAddress, byte[] leaseTime){
		this.ip         = ip        ;
		this.hwAddress  = hwAddress ;
		this.LeaseStart = Calendar.getInstance().getTime();
		this.leaseTime  = new Date(ByteFactory.getBytesAsInt(leaseTime)) ;
	}
	
	public  Lease(byte[] ip, byte[] hwAddress, Date leaseTime, Date leaseStart){
		this.ip         = ip        ;
		this.hwAddress  = hwAddress ;
		this.LeaseStart = leaseStart;
		this.leaseTime  = leaseTime ;
	}
	public  byte[] getIP()         { return ip;         }
	public  Date   getLeaseTime () { return leaseTime;  }
	public  byte[] getHwAddress () { return hwAddress;  }
	public  Date   getLeaseStart() { return LeaseStart; }
	public  void   setIP        (byte[] ip)         { this.ip = ip;               }
	public  void   setLeaseTime (Date   leaseTime ) { this.leaseTime = leaseTime; }
	public  void   setHwAddress (byte[] hwAddress ) { this.hwAddress = hwAddress; }
	public  void   setLeaseStart(Date   leaseStart) { LeaseStart = leaseStart;    }
}
