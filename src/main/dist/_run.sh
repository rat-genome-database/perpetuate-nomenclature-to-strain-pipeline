#!/usr/bin/env bash
# shell script to run PerpetuateNomenclature2Strain pipeline
. /etc/profile

APPNAME=perpetuateNomenclature2Strain
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export PERPETUATE_NOMENCLATURE2_STRAIN_OPTS="$DB_OPTS $LOG4J_OPTS"

bin/$APPNAME "$@"
