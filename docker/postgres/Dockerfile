FROM postgres:17-trixie
ARG AGE_VERSION=PG17/v1.6.0-rc0
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates wget build-essential postgresql-server-dev-17 libreadline-dev zlib1g-dev flex bison \
    && wget -q https://packages.groonga.org/debian/groonga-apt-source-latest-trixie.deb -O /tmp/groonga.deb \
    && apt-get install -y /tmp/groonga.deb \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-17-pgdg-pgroonga \
    && wget -q "https://github.com/apache/age/archive/refs/tags/${AGE_VERSION}.tar.gz" -O /tmp/age.tar.gz \
    && mkdir /tmp/age \
    && tar -xzf /tmp/age.tar.gz -C /tmp/age --strip-components=1 \
    && make -C /tmp/age PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config \
    && make -C /tmp/age PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config install \
    && rm -rf /tmp/age /tmp/age.tar.gz /tmp/groonga.deb /var/lib/apt/lists/*
