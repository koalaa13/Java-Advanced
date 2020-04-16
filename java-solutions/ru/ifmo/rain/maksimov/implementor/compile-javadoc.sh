#!/bin/bash
MODULE="ru.ifmo.rain.maksimov"
ROOT="$(dirname $0)/../../../../../../.."
MY_P="${ROOT}/java-advanced-2020-solutions"
TEMP="${MY_P}/_build/my"
TEMPKG="${MY_P}/_build/kg"
KG_P="${ROOT}/java-advanced-2020"
LINK="https://docs.oracle.com/en/java/javase/11/docs/api/"
DATA="${TEMPKG}/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/"

mkdir -p "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/implementor"
mkdir -p "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/utils"
mkdir -p "${TEMPKG}"

cp -r "${MY_P}/java-solutions/ru/ifmo/rain/maksimov/implementor/." "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/implementor"
cp -r "${MY_P}/java-solutions/ru/ifmo/rain/maksimov/utils/Helper.java" "${TEMP}/${MODULE}/ru/ifmo/rain/maksimov/utils"
cp "${MY_P}/java-solutions/module-info.java" "${TEMP}/${MODULE}"
cp -r "${KG_P}/modules/." "${TEMPKG}"

javadoc -link ${LINK} \
    -private \
    -d "_javadoc" \
    -p "${KG_P}/artifacts":"${KG_P}/lib" \
    --module-source-path "${TEMP}:${TEMPKG}" --module "${MODULE}" \
    "${DATA}Impler.java" "${DATA}ImplerException.java" "${DATA}JarImpler.java"

rm -r "${MY_P}/_build"
