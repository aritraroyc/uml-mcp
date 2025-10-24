# Deployment Guide

This guide covers deploying the UML MCP Service in various environments.

## Table of Contents

- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [Production Deployment](#production-deployment)
- [Cloud Deployments](#cloud-deployments)
- [Security Considerations](#security-considerations)
- [Monitoring and Logging](#monitoring-and-logging)

## Local Development

### Prerequisites

- Java 17+
- Maven 3.6+
- 2GB RAM minimum

### Build and Run

```bash
# Build
cd server
mvn clean package

# Run
java -jar target/uml-mcp-server-1.0.0.jar

# Run with custom configuration
java -jar target/uml-mcp-server-1.0.0.jar --host localhost --port 9000
```

### Environment Variables

```bash
# Java memory settings
export JAVA_OPTS="-Xmx512m -Xms256m"
java $JAVA_OPTS -jar server.jar
```

## Docker Deployment

### Single Container

```bash
cd server

# Build image
docker build -t uml-mcp-server:1.0.0 .

# Run container
docker run -d \
  --name uml-mcp \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx1g" \
  --restart unless-stopped \
  uml-mcp-server:1.0.0

# View logs
docker logs -f uml-mcp

# Stop container
docker stop uml-mcp
docker rm uml-mcp
```

### Docker Compose

```bash
cd server

# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

### Custom Docker Compose Configuration

Create `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  uml-mcp-server:
    environment:
      - JAVA_OPTS=-Xmx2g -Xms1g
    ports:
      - "9000:8080"
    volumes:
      - ./logs:/app/logs
```

## Production Deployment

### System Requirements

**Minimum:**
- 2 CPU cores
- 2GB RAM
- 10GB disk space

**Recommended:**
- 4 CPU cores
- 4GB RAM
- 50GB disk space (for logs)

### Reverse Proxy with Nginx

```nginx
upstream uml_mcp_backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name uml.example.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name uml.example.com;

    ssl_certificate /etc/ssl/certs/uml.example.com.crt;
    ssl_certificate_key /etc/ssl/private/uml.example.com.key;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Logging
    access_log /var/log/nginx/uml-mcp-access.log;
    error_log /var/log/nginx/uml-mcp-error.log;

    # Request size limits
    client_max_body_size 10M;

    # Timeouts
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    location / {
        proxy_pass http://uml_mcp_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # CORS headers (if needed)
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods "GET, POST, OPTIONS";
        add_header Access-Control-Allow-Headers "Content-Type";
    }

    # Health check endpoint
    location /health {
        proxy_pass http://uml_mcp_backend/health;
        access_log off;
    }
}
```

### Systemd Service

Create `/etc/systemd/system/uml-mcp.service`:

```ini
[Unit]
Description=UML MCP Service
After=network.target

[Service]
Type=simple
User=umlmcp
Group=umlmcp
WorkingDirectory=/opt/uml-mcp
ExecStart=/usr/bin/java -Xmx1g -Xms512m -jar /opt/uml-mcp/server.jar --host 0.0.0.0 --port 8080
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=uml-mcp

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/uml-mcp/logs

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
# Create user
sudo useradd -r -s /bin/false umlmcp

# Create directories
sudo mkdir -p /opt/uml-mcp/logs
sudo cp server/target/uml-mcp-server-1.0.0.jar /opt/uml-mcp/server.jar
sudo chown -R umlmcp:umlmcp /opt/uml-mcp

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable uml-mcp
sudo systemctl start uml-mcp

# Check status
sudo systemctl status uml-mcp

# View logs
sudo journalctl -u uml-mcp -f
```

## Cloud Deployments

### AWS EC2

```bash
# Launch EC2 instance (Amazon Linux 2 or Ubuntu)
# Install Java 17
sudo yum install -y java-17-amazon-corretto

# Copy JAR file
scp server.jar ec2-user@<instance-ip>:/home/ec2-user/

# SSH to instance
ssh ec2-user@<instance-ip>

# Run server
nohup java -jar server.jar &

# Or use systemd as described above
```

### AWS ECS (Docker)

Create task definition:

```json
{
  "family": "uml-mcp-service",
  "containerDefinitions": [
    {
      "name": "uml-mcp-server",
      "image": "your-registry/uml-mcp-server:1.0.0",
      "memory": 2048,
      "cpu": 1024,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "JAVA_OPTS",
          "value": "-Xmx1g -Xms512m"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/uml-mcp",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3
      }
    }
  ]
}
```

### Google Cloud Run

```bash
# Build and push image
docker build -t gcr.io/PROJECT_ID/uml-mcp-server:1.0.0 .
docker push gcr.io/PROJECT_ID/uml-mcp-server:1.0.0

# Deploy to Cloud Run
gcloud run deploy uml-mcp-service \
  --image gcr.io/PROJECT_ID/uml-mcp-server:1.0.0 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 2Gi \
  --cpu 2 \
  --port 8080
```

### Kubernetes

Create deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: uml-mcp-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: uml-mcp-server
  template:
    metadata:
      labels:
        app: uml-mcp-server
    spec:
      containers:
      - name: uml-mcp-server
        image: uml-mcp-server:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-Xmx1g -Xms512m"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: uml-mcp-service
spec:
  selector:
    app: uml-mcp-server
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## Security Considerations

### 1. Network Security

```bash
# Firewall rules (iptables)
sudo iptables -A INPUT -p tcp --dport 8080 -s 10.0.0.0/8 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP

# Or use cloud security groups
```

### 2. Rate Limiting (Nginx)

```nginx
http {
    limit_req_zone $binary_remote_addr zone=uml_mcp:10m rate=10r/s;

    server {
        location /mcp/ {
            limit_req zone=uml_mcp burst=20 nodelay;
            proxy_pass http://uml_mcp_backend;
        }
    }
}
```

### 3. Authentication

Add API key authentication at the reverse proxy level:

```nginx
location /mcp/ {
    if ($http_x_api_key != "your-secret-key") {
        return 401;
    }
    proxy_pass http://uml_mcp_backend;
}
```

### 4. HTTPS Only

Ensure all traffic uses HTTPS in production.

## Monitoring and Logging

### Application Logs

Configure log level in `application.properties`:

```properties
logging.level=INFO
```

### Health Monitoring

Set up health check monitoring:

```bash
# Cron job for health check
*/5 * * * * curl -f http://localhost:8080/health || systemctl restart uml-mcp
```

### Metrics (Prometheus)

Add Prometheus metrics endpoint (requires code modification).

### Log Aggregation

Forward logs to centralized logging:

```bash
# Using rsyslog or fluentd
# Configure to send logs to ELK stack or CloudWatch
```

## Backup and Recovery

### Backup Strategy

The service is stateless, but backup:
- Configuration files
- Custom PlantUML includes
- Deployment scripts

### Disaster Recovery

1. Maintain infrastructure as code (Terraform, CloudFormation)
2. Store Docker images in registry
3. Document deployment procedures
4. Test recovery procedures regularly

## Performance Tuning

### JVM Tuning

```bash
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
java $JAVA_OPTS -jar server.jar
```

### OS Tuning

```bash
# Increase file descriptors
ulimit -n 65536

# TCP tuning
sysctl -w net.core.somaxconn=1024
```

## Scaling

### Horizontal Scaling

Deploy multiple instances behind a load balancer:

```
Load Balancer
    ├── UML MCP Server 1
    ├── UML MCP Server 2
    └── UML MCP Server 3
```

### Caching

Add Redis/Memcached for diagram caching (requires code modification).

## Maintenance

### Update Procedure

```bash
# 1. Build new version
mvn clean package

# 2. Stop service
sudo systemctl stop uml-mcp

# 3. Backup current version
sudo cp /opt/uml-mcp/server.jar /opt/uml-mcp/server.jar.backup

# 4. Deploy new version
sudo cp target/uml-mcp-server-1.0.0.jar /opt/uml-mcp/server.jar

# 5. Start service
sudo systemctl start uml-mcp

# 6. Verify
curl http://localhost:8080/health
```

### Rolling Update (Kubernetes)

```bash
kubectl set image deployment/uml-mcp-server \
  uml-mcp-server=uml-mcp-server:1.1.0

kubectl rollout status deployment/uml-mcp-server
```

## Troubleshooting

See logs:
```bash
# Systemd
sudo journalctl -u uml-mcp -f

# Docker
docker logs -f uml-mcp

# Kubernetes
kubectl logs -f deployment/uml-mcp-server
```

Check resources:
```bash
# Memory
free -h

# CPU
top

# Disk
df -h
```
