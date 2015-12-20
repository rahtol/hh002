/*
 * Connects to ODBC database link "Haushalt-Becke", which is asumed to be a WISO-MeinGeld-Database. 
 * 
 * Important: Must run under 32bit JRE because Access2007 is also 32 bit application and ODBC driver is 32 Bit!
 * Important: Use 32bit version of ODBC administration tool, i.e. "c:\Windows\SysWOW64\odbcad32.exe".
 * Important: JDBC-ODBC driver has been removed in Java SE8 -> Java SE7 must be used.
 * 
 * Matches Orders without Buchung (Query: BEC_OrderOhneBuchung) against
 * Effekten-Buchungen (Query: BEC_EffektenBuchung).
 * 
 * Match-criteria:
 *   1) exact match: the traded paper recognised by KategorieID
 *   2) limitting the timespan: the date of the trade in DepotOrder vs Buchung
 *   3) limiting criteria on the difference: the amount of EUR moved in DepotOrder vs Buchung
 * The details (exact limitting values) are documnented in JOrderOhneBuchung.findMatchingBuchung.
 * 
 * The matching is documented in a CSV-File, using currently the hard-coded filename "c:\\work\\matchd.txt"
 * 
 * If "--write" is given as argv-parameter column "DepotOrder.BuchungID" is updated.
 * Use this as a second pass after verifying the result of first pass as documented in CSV output.
 * 
 */



import java.io.*;
import java.util.*;
import java.sql.*;  //import all the JDBC classes

/**
 *
 * @author bec
 */
public class Main {

    private static TreeMap OrderOhneBuchung;
    private static TreeMap EffektenBuchung;
    
    private static void readOrderOhneBuchung (Statement stmt) throws SQLException
    {
        int count = 0;
        OrderOhneBuchung = new TreeMap();
        ResultSet result = stmt.executeQuery ("SELECT * FROM BEC_OrderOhneBuchung");
        
        while(result.next()) {
            
            int DOID = result.getInt("DOID");
            int BuchungId = result.getInt("BuchungId");
            java.sql.Date OrderDatum = result.getDate("OrderDatum");
            int KategorieID = result.getInt("KategorieID");
            double Cashflow = result.getDouble("Cashflow");
            
            JOrderOhneBuchung OOB = new JOrderOhneBuchung (DOID, BuchungId, OrderDatum, KategorieID, Cashflow);
            OrderOhneBuchung.put(DOID, OOB);
            
            count++;
        } 

        System.out.println("OrderOhneBuchung: " + count);
    };
    
    private static void readEffektenBuchung (Statement stmt) throws SQLException
    {
        int count = 0;
        EffektenBuchung = new TreeMap();
        ResultSet result = stmt.executeQuery ("SELECT * FROM BEC_EffektenBuchung");
        
        while(result.next()) {
            
            java.sql.Date BuchungsDatum = result.getDate("BuchungsDatum");
            int BuchungID = result.getInt("BuchungID");
            int KategorieID3 = result.getInt("KategorieID3");
            String Kategorie3 = result.getString("Kategorie3");
            double BetragEUR = result.getDouble("BetragEUR");
            
            JEffektenBuchung EB = new JEffektenBuchung (BuchungsDatum, BuchungID, KategorieID3, Kategorie3, BetragEUR);
            EffektenBuchung.put(BuchungID, EB);
            
            count++;
        }
        
        System.out.println("EffektenBuchung: " + count);
    };
    
    
    private static void matchOrderWithBuchung ()
    {
        Collection c = OrderOhneBuchung.entrySet();
        Iterator i = c.iterator();

        while (i.hasNext()) {
            Map.Entry cur = (Map.Entry) i.next();
            JOrderOhneBuchung oob = (JOrderOhneBuchung) cur.getValue();
            oob.findMatchingBuchung(EffektenBuchung);
        }
    }
    
    private static void writeMatchedOrders (String fname) throws IOException
    {
        PrintWriter outf = new PrintWriter (new BufferedWriter (new FileWriter (fname)));
        
        Collection c = OrderOhneBuchung.entrySet();
        Iterator i = c.iterator();
        int matchCount = 0;

        while (i.hasNext()) {
            Map.Entry cur = (Map.Entry) i.next();
            JOrderOhneBuchung oob = (JOrderOhneBuchung) cur.getValue();
            
            if (oob.matched ()) {
                matchCount++;
                outf.printf(Locale.GERMAN, "%d;%d;%g;%g;%g;%s;%s;%g;%g;%s\n", 
                        oob.DOID, oob.eb.BuchungID, 
                        oob.DeltaT, oob.DeltaEUR, oob.RelEUR, 
                        oob.OrderDatum.toString(), oob.eb.BuchungsDatum.toString(), 
                        oob.Cashflow, oob.eb.BetragEUR, 
                        oob.eb.Kategorie3);
            }
            else {
                System.out.printf("no match DOID=%d\n", oob.DOID);
            }
        }

        outf.close();
        System.out.println("MatchCount: " + matchCount);
   }
    
    private static void writeDepotOrder (Statement stmt) throws SQLException
    {
        System.out.println("writeDepotOrder due to --write !!!");
        
        Collection c = OrderOhneBuchung.entrySet();
        Iterator i = c.iterator();

        while (i.hasNext()) {
            Map.Entry cur = (Map.Entry) i.next();
            JOrderOhneBuchung oob = (JOrderOhneBuchung) cur.getValue();
            
            if (oob.matched ()) {
                stmt.execute("UPDATE DepotOrder SET DepotOrder.BuchungID=" + oob.eb.BuchungID + " WHERE DOID=" + oob.DOID + ";");
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        String URL = "jdbc:odbc:Haushalt-Becke";
        String username = "Administrator";
        String password = "0B1u2H3l4";

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (Exception e) {
            System.out.println("Failed to load JDBC/ODBC driver.");
            return;
        }

        Statement stmt = null;
        Connection con=null;
        try {
            con = DriverManager.getConnection (
                URL,
                username,
                password);
            stmt = con.createStatement();
        } catch (Exception e) {
            System.err.println("problems connecting to "+URL);
        }
        
        try {
            readOrderOhneBuchung (stmt);
            readEffektenBuchung (stmt);
            matchOrderWithBuchung ();
            writeMatchedOrders ("c:\\work\\matched.txt");
            
            if ((args.length == 1) && (args [0].equals ("--write"))) {
                writeDepotOrder (stmt);    
            }
            
            stmt.close();
            con.close ();
            
        } catch (Exception e) {
            System.err.println("problems.");
        }
        

    }

}
