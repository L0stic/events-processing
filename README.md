# chain-events-processing
---
Tags: #java, #springboot, #reactor, #postgreSQL

Project uses: JDK17, Remote/Local PostgreSQL server.

### Requirements

- Available ports to external user: 5002
- Installed: gradle and jdk17 for development
- Installed: docker and docker-compose Windows / Mac / Linux for deploy

### Configuration for project infrastructure start in Docker:
Copy .env.example to .env file and then configure it to your needs

`cp .env.example .env`

Then create and run docker image
```
# Open project directory
$ cd ./chain-events-processing
# Build image
$ docker-compose -p rivada-sattelite -f docker/docker-compose.yml build
# Run docker image
$ docker-compose -p rivada-sattelite -f docker/docker-compose.yml up -d
```

### Configuration for project start in IDE:

1. Use external (dev) PostgreSQL server or local server.
2. Use src/main/resources/application.yml for configure connection to DB PostgreSQL.

### Configuration for project start in Docker:
Docker  app image contain in `docker` folder

---
#### REST API methods could be retrieved with Swagger
`http://localhost:5002/gui/`
