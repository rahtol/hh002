/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author bec
 */
public class JEffektenBuchung {

    public java.sql.Date BuchungsDatum;
    public int BuchungID;
    public int KategorieID3;
    public String Kategorie3;
    public double BetragEUR;
    public int matchCount;
    public JOrderOhneBuchung oob;
    
    public JEffektenBuchung (java.sql.Date _BuchungsDatum, int _BuchungID, int _KategorieID3, String _Kategorie3, double _BetragEUR)
    {
        BuchungsDatum = _BuchungsDatum;
        BuchungID = _BuchungID;
        KategorieID3 = _KategorieID3;
        Kategorie3 = _Kategorie3;
        BetragEUR = _BetragEUR;
        matchCount = 0;
    }
}
