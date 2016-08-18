#!/usr/bin/env bash
extendJars="../ShaanxiyidongFeature/target/ShaanxiyidongFeature-1.0-SNAPSHOT.jar"
#mysqlJar="../mysql-connector-java-5.1.34.jar"
0.

rm -f OCDP_Stream.tar.gz

mvn clean package;

mkdir OCDP_Stream;
cd OCDP_Stream;
cp -r ../bin .;
cp -r . ./conf .;

mkdir lib;
cp ../core/target/core-1.0-SNAPSHOT.jar lib;
cp ${extendJars} lib;
cp ${mysqlJar} lib;

mkdir logs;

cd ..;
tar zcvf OCDP_Stream.tar.gz OCDP_Stream;
rm -r OCDP_Stream;

