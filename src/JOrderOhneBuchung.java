

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.*;
import java.sql.*;  //import all the JDBC classes

/**
 *
 * @author bec
 */
public class JOrderOhneBuchung {

    public int DOID;
    public int BuchungId;
    public java.sql.Date OrderDatum;
    public int KategorieID;
    public double Cashflow;
    public int matchCount;
    public JEffektenBuchung eb;
    public double DeltaT;
    public double DeltaEUR;
    public double RelEUR;
    
    public JOrderOhneBuchung (int _DOID, int _BuchungId, java.sql.Date _OrderDatum, int _KategorieID, double _Cashflow)
    {
        DOID = _DOID;
        BuchungId = _BuchungId;
        OrderDatum = _OrderDatum;
        KategorieID = _KategorieID;
        Cashflow = _Cashflow;
        matchCount = 0;
        DeltaT = 0.0;
        DeltaEUR = 0.0;
        RelEUR = 0.0;
    }
    
    private boolean findMatchingBuchung (JEffektenBuchung eb)
    {
        if (KategorieID != eb.KategorieID3) return false;
        
        long tOOB = OrderDatum.getTime();
        long tEB = eb.BuchungsDatum.getTime();
        double dT = java.lang.Math.abs((tEB - tOOB) / 1000);  // seconds
        dT = dT / 86400; // days
        if (dT > 5.5 ) return false;
        
        double dEUR = java.lang.Math.abs (eb.BetragEUR - Cashflow);
        double relEUR = dEUR / java.lang.Math.abs (eb.BetragEUR);
        if ((relEUR > 0.045) && (dEUR > 15.0)) return false;

        DeltaT = dT;
        DeltaEUR = dEUR;
        RelEUR = relEUR;
        return true;
    }
    
    public void findMatchingBuchung (TreeMap EffektenBuchung)
    {
        Collection c = EffektenBuchung.entrySet();
        Iterator i = c.iterator();
        JEffektenBuchung matchingEb;

        while (i.hasNext()) {
            Map.Entry cur = (Map.Entry) i.next();
            JEffektenBuchung ebCandidate = (JEffektenBuchung) cur.getValue();
            
            if (findMatchingBuchung(ebCandidate)) {
                eb = ebCandidate;
                eb.oob = this;
                matchCount ++;
                eb.matchCount ++;
            }
        }
    }
    
    public boolean matched ()
    {
        return (matchCount == 1) && (eb.matchCount == 1);
    }

}
