//-----------------------------------------------------------------------------
// $RCSfile: QuickSort.java,v $
// $Revision: 1.1 $
// $Author: cvs $
// $Date: 2006/12/28 17:17:09 $
//-----------------------------------------------------------------------------


package gld.utils;

import java.util.*;

///////////////////////////////////////////////////////////////////////////

/**
 * Quick sort implementation that will sort an array or Vector of Comparable objects.
 * @see org.relayirc.util.Comparable
 * @see java.util.Vector
 */
 
 /*POMDPGLD*/
public class QuickSort
{

    //------------------------------------------------------------------
    private static void swap(Vector v, int i, int j)
    {
        Object tmp = v.elementAt(i);
        v.setElementAt(v.elementAt(j), i);
        v.setElementAt(tmp, j);
    }

    //------------------------------------------------------------------
    private static void swap(Object[] arr, int i, int j)
    {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    //------------------------------------------------------------------
    /*
     * quicksort a vector of objects.
     *
     * @param v - a vector of objects
     * @param left - the start index - from where to begin sorting
     * @param right - the last index.
     */
    private static void quicksort(Vector v, int left, int right, boolean ascending)
    {

        int i, last;

        if(left >= right)
        { // do nothing if array size < 2
            return;
        }
        swap(v, left, (left + right) / 2);
        last = left;
        for(i = left + 1; i <= right; i++)
        {
            Comparable ic = (Comparable)v.elementAt(i);
            Comparable icleft = (Comparable)v.elementAt(left);
            if(ascending && ic.compareTo(icleft) < 0)
            {
                swap(v, ++last, i);
            }
            else if(!ascending && ic.compareTo(icleft) > 0)
            {
                swap(v, ++last, i);
            }
        }
        swap(v, left, last);
        quicksort(v, left, last - 1, ascending);
        quicksort(v, last + 1, right, ascending);
    }

    //------------------------------------------------------------------
    /*
     * quicksort an array of objects.
     *
     * @param arr[] - an array of objects
     * @param left - the start index - from where to begin sorting
     * @param right - the last index.
     */
    private static void quicksort(Object[] arr, int left, int right, boolean ascending)
    {

        int i, last;

        if(left >= right)
        { // do nothing if array size < 2
            return;
        }
        swap(arr, left, (left + right) / 2);
        last = left;
        for(i = left + 1; i <= right; i++)
        {
            if(ascending && ((Comparable)arr[i]).compareTo(arr[left]) < 0)
            {
                swap(arr, ++last, i);
            }
            else if(!ascending && ((Comparable)arr[i]).compareTo(arr[left]) < 0)
            {
                swap(arr, ++last, i);
            }
        }
        swap(arr, left, last);
        quicksort(arr, left, last - 1, ascending);
        quicksort(arr, last + 1, right, ascending);
    }

    //------------------------------------------------------------------
    /**
     * Quicksort will rearrange elements when they are all equal. Make sure
     * at least two elements differ
     */
    public static boolean needsSorting(Vector v)
    {
        Comparable prev = null;
        Comparable curr;
        for(Enumeration e = v.elements(); e.hasMoreElements(); )
        {
            curr = (Comparable)e.nextElement();
            if(prev != null && prev.compareTo(curr) != 0)
            {
                return true;
            }

            prev = curr;
        }
        return false;
    }

    //------------------------------------------------------------------
    /*
     * Preform a sort using the specified comparitor object.
     */
    public static void quicksort(Object[] arr, boolean ascending)
    {
        quicksort(arr, 0, arr.length - 1, ascending);
    }

    //------------------------------------------------------------------
    /*
     * Preform a sort using the specified comparitor object.
     */
    public static void quicksort(Vector v, boolean ascending)
    {
        if(needsSorting(v))
        {
            quicksort(v, 0, v.size() - 1, ascending);
        }
    }
}
