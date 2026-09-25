set -eu

echo "Initializing L2J databases"

mysql -uroot -p"$MARIADB_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS l2jls DEFAULT CHARACTER SET utf8;
CREATE DATABASE IF NOT EXISTS l2jdb DEFAULT CHARACTER SET utf8;
CREATE USER IF NOT EXISTS 'l2j'@'%' IDENTIFIED BY 'l2jserver2019';
GRANT ALL PRIVILEGES ON l2jls.* TO 'l2j'@'%';
GRANT ALL PRIVILEGES ON l2jdb.* TO 'l2j'@'%';
FLUSH PRIVILEGES;
SQL

find /opt/l2j/deploy/login/sql -maxdepth 1 -type f -name '*.sql' | sort | while read -r sql_file; do
  echo "Importing login SQL: $(basename "$sql_file")"
  mysql -uroot -p"$MARIADB_ROOT_PASSWORD" l2jls < "$sql_file"
done

find /opt/l2j/deploy/game/sql -maxdepth 1 -type f -name '*.sql' ! -name 'l2jdb_create.sql' | sort | while read -r sql_file; do
  echo "Importing game SQL: $(basename "$sql_file")"
  mysql -uroot -p"$MARIADB_ROOT_PASSWORD" l2jdb < "$sql_file"
done

echo "L2J database initialization complete"
