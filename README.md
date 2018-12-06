# perpetuate-nomenclature-to-strain-pipeline

- Update STRAIN_SYMBOL and STRAIN fields in the STRAINS table based on nomenclature event.
- Old symbols are inserted into ALIASES and NOMEN_EVENT tables.
- Only the strains of type 'mutant' or 'transgenic' are updated.