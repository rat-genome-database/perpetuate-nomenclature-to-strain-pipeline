package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.AliasDAO;
import edu.mcw.rgd.dao.impl.AssociationDAO;
import edu.mcw.rgd.dao.impl.NomenclatureDAO;
import edu.mcw.rgd.dao.impl.StrainDAO;
import edu.mcw.rgd.datamodel.*;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Mar 16, 2011
 * Time: 3:48:49 PM
 */
public class PerpetuateNomenclature2StrainDAO {

    AliasDAO aliasDAO = new AliasDAO();
    AssociationDAO associationDAO = new AssociationDAO();
    StrainDAO strainDAO = associationDAO.getStrainDAO();
    NomenclatureDAO nomenDAO = new NomenclatureDAO();

    /**
     * get list of strains of given type
     * @param strainType one of strain types
     * @return List of Strain objects belonging to given type
     * @throws Exception when something really bad happens in spring framework
     */
    public List<Strain> getStrainsByType(String strainType) throws Exception {
        return strainDAO.getStrainsByType(strainType);
    }

    /**
     * Update strain in the datastore based on rgdID
     *
     * @param strain strain
     * @throws Exception when something really bad happens in spring framework
     */
    public void updateStrain(Strain strain) throws Exception{
        strainDAO.updateStrain(strain);
    }

    /**
     * return list of genes associated with given strain
     * @param strainRgdId strain rgd id
     * @return list of Gene objects -- associated with given strain
     * @throws Exception when something bad happens in spring framework
     */
    public List<Gene> getStrainToGeneAssociations(int strainRgdId) throws Exception {
        return associationDAO.getStrainAssociations(strainRgdId, RgdId.OBJECT_KEY_GENES);        
    }

    /**
     * Returns all nomenclature events based on an RGD ID ordered from the most recent ones
     *
     * @param rgdId nomenclature rgd id
     * @return list of all nomenclature events for given rgd id
     * @throws Exception when something bad happens in spring framework
     */
    public List<NomenclatureEvent> getNomenclatureEvents(int rgdId) throws Exception {
        return nomenDAO.getNomenclatureEvents(rgdId);
    }

    /**
     *  Creates a new nomenclature event in the datastore.
     * @param event NomenclatureEvent object
     * @throws Exception when something bad happens in spring framework
     */
    public void createNomenEvent(NomenclatureEvent event) throws Exception {
        nomenDAO.createNomenEvent(event);
    }

    /**
     * insert new alias
     * @param alias Alias object
     * @throws Exception when something bad happens in spring framework
     */
    public void insertAlias(Alias alias) throws Exception {
        aliasDAO.insertAlias(alias);
    }
}
