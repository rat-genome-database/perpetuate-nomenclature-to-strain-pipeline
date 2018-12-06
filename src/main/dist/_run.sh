#!/usr/bin/env bash
# shell script to run PerpetuateNomenclature2Strain pipeline
. /etc/profile

APPNAME=perpetuateNomenclature2Strain
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/${APPNAME}.jar "$@"
