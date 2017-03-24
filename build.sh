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

mkdir -p build/OCDP_Stream/lib
mkdir -p build/OCDP_Stream/logs
mkdir -p build/OCDP_Stream/web

cp -r bin build/OCDP_Stream
cp -r conf build/OCDP_Stream

cp core/target/ocsp-core_${spark_version}-${version}.jar build/OCDP_Stream/lib
cp core/target/ocsp-core_${spark_version}-${version}-dist/lib/*.jar build/OCDP_Stream/lib

tar -xzf web/target/web-${version}-bundle.tar.gz -C build/OCDP_Stream/web

cd build

tar -czf OCDP_Stream_${version}.tar.gz OCDP_Stream

exit 0
