# Update STRAIN_SYMBOL and STRAIN fields in the STRAINS table based
# on nomenclature event. Old symbols are inserted into ALIASES and NOMEN_EVENT tables.
# Only the strains with type mutant or transgenic are updated.
#
. /etc/profile
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu,sjwang@mcw.edu
fi

WORKDIR=/home/rgddata/pipelines/perpetuateNomenclature2Strain
$WORKDIR/_run.sh 2>&1

mailx -s "[$SERVER] strain nomenclature conflicts" $EMAIL_LIST < $WORKDIR/log/conflicts.log
