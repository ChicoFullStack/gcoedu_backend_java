#!/bin/bash

# Configurações do MinIO
# Lê as variáveis de ambiente ou usa os valores padrões
MINIO_URL=${MINIO_SERVER_URL:-"http://backend-minio-5b3619-179-197-232-2.sslip.io"}
MINIO_USER=${MINIO_ROOT_USER:-"minioadmin"}
MINIO_PASS=${MINIO_ROOT_PASSWORD:-"23rhdehny5hppnmr"}

echo "====================================================="
echo "Criando buckets no MinIO em:"
echo "URL: $MINIO_URL"
echo "====================================================="

# Utiliza a imagem oficial do MinIO Client (mc) via Docker
docker run -it --rm minio/mc sh -c "
  # 1. Configura a conexão com o seu MinIO
  mc alias set myminio $MINIO_URL $MINIO_USER $MINIO_PASS
  
  # 2. Cria todos os buckets solicitados (|| true ignora erro se já existir)
  echo 'Criando buckets...'
  mc mb myminio/gcoedu || true
  mc mb myminio/answer-sheets || true
  mc mb myminio/physical-tests || true
  mc mb myminio/municipality-logos || true
  mc mb myminio/school-logos || true
  mc mb myminio/question-images || true
  mc mb myminio/user-uploads || true
  
  echo '====================================================='
  echo 'Buckets atuais no servidor:'
  mc ls myminio
"
