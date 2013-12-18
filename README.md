li.strolch.persistence.postgresql
=======================================================================

PostgreSQL Persistence Implementation for Strolch


Setup
=======================================================================
1. Install PostgreSQL version with at least version 9.1:
   $ sudo aptitude install postgresql postgresql-client
2. Set a password for user 'postgres'
   $ sudo -u postgres psql postgres
   postgres=# \password postgres
3. Create the user and DB:
   $ sudo su - postgres
   postgres=# create user testUser with password 'test';
   postgres=# create database testdb;
   postgres=# GRANT ALL PRIVILEGES ON DATABASE testdb to testuser;
   postgres=# GRANT CONNECT ON DATABASE testdb TO testuser ;

4. Added new component, setting properties for PostgreSQL DB:
	<Component>
		<name>PersistenceHandler</name>
		<api>li.strolch.persistence.api.StrolchPersistenceHandler</api>
		<impl>li.strolch.persistence.postgresql.PostgreSqlPersistenceHandler</impl>
		<Properties>
			<allowSchemaCreation>false</allowSchemaCreation>
			<db.url>jdbc:postgresql://localhost/testdb</db.url>
			<db.username>testUser</db.username>
			<db.password>test</db.password>
		</Properties>
	</Component>

5. Create tables, or allow strolch to due it for you.


Appendix
=======================================================================
1. To drop the user and DB:
   postgres=# revoke ALL PRIVILEGES ON DATABASE testdb from testuser;
   postgres=# drop user testuser;
   postgres=# drop database testdb;
2. Create a database:
  $ createdb -p 5432 -O drupal -U drupal -E UTF8 testingsiteone -T template0
3. Dropping the database
  $ dropdb -p 5432 -U drupal testingsiteone
4. Dumping the database
  $ pg_dump -p 5432 -h localhost -Fc -U drupal --no-owner testingsiteone > /tmp/testingsiteone_$(date +"%Y-%m-%d_%s").pgdump
5. Restoring the database
  $ pg_restore -p 5432 -h localhost -Fc -d testingsiteone -U drupal --no-owner < /tmp/path-to-the-file.pgdump

References
=======================================================================
http://www.pixelite.co.nz/article/installing-and-configuring-postgresql-91-ubuntu-1204-local-drupal-development