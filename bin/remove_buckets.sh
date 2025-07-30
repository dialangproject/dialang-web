#!/bin/bash

echo $1

for bucket in $(aws s3 ls | awk '{print $3}' | grep $1);
do
  echo "Deleting bucket $bucket"
  aws s3 rb s3://$bucket --force
done
