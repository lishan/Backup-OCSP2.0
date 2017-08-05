#!/usr/bin/env bash

set -e

HOME_PATH=$(cd `dirname $0`; pwd)

cd ${HOME_PATH}
version=`awk '/<ocsp.version>[^<]+<\/ocsp.version>/{gsub(/<ocsp.version>|<\/ocsp.version>/,"",$1);print $1;exit;}' pom.xml`
spark_version=1.6
if [[ -z ${1} ]];then
    echo "Build based on spark 1.6"
    mvn clean package -Dmaven.test.skip=true -Pspark-1.6
else
    case ${1} in
    1.6|2.1)
        echo "Build based on spark ${1}"
        mvn clean package -Dmaven.test.skip=true -Pspark-${1};;
    *)
        echo "Invalid argument, only support to spark version 1.6 or 2.1"
        exit 2
    esac
fi

if [ $? -ne 0 ]; then
   echo "Build failed..."
   exit 1
fi


rm -fr build

mkdir -p build/OCSP/lib/native
mkdir -p build/OCSP/logs
mkdir -p build/OCSP/web

cp -r bin build/OCSP
cp -r conf build/OCSP

cp thirdparty/Jpam-1.1/net-sf-jpam build/OCSP/conf
cp thirdparty/JPam-1.1/libjpam.so build/OCSP/lib/native

cp core/target/ocsp-core_${spark_version}-${version}.jar build/OCSP/lib
cp core/target/ocsp-core_${spark_version}-${version}-dist/lib/*.jar build/OCSP/lib

if [ -z ${1} ]||[ "1.6" == "${1}" ]; then
    cp lib/*.jar build/OCSP/lib
fi


tar -xzf web/target/web-${version}-bundle.tar.gz -C build/OCSP/web

mkdir build/OCSP/web/uploads
mkdir build/OCSP/web/tmp

cd build

tar -czf OCSP_${version}.tar.gz OCSP

exit 0
