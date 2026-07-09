#!/bin/bash
# ============================================================
# SecureChat — Production Deployment Script
# Run on your VPS after uploading the JAR
# ============================================================

set -e

DOMAIN="yourdomain.com"
APP_DIR="/opt/securechat"
JAR_NAME="SecureMessaging-0.0.1-SNAPSHOT.jar"

echo "=== SecureChat Deployment ==="

# 1. Install Nginx if not present
if ! command -v nginx &> /dev/null; then
    echo "Installing Nginx..."
    sudo apt-get update && sudo apt-get install -y nginx
fi

# 2. Install Certbot if not present
if ! command -v certbot &> /dev/null; then
    echo "Installing Certbot..."
    sudo apt-get install -y certbot python3-certbot-nginx
fi

# 3. Create app directory
sudo mkdir -p $APP_DIR
sudo chown $USER:$USER $APP_DIR

# 4. Copy Nginx config
sudo cp nginx/securechat.conf /etc/nginx/sites-available/securechat
sudo sed -i "s/yourdomain.com/$DOMAIN/g" /etc/nginx/sites-available/securechat
sudo ln -sf /etc/nginx/sites-available/securechat /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# 5. Obtain SSL certificate (first time only)
if [ ! -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    echo "Obtaining SSL certificate for $DOMAIN..."
    sudo certbot --nginx -d $DOMAIN -d www.$DOMAIN --non-interactive --agree-tos -m admin@$DOMAIN
fi

# 6. Set up auto-renewal cron
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && systemctl reload nginx") | crontab -

# 7. Create systemd service for Spring Boot
sudo tee /etc/systemd/system/securechat.service > /dev/null <<EOF
[Unit]
Description=SecureChat Spring Boot Application
After=network.target mysql.service

[Service]
Type=simple
User=$USER
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/java -jar $APP_DIR/$JAR_NAME --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=securechat

# Environment variables — set these before deploying
Environment="DB_USERNAME=root"
Environment="DB_PASSWORD=your_db_password"
Environment="JWT_SECRET=your_jwt_secret"
Environment="MAIL_USERNAME=your_email@gmail.com"
Environment="MAIL_PASSWORD=your_app_password"

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable securechat
sudo systemctl restart securechat

echo ""
echo "=== Deployment complete ==="
echo "App running at: https://$DOMAIN"
echo "Check status:   sudo systemctl status securechat"
echo "View logs:      sudo journalctl -u securechat -f"
