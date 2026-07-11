<p align="center" width="100%">
    <img width="24%" src="./logo.png">
</p>
<p align="center" >
  A debugging tool for testing connectivity and access to cloud services and other endpoints.
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=flat&logo=springboot" alt="Spring Boot" />
</p>

## About

**Probe** is a Spring Boot service that verifies live connectivity and access to
cloud services and networking endpoints.

The first supported target is **GCP Valkey** over **TLS + IAM auth**
(PING/SET/GET via `/api/valkey/*`).

Support for additional services and
networking endpoints is planned.

