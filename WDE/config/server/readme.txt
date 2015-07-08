The server.xml needs to be deployed at the container level.  For instance CATILINA_HOME/conf/server.xml.

If overwriting the xml file is undesirable, then these two nodes will have to be copied over.

	<GlobalNamingResources>
		...
		<Resource auth="Container" driverClassName="org.postgresql.Driver"
			maxActive="20" maxIdle="10" maxWait="-1" name="jdbc/wxde"
			password="d0t_dev$" type="javax.sql.DataSource" url="jdbc:postgresql://localhost:5432/wxde"
			username="wxde" />
		...
	</GlobalNamingResources>
	
	<Engine defaultHost="localhost" name="Catalina">
		...
		<Realm className="org.apache.catalina.realm.DataSourceRealm"
			dataSourceName="jdbc/wxde" roleNameCol="user_role" userCredCol="user_password"
			userNameCol="user_name" userRoleTable="auth.user_role" userTable="auth.user_table" />
		...
	</Engine>
	
	
The first node is to set up a datasource at the container level.  The second node is create the realm and identify the tables and columns to be used.