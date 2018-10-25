FROM postgres:10
ENV POSTGRES_USER cpic
EXPOSE 5432
COPY ./api.roles.sql /docker-entrypoint-initdb.d/create.roles.sql