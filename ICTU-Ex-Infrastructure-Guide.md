# ICTU-Ex Infrastructure Setup Guide
**Project:** Smart Student Marketplace  
**Stack:** Kotlin + Spring Boot + Kafka + PostgreSQL + Kubernetes  
**Server:** DigitalOcean Ubuntu 24.04 (167.172.99.14)

---

## PHASE 1 — Create DigitalOcean Droplet

1. Go to cloud.digitalocean.com → Create → Droplets
2. Choose:
   - **Image:** Ubuntu 24.04 LTS
   - **Plan:** Basic → Regular SSD → $18/month (2vCPU, 2GB RAM)
   - **Region:** Frankfurt (closest to Cameroon)
   - **Authentication:** SSH Key
3. Generate SSH key on your laptop (Git Bash):
```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
cat ~/.ssh/id_ed25519.pub
```
4. Paste the public key into DigitalOcean
5. Hostname: `ictu-ex-server` → Click Create

**Connect to server:**
```bash
ssh root@167.172.99.14
```

---

## PHASE 2 — Add Swap Space (Virtual RAM)

Run on the server:
```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
free -h  # Verify swap shows 2.0Gi
```

---

## PHASE 3 — Install Ansible

```bash
apt update && apt install -y ansible
ansible --version  # Verify installation
```

---

## PHASE 4 — Ansible Playbooks

Create folder:
```bash
mkdir -p ~/ansible && cd ~/ansible
```

### Playbook 1 — Install Dependencies
```bash
nano playbook1-install.yml
```
```yaml
---
- name: Install all project dependencies
  hosts: localhost
  connection: local
  become: false

  tasks:
    - name: Update apt cache
      apt:
        update_cache: yes
      become: true

    - name: Install Java 21
      apt:
        name: openjdk-21-jdk
        state: present
      become: true

    - name: Install Docker
      apt:
        name: docker.io
        state: present
      become: true

    - name: Install Git
      apt:
        name: git
        state: present
      become: true

    - name: Install curl
      apt:
        name: curl
        state: present
      become: true

    - name: Verify Java installation
      command: java -version
      register: java_out
      ignore_errors: yes

    - name: Verify Docker installation
      command: docker --version
      register: docker_out
      ignore_errors: yes

    - name: Print versions
      debug:
        msg:
          - "Docker: {{ docker_out.stderr | default(docker_out.stdout) }}"
```

Run it:
```bash
ansible-playbook playbook1-install.yml
```

---

### Playbook 2 — Start Services
```bash
nano playbook2-services.yml
```
```yaml
---
- name: Start and enable all services
  hosts: localhost
  connection: local
  become: true

  tasks:
    - name: Start Docker service
      service:
        name: docker
        state: started
        enabled: yes

    - name: Verify Docker is running
      command: docker ps
      register: docker_ps

    - name: Print Docker status
      debug:
        msg: "{{ docker_ps.stdout }}"

    - name: Install k3s (lightweight Kubernetes)
      shell: curl -sfL https://get.k3s.io | sh -
      args:
        creates: /usr/local/bin/k3s

    - name: Wait for k3s to be ready
      wait_for:
        path: /etc/rancher/k3s/k3s.yaml
        timeout: 60

    - name: Verify k3s is running
      command: k3s kubectl get nodes
      register: k3s_out

    - name: Print k3s node status
      debug:
        msg: "{{ k3s_out.stdout }}"
```

Run it:
```bash
ansible-playbook playbook2-services.yml
```

---

### Playbook 3 — Health Check
```bash
nano playbook3-healthcheck.yml
```
```yaml
---
- name: Health check — verify all services
  hosts: localhost
  connection: local
  become: true

  tasks:
    - name: Check Docker status
      command: docker --version
      register: docker_ver

    - name: Check Java version
      command: java -version
      register: java_ver
      ignore_errors: yes

    - name: Check Git version
      command: git --version
      register: git_ver

    - name: Check k3s node status
      command: k3s kubectl get nodes
      register: k3s_nodes

    - name: Check swap is active
      command: free -h
      register: swap_status

    - name: "HEALTH REPORT"
      debug:
        msg:
          - "========================================="
          - "     ICTU-EX SERVER HEALTH REPORT        "
          - "========================================="
          - "Docker  : {{ docker_ver.stdout }}"
          - "Git     : {{ git_ver.stdout }}"
          - "Java    : {{ java_ver.stderr }}"
          - "K3s     : {{ k3s_nodes.stdout }}"
          - "Memory  : {{ swap_status.stdout_lines[1] }}"
          - "Swap    : {{ swap_status.stdout_lines[2] }}"
          - "========================================="
          - "STATUS  : ALL SYSTEMS OK ✅"
          - "========================================="
```

Run anytime to verify server health:
```bash
ansible-playbook playbook3-healthcheck.yml
```

---

## PHASE 5 — Jenkins Setup

Run Jenkins as a Docker container:
```bash
mkdir -p ~/jenkins_home && chmod 777 ~/jenkins_home

docker run -d \
  --name jenkins \
  --restart=always \
  -p 8080:8080 \
  -p 50000:50000 \
  -v ~/jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts-jdk21
```

Verify running:
```bash
docker ps
```

Get initial admin password:
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Give Jenkins access to kubectl:
```bash
docker exec jenkins mkdir -p /var/jenkins_home/.kube
docker cp /etc/rancher/k3s/k3s.yaml jenkins:/var/jenkins_home/.kube/config
docker exec jenkins sed -i 's/127.0.0.1/167.172.99.14/g' /var/jenkins_home/.kube/config
```

Install kubectl inside Jenkins:
```bash
docker exec -u root jenkins bash -c "curl -LO https://dl.k8s.io/release/v1.29.0/bin/linux/amd64/kubectl && chmod +x kubectl && mv kubectl /usr/local/bin/"
```

Verify Jenkins sees the cluster:
```bash
docker exec jenkins kubectl get nodes
```

Open Jenkins in browser: `http://167.172.99.14:8080`
- Paste the admin password
- Install suggested plugins
- Create admin user
- Install extra plugins: Docker Pipeline, Kubernetes CLI, GitHub Integration
- Add DockerHub credentials (ID must be: `dockerhub-credentials`)

---

## PHASE 6 — Clone Project & Deploy to Kubernetes

Clone the repo:
```bash
cd ~
git clone https://github.com/fanyicharllson/ictu-ex-backend.git
cd ictu-ex-backend
```

Apply Kubernetes manifests in order:
```bash
# 1. Create namespace
k3s kubectl apply -f k8s/namespace.yaml

# 2. Deploy Postgres
k3s kubectl apply -f k8s/postgres.yaml

# 3. Deploy Kafka (KRaft mode - no Zookeeper needed)
k3s kubectl apply -f k8s/kafka.yaml

# 4. Deploy Monitoring
k3s kubectl apply -f k8s/monitoring.yaml

# 5. Deploy Nginx (only after app is deployed by Jenkins)
k3s kubectl apply -f k8s/nginx.yaml
```

Check all pods running:
```bash
k3s kubectl get all -n ictu-ex
```

Open firewall ports:
```bash
ufw allow 30080/tcp  # App
ufw allow 30090/tcp  # Prometheus
ufw allow 30030/tcp  # Grafana
ufw allow 8080/tcp   # Jenkins
ufw status
```

---

## PHASE 7 — Access URLs

| Service | URL |
|---|---|
| Jenkins | http://167.172.99.14:8080 |
| App (via Nginx) | http://167.172.99.14:30080 |
| Prometheus | http://167.172.99.14:30090 |
| Grafana | http://167.172.99.14:30030 |

Grafana login: `admin / admin`

---

## Quick Server Health Check

Run anytime:
```bash
ssh root@167.172.99.14
cd ~/ansible
ansible-playbook playbook3-healthcheck.yml
k3s kubectl get pods -n ictu-ex
docker ps
```

---

## What Each Technology Does

| Technology | Role |
|---|---|
| DigitalOcean | Cloud server provider |
| Ubuntu 24.04 | Server operating system |
| Ansible | Automates server setup (Infrastructure as Code) |
| Docker | Runs Jenkins as a container |
| k3s | Lightweight Kubernetes — orchestrates all pods |
| Jenkins | CI/CD pipeline — builds, tests, deploys on every push |
| Kafka (KRaft) | Event bus between modules — no Zookeeper needed |
| PostgreSQL | Single database for all modules |
| Prometheus | Scrapes app metrics every 15 seconds |
| Grafana | Displays metrics as beautiful dashboards |
| Nginx | Reverse proxy — routes traffic to Spring Boot app |

