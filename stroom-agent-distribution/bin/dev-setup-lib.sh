#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. ${DIR}/common.sh

cd ${DIR}/..

OURLIB="stroom,org.springframework,com.jcraft.jsch,commons,org.apache,org.slf4j,log4j,joda-time"

rm -fr lib/*
rm -fr lib-extra/*
mvn dependency:copy-dependencies -DoutputDirectory=lib -DincludeGroupIds=${OURLIB}
mvn dependency:copy-dependencies -DoutputDirectory=lib-extra -DexcludeGroupIds=${OURLIB}

