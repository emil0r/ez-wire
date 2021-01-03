#!/bin/bash

SHA=$(git rev-parse HEAD)

rm -rf demo/resources/js/prod

lein cljsbuild once demo-prod

rm -rf tmp

git clone git@github.com:emil0r/ez-wire-docs.git tmp

cp demo/resources/index.html tmp/index.html
cp -R demo/resources/img tmp/
cp -R demo/resources/css tmp/
mkdir -p tmp/js
cp demo/resources/js/highlight.pack.js tmp/js/
cp demo/resources/js/prod/app.js tmp/js/

cd tmp

git checkout -- README.md
git add .
git commit -m "Built site from $SHA"
git push

cd ..

rm -rf tmp
