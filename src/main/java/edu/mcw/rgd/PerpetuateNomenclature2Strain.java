package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Report strains with their symbols inconsistent with gene nomenclature.
 * That means, we lookup strain nomenclature events; and if a gene symbol used to be a part of strain symbol,
 * but it is no longer, we report this case.
 * Only the strains with type mutant or transgenic are updated.
 */
public class PerpetuateNomenclature2Strain {

    private PerpetuateNomenclature2StrainDAO dao = new PerpetuateNomenclature2StrainDAO();
    private Logger logConflicts = LogManager.getLogger("conflicts");
    private Logger logUpdates = LogManager.getLogger("updates");
    private Logger logExceptions = LogManager.getLogger("exceptions");

    private String version;
    private List<String> strainSkipList;
    private List<String> processedStrainTypes;
    private String nomenclatureRefKey;

    private boolean readOnlyMode = true;
    private int updatedStrainCounter = 0;
    private int nomenclatureCounter = 0;
    private int aliasCounter = 0;
    private int conflictCount = 0;

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        PerpetuateNomenclature2Strain manager = (PerpetuateNomenclature2Strain) (bf.getBean("manager"));

        try {
            manager.run();
        } catch(Exception e) {
            Utils.printStackTrace(e, manager.logExceptions);
            throw new Exception(e);
        }
    }

    public void run() throws Exception {

        logUpdates.info(getVersion());

        Set<Integer> strainSkipSet = new HashSet<>();
        for( String strainRgdIdStr: getStrainSkipList() ) {
            strainSkipSet.add(Integer.parseInt(strainRgdIdStr));
        }

        Date date = new Date();
        Format note = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String forNotes = note.format(date);


        // load all strains to be processed
        List<Strain> strains = new ArrayList<>();
        String msg = "processed strain types: ";
        for( String strainType: getProcessedStrainTypes() ) {
            msg += strainType + " ";
            strains.addAll(dao.getStrainsByType(strainType));
        }
        logUpdates.info(msg);

        // process all strains loaded
        for (Strain strain: strains) {

            // do not process some strains explicitly excluded from processing
            if( strainSkipSet.contains(strain.getRgdId()) ) {
                continue;
            }

            // get all genes associated with given strain (through RGD_STRAINS_RGD table)
            for( Gene gene: dao.getStrainToGeneAssociations(strain.getRgdId()) ) {

                if( strainSymbolDeviatesFromGeneAlleleSymbol(strain.getSymbol(), gene.getSymbol()) ) {
                    generateEvents(gene, strain, forNotes);
                }
            }
        }
        logUpdates.info("strains processed: "+strains.size());

        if( readOnlyMode ) {
            logUpdates.info(conflictCount+" conflict count");
        } else {
            logUpdates.info(updatedStrainCounter + " gene updates, " + aliasCounter + " aliases inserted, " + nomenclatureCounter + " nomen events added");
        }
    }

    boolean strainSymbolDeviatesFromGeneAlleleSymbol(String strainSymbol, String geneSymbol) {

        // all parts of gene symbol must be present in strain symbol
        String[] parts = geneSymbol.split("<.+>");
        for( String geneSymbolPart: parts ) {
            if( !strainSymbol.contains(geneSymbolPart) ) {
                return true;
            }
        }
        return false;
    }

    /// strain nomenclature is complex and rules are changing;
    /// to avoid possibly spurious strain symbol/name changes (and associated with it nomenclature events and aliases entries)
    /// we discontinue any updates; instead we only report strains that should be reviewed for nomenclature changes
    ///
    void generateEvents(Gene gene, Strain strain, String forNotes) throws Exception {

        if( readOnlyMode && strain.getSubstrain()!=null ) {

            for( NomenclatureEvent nomen: dao.getNomenclatureEvents(gene.getRgdId())) {
                String prevGeneSymbol = nomen.getPreviousSymbol();
                if (prevGeneSymbol == null) {
                    continue;
                }
                if (!strainSymbolDeviatesFromGeneAlleleSymbol(strain.getSymbol(), prevGeneSymbol)) {
                    logConflicts.info("STRAIN RGD ID: " + strain.getRgdId());
                    logConflicts.info("STRAIN SYMBOL: " + strain.getSymbol());
                    logConflicts.info("STRAIN NAME:   " + strain.getName());
                    logConflicts.info("SUBSTRAIN NAME:" + strain.getSubstrain());
                    logConflicts.info("CURRENT GENE SYMBOL:  " + gene.getSymbol());
                    logConflicts.info("PREVIOUS GENE SYMBOL: " + prevGeneSymbol);
                    logConflicts.info("");
                    conflictCount++;
                    return;
                }
            }
        }

        // strain symbol does not contain gene symbol -- get all nomenclature events
        for( NomenclatureEvent nomen: dao.getNomenclatureEvents(gene.getRgdId())) {
            String currGeneSymbol = nomen.getSymbol();
            String prevGeneSymbol = nomen.getPreviousSymbol();
            if( prevGeneSymbol==null )
                continue;

            if( strain.getSymbol().contains(prevGeneSymbol) ) {
                if( strain.getSubstrain()!=null ) {
                    String currGeneSymbol_substrain = currGeneSymbol.replace(strain.getSubstrain(), "");
                    String prevGeneSymbol_substrain = prevGeneSymbol.replace(strain.getSubstrain(), "");
                    String newStrainSymbol = strain.getSymbol().replace(prevGeneSymbol, currGeneSymbol);
                    String newStrain = strain.getStrain().replace(prevGeneSymbol_substrain, currGeneSymbol_substrain);

                    Alias alias = new Alias();
                    alias.setRgdId(strain.getRgdId());
                    alias.setTypeName("old_strain_symbol");
                    alias.setValue(strain.getSymbol());
                    alias.setNotes("based on gene nomenclature " + forNotes);
                    dao.insertAlias(alias);
                    aliasCounter++;

                    NomenclatureEvent nomenclatureEvent = new NomenclatureEvent();
                    nomenclatureEvent.setRgdId(strain.getRgdId());
                    nomenclatureEvent.setName(strain.getName());
                    nomenclatureEvent.setSymbol(newStrainSymbol);
                    nomenclatureEvent.setRefKey(getNomenclatureRefKey());
                    nomenclatureEvent.setNomenStatusType("APPROVED");
                    nomenclatureEvent.setDesc("gene nomenclature is perpetuated to the strain symbol");
                    nomenclatureEvent.setEventDate(new Date());
                    nomenclatureEvent.setOriginalRGDId(strain.getRgdId());
                    nomenclatureEvent.setPreviousSymbol(strain.getSymbol());
                    nomenclatureEvent.setPreviousName(strain.getName());
                    dao.createNomenEvent(nomenclatureEvent);
                    nomenclatureCounter++;

                    strain.setSymbol(newStrainSymbol);
                    strain.setName(newStrain);
                    dao.updateStrain(strain);

                    logUpdates.info("STRAIN_RGD_ID:" + strain.getRgdId() + ", STRAIN_SYMBOL:" + strain.getSymbol() + "\n"
                            + "CURRENT_GENE_SYMBOL:" + currGeneSymbol + ", PREVIOUS_GENE_SYMBOL:" + prevGeneSymbol + "\n"
                            + "NEW_STRAIN_SYMBOL:" + newStrainSymbol + ", STRAIN:" + strain.getName() + ", NEW_STRAIN:" + newStrain + ", TYPE:" + strain.getStrainTypeName());
                    updatedStrainCounter++;
                    break; // no more processing of nomenclature events
                }
                else{
                    logExceptions.info("NULL SUBSTRAIN\n"
                            +" STRAIN_RGD_ID:" + strain.getRgdId() + ", STRAIN_SYMBOL:" + strain.getSymbol() +"\n"
                            + "CURRENT_GENE_SYMBOL:" + currGeneSymbol + ", PREVIOUS_GENE_SYMBOL:" + prevGeneSymbol + "\n"
                            + "STRAIN:" + strain.getName() + ", TYPE:"+ strain.getStrainTypeName());
                }
            }
        }
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setStrainSkipList(List<String> strainSkipList) {
        this.strainSkipList = strainSkipList;
    }

    public List<String> getStrainSkipList() {
        return strainSkipList;
    }

    public List<String> getProcessedStrainTypes() {
        return processedStrainTypes;
    }

    public void setProcessedStrainTypes(List<String> processedStrainTypes) {
        this.processedStrainTypes = processedStrainTypes;
    }

    public void setNomenclatureRefKey(String nomenclatureRefKey) {
        this.nomenclatureRefKey = nomenclatureRefKey;
    }

    public String getNomenclatureRefKey() {
        return nomenclatureRefKey;
    }
}

