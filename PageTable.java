//I pledge my honor that all parts of this project were done by me individually and without collaboration with anybody else.
//Jayesh Ranjan 109962199
package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{   int sizeOfArray;//This is where we store the pages
    /**
	The page table constructor. Must call

	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
        // your code goes here
        super(ownerTask);
        sizeOfArray = (int) Math.pow(2,MMU.getPageAddressBits());//Getting the size we require for our pageTable
        pages = new PageTableEntry[sizeOfArray];

        int i =0;
        while(i < sizeOfArray){
            pages[i] = new PageTableEntry(this, i);//initializing it with pages
            i++;
        }

    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        // your code goes here
        //Since the method is called by a terminating task, we will first find the task, and set it's page to null, dirty bits to false, and referenced bit to false.
        for(int i = 0 ; i < sizeOfArray ; i++){
			FrameTableEntry frame = pages[i].getFrame();
            if (frame!=null){//check if the frame is not null
                try{//We use try catch for debugging
                    TaskCB task = frame.getPage().getTask();
                    if(task == getTask()){//if page that belongs to the task is same as the current task, set the page the null, dirty to false, and setReferenced to false
                        frame.setPage(null);
                        frame.setDirty(false);
                        frame.setReferenced(false);
                    }
                }catch(Exception e){//debugging only
                    // My.Out(this,"do_deallocateMemory has issues");
                }
            }
		}

        for(int i = 0 ; i < MMU.getFrameTableSize() ; i++){//Gets every single frame from MMU
			if(MMU.getFrame(i).getReserved() == getTask()){//If the task is the same as the one in the current index of FrameTable
				MMU.getFrame(i).setUnreserved(getTask());//Unreserve it
			}
		}

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
