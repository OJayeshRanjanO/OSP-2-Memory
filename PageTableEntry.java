//I pledge my honor that all parts of this project were done by me individually and without collaboration with anybody else.
//Jayesh Ranjan 109962199
package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.

   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    //NEWLY ADDED!!
    public long currentTime;
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);

       as its first statement.

       @OSPProject Memory
    */
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        // your code goes here
        super(ownerPageTable,pageNumber);
        currentTime = HClock.get();//Getting the current time,once a new page is created

    }

    /**
       This method increases the lock count on the page by one.

	The method must FIRST increment lockCount, THEN
	check if the page is valid, and if it is not and no
	page validation event is present for the page, start page fault
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
        // your code goes here
		currentTime = HClock.get();//When the page was locked


        ThreadCB currentThread = iorb.getThread();

        if(!isValid()){//Check if the page is invalid, if so, then initiate a pageFault
            if(getValidatingThread() == null){//if no page cause pageFault
                PageFaultHandler.handlePageFault(currentThread, MemoryLock, this);
            }else if(getValidatingThread() == currentThread){//Th2 == Th1
                if(getFrame()!=null){//check if the current Frame is null or not
                    // My.Out(this,"Something went wrong in PageTableEntry, current frame:  " + currentFrame);//If some thing goes wrong
                    getFrame().incrementLockCount();//incrementLockCount of the current Frame
                    return SUCCESS;
                };
                return FAILURE;
            }else{
                currentThread.suspend(this);//Suspend the current thread
            }
        }

        if(!isValid()||currentThread.getStatus() == ThreadKill){//if either the pagefault fails or if the thread that created iorb was killed while waiting for the lock operation to complete.
			return FAILURE;
        }


        try{//If everything goes well then increment lock count of current Frame, and return success
		    getFrame().incrementLockCount();
        }
        catch(Exception e){
            // My.Out(this,"Something went wrong in PageTableEntry, getFrame().incrementLockCount(): " + getFrame().incrementLockCount());
        }
		return SUCCESS;

    }

    /** This method decreases the lock count on the page by one.

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
        // your code goes here
        int lockCount = getFrame().getLockCount();//get the current lock count
        // My.Out(this,lockCount);//Debugging only
        if(lockCount>0){//Only decrement if the lock count is more than 0,(since 0 can make it fall in negative, which is a problem)
			getFrame().decrementLockCount();
		}

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
