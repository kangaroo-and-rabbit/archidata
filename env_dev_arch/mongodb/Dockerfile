FROM archlinux:latest

# Install build tools and runtime dependencies
RUN pacman -Syu --noconfirm base-devel git openssl curl && \
    pacman -Scc --noconfirm

# Non-root user required by makepkg
RUN useradd -m builder && \
    echo "builder ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# Install mongosh-bin first (dependency of mongodb-bin), then mongodb-bin
USER builder
RUN cd /tmp && \
    git clone https://aur.archlinux.org/mongosh-bin.git && \
    cd mongosh-bin && \
    makepkg -si --noconfirm --skippgpcheck && \
    cd .. && rm -rf mongosh-bin

RUN cd /tmp && \
    git clone https://aur.archlinux.org/mongodb-bin.git && \
    cd mongodb-bin && \
    makepkg -si --noconfirm --skippgpcheck && \
    cd .. && rm -rf mongodb-bin

USER root

# Clean up build tools
RUN pacman -Rdd --noconfirm base-devel git 2>/dev/null; \
    pacman -Scc --noconfirm; \
    userdel -r builder 2>/dev/null; \
    true

# Setup data directories
RUN mkdir -p /data/db /data/keys && \
    chmod 755 /data/db /data/keys

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 27017

ENTRYPOINT ["/entrypoint.sh"]
