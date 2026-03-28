#!/bin/sh
# Runs inside LocalStack once S3 is ready.
awslocal s3 mb "s3://${S3_BUCKET:-tickettide-event-media}" || true
