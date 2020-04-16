#!/bin/bash
cd ../../java-advanced-2020
git pull
cd ../java-advanced-2020-solutions/java-solutions
mkdir tmp
cd tmp
cp -r /home/koalaa13/Desktop/github_loadings/java-advanced-2020/artifacts/. .
cp -r /home/koalaa13/Desktop/github_loadings/java-advanced-2020/lib/. .
cp -r /home/koalaa13/Desktop/github_loadings/java-advanced-2020-solutions/java-solutions/out/production/java-solutions/ru .
rm -f module-info.class
java -cp . -p . -m info.kgeorgiy.java.advanced.implementor jar-advanced ru.ifmo.rain.maksimov.implementor.JarImplementor $1
cd ..
rm -rf tmp
