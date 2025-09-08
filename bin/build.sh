#!/bin/bash

docker build -t adrianfish/dialang-web .
docker compose down -v
docker compose up -d
