#!/usr/bin/env bash
set -e

# ensure we use the LocalStack CLI wrapper
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test

# create the bucket
awslocal s3api create-bucket --bucket my-new-custom-bucket
