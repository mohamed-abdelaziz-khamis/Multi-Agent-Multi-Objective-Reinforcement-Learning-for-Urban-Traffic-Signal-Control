package gld.algo.heuristic;

/*
   Statisch bibliotheek classe voor een functie die kan worden gebruikt voor een
   eventuele HEC implementatie bij een TLC, TC1TLCOpt en CBG gebruiken op dit moment deze
   classe.
   
   EJUST: Dutch to English translation using Google Translator :)
   Static class library for a function that can be used for a HEC possible implementation at a TLC, 
   TC1TLCOpt and CBG currently use this class.
*/

import gld.infra.InfraException;
import gld.infra.Node;
import gld.algo.dp.DrivingPolicy;
import gld.sim.SimModel;
import gld.infra.Drivelane;
import gld.infra.Roaduser;

public class HEC
{
     public static float getCongestion(Roaduser ru,Drivelane currentLane, Node currentNode)
       {
         DrivingPolicy dp = SimModel.getDrivingPolicy();
         Drivelane destLane;
         float percWaiting = 0; /*Percentage of Waiting road users*/

         try
         {
            destLane = dp.getDirection(ru, currentLane, currentNode);
            
            if (destLane == null)
            {  
               //Edgenode, functie returns 0 because an edgenode is never congested;
               return 1;
            }

            percWaiting = (float)destLane.getNumBlocksWaiting() / (float)destLane.getLength();

          }
         catch(InfraException e) 
         {
           System.out.println(e.getMessage());
         }
         
         return (1-percWaiting);
     }
}
