package gld.algo.tlc;

/*
   HEC Interface, deze interface toggled bij een TLC die een implementatie
   heeft van hec, de hec versie aan of uit.

*/


import gld.Controller;


public interface HECinterface
{
    public void setHecAddon(boolean b, Controller c);
}
