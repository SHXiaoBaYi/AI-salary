#!/bin/bash
# 在 ECS 上签发 Let's Encrypt IP 证书，并让 80 永久跳到 443。
# 由本机上传后执行一次。证书约 6 天有效，acme.sh 每 3 天续期。
set -euo pipefail

IP="47.99.95.38"
WEBROOT="/var/www/letsencrypt"
SSL_DIR="/etc/nginx/ssl/ip_cert"
CONF_SRC="${1:-/tmp/xby-admin.https.conf}"
ACME="/root/.acme.sh/acme.sh"

mkdir -p "$WEBROOT/.well-known/acme-challenge" "$SSL_DIR"

python3 - <<'PY'
from pathlib import Path
p = Path("/etc/nginx/conf.d/xby-admin.conf")
text = p.read_text()
if "acme-challenge" not in text:
    snippet = """
    location ^~ /.well-known/acme-challenge/ {
        default_type text/plain;
        root /var/www/letsencrypt;
    }
"""
    text = text.replace("server_name _;", "server_name _;\n" + snippet, 1)
    p.write_text(text)
    print("ACME_LOCATION_ADDED")
else:
    print("ACME_LOCATION_EXISTS")
PY

nginx -t
nginx -s reload

if [[ ! -x "$ACME" ]]; then
  curl -fsSL --retry 3 --max-time 60 \
    -o /tmp/acme.sh.tar.gz \
    https://github.com/acmesh-official/acme.sh/archive/refs/heads/master.tar.gz
  rm -rf /tmp/acme-src
  mkdir -p /tmp/acme-src
  tar -xzf /tmp/acme.sh.tar.gz -C /tmp/acme-src
  (
    cd /tmp/acme-src/acme.sh-master
    ./acme.sh --install -m xby-admin@aliyun.com
  )
fi

"$ACME" --set-default-ca --server letsencrypt

set +e
"$ACME" --issue --server letsencrypt \
  -d "$IP" \
  -w "$WEBROOT" \
  --certificate-profile shortlived \
  --days 3 \
  --keylength ec-256
ISSUE_RC=$?
set -e
if [[ "$ISSUE_RC" -ne 0 ]]; then
  echo "ISSUE_EXIT=$ISSUE_RC （已有未过期证书时 acme.sh 会返回非 0，继续安装）"
fi

"$ACME" --install-cert -d "$IP" --ecc \
  --key-file "$SSL_DIR/privkey.pem" \
  --fullchain-file "$SSL_DIR/fullchain.pem" \
  --reloadcmd "systemctl reload nginx"

if [[ ! -s "$SSL_DIR/fullchain.pem" || ! -s "$SSL_DIR/privkey.pem" ]]; then
  echo "证书文件没有写到 $SSL_DIR" >&2
  exit 1
fi

sed -i 's/\r$//' "$CONF_SRC"
cp "$CONF_SRC" /etc/nginx/conf.d/xby-admin.conf
nginx -t
systemctl reload nginx

# 公网附件地址改成 https，且跟当前站点的 /api 前缀一致
python3 - <<'PY'
from pathlib import Path
p = Path("/opt/xby-admin/config/application.yml")
if not p.exists():
    print("NO_OVERRIDE")
    raise SystemExit(0)
text = p.read_text()
old = "upload-public-base: http://47.99.95.38/shxby/api"
new = "upload-public-base: https://47.99.95.38/api"
if old in text:
    text = text.replace(old, new)
    p.write_text(text)
    print("OVERRIDE_UPDATED")
elif "upload-public-base: https://47.99.95.38/api" in text:
    print("OVERRIDE_ALREADY_HTTPS")
else:
    print("OVERRIDE_UNCHANGED")
PY

openssl x509 -in "$SSL_DIR/fullchain.pem" -noout -subject -ext subjectAltName -dates
echo "HTTPS_ENABLED"
