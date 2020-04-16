#!/bin/bash
MODULE="ru.ifmo.rain.maksimov"
ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
TEMP="${MY_P}/_build/my"
TEMPKG="${MY_P}/_build/kg"
TEMP_JAVAC="${MY_P}/_build/my_javac"
TEMPKG_JAVAC="${MY_P}/_build/kg_javac"
KG_P="${ROOT}/java-advanced-2020"
DATA="${TEMPKG}/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/"
MAN="${MY_P}/java-solutions/ru/ifmo/rain/maksimov/implementor/Manifest.txt"

mkdir -p "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/implementor"
mkdir -p "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/utils"
mkdir -p "${TEMPKG}"

cp -r "${MY_P}/java-solutions/ru/ifmo/rain/maksimov/implementor/." "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/implementor"
cp -r "${MY_P}/java-solutions/ru/ifmo/rain/maksimov/utils/Helper.java" "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/utils"
cp "${MY_P}/java-solutions/module-info.java" "${TEMP}/${MODULE}"
cp -r "${KG_P}/modules/." "${TEMPKG}"

javac -d "${TEMPKG_JAVAC}" \
    -p "${KG_P}/lib" \
    --module-source-path "${KG_P}/modules/." \
    -m info.kgeorgiy.java.advanced.implementor

javac -d "${TEMP_JAVAC}" \
      -p "${KG_P}/lib:${KG_P}/modules/." \
      --module-source-path "${TEMP}/.:${KG_P}/modules/." \
      -m ${MODULE}

jar --create -v --file _implementor.jar --manifest ${MAN} -C "${TEMP_JAVAC}/${MODULE}" .

rm -r "${MY_P}/_build"
