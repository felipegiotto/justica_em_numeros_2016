#!/bin/bash


# Consulta a pasta atual
SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
cd $SCRIPTPATH


# Exclui a keystore, para gerar novamente
rm -rf keystore
mkdir keystore


# Importa certificados de homologação e produção do CNJ
CERTIFICADOS=certificados/*
for f in $CERTIFICADOS
do
  echo "Importando certificado $f..."
  keytool -import -noprompt -file $f -storepass storepasscnj -keystore keystore/cnj.keystore -alias $f 
done
