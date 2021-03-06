#!/bin/bash
#include db general functions
pushd $(dirname ${0})>/dev/null
source ./dbfunctions.sh
source ./dbcustomfunctions.sh

#setting defaults
set_defaults

usage() {
    printf "Usage: ${ME} -t TYPE [-h] [-s SERVERNAME [-p PORT]] [-d DATABASE] [-u USERNAME] [-l LOGFILE] [-r] [-q] [-v] ENTITIES\n"
    printf "\n"
    printf "\t-t TYPE       - The object type {vm | template | disk | snapshot} \n"
    printf "\t-s SERVERNAME - The database servername for the database  (def. ${SERVERNAME})\n"
    printf "\t-p PORT       - The database port for the database        (def. ${PORT})\n"
    printf "\t-d DATABASE   - The database name                         (def. ${DATABASE})\n"
    printf "\t-u USERNAME   - The username for the database             (def. engine)\n"
    printf "\t-l LOGFILE    - The logfile for capturing output          (def. ${LOGFILE})\n"
    printf "\t-r            - Recursive, unlocks all disks under the selected vm/template.\n"
    printf "\t-q            - Query db and display a list of the locked entites.\n"
    printf "\t-v            - Turn on verbosity                         (WARNING: lots of output)\n"
    printf "\t-h            - This help text.\n"
    printf "\tENTITIES      - The list of object names in case of vm/template, UUIDs in case of a disk \n"
    printf "\n"

    popd>/dev/null
    exit $ret
}

DEBUG () {
    if $VERBOSE; then
        printf "DEBUG: $*"
    fi
}

caution() {
    if [ ! -n "${QUERY}" ]; then
        echo "Caution, this operation may lead to data corruption and should be used with care. Please contact support prior to running this command"
        echo "Are you sure you want to proceed? [y/n]"
        read answer

        if [ "${answer}" = "n" ]; then
           echo "Please contact support for further assistance."
           popd>/dev/null
           exit 1
        fi
    fi
}

while getopts :ht:s:d:u:p:l:f:qrv option; do
    case $option in
        t) TYPE=$OPTARG;;
        s) SERVERNAME=$OPTARG;;
        p) PORT=$OPTARG;;
        d) DATABASE=$OPTARG;;
        u) USERNAME=$OPTARG;;
        l) LOGFILE=$OPTARG;;
        r) RECURSIVE=true;;
        q) QUERY=true;;
        v) VERBOSE=true;;
        h) ret=0 && usage;;
       \?) ret=1 && usage;;
    esac
done

shift $(( OPTIND - 1 ))
IDS="$@"


if [[ -n "${TYPE}" && -n "${IDS}" ]]; then
    caution
    for ID in ${IDS} ; do
        unlock_entity "${TYPE}" "${ID}" "$(whoami)" ${RECURSIVE}
    done
elif [[ -n "${TYPE}" && -n "${QUERY}" ]]; then
    query_locked_entities "${TYPE}"
else
    usage
fi

popd>/dev/null
exit $?
