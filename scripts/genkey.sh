#!/bin/sh
rm -f compstore
keytool -genkey -alias signFiles -keystore compstore -keypass kpi555 -dname "cn=sheltermanager.com" -storepass ab123d
