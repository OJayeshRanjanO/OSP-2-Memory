//I pledge my honor that all parts of this project were done by me individually and without collaboration with anybody else.
//Jayesh Ranjan 109962199
package osp.Memory;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /**
        This method is called once before the simulation starts.
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
        // your code goes here
        for(int i = 0 ; i < MMU.getFrameTableSize() ; i++){//Since we know the size of FrameTable, we will initalize all the frames
            FrameTableEntry newFrame = new FrameTableEntry(i);//Create a new table entry, using index from the loop
			MMU.setFrame(i, newFrame);//set the frame
		}
    }

    /**
       This method handlies memory references. The method must
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault
       by making an interrupt if the page is invalid, finally,
       if the page is still valid, i.e., not swapped out by another
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue,
       and it is possible that some other thread will take away the frame.)

       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,int referenceType, ThreadCB thread)
    {
        // your code goes here
		int pageNumber = memoryAddress/((int) Math.pow(2,  (MMU.getVirtualAddressBits() - MMU.getPageAddressBits())));
        PageTableEntry page = thread.getTask().getPageTable().pages[pageNumber];

        if(!page.isValid()){//page is invalid
            if(page.getValidatingThread() != null){//no thread caused page fault on the current invalid page
                thread.suspend(page);//Suspend the page
				if(thread.getStatus() != ThreadKill){//If the thread hasnt been killed due to long wait
					page.getFrame().setReferenced(true);//set the reference bits of the page
					if(referenceType == MemoryWrite){
						page.getFrame().setDirty(true);//Only set dirty bit to true, if something was written to the page.
					}
				}
                return page;
            }else{
                //Procedure taken from manual
                InterruptVector.setPage(page);//Followed the Manual
                InterruptVector.setReferenceType(referenceType);
                InterruptVector.setThread(thread);
                CPU.interrupt(PageFault);
            }
        }

        //Irrespective of Page being valid or not, set the reference and dirty bits
        if(thread.getStatus()!=ThreadKill){//Check if the thread is not dead yet
            page.getFrame().setReferenced(true);//Set the reference of the frame, to true
            if(referenceType== MemoryWrite){//Only set dirty to true, is there was something written in the Page
                page.getFrame().setDirty(true);
            }
        }
        page.currentTime = HClock.get();//set the currentTime for the page which will be used for LRU calculation
        return page;//returns the page

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.

	@OSPProject Memory
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.

      @OSPProject Memory
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
