package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the AipGff dataset via GFF files.
 */

public class AipGffGFF3RecordHandler extends GFF3RecordHandler
{

    /**
     * Create a new AipGffGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public AipGffGFF3RecordHandler (Model model) {
        super(model);
        refsAndCollections.put("MRNA", "gene");
        refsAndCollections.put("Exon", "transcripts");
        refsAndCollections.put("FivePrimeUTR", "mRNAs");
        refsAndCollections.put("ThreePrimeUTR", "mRNAs");
        refsAndCollections.put("TransposonFragment", "transposableelements");
        refsAndCollections.put("PseudogenicExon","pseudogenictranscripts");
        refsAndCollections.put("PseudogenicTranscript","pseudogene");
    }

    /**
     * {@inheritDoc}
     */
    @Override
        public void process(GFF3Record record) {
            // This method is called for every line of GFF3 file(s) being read.  Features and their
            // locations are already created but not stored so you can make changes here.  Attributes
            // are from the last column of the file are available in a map with the attribute name as
            // the key.
            Item feature = getFeature();
            String clsName = feature.getClassName();

            // For the following feature classes, check if the GFF3 attribute `full_name`, is defined
            // If true, assign its value to the InterMine attribute `name`
            String regexp = "Gene|MRNA|TransposableElement|TransposableElementGene";
            Pattern p = Pattern.compile(regexp);
            Matcher m = p.matcher(clsName);
            if(m.find()) {
                if(record.getAttributes().get("Name") != null){
                    String name = record.getAttributes().get("Name").iterator().next();
                    System.out.println(name);
                    feature.setAttribute("name", name);
                }
                if(record.getAttributes().get("symbol_ncbi") != null) {
                    String symbol = record.getAttributes().get("symbol_ncbi").iterator().next();
                    feature.setAttribute("symbol", symbol);
                }
            }

            // For the following feature classes, check if the Dbxref(s) to the TAIR
            // `locus` and `gene` are defined. If true, assign their values to the
            // InterMine attribute `secondaryIdentifier`
            regexp = "Gene|MRNA|Pseudogene|TransposableElementGene";
            p = Pattern.compile(regexp);
            m = p.matcher(clsName);
            if(m.find()) {
                List<String> dbxrefs = record.getDbxrefs();
                if (dbxrefs != null) {
                    Iterator<String> dbxrefsIter = dbxrefs.iterator();

                    while (dbxrefsIter.hasNext()) {
                        String dbxref = dbxrefsIter.next();

                        List<String> refList = new ArrayList<String>(
                                Arrays.asList(StringUtil.split(dbxref, ",")));
                        for (String ref : refList) {
                            ref = ref.trim();
                            int colonIndex = ref.indexOf(":");
                            if (colonIndex == -1) {
                                throw new RuntimeException("external reference not understood: " + ref);
                            }

                            if (ref.startsWith("NCBI_Gene") || ref.startsWith("locus:")) {
                                feature.setAttribute("secondaryIdentifier", ref.replace("NCBI_Gene:", ""));
                            } 
                            else if (ref.startsWith("RefSeq_NA:") || ref.startsWith("locus:")) {
                                feature.setAttribute("name", ref.replace("RefSeq_NA:", ""));
                            } 
                            else if (ref.startsWith("RefSeq_Prot:") || ref.startsWith("locus:")) {
                                feature.setAttribute("proteinProduct", ref.replace("RefSeq_Prot:", ""));
                                //feature.setAttribute("primaryIdentifier", ref.replace("RefSeq_Prot:", ""));
                            } 
                            
                            // else {
                            //     throw new RuntimeException("unknown external reference type: " + ref);
                            // }
                        }
                    }
                }
            }

            // For the following feature classes, check if the GFF3 attribute `Name` is defined.
            // If true, assign its value to the InterMine `symbol` attribute
            regexp = "Exon|CDS|UTR|Fragment";
            p = Pattern.compile(regexp);
            m = p.matcher(clsName);
            if(m.find()) {
                if(record.getAttributes().get("Name") != null){
                    String name = record.getAttributes().get("Name").iterator().next();
                    feature.setAttribute("name", name);
                }
                if(record.getAttributes().get("symbol_ncbi") != null){
                    String symbol = record.getAttributes().get("symbol_ncbi").iterator().next();
                    feature.setAttribute("symbol", symbol);
                }                
            }
        }
}
