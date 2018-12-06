# Update STRAIN_SYMBOL and STRAIN fields in the STRAINS table based
# on nomenclature event. Old symbols are inserted into ALIASES and NOMEN_EVENT tables.
# Only the strains with type mutant or transgenic are updated.
#
. /etc/profile
WORKDIR=/home/rgddata/pipelines/perpetuateNomenclature2Strain
$WORKDIR/_run.sh 2>&1
