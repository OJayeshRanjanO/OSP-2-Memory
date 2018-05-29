//I pledge my honor that all parts of this project were done by me individually and without collaboration with anybody else.
//Jayesh Ranjan 109962199
package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault.

        It must check and return if the page is valid,

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.

        If none of the above is true, a new frame must be chosen
        and reserved until the swap in of the requested
        page into this frame is complete.

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated
        from the frame and marked invalid. After the swap-in, the
        frame must be marked clean. The swap-ins and swap-outs
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g,
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */

	public static int do_handlePageFault(ThreadCB thread, int referenceType,PageTableEntry page){
        // your code goes here
		FrameTableEntry frame;
        TaskCB task = thread.getTask();
        //FIRST STEP IS TO CHECK IF THE PAGE IS VALID OR NOT
    	if(page.isValid())
    	{
    		return FAILURE;
    	}
        //SECOND STEP
		frame = findSuitableFrame();//Suitable frame is found
        if(frame == null){
            return NotEnoughMemory;//If no frames were found, then return NotEnoughMemory
        }

		//THIRD STEP
    	Event event = new SystemEvent("PageFaultHappened");//Event object we will use to notify other thread later on is created here
    	thread.suspend(event);
    	page.setValidatingThread(thread);
    	frame.setReserved(thread.getTask());//Reserve the frame to prevent it from theft

		PageTableEntry newPage = frame.getPage();
		if(newPage  == null){//If the page of the current frame is empty, set the page to frame we just found
			page.setFrame(frame);
    		swapIn(thread, page);//swap in
			if(isThreadKill(thread, frame, page, event)){//If the thread not killed at the end
				return FAILURE;//return FAILURE
			}
		}else{//Else if the page is not empty
			if(!frame.isDirty()){//Check if the page is not dirty (nothing written to the page)
				freeFrames(frame, newPage);//If so then free the frame
			}else if(frame.isDirty()){//if things were written to the page
    			swapOut(thread, frame.getPage());//swap out
				if(isThreadKill(thread, frame, page, event)){//check if the thread is killed
					return FAILURE;
				}
				freeFrames(frame, newPage);
			}
			page.setFrame(frame);
    		swapIn(thread, page);
			if(isThreadKill(thread, frame, page, event)){//If if the thread is not killed at the end
				return FAILURE;
			}
		}

		//Updating the page (Setting page to valid)
    	frame.setPage(page);
    	page.setValid(true);

		//Doing the final task
    	if(frame.getReserved() == task)
    	{
    		frame.setUnreserved(task);//the frame that is used to satisfy the pageFault is unreserved
    	}
    	page.setValidatingThread(null);
    	page.notifyThreads();
    	event.notifyThreads();
    	ThreadCB.dispatch();
    	return SUCCESS;

    }



	//	BELOW WE HAVE LIST OF HELPER METHODS TO CHECK IF THE THREAD IS KILLED OR TO FREE FRAMES, SWAP IN,  SWAP OUT AND FINDING SUITABLE FRAME
	private static FrameTableEntry findSuitableFrame()
    {
    	FrameTableEntry currentFrame = null;
		FrameTableEntry lruFrame = null;
		long maxTime=-1;

		for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		currentFrame = MMU.getFrame(i);
    		if((!currentFrame.isReserved()) && (currentFrame.getLockCount() == 0))//find a new that is not reserved and lock
    		{
				if (currentFrame.getPage()==null){//If the current page if the frame is null, then return the frame
    				return currentFrame;
				}
    		}
    	}
		//If we cannot find a frame that is no reserved and lockCount is 0, and has null page, then we will have to find the page that was Least Recently used.
		for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		currentFrame = MMU.getFrame(i);
			PageTableEntry page = currentFrame.getPage();
			if((!currentFrame.isReserved()) && (currentFrame.getLockCount() == 0)){
				if(Math.abs(HClock.get() - page.currentTime) > maxTime){
					lruFrame = currentFrame;
					maxTime = Math.abs(HClock.get() - page.currentTime);
				}
			}
    	}
		return lruFrame;//If null is returned then memory is full, else a frame is returned
    }

	public static boolean isThreadKill(ThreadCB thread, FrameTableEntry frame, PageTableEntry page, Event event){
		if(thread.getStatus() == ThreadKill)//If the thread is killed then notify all threads
    	{
    		if(frame.getPage() != null)//If the frame still contains a page
    		{
    			if(frame.getPage().getTask() == thread.getTask())//Is the current page's task is same as the tread's task, then set the get rid of the page
    			{
	    			frame.setPage(null);
	    		}
    		}
    		page.notifyThreads();
    		page.setValidatingThread(null);
    		page.setFrame(null);
    		event.notifyThreads();
    		ThreadCB.dispatch();
    		return true;//return true is the thread is killed
		}
		return false;//return false is the thread is not killed

	}

	public static void freeFrames(FrameTableEntry frame, PageTableEntry page){
		//General procedure taken from manul for freeing frames and updating page table
			frame.setDirty(false);
			frame.setReferenced(false);
			frame.setPage(null);

			//updating pageTable if the page is invalid
			page.setValid(false);
			page.setFrame(null);
	}

//Swap in and swap out procedues are taken from manual
    public static void swapIn(ThreadCB thread, PageTableEntry page){
		MyOut.print(thread,"Swapping in");//this is what we will count the number of swap in after simulation ends
    	TaskCB task = page.getTask();//First get the task that owns the page
        OpenFile file = task.getSwapFile();//get the swap file
        if (file!=null){//safety check if file is not null
            file.read(page.getID(),page,thread);//Perform read operation
        }
    }


    public static void swapOut(ThreadCB thread, PageTableEntry page){
		MyOut.print(thread,"Swapping out");//this is what we will count the number of swap out after simulation ends
        TaskCB task = page.getTask();//First get the task that owns the page
        OpenFile file = task.getSwapFile();//get the swap file
        if (file!=null){//safety check if file is not null
            file.write(page.getID(), page, thread);//perform write operation
        }
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}