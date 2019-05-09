# perpetuate-nomenclature-to-strain-pipeline

- Only the strains of type 'mutant' or 'transgenic' with a substrain are processed.
- Report strains with their symbols inconsistent with gene nomenclature.
- That means, we lookup strain nomenclature events; and if a gene symbol used to be a part of strain symbol,
  but it is no longer, we report this case.

In May 2019 we discontinued the update logic:

 - Update STRAIN_SYMBOL and STRAIN fields in the STRAINS table based on nomenclature event.
 - Old symbols are inserted into ALIASES and NOMEN_EVENT tables.
