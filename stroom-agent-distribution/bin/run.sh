#!/bin/bash

export BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. ${BIN_DIR}/common.sh


unset CONFIGFILE
unset LOCKFILE
unset CLASSPATH

for i in "$@"
do
  case $i in
    --configfile=*)
      CONFIGFILE="${i#*=}"
      shift
      ;;
    --lockfile=*)
      LOCKFILE="${i#*=}"
      shift
      ;;
    --classpath=*)
      CLASSPATH="${i#*=}"
      shift
      ;;
      
    *)
    # Unknown Option
    ;;
  esac
done

checkSet() {
        local v="${1}"
        [[ ! ${!v} && ${!v-unset} ]] && {
            echo "usage : --configfile=<config file to use> "
            echo "        --lockfile=<lock file to use> "
            echo "$2"
            exit 1
        }
}

checkSet CONFIGFILE "--configfile= not set"

if [ ! -f "${CONFIGFILE}" ]; then
  echo "No config file ${CONFIGFILE}"
  exit 1
fi

# Do JVM Stuff
JVM_ARGS="configFile=${CONFIGFILE}"
if [ ! -z "${LOCKFILE}" ]; then
   JVM_ARGS="${JVM_ARGS} lockFile=${LOCKFILE}"
fi

# Add any patch jars
for f in `ls ${INSTALL_DIR}/lib/*.jar` ;
do
        CLASSPATH=${CLASSPATH}:$f
done
for f in `ls ${INSTALL_DIR}/lib-extra/*.jar` ;
do
        CLASSPATH=${CLASSPATH}:$f
done

# Add conf on at the end
CLASSPATH=${CLASSPATH}:${INSTALL_DIR}/conf

#echo java -cp ${CLASSPATH} ${JAVA_OPTS} stroom.agent.main.StroomAgent ${JVM_ARGS}
java -cp ${CLASSPATH} ${JAVA_OPTS} stroom.agent.main.StroomAgent ${JVM_ARGS}
