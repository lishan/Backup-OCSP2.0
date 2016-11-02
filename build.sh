#!/usr/bin/env bash

HOME_PATH=$(cd `dirname $0`; pwd)

PROJECT=$1

cd ${HOME_PATH}

mvn clean package

if [ $? -ne 0 ]; then
   echo "Build failed..."
   exit 1
fi


rm -fr build

mkdir -p build/OCDP_Stream/lib
mkdir -p build/OCDP_Stream/logs

cp -r bin build/OCDP_Stream
cp -r conf build/OCDP_Stream

cp core/target/core-1.0-SNAPSHOT.jar build/OCDP_Stream/lib
cp core/target/core-1.0-SNAPSHOT-dist/lib/jedis-2.6.1.jar build/OCDP_Stream/lib
cp core/target/core-1.0-SNAPSHOT-dist/lib/commons-pool2-2.0.jar build/OCDP_Stream/lib
cp core/target/core-1.0-SNAPSHOT-dist/lib/mysql-connector-java-5.1.34.jar build/OCDP_Stream/lib
cp core/target/core-1.0-SNAPSHOT-dist/lib/spark-streaming-kafka-assembly_2.10-1.6.0.jar build/OCDP_Stream/lib
cp core/target/core-1.0-SNAPSHOT-dist/lib/scala-library-2.10.4.jar build/OCDP_Stream/lib
cp conf/common.xml build/OCDP_Stream/lib
if [ -d "$PROJECT" ]; then
  cp ${PROJECT}/target/"$PROJECT"*.jar build/OCDP_Stream/lib
else
  echo "WARN: Can not find '$PROJECT' project."
fi

cd build

tar czf OCDP_Stream.tar.gz OCDP_Stream

exit 0