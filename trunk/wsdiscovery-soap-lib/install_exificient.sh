# Exificient does not have a Maven project yet, so we have to install it manually before compiling.

mvn install:install-file -DgroupId=com.siemens.ct.exi -DartifactId=exificient -Dversion=0.7 -Dpackaging=jar -Dfile=lib/exificient/exificient.jar 
mvn install:install-file -DgroupId=xml-apis -DartifactId=xml-apis -Dversion=apis-exificient -Dpackaging=jar -Dfile=lib/exificient/xml-apis.jar
mvn install:install-file -DgroupId=xerces -DartifactId=xercesImpl -Dversion=xerces-exificient -Dpackaging=jar -Dfile=lib/exificient/xercesImpl.jar
